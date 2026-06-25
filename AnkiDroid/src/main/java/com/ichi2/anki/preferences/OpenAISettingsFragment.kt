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

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
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

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        // Initialize GPT Prompts category
        val gptPromptsCategory = findPreference<PreferenceCategory>(getString(R.string.pref_gpt_prompts_category_key))
        val addPromptPreference = findPreference<Preference>(getString(R.string.pref_add_gpt_prompt))

        addPromptPreference?.setOnPreferenceClickListener {
            // Logic to show a dialog for adding a new GPT prompt
            showAddPromptDialog(gptPromptsCategory)
            true
        }

        // Load existing prompts
        loadExistingPrompts(gptPromptsCategory)
    }

    private fun showAddPromptDialog(category: PreferenceCategory?) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gpt_prompt, null)
        val cardTypeInput = dialogView.findViewById<EditText>(R.id.card_type_input)
        val promptInput = dialogView.findViewById<EditText>(R.id.prompt_input)
        val fieldInput = dialogView.findViewById<EditText>(R.id.field_input)

        AlertDialog
            .Builder(context)
            .setTitle(R.string.add_gpt_prompt_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val cardType = cardTypeInput.text.toString()
                val prompt = promptInput.text.toString()
                val field = fieldInput.text.toString()

                if (cardType.isNotBlank() && prompt.isNotBlank() && field.isNotBlank()) {
                    savePrompt(cardType, prompt, field)
                    addPromptToCategory(category, cardType, prompt, field)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getSharedPreferences(): SharedPreferences = requireContext().getSharedPreferences("openai_settings", Context.MODE_PRIVATE)

    private fun loadExistingPrompts(category: PreferenceCategory?) {
        val prompts = getSharedPreferences().getStringSet("gpt_prompts", emptySet()) ?: emptySet()
        prompts.forEach { promptConfig ->
            val parts = promptConfig.split("||")
            if (parts.size == 3) {
                val (cardType, prompt, field) = parts
                addPromptToCategory(category, cardType, prompt, field)
            }
        }
    }

    private fun savePrompt(
        cardType: String,
        prompt: String,
        field: String,
    ) {
        val sharedPreferences = getSharedPreferences()
        val prompts = sharedPreferences.getStringSet("gpt_prompts", emptySet())?.toMutableSet() ?: mutableSetOf()
        prompts.add("$cardType||$prompt||$field")
        sharedPreferences.edit { putStringSet("gpt_prompts", prompts) }
    }

    private fun addPromptToCategory(
        category: PreferenceCategory?,
        cardType: String,
        prompt: String,
        field: String,
    ) {
        val context = requireContext()
        val preference =
            Preference(context).apply {
                title = context.getString(R.string.gpt_prompt_title, cardType)
                summary = context.getString(R.string.gpt_prompt_summary, prompt, field)
                setOnPreferenceClickListener {
                    showEditOrRemoveDialog(category, this, cardType, prompt, field)
                    true
                }
            }
        category?.addPreference(preference)
    }

    private fun showEditOrRemoveDialog(
        category: PreferenceCategory?,
        preference: Preference,
        cardType: String,
        prompt: String,
        field: String,
    ) {
        val context = requireContext()
        val options = arrayOf(context.getString(R.string.edit), context.getString(R.string.remove))

        AlertDialog
            .Builder(context)
            .setTitle(R.string.edit_or_remove_prompt_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPromptDialog(category, preference, cardType, prompt, field)
                    1 -> removePrompt(category, preference, cardType, prompt, field)
                }
            }.show()
    }

    private fun showEditPromptDialog(
        category: PreferenceCategory?,
        preference: Preference,
        oldCardType: String,
        oldPrompt: String,
        oldField: String,
    ) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gpt_prompt, null)
        val cardTypeInput = dialogView.findViewById<EditText>(R.id.card_type_input)
        val promptInput = dialogView.findViewById<EditText>(R.id.prompt_input)
        val fieldInput = dialogView.findViewById<EditText>(R.id.field_input)

        cardTypeInput.setText(oldCardType)
        promptInput.setText(oldPrompt)
        fieldInput.setText(oldField)

        AlertDialog
            .Builder(context)
            .setTitle(R.string.edit_gpt_prompt_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newCardType = cardTypeInput.text.toString()
                val newPrompt = promptInput.text.toString()
                val newField = fieldInput.text.toString()

                if (newCardType.isNotBlank() && newPrompt.isNotBlank() && newField.isNotBlank()) {
                    removePrompt(category, preference, oldCardType, oldPrompt, oldField, false)
                    savePrompt(newCardType, newPrompt, newField)
                    addPromptToCategory(category, newCardType, newPrompt, newField)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removePrompt(
        category: PreferenceCategory?,
        preference: Preference,
        cardType: String,
        prompt: String,
        field: String,
        removeFromUI: Boolean = true,
    ) {
        val sharedPreferences = getSharedPreferences()
        val prompts = sharedPreferences.getStringSet("gpt_prompts", emptySet())?.toMutableSet() ?: mutableSetOf()
        prompts.remove("$cardType||$prompt||$field")
        sharedPreferences.edit().putStringSet("gpt_prompts", prompts).apply()

        if (removeFromUI) {
            category?.removePreference(preference)
        }
    }
}
