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
    var freqIndex: Int? = null,
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

fun generatedCardFromNote(note: Note): GeneratedCard =
    GeneratedCard(
        word = note.getItem("Word"),
        meaning = note.getItem("Meaning"),
        pronunciation = note.getItem("Pronunciation"),
        mnemonic = note.getItem("Mnemonic"),
    )
