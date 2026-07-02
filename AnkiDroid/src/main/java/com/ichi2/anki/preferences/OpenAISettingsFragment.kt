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

import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.ichi2.anki.R
import com.ichi2.anki.utils.PromptAutomation
import com.ichi2.anki.utils.deletePrompt
import com.ichi2.anki.utils.getPromptAutomations
import com.ichi2.anki.utils.savePrompt

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
        val nameInput = dialogView.findViewById<EditText>(R.id.name_input)
        val noteTypeInput = dialogView.findViewById<EditText>(R.id.note_type_input)
        val promptInput = dialogView.findViewById<EditText>(R.id.prompt_input)
        val fieldInput = dialogView.findViewById<EditText>(R.id.field_input)
        val runOnlyOnLeechesCheckbox = dialogView.findViewById<CheckBox>(R.id.run_only_on_leeches_checkbox)
        val runOnlyOnceCheckbox = dialogView.findViewById<CheckBox>(R.id.run_only_once_checkbox)
        val modelInput = dialogView.findViewById<EditText>(R.id.model_input)
        val reasoningEffortInput = dialogView.findViewById<EditText>(R.id.reasoning_input)

        AlertDialog
            .Builder(context)
            .setTitle(R.string.add_gpt_prompt_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameInput.text.toString()
                val cardType = noteTypeInput.text.toString()
                val prompt = promptInput.text.toString()
                val field = fieldInput.text.toString()
                val model = modelInput.text.toString()
                val reasoningEffort = reasoningEffortInput.text.toString()

                val runOnlyOnLeeches = runOnlyOnLeechesCheckbox.isChecked
                val runOnlyOnce = runOnlyOnceCheckbox.isChecked

                if (cardType.isNotBlank() && prompt.isNotBlank() && field.isNotBlank()) {
                    savePrompt(PromptAutomation(name, cardType, prompt, field, runOnlyOnLeeches, runOnlyOnce, model, reasoningEffort))
                    addPromptToCategory(
                        category,
                        PromptAutomation(name, cardType, prompt, field, runOnlyOnLeeches, runOnlyOnce, model, reasoningEffort),
                    )
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadExistingPrompts(category: PreferenceCategory?) {
        val promptAutomations = getPromptAutomations()
        promptAutomations.forEach { promptAutomation ->
            addPromptToCategory(category, promptAutomation)
        }
    }

    private fun addPromptToCategory(
        category: PreferenceCategory?,
        promptAutomation: PromptAutomation,
    ) {
        val context = requireContext()
        val preference =
            Preference(context).apply {
                title = context.getString(R.string.gpt_prompt_title, promptAutomation.promptName)
                summary =
                    context.getString(
                        R.string.gpt_prompt_summary,
                        promptAutomation.noteType,
                        promptAutomation.field,
                        promptAutomation.runOnlyOnLeeches.toString(),
                        promptAutomation.runOnlyOnce.toString(),
                    )
                setOnPreferenceClickListener {
                    showEditOrRemoveDialog(category, this, promptAutomation)
                    true
                }
            }
        category?.addPreference(preference)
    }

    private fun showEditOrRemoveDialog(
        category: PreferenceCategory?,
        preference: Preference,
        promptAutomation: PromptAutomation,
    ) {
        val context = requireContext()
        val options = arrayOf(context.getString(R.string.edit), context.getString(R.string.remove))

        AlertDialog
            .Builder(context)
            .setTitle(R.string.edit_or_remove_prompt_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPromptDialog(category, preference, promptAutomation)
                    1 -> removePrompt(category, preference, promptAutomation)
                }
            }.show()
    }

    private fun showEditPromptDialog(
        category: PreferenceCategory?,
        preference: Preference,
        oldPromptAutomation: PromptAutomation,
    ) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gpt_prompt, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.name_input)
        val noteTypeInput = dialogView.findViewById<EditText>(R.id.note_type_input)
        val promptInput = dialogView.findViewById<EditText>(R.id.prompt_input)
        val fieldInput = dialogView.findViewById<EditText>(R.id.field_input)
        val runOnlyOnLeechesCheckbox = dialogView.findViewById<CheckBox>(R.id.run_only_on_leeches_checkbox)
        val runOnlyOnceCheckbox = dialogView.findViewById<CheckBox>(R.id.run_only_once_checkbox)
        val modelInput = dialogView.findViewById<EditText>(R.id.model_input)
        val reasoningEffortInput = dialogView.findViewById<EditText>(R.id.reasoning_input)

        nameInput.setText(oldPromptAutomation.promptName)
        noteTypeInput.setText(oldPromptAutomation.noteType)
        promptInput.setText(oldPromptAutomation.prompt)
        fieldInput.setText(oldPromptAutomation.field)
        runOnlyOnLeechesCheckbox.isChecked = oldPromptAutomation.runOnlyOnLeeches
        runOnlyOnceCheckbox.isChecked = oldPromptAutomation.runOnlyOnce
        modelInput.setText(oldPromptAutomation.model.toString())
        reasoningEffortInput.setText(oldPromptAutomation.reasoningEffort.toString())

        AlertDialog
            .Builder(context)
            .setTitle(R.string.edit_gpt_prompt_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = nameInput.text.toString()
                val newNoteType = noteTypeInput.text.toString()
                val newPrompt = promptInput.text.toString()
                val newField = fieldInput.text.toString()
                val newLeeches = runOnlyOnLeechesCheckbox.isChecked
                val newOnce = runOnlyOnceCheckbox.isChecked
                val model = modelInput.text.toString()
                val reasoningEffort = reasoningEffortInput.text.toString()
                val newPromptAutomation =
                    PromptAutomation(newName, newNoteType, newPrompt, newField, newLeeches, newOnce, model, reasoningEffort)

                if (newNoteType.isNotBlank() && newPrompt.isNotBlank() && newField.isNotBlank()) {
                    savePrompt(newPromptAutomation)
                    removePrompt(category, preference, oldPromptAutomation)
                    addPromptToCategory(category, newPromptAutomation)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removePrompt(
        category: PreferenceCategory?,
        preference: Preference,
        promptAutomation: PromptAutomation,
    ) {
        deletePrompt(promptAutomation)
        category?.removePreference(preference)
    }
}
