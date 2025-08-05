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
package com.ichi2.anki.preferences

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.ichi2.anki.R

class OpenAISettingsFragment : SettingsFragment() {
    override val analyticsScreenNameConstant: String
        get() = "prefs.openai"

    override val preferenceResource: Int
        get() = R.xml.preferences_openai

    override fun initSubscreen() {
        // Configure API key preference to mask input for security
        requirePreference<EditTextPreference>(R.string.pref_openai_api_key).apply {
            setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Custom summary provider to show masked API key
            summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    val value = preference.text
                    if (!value.isNullOrEmpty()) "\u2022".repeat(value.length) else ""
                }
        }

        // Set up organization ID preference
        requirePreference<EditTextPreference>(R.string.pref_openai_organization_key).apply {
            setOnBindEditTextListener { editText ->
                editText.hint = "org-xxxxxxxxxxxxxxxxxxxxxxxxxx"
            }
        }

        // Set up project ID preference
        requirePreference<EditTextPreference>(R.string.pref_openai_project_key).apply {
            setOnBindEditTextListener { editText ->
                editText.hint = "proj_xxxxxxxxxxxxxxxxxxxxxxxx"
            }
        }
    }
}
