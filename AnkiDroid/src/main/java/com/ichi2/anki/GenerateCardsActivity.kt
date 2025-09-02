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
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.adapters.GeneratedCardsAdapter
import com.ichi2.anki.model.GeneratedCard
import com.ichi2.anki.snackbar.BaseSnackbarBuilderProvider
import com.ichi2.anki.snackbar.SnackbarBuilder
import com.ichi2.anki.utils.GptUtils
import kotlinx.coroutines.launch
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

    private lateinit var wordsInput: TextInputEditText
    private lateinit var wordsInputSection: View
    private lateinit var topicInput: TextInputEditText
    private lateinit var topicInputSection: View
    private lateinit var cardCountSection: View
    private lateinit var btn5Cards: MaterialButton
    private lateinit var btn10Cards: MaterialButton
    private lateinit var btn20Cards: MaterialButton
    private lateinit var btnGenerate: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var previewSection: View
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var btnSelectAll: MaterialButton
    private lateinit var btnReverseAll: MaterialButton
    private lateinit var btnApproveSelected: MaterialButton

    private var selectedCardCount = 0
    private lateinit var selectedDeckName: String
    private lateinit var cardsAdapter: GeneratedCardsAdapter
    private val previewedCards = mutableListOf<GeneratedCard>()

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, GenerateCardsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_cards)

        setupToolbar()
        initializeViews()
        setupListeners()
        lifecycleScope.launch {
            selectedDeckName =
                withCol {
                    // Load the selected deck from the collection
                    decks.name(decks.selected())
                }

            // RecyclerView setup should be done after we have the deck name
            setupRecyclerView()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generate Cards with AI"
    }

    private fun initializeViews() {
        wordsInput = findViewById(R.id.words_input)
        wordsInputSection = findViewById(R.id.words_input_section)
        topicInput = findViewById(R.id.topic_input)
        topicInputSection = findViewById(R.id.topic_input_section)
        cardCountSection = findViewById(R.id.card_count_section)
        btn5Cards = findViewById(R.id.btn_5_cards)
        btn10Cards = findViewById(R.id.btn_10_cards)
        btn20Cards = findViewById(R.id.btn_20_cards)
        btnGenerate = findViewById(R.id.btn_generate)
        progressBar = findViewById(R.id.progress_bar)
        previewSection = findViewById(R.id.preview_section)
        cardsRecyclerView = findViewById(R.id.cards_recycler_view)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnReverseAll = findViewById(R.id.btn_reverse_all)
        btnApproveSelected = findViewById(R.id.btn_approve_selected)
    }

    private fun setupListeners() {
        // Topic input listener
        topicInput.addTextChangedListener {
            updateGenerateButtonState()
        }
        // Topic input listener
        wordsInput.addTextChangedListener {
            updateGenerateButtonState()
        }

        // Card count selection buttons
        btn5Cards.setOnClickListener { selectCardCount(5, btn5Cards) }
        btn10Cards.setOnClickListener { selectCardCount(10, btn10Cards) }
        btn20Cards.setOnClickListener { selectCardCount(20, btn20Cards) }

        // Generate button
        btnGenerate.setOnClickListener { generateCards() }

        // Select all button
        btnSelectAll.setOnClickListener { toggleSelectAll() }

        // Reverse all button
        btnReverseAll.setOnClickListener { toggleReverseAll() }

        // Approve selected button
        btnApproveSelected.setOnClickListener { approveSelectedCards() }
    }

    private fun setupRecyclerView() {
        cardsAdapter = GeneratedCardsAdapter(previewedCards, selectedDeckName) { updateApproveButtonState() }
        cardsRecyclerView.layoutManager = LinearLayoutManager(this)
        cardsRecyclerView.adapter = cardsAdapter
    }

    private fun selectCardCount(
        count: Int,
        selectedButton: MaterialButton,
    ) {
        selectedCardCount = count

        // Reset all button styles
        val buttons = listOf(btn5Cards, btn10Cards, btn20Cards)
        buttons.forEach { button ->
            button.isSelected = false
        }

        // Highlight selected button
        selectedButton.isSelected = true

        updateGenerateButtonState()
    }

    private fun updateGenerateButtonState() {
        btnGenerate.isEnabled = wordsInput.text?.isNotBlank() == true || (topicInput.text?.isNotBlank() == true && selectedCardCount > 0)
    }

    private fun generateCards() {
        val wordList =
            wordsInput.text
                ?.toString()
                ?.trim()
                ?.split('\n')
                ?.filter { it.isNotBlank() }
        val wordListGiven = !wordList.isNullOrEmpty()
        val topic = topicInput.text?.toString()?.trim()
        val topicGiven = !topic.isNullOrBlank() && selectedCardCount > 0

        if (!wordListGiven && !topicGiven) {
            showThemedToast(this, "Please provide words or topic and number of cards", true)
            return
        }

        if (wordListGiven && topicGiven) {
            showThemedToast(this, "Please provide only a list of word or only topic and number of cards", true)
            return
        }

        showProgress(true)
        btnGenerate.isEnabled = false
        // Make some space for preview
        cardCountSection.visibility = View.GONE
        findViewById<View>(R.id.tv_or).visibility = View.GONE
        wordsInputSection.visibility = View.GONE

        if (wordListGiven) {
            topicInputSection.visibility = View.GONE

            generateCardsForWordList(wordList!!)
            return
        }
        // else if (topicGiven):
        lifecycleScope.launch {
            val knownWords: List<String> =
                try {
                    // Use the withCol from AnkiActivity, which handles the collection lifecycle.
                    withCol {
                        val currentDeckId = decks.selected()
                        // Find notes in the current deck.
                        val noteIds = findNotes("did:$currentDeckId")
                        val words = mutableListOf<String>()
                        for (noteId in noteIds) {
                            getNote(noteId).let { note ->
                                // Assuming "Word" with index 1 is the field containing the word.
                                // This is based on the "Langki Language (REVERSED)" notetype.
                                note.fields[1].let { wordField ->
                                    if (wordField.isNotBlank()) {
                                        words.add(wordField)
                                    }
                                }
                            }
                        }
                        words.toList() // This list is returned by the withCol block
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching known words from deck.")
                    showThemedToast(
                        this@GenerateCardsActivity,
                        "Failed to retrieve known words. Please try again.",
                        false,
                    )
                    showProgress(false)
                    btnGenerate.isEnabled = true
                    // Show input sections again if there was an error early on
                    topicInputSection.visibility = View.VISIBLE
                    cardCountSection.visibility = View.VISIBLE
                    return@launch // Exit the coroutine
                }

            // Call the new GptUtils function with the list of known words.
            GptUtils.suggestNewVocab(
                topic = topic!!,
                count = selectedCardCount,
                knownWords = knownWords,
                language = selectedDeckName,
                onSuccess = { newWords ->
                    Timber.d("GPT Suggested new words: $newWords")

                    generateCardsForWordList(newWords)
                },
                onError = { error ->
                    handleGenerationError(error)
                },
            )
        }
    }

    private fun generateCardsForWordList(words: List<String>) {
        // Load frequency list for the selected language (e.g., "Thai")
        val freqMap = FreqListUtils.loadFreqList(this, selectedDeckName)

        // Display preview of new words
        previewedCards.clear()
        previewedCards.addAll(
            words.map { word ->
                // Check if the word is in the frequency list
                val wordInfo = freqMap[word]
                GeneratedCard(
                    word = word,
                    meaning = wordInfo?.meaning ?: "",
                    pronunciation = wordInfo?.ipa ?: "",
                    mnemonic = wordInfo?.example ?: "",
                    isSelected = true,
                    isReversed = false,
                    freqIndex = wordInfo?.freq,
                )
            },
        )
        cardsAdapter.itemsEnabled = false // Disable interaction until full details are loaded
        cardsAdapter.notifyDataSetChanged()
        previewSection.visibility = View.VISIBLE

        val cardsLeftToGenerate = previewedCards.filter { it.meaning.isEmpty() }.map { it.word }

        // Now generate full card details for the words not found in freqList
        if (cardsLeftToGenerate.isEmpty()) {
            handleGenerationSuccess(previewedCards)
        } else {
            GptUtils.generateCardsForNewWords(
                words = cardsLeftToGenerate,
                language = selectedDeckName,
                nativeLanguage = "German",
                onSuccess = { languageCards ->
                    handleGenerationSuccess(languageCards) // This will update the cards with full details
                },
                onError = { error ->
                    handleGenerationError(error)
                },
            )
        }
    }

    private fun handleGenerationSuccess(generatedCards: List<GeneratedCard>) {
        Timber.d("Generated ${generatedCards.size} language cards successfully")

        // Merge generated cards with any existing cards that had meanings from freqList
        val completeCards = previewedCards.filter { it.meaning.isNotEmpty() }
        previewedCards.clear()
        previewedCards.addAll(completeCards)
        previewedCards.addAll(generatedCards)

        cardsAdapter.itemsEnabled = true
        cardsAdapter.notifyDataSetChanged()

        showProgress(false)
        previewSection.visibility = View.VISIBLE
        updateApproveButtonState()
        btnGenerate.text = "Regenerate"
        btnGenerate.isEnabled = true

        // make more space
        topicInputSection.visibility = View.GONE
    }

    private fun handleGenerationError(error: String) {
        Timber.e("Failed to generate flashcards: $error")

        showProgress(false)
        btnGenerate.text = "Retry"
        btnGenerate.isEnabled = true
        wordsInputSection.visibility = View.VISIBLE
        topicInputSection.visibility = View.VISIBLE
        cardCountSection.visibility = View.VISIBLE
        previewSection.visibility = View.GONE

        showThemedToast(this, "Failed to generate cards: $error", false)
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun toggleSelectAll() {
        val allSelected = previewedCards.all { it.isSelected }
        val newSelectionState = !allSelected

        previewedCards.forEach { it.isSelected = newSelectionState }
        cardsAdapter.notifyDataSetChanged()

        btnSelectAll.text = if (newSelectionState) "Deselect All" else "Select All"
        updateApproveButtonState()
    }

    private fun toggleReverseAll() {
        val allReversed = previewedCards.all { it.isReversed }
        val newReversedState = !allReversed

        previewedCards.forEach { it.isReversed = newReversedState }
        cardsAdapter.notifyDataSetChanged()

        btnReverseAll.text = if (newReversedState) "Unreverse All" else "Reverse All"
    }

    private fun updateApproveButtonState() {
        val selectedCount = previewedCards.count { it.isSelected }
        btnApproveSelected.isEnabled = selectedCount > 0
        btnApproveSelected.text =
            if (selectedCount > 0) {
                "Add $selectedCount Cards to Deck"
            } else {
                "Add Selected Cards to Deck"
            }

        // Update select all button text
        val allSelected = previewedCards.isNotEmpty() && previewedCards.all { it.isSelected }
        btnSelectAll.text = if (allSelected) "Deselect All" else "Select All"

        // Update reverse all button text
        val allReversed = previewedCards.isNotEmpty() && previewedCards.all { it.isReversed }
        btnReverseAll.text = if (allReversed) "Unreverse All" else "Reverse All"
    }

    private fun approveSelectedCards() {
        val selectedCards = previewedCards.filter { it.isSelected }

        if (selectedCards.isEmpty()) {
            showThemedToast(this, "No cards selected", true)
            return
        }

        showProgress(true)
        btnApproveSelected.isEnabled = false

        lifecycleScope.launch {
            try {
                val result =
                    withCol {
                        val noteTypes = notetypes.all()

                        // Get current deck ID
                        val currentDeckId = decks.selected()

                        var successCount = 0
                        var errorCount = 0

                        // Create and add notes for each selected card
                        selectedCards.forEach { generatedCard ->
                            try {
                                val noteType =
                                    when {
                                        generatedCard.isReversed -> {
                                            noteTypes.find { it.name == "Langki Language REVERSED" }
                                                ?: throw IllegalStateException("Reversed note type not available")
                                        }
                                        else -> {
                                            noteTypes.find { it.name == "Langki Language" }
                                                ?: throw IllegalStateException("Note type not available")
                                        }
                                    }

                                // Create a new note using the selected note type
                                val note = newNote(noteType)

                                // Set the fields based on available fields in the note type
                                val fields = noteType.fieldsNames

                                // Try to map to specific language learning fields if they exist
                                when {
                                    fields.contains("Word") && fields.contains("Meaning") -> {
                                        note.setItem("Word", generatedCard.word)
                                        note.setItem("Meaning", generatedCard.meaning)
                                        if (fields.contains("Pronunciation")) {
                                            note.setItem(
                                                "Pronunciation",
                                                generatedCard.pronunciation,
                                            )
                                        }
                                        if (fields.contains("Mnemonic") && generatedCard.mnemonic.isNotEmpty()) {
                                            note.setItem("Mnemonic", generatedCard.mnemonic)
                                        }
                                    }
                                    fields.contains("Front") && fields.contains("Back") -> {
                                        // Fallback to basic Front/Back format
                                        note.setItem("Front", generatedCard.word)
                                        val backContent =
                                            buildString {
                                                append(generatedCard.meaning)
                                                append("\n\nPronunciation: ${generatedCard.pronunciation}")
                                                if (generatedCard.mnemonic.isNotEmpty()) {
                                                    append("\nMnemonic: ${generatedCard.mnemonic}")
                                                }
                                            }
                                        note.setItem("Back", backContent)
                                    }

                                    else -> {
                                        // Use first two fields as fallback
                                        if (fields.isNotEmpty()) {
                                            note.setItem(
                                                fields[0],
                                                generatedCard.word,
                                            )
                                        }
                                        if (fields.size > 1) {
                                            val backContent =
                                                buildString {
                                                    append(generatedCard.meaning)
                                                    append("\n${generatedCard.pronunciation}")
                                                    if (generatedCard.mnemonic.isNotEmpty()) {
                                                        append("\n${generatedCard.mnemonic}")
                                                    }
                                                }
                                            note.setItem(fields[1], backContent)
                                        }
                                    }
                                }

                                // Add the note to the current deck
                                addNote(note, currentDeckId)
                                successCount++
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to add note: ${generatedCard.word}")
                                errorCount++
                            }
                        }

                        Pair(successCount, errorCount)
                    }

                val (successCount, errorCount) = result

                showProgress(false)

                if (errorCount == 0) {
                    Toast
                        .makeText(
                            this@GenerateCardsActivity,
                            "Successfully added $successCount cards to deck!",
                            Toast.LENGTH_SHORT,
                        ).show()
                    finish() // Return to previous screen
                } else {
                    Toast
                        .makeText(
                            this@GenerateCardsActivity,
                            "Added $successCount cards, $errorCount failed",
                            Toast.LENGTH_LONG,
                        ).show()
                    btnApproveSelected.isEnabled = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to add cards to deck")
                showProgress(false)
                btnApproveSelected.isEnabled = true
                Toast
                    .makeText(
                        this@GenerateCardsActivity,
                        "Failed to add cards: ${e.message}",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
