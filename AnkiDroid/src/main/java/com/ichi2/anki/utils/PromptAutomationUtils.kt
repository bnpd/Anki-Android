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
    val runOnlyOnLeeches: Boolean = false, // New field
    val runOnlyOnce: Boolean = false, // New field
) {
    fun replaceFieldPlaceholders(note: Note): String {
        note.notetype.fieldsNames.forEach { noteField ->
            prompt = prompt.replace("{{$noteField}}", note.getItem(noteField))
        }
        return prompt
    }

    fun tagNameAfterRan(): String = "${this.promptName.replace(" ", "-")}-prompt-ran"

    override fun toString(): String = "$promptName||$noteType||$prompt||$field||$runOnlyOnLeeches||$runOnlyOnce"

    companion object {
        fun fromString(promptString: String): PromptAutomation {
            val parts = promptString.split("||")
            if (parts.size != 6) {
                throw IllegalArgumentException("Invalid prompt format: $promptString")
            }
            return PromptAutomation(
                parts[0], // promptName
                parts[1], // noteType
                parts[2], // prompt
                parts[3], // field
                parts[4].toBoolean(), // runOnlyOnLeeches
                parts[5].toBoolean(), // runOnlyOnce
            )
        }
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
        PromptAutomation.fromString(prompt)
    }
}

fun savePrompt(promptAutomation: PromptAutomation) {
    val prompts = getPromptAutomationsAsStrings().toMutableSet()
    prompts.add(promptAutomation.toString())
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}

fun deletePrompt(promptAutomation: PromptAutomation) {
    val prompts = getPromptAutomationsAsStrings().toMutableSet()
    prompts.remove(promptAutomation.toString())
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}
