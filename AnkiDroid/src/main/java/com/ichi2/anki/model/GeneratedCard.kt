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

package com.ichi2.anki.model

import com.ichi2.anki.libanki.Note

/**
 * Data class representing a generated language learning flashcard that can be edited and selected
 */
data class GeneratedCard(
    var word: String,
    var meaning: String,
    var pronunciation: String,
    var mnemonic: String = "",
    var isSelected: Boolean = true,
    var isReversed: Boolean = false,
) {
    /**
     * Returns a string representation of the card, useful for debugging
     */
    override fun toString(): String =
        """
        WORD: $word
        IPA: $pronunciation
        MEANING: $meaning
        USAGE: $mnemonic
        """.trimIndent()
}

fun generatedCardFromNote(note: Note): GeneratedCard {
    val wordFieldIndex = note.notetype.fields.indexOfFirst { it.name == "Word" }
    val meaningFieldIndex = note.notetype.fields.indexOfFirst { it.name == "Meaning" }
    val pronunciationFieldIndex = note.notetype.fields.indexOfFirst { it.name == "Pronunciation" }
    val mnemonicFieldIndex = note.notetype.fields.indexOfFirst { it.name == "Mnemonic" }
    if (wordFieldIndex < 0 || meaningFieldIndex < 0 || pronunciationFieldIndex < 0 || mnemonicFieldIndex < 0) {
        throw IllegalArgumentException("Note does not contain required fields")
    }

    return GeneratedCard(
        word = note.fields[wordFieldIndex],
        meaning = note.fields[meaningFieldIndex],
        pronunciation = note.fields[pronunciationFieldIndex],
        mnemonic = note.fields[mnemonicFieldIndex],
        isSelected = true, // not used in this context, so defaulting to true
        isReversed = false, // not used in this context, so defaulting to false
    )
}
