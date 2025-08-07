/*
 *  Copyright (c) 2025 AnkiDroid Contributors
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.ichi2.anki.adapters.GeneratedCardsAdapter
import com.ichi2.anki.model.GeneratedCard
import com.ichi2.anki.snackbar.BaseSnackbarBuilderProvider
import com.ichi2.anki.snackbar.SnackbarBuilder
import com.ichi2.anki.utils.GptUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Activity for generating multiple flashcards using AI (GPT)
 * Allows users to specify a topic, select number of cards to generate,
 * preview and edit the generated cards, and approve selected cards for addition to the deck
 */
class GenerateCardsActivity :
    AnkiActivity(),
    BaseSnackbarBuilderProvider {
    override val baseSnackbarBuilder: SnackbarBuilder = { }

    private lateinit var topicInput: TextInputEditText
    private lateinit var btn5Cards: MaterialButton
    private lateinit var btn10Cards: MaterialButton
    private lateinit var btn20Cards: MaterialButton
    private lateinit var btn50Cards: MaterialButton
    private lateinit var btnGenerate: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var previewSection: View
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var btnSelectAll: MaterialButton
    private lateinit var btnApproveSelected: MaterialButton

    private var selectedCardCount = 0
    private lateinit var cardsAdapter: GeneratedCardsAdapter
    private val generatedCards = mutableListOf<GeneratedCard>()

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, GenerateCardsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_cards)

        setupToolbar()
        initializeViews()
        setupListeners()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generate Cards with AI"
    }

    private fun initializeViews() {
        topicInput = findViewById(R.id.topic_input)
        btn5Cards = findViewById(R.id.btn_5_cards)
        btn10Cards = findViewById(R.id.btn_10_cards)
        btn20Cards = findViewById(R.id.btn_20_cards)
        btn50Cards = findViewById(R.id.btn_50_cards)
        btnGenerate = findViewById(R.id.btn_generate)
        progressBar = findViewById(R.id.progress_bar)
        previewSection = findViewById(R.id.preview_section)
        cardsRecyclerView = findViewById(R.id.cards_recycler_view)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnApproveSelected = findViewById(R.id.btn_approve_selected)
    }

    private fun setupListeners() {
        // Topic input listener
        topicInput.addTextChangedListener {
            updateGenerateButtonState()
        }

        // Card count selection buttons
        btn5Cards.setOnClickListener { selectCardCount(5, btn5Cards) }
        btn10Cards.setOnClickListener { selectCardCount(10, btn10Cards) }
        btn20Cards.setOnClickListener { selectCardCount(20, btn20Cards) }
        btn50Cards.setOnClickListener { selectCardCount(50, btn50Cards) }

        // Generate button
        btnGenerate.setOnClickListener { generateCards() }

        // Select all button
        btnSelectAll.setOnClickListener { toggleSelectAll() }

        // Approve selected button
        btnApproveSelected.setOnClickListener { approveSelectedCards() }
    }

    private fun setupRecyclerView() {
        cardsAdapter = GeneratedCardsAdapter(generatedCards) { updateApproveButtonState() }
        cardsRecyclerView.layoutManager = LinearLayoutManager(this)
        cardsRecyclerView.adapter = cardsAdapter
    }

    private fun selectCardCount(
        count: Int,
        selectedButton: MaterialButton,
    ) {
        selectedCardCount = count

        // Reset all button styles
        val buttons = listOf(btn5Cards, btn10Cards, btn20Cards, btn50Cards)
        buttons.forEach { button ->
            button.isSelected = false
        }

        // Highlight selected button
        selectedButton.isSelected = true

        updateGenerateButtonState()
    }

    private fun updateGenerateButtonState() {
        val hasTopicText = topicInput.text?.isNotBlank() == true
        val hasSelectedCount = selectedCardCount > 0
        btnGenerate.isEnabled = hasTopicText && hasSelectedCount
    }

    private fun generateCards() {
        val topic = topicInput.text?.toString()?.trim()
        if (topic.isNullOrBlank()) {
            Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCardCount == 0) {
            Toast.makeText(this, "Please select number of cards", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)
        btnGenerate.isEnabled = false

        GptUtils.generateMultipleFlashcards(
            topic = topic,
            count = selectedCardCount,
            onSuccess = { flashcards ->
                handleGenerationSuccess(flashcards)
            },
            onError = { error ->
                handleGenerationError(error)
            },
        )
    }

    private fun handleGenerationSuccess(flashcards: List<Pair<String, String>>) {
        Timber.d("Generated ${flashcards.size} flashcards successfully")

        generatedCards.clear()
        flashcards.forEach { (front, back) ->
            generatedCards.add(GeneratedCard(front, back, isSelected = true))
        }

        cardsAdapter.notifyDataSetChanged()

        showProgress(false)
        previewSection.visibility = View.VISIBLE
        updateApproveButtonState()
        btnGenerate.isEnabled = true

        Toast.makeText(this, "Generated ${flashcards.size} cards successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun handleGenerationError(error: String) {
        Timber.e("Failed to generate flashcards: $error")

        showProgress(false)
        btnGenerate.isEnabled = true

        Toast.makeText(this, "Failed to generate cards: $error", Toast.LENGTH_LONG).show()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun toggleSelectAll() {
        val allSelected = generatedCards.all { it.isSelected }
        val newSelectionState = !allSelected

        generatedCards.forEach { it.isSelected = newSelectionState }
        cardsAdapter.notifyDataSetChanged()

        btnSelectAll.text = if (newSelectionState) "Deselect All" else "Select All"
        updateApproveButtonState()
    }

    private fun updateApproveButtonState() {
        val selectedCount = generatedCards.count { it.isSelected }
        btnApproveSelected.isEnabled = selectedCount > 0
        btnApproveSelected.text =
            if (selectedCount > 0) {
                "Add $selectedCount Cards to Deck"
            } else {
                "Add Selected Cards to Deck"
            }

        // Update select all button text
        val allSelected = generatedCards.isNotEmpty() && generatedCards.all { it.isSelected }
        btnSelectAll.text = if (allSelected) "Deselect All" else "Select All"
    }

    private fun approveSelectedCards() {
        val selectedCards = generatedCards.filter { it.isSelected }

        if (selectedCards.isEmpty()) {
            Toast.makeText(this, "No cards selected", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)
        btnApproveSelected.isEnabled = false

        lifecycleScope.launch {
            try {
                val result =
                    withContext(Dispatchers.IO) {
                        CollectionManager.withCol {
                            // Get the basic note type (first available note type, typically "Basic")
                            val noteTypes = notetypes.all()
                            val basicNoteType =
                                noteTypes.find { it.name == "Basic" }
                                    ?: noteTypes.firstOrNull()
                                    ?: throw IllegalStateException("No note types available")

                            // Get current deck ID
                            val currentDeckId = decks.selected()

                            var successCount = 0
                            var errorCount = 0

                            // Create and add notes for each selected card
                            selectedCards.forEach { generatedCard ->
                                try {
                                    // Create a new note using the basic note type
                                    val note = newNote(basicNoteType)

                                    // Set the fields (Basic note type has "Front" and "Back" fields)
                                    note.setItem("Front", generatedCard.front)
                                    note.setItem("Back", generatedCard.back)

                                    // Add the note to the current deck
                                    addNote(note, currentDeckId)
                                    successCount++
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to add note: ${generatedCard.front}")
                                    errorCount++
                                }
                            }

                            Pair(successCount, errorCount)
                        }
                    }

                val (successCount, errorCount) = result

                showProgress(false)

                if (errorCount == 0) {
                    Toast.makeText(this@GenerateCardsActivity, "Successfully added $successCount cards to deck!", Toast.LENGTH_SHORT).show()
                    finish() // Return to previous screen
                } else {
                    Toast.makeText(this@GenerateCardsActivity, "Added $successCount cards, $errorCount failed", Toast.LENGTH_LONG).show()
                    btnApproveSelected.isEnabled = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to add cards to deck")
                showProgress(false)
                btnApproveSelected.isEnabled = true
                Toast.makeText(this@GenerateCardsActivity, "Failed to add cards: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
