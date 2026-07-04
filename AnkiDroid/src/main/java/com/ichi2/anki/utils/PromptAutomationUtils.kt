package com.ichi2.anki.utils

import androidx.core.content.edit
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.AnkiDroidApp.Companion.sharedPrefs
import com.ichi2.anki.R
import com.ichi2.anki.libanki.Note
import com.openai.models.ChatModel
import com.openai.models.ReasoningEffort

class PromptAutomation(
    val promptName: String,
    val noteType: String,
    var prompt: String,
    val field: String,
    val runOnlyOnLeeches: Boolean = false,
    val runOnlyOnce: Boolean = false,
    val model: ChatModel,
    val reasoningEffort: ReasoningEffort,
) {
    fun replaceFieldPlaceholders(note: Note): String {
        note.notetype.fieldsNames.forEach { noteField ->
            prompt = prompt.replace("{{$noteField}}", note.getItem(noteField))
        }
        return prompt
    }

    fun tagNameAfterRan(): String = "${this.promptName.replace(" ", "-")}-prompt-ran"

    override fun toString(): String = "$promptName||$noteType||$prompt||$field||$runOnlyOnLeeches||$runOnlyOnce||$model||$reasoningEffort"

    constructor(
        newName: String,
        newNoteType: String,
        newPrompt: String,
        newField: String,
        newLeeches: Boolean,
        newOnce: Boolean,
        model: String,
        reasoningEffort: String,
    ) : this(
        promptName = newName,
        noteType = newNoteType,
        prompt = newPrompt,
        field = newField,
        runOnlyOnLeeches = newLeeches,
        runOnlyOnce = newOnce,
        model = if (model.isNotEmpty()) ChatModel.of(model) else DEFAULT_MODEL,
        reasoningEffort =
            if (reasoningEffort.isNotEmpty()) {
                ReasoningEffort.of(reasoningEffort)
            } else {
                DEFAULT_REASONING_EFFORT
            },
    )

    companion object {
        fun fromString(promptString: String): PromptAutomation {
            val parts = promptString.split("||")
            if (parts.size != 8) {
                if (parts.size == 4) {
                    return PromptAutomation(
                        parts[0], // promptName
                        parts[1], // noteType
                        parts[2], // prompt
                        parts[3], // field
                        newLeeches = false, // runOnlyOnLeeches
                        newOnce = false, // runOnlyOnce
                        model = "",
                        reasoningEffort = "",
                    )
                } else {
                    throw IllegalArgumentException("Invalid prompt format: $promptString")
                }
            }
            return PromptAutomation(
                parts[0], // promptName
                parts[1], // noteType
                parts[2], // prompt
                parts[3], // field
                parts[4].toBoolean(), // runOnlyOnLeeches
                parts[5].toBoolean(), // runOnlyOnce
                ChatModel.of(parts[6]), // model
                ReasoningEffort.of(parts[7]), // reasoningEffort
            )
        }

        const val LEECH_THRESHOLD = 3
        val DEFAULT_MODEL = ChatModel.GPT_5_MINI
        val DEFAULT_REASONING_EFFORT = ReasoningEffort.LOW
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
    if (!prompts.add(promptAutomation.toString())) {
        throw IllegalArgumentException("Prompt with name ${promptAutomation.promptName} already exists.")
    }
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}

fun deletePrompt(promptAutomation: PromptAutomation) {
    val prompts = getPromptAutomationsAsStrings().toMutableSet()
    if (!prompts.removeIf { PromptAutomation.fromString(it).promptName == promptAutomation.promptName }) {
        throw IllegalArgumentException("Prompt with name ${promptAutomation.promptName} does not exist.")
    }
    sharedPrefs().edit { putStringSet(AnkiDroidApp.instance.getString(R.string.gpt_prompts_pref_key), prompts) }
}
