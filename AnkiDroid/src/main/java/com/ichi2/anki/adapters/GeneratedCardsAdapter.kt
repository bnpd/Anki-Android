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
 * Adapter for displaying generated flashcards in a RecyclerView
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
        val editFront: TextInputEditText = itemView.findViewById(R.id.edit_front)
        val editBack: TextInputEditText = itemView.findViewById(R.id.edit_back)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CardViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_generated_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CardViewHolder,
        position: Int,
    ) {
        val card = cards[position]

        // Set initial values
        holder.checkboxSelect.isChecked = card.isSelected
        holder.editFront.setText(card.front)
        holder.editBack.setText(card.back)

        // Remove previous listeners to avoid conflicts
        holder.checkboxSelect.setOnCheckedChangeListener(null)
        holder.editFront.clearTextChangedListeners()
        holder.editBack.clearTextChangedListeners()

        // Set up checkbox listener
        holder.checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
            card.isSelected = isChecked
            onSelectionChanged()
        }

        // Set up text change listeners
        holder.editFront.addTextChangedListener { text ->
            card.front = text?.toString() ?: ""
        }

        holder.editBack.addTextChangedListener { text ->
            card.back = text?.toString() ?: ""
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
