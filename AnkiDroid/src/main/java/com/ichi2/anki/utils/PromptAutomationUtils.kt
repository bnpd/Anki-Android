package com.ichi2.anki.utils

import androidx.core.content.edit
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.AnkiDroidApp.Companion.sharedPrefs
import com.ichi2.anki.R
import com.ichi2.anki.libanki.Note

class PromptAutomation(
    val promptName: String,
    val noteType: String,
    var prompt: String,
    val field: String,
) {
    fun replaceFieldPlaceholders(note: Note): String {
        note.notetype.fieldsNames.forEach { noteField ->
            prompt = prompt.replace("{{$noteField}}", note.getItem(noteField))
        }
        return prompt
    }
}

data class PromptAutomationResult(
    val completed: Boolean,
    val response: String? = null,
    val promptAutomation: PromptAutomation,
)

private fun getPromptAutomationsAsStrings(): Set<String> =
    sharedPrefs().getStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), emptySet()) ?: emptySet()

fun getPromptAutomations(): List<PromptAutomation> {
    val prompts = getPromptAutomationsAsStrings()
    return prompts.map { prompt ->
        val parts = prompt.split("||")
        if (parts.size == 4) {
            val (name, cardType, promptText, field) = parts
            PromptAutomation(name, cardType, promptText, field)
        } else {
            throw IllegalArgumentException("Invalid prompt format: $prompt")
        }
    }
}

fun savePrompt(promptAutomation: PromptAutomation) {
    val prompts = getPromptAutomationsAsStrings().toMutableSet()
    prompts.add("${promptAutomation.promptName}||${promptAutomation.noteType}||${promptAutomation.prompt}||${promptAutomation.field}")
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}

fun deletePrompt(promptAutomation: PromptAutomation) {
    val prompts = getPromptAutomationsAsStrings().toMutableSet()
    prompts.remove("${promptAutomation.promptName}||${promptAutomation.noteType}||${promptAutomation.prompt}||${promptAutomation.field}")
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}
