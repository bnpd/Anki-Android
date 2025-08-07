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
import android.widget.CheckBox
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.ichi2.anki.R
import com.ichi2.anki.model.GeneratedCard

/**
 * Adapter for displaying generated language learning flashcards in a RecyclerView
 * Allows users to edit card content and select/deselect cards for approval
 */
class GeneratedCardsAdapter(
    private val cards: List<GeneratedCard>,
    private val onSelectionChanged: () -> Unit,
) : RecyclerView.Adapter<GeneratedCardsAdapter.CardViewHolder>() {
    class CardViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val checkboxSelect: CheckBox = itemView.findViewById(R.id.checkbox_select)
        val checkboxReversed: CheckBox = itemView.findViewById(R.id.checkbox_reversed)
        val editWord: TextInputEditText = itemView.findViewById(R.id.edit_word)
        val editMeaning: TextInputEditText = itemView.findViewById(R.id.edit_meaning)
        val editPronunciation: TextInputEditText = itemView.findViewById(R.id.edit_pronunciation)
        val editMnemonic: TextInputEditText = itemView.findViewById(R.id.edit_mnemonic)
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

        // Set initial values
        holder.checkboxSelect.isChecked = card.isSelected
        holder.checkboxReversed.isChecked = card.isReversed
        holder.editWord.setText(card.word)
        holder.editMeaning.setText(card.meaning)
        holder.editPronunciation.setText(card.pronunciation)
        holder.editMnemonic.setText(card.mnemonic)

        // Remove previous listeners to avoid conflicts
        holder.checkboxSelect.setOnCheckedChangeListener(null)
        holder.checkboxReversed.setOnCheckedChangeListener(null)
        holder.editWord.clearTextChangedListeners()
        holder.editMeaning.clearTextChangedListeners()
        holder.editPronunciation.clearTextChangedListeners()
        holder.editMnemonic.clearTextChangedListeners()

        // Set up checkbox listeners
        holder.checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
            card.isSelected = isChecked
            onSelectionChanged()
        }

        holder.checkboxReversed.setOnCheckedChangeListener { _, isChecked ->
            card.isReversed = isChecked
        }

        // Set up text change listeners
        holder.editWord.addTextChangedListener { text ->
            card.word = text?.toString() ?: ""
        }

        holder.editMeaning.addTextChangedListener { text ->
            card.meaning = text?.toString() ?: ""
        }

        holder.editPronunciation.addTextChangedListener { text ->
            card.pronunciation = text?.toString() ?: ""
        }

        holder.editMnemonic.addTextChangedListener { text ->
            card.mnemonic = text?.toString() ?: ""
        }
    }

    override fun getItemCount(): Int = cards.size

    private fun TextInputEditText.clearTextChangedListeners() {
        // Remove all text watchers by setting text without triggering them
        val currentText = this.text.toString()
        this.setText("")
        this.setText(currentText)
    }
}
