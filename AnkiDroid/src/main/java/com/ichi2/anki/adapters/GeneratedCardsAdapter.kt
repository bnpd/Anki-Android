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

package com.ichi2.anki.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.ichi2.anki.R
import com.ichi2.anki.model.GeneratedCard
import com.ichi2.anki.utils.GptUtils

/**
 * Adapter for displaying generated language learning flashcards in a RecyclerView
 * Allows users to edit card content and select/deselect cards for approval
 */
class GeneratedCardsAdapter(
    private val cards: List<GeneratedCard>,
    private val selectedDeckName: String,
    private val onSelectionChanged: () -> Unit,
) : RecyclerView.Adapter<GeneratedCardsAdapter.CardViewHolder>() {
    init {
        setHasStableIds(true) // Enable stable IDs for better recycling
    }

    override fun getItemId(position: Int): Long = cards[position].hashCode().toLong()

    class CardViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val checkboxSelect: CheckBox = itemView.findViewById(R.id.checkbox_select)
        val checkboxReversed: CheckBox = itemView.findViewById(R.id.checkbox_reversed)
        val editWord: TextInputEditText = itemView.findViewById(R.id.edit_word)
        val editMeaning: TextInputEditText = itemView.findViewById(R.id.edit_meaning)
        val editPronunciation: TextInputEditText = itemView.findViewById(R.id.edit_pronunciation)
        val editMnemonic: TextInputEditText = itemView.findViewById(R.id.edit_mnemonic)

        // AI Edit Views
        val buttonEditCardWithAi: Button = itemView.findViewById(R.id.button_edit_card_with_ai)
        val layoutAiEditSection: LinearLayout = itemView.findViewById(R.id.ai_edit_section) // Updated ID
        val editTextAiPrompt: TextInputEditText = itemView.findViewById(R.id.edit_ai_prompt) // Updated ID
        val buttonSubmitAiPrompt: Button = itemView.findViewById(R.id.button_submit_ai_edit) // Updated ID

        // Track listeners for cleanup
        var wordTextWatcher: android.text.TextWatcher? = null
        var meaningTextWatcher: android.text.TextWatcher? = null
        var pronunciationTextWatcher: android.text.TextWatcher? = null
        var mnemonicTextWatcher: android.text.TextWatcher? = null

        fun cleanup() {
            wordTextWatcher?.let { editWord.removeTextChangedListener(it) }
            meaningTextWatcher?.let { editMeaning.removeTextChangedListener(it) }
            pronunciationTextWatcher?.let { editPronunciation.removeTextChangedListener(it) }
            mnemonicTextWatcher?.let { editMnemonic.removeTextChangedListener(it) }
            checkboxSelect.setOnCheckedChangeListener(null)
            checkboxReversed.setOnCheckedChangeListener(null)
            buttonEditCardWithAi.setOnClickListener(null)
            buttonSubmitAiPrompt.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CardViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_generated_language_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CardViewHolder,
        position: Int,
    ) {
        val card = cards[position]

        // Clean up previous listeners
        holder.cleanup()

        // Tag views with position to prevent wrong updates
        holder.itemView.setTag(R.id.view_holder_tag, position)

        // Set initial values
        holder.checkboxSelect.isChecked = card.isSelected
        holder.checkboxReversed.isChecked = card.isReversed
        holder.editWord.setText(card.word)
        holder.editMeaning.setText(card.meaning)
        holder.editPronunciation.setText(card.pronunciation)
        holder.editMnemonic.setText(card.mnemonic)

        // Initially hide AI edit section and clear prompt
        holder.layoutAiEditSection.isVisible = false
        holder.editTextAiPrompt.setText("")

        // Set up checkbox listeners
        holder.checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
            if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
            ) {
                card.isSelected = isChecked
                onSelectionChanged()
            }
        }

        holder.checkboxReversed.setOnCheckedChangeListener { _, isChecked ->
            if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
            ) {
                card.isReversed = isChecked
            }
        }

        // Set up text change listeners with position validation
        holder.wordTextWatcher =
            holder.editWord.addTextChangedListener { text ->
                if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                    holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
                ) {
                    card.word = text?.toString() ?: ""
                }
            }

        holder.meaningTextWatcher =
            holder.editMeaning.addTextChangedListener { text ->
                if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                    holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
                ) {
                    card.meaning = text?.toString() ?: ""
                }
            }

        holder.pronunciationTextWatcher =
            holder.editPronunciation.addTextChangedListener { text ->
                if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                    holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
                ) {
                    card.pronunciation = text?.toString() ?: ""
                }
            }

        holder.mnemonicTextWatcher =
            holder.editMnemonic.addTextChangedListener { text ->
                if (holder.getAbsoluteAdapterPosition() != RecyclerView.NO_POSITION &&
                    holder.itemView.getTag(R.id.view_holder_tag) as Int == holder.getAbsoluteAdapterPosition()
                ) {
                    card.mnemonic = text?.toString() ?: ""
                }
            }

        // AI Edit Button Listener
        holder.buttonEditCardWithAi.setOnClickListener {
            // Toggle visibility of the AI edit section
            holder.layoutAiEditSection.isVisible = !holder.layoutAiEditSection.isVisible
        }

        // AI Submit Button Listener
        holder.buttonSubmitAiPrompt.setOnClickListener {
            val currentPosition = holder.getAbsoluteAdapterPosition()
            if (currentPosition != RecyclerView.NO_POSITION &&
                holder.itemView.getTag(R.id.view_holder_tag) as Int == currentPosition
            ) {
                val prompt =
                    holder.editTextAiPrompt.text
                        .toString()
                        .trim()
                if (prompt.isNotEmpty()) {
                    holder.buttonSubmitAiPrompt.isEnabled = false
                    holder.buttonSubmitAiPrompt.text = "Loading..."

                    GptUtils.editCard(
                        card,
                        prompt,
                        selectedDeckName,
                        onSuccess = { updatedCard ->
                            // Update the card in the adapter
                            card.word = updatedCard.word
                            card.meaning = updatedCard.meaning
                            card.pronunciation = updatedCard.pronunciation
                            card.mnemonic = updatedCard.mnemonic
                            Toast
                                .makeText(
                                    holder.itemView.context,
                                    "Card edited successfully!",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            holder.buttonSubmitAiPrompt.isEnabled = false
                            holder.buttonSubmitAiPrompt.text = "Request Edit"
                            holder.layoutAiEditSection.isVisible = false
                        },
                        onError = { error ->
                            Toast
                                .makeText(
                                    holder.itemView.context,
                                    "Error editing card: $error",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            holder.buttonSubmitAiPrompt.isEnabled = false
                            holder.buttonSubmitAiPrompt.text = "Request Edit"
                        },
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = cards.size
}
