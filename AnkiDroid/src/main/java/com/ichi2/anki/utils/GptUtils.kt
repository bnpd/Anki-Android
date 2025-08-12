package com.ichi2.anki.utils

import com.ichi2.anki.model.GeneratedCard
import com.ichi2.anki.services.GptService
import com.openai.models.ChatModel
import com.openai.models.ReasoningEffort
import com.openai.models.responses.ResponseCreateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Utility class for easy GPT integration in AnkiDroid
 * Provides simple methods to interact with OpenAI's GPT API
 * Uses OpenAI credentials from app preferences
 */
object GptUtils {
    private val gptService = GptService()

    /**
     * Send a simple prompt to GPT and handle the response with a callback
     * Uses OpenAI credentials from app preferences
     *
     * @param prompt The text to send to GPT
     * @param onSuccess Callback when GPT responds successfully
     * @param onError Callback when there's an error
     * @param model GPT model to use (optional)
     */
    fun askGpt(
        prompt: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        model: ChatModel = ChatModel.GPT_5_MINI,
        reasoningEffort: ReasoningEffort = ReasoningEffort.MINIMAL,
        serviceTier: ResponseCreateParams.ServiceTier = ResponseCreateParams.ServiceTier.DEFAULT,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result =
                    gptService.sendPrompt(
                        prompt = prompt,
                        model = model,
                        reasoningEffort = reasoningEffort,
                        serviceTier = serviceTier,
                    )

                result.fold(
                    onSuccess = { response ->
                        onSuccess(response)
                    },
                    onFailure = { error ->
                        Timber.e(error, "GPT request failed")
                        onError(error.message ?: "Unknown error occurred")
                    },
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error in GPT request")
                onError(e.message ?: "Unexpected error occurred")
            }
        }
    }

    /**
     * Generate multiple language learning flashcards for a topic
     * Uses OpenAI credentials from app preferences
     *
     * @param topic The topic for language learning flashcards
     * @param count Number of flashcards to generate
     * @param onSuccess Callback with list of GeneratedCard objects
     * @param onError Callback when there's an error
     */
    fun generateMultipleLanguageCards(
        topic: String,
        count: Int,
        onSuccess: (List<GeneratedCard>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt =
            """
            Create $count language learning flashcards for words/phrases related to: "$topic"
            
            Please respond with each flashcard in the following format:
            CARD 1:
            WORD: [Thai word or phrase (only Thai script)]
            MEANING: [German translation]
            PRONUNCIATION: [IPA transcription of WORD, including pitch diacritics and vowel length. Without initial/final slashes/brackets and tone letters]
            USAGE: [keep empty if usage is like in german/english. Otherwise, short explanation of the difference in usage]
            
            CARD 2:
            WORD: ...
            And so on. Make them useful for language learning with accurate translations and pronunciations.
            Start your response right away, following the instructions strictly. Do not ask for clarification or additional information.
            """.trimIndent()

        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseLanguageCardsResponse(response, onSuccess, onError)
            },
            onError = onError,
        )
    }

    /**
     * Suggest new vocabulary for a topic, excluding already known words.
     * Uses OpenAI credentials from app preferences.
     *
     * @param topic The topic for the words.
     * @param knownWords A list of words/phrases already known by the user.
     * @param language The language being learned (e.g., "Thai").
     * @param count Number of new words to generate.
     * @param onSuccess Callback taking a List<String> containing the new words.
     * @param onError Callback when there's an error.
     */
    fun suggestNewVocab(
        topic: String,
        knownWords: List<String>,
        language: String,
        count: Int,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val knownWordsString = if (knownWords.isEmpty()) "none" else knownWords.joinToString("") // for Thai, no spaces needed

        val prompt =
            """
            I want to learn $language vocabulary, specifically: $topic.
            I already know the following $language words:
              $knownWordsString

            Please suggest $count new words/expressions which would have the biggest benefit for my ability to communicate myself.
            Format them as a list with one Thai word per line.
            Only provide the list, following the instructions strictly. Do not ask for clarification or additional information. 
            Only provide words or expressions with at least one word not present in my known list.
            """.trimIndent()
        askGpt(
            prompt = prompt,
            onSuccess = { wordsString -> onSuccess(wordsString.lines().filter { it.isNotBlank() && !knownWordsString.contains(it) }) },
            onError = onError,
            model = ChatModel.GPT_5_MINI,
            reasoningEffort = ReasoningEffort.LOW,
            serviceTier = ResponseCreateParams.ServiceTier.PRIORITY,
        )
    }

    /**
     * Generate language learning flashcards for specified words.
     * Uses OpenAI credentials from app preferences.
     *
     * @param words A List containing the words/expressions to create flashcards for.
     * @param language The language being learned (e.g., "Thai").
     * @param nativeLanguage The language for translations (e.g., "German").
     * @param onSuccess Callback with a list of GeneratedCard objects for new words.
     * @param onError Callback when there's an error.
     */
    fun generateCardsForNewWords(
        words: List<String>,
        language: String,
        nativeLanguage: String,
        onSuccess: (List<GeneratedCard>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt =
            """
            Please generate $language language learning flashcards for the following words/expressions:
            ${words.joinToString("\n")}
            Respond with each flashcard in the following format: 
            CARD 1:
            WORD: [$language word or phrase (only $language script)]
            IPA: [IPA transcription of WORD, e.g. tɕʰûa moːŋ]
            MEANING: [$nativeLanguage translation of WORD]
            USAGE: [keep empty if usage is like in $nativeLanguage. Otherwise, short explanation of the difference in usage]
            CARD 2:
            WORD: ...
            And so on. Make them useful for language learning with accurate translations and pronunciations.
            Only provide the final response, following the instructions strictly. Do not ask for clarification or additional information.
            """.trimIndent()
        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseLanguageCardsResponse(response, onSuccess, onError)
            },
            onError = onError,
            model = ChatModel.GPT_5,
            reasoningEffort = ReasoningEffort.LOW,
            // for now use LOW, even though kinda expensive, but seems to be more accurate than MINIMAL
            serviceTier = ResponseCreateParams.ServiceTier.FLEX,
        )
    }

    /**
     * Edit a language learning flashcard by following instructions.
     * Uses OpenAI credentials from app preferences.
     *
     * @param card The card to edit.
     * @param instructions Instruction how & what on the card should be edited.
     * @param language The language being learned (e.g., "Thai").
     * @param onSuccess Callback with a list of GeneratedCard objects for new words.
     * @param onError Callback when there's an error.
     */
    fun editCard(
        card: GeneratedCard,
        instructions: String,
        language: String,
        onSuccess: (GeneratedCard) -> Unit,
        onError: (String) -> Unit,
    ) {
        val maxInstructionChars = 300 // reasonable limit, ca 3-5 sentences. Might help avoid prompt injection
        if (instructions.isBlank()) {
            Timber.w("No instructions provided for editing card")
            onError("No instructions provided for editing card")
            return
        }
        if (instructions.length > maxInstructionChars) {
            Timber.w("Instructions too long for editing card: ${instructions.length} characters. Max is $maxInstructionChars")
            onError("Instructions too long for editing card. Max is $maxInstructionChars characters")
            return
        }
        val prompt =
            """
            The following is a $language language learning flashcard:
            === BEGIN CARD ===
            $card
            === END CARD ===
            
            Please edit the card according to the following instructions:
            === BEGIN INSTRUCTIONS ===
            $instructions
            === END INSTRUCTIONS ===
            
            Leave the other fields unchanged.
            Only provide the final edited card in the same format (excluding BEGIN & END CARD markers). Do not ask for clarification or additional information.
            """.trimIndent()
        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseLanguageCardsResponse(response, { words -> onSuccess(words[0]) }, onError)
            },
            onError = onError,
            model = ChatModel.GPT_5,
            reasoningEffort = ReasoningEffort.LOW,
            // for now use LOW, even though kinda expensive, but seems to be more accurate than MINIMAL
            serviceTier = ResponseCreateParams.ServiceTier.PRIORITY,
        )
    }

    /**
     * Identify errors on a language learning flashcard.
     * Uses OpenAI credentials from app preferences.
     *
     * @param card The card to check.
     * @param language The language being learned (e.g., "Thai").
     * @param onSuccess Callback with a list of GeneratedCard objects for new words.
     * @param onError Callback when there's an error.
     */
    fun identifyErrorsOnCard(
        card: GeneratedCard,
        language: String,
        onSuccess: (LintResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt =
            """
            The following is a $language language learning flashcard:
            === BEGIN CARD ===
            WORD: ${card.word}
            IPA: ${card.pronunciation}
            MEANING: ${card.meaning}
            === END CARD ===
            
            Identify any incorrect information on the card, such as:
            - Incorrect word/phrase on the card
            - Incorrect IPA transcription (empty is fine. Broad transcription is fine)
            - Incorrect meaning/translation
            
            Respond with either "NO ERRORS" if everything is correct, or with your remarks in the following format:
            NAME_OF_WRONG_FIELD: [your remarks]
            MAYBE_NAME_OF_OTHER_WRONG_FIELD: [your remarks]
            Follow the instructions exactly. Do not ask for clarification or additional information.
            """.trimIndent()
        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                Timber.d("GPT response for identifying errors:\n$response")
                if (response.trim().replace("\"", "") == "NO ERRORS") {
                    Timber.d("No errors found on card")
                    onSuccess(
                        LintResult(
                            completed = true,
                            errorsFound = false,
                            response = response,
                            remarksPerField = card,
                        ),
                    )
                } else {
                    try {
                        val updatedCard = parseSinglePartialLanguageCardResponse(response)
                        Timber.d("Errors found on card, returning updated card: $updatedCard")
                        onSuccess(
                            LintResult(
                                completed = true,
                                errorsFound = true,
                                response = response,
                                remarksPerField = updatedCard,
                            ),
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing GPT response for identifying errors")
                        onError("Error parsing GPT response: ${e.message}")
                    }
                }
            },
            onError = onError,
            model = ChatModel.GPT_5_MINI,
            reasoningEffort = ReasoningEffort.LOW,
            // for now use LOW, even though kinda expensive, but seems to be more accurate than MINIMAL
            serviceTier = ResponseCreateParams.ServiceTier.PRIORITY,
        )
    }

    private fun parseLanguageCardsResponse(
        response: String,
        onSuccess: (List<GeneratedCard>) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val cards = mutableListOf<GeneratedCard>()
            val lines = response.lines().map { it.trim() }

            var currentWord = ""
            var currentMeaning = ""
            var currentPronunciation = ""
            var currentMnemonic = ""

            for (line in lines) {
                when {
                    line.startsWith("WORD:", ignoreCase = true) -> {
                        currentWord = line.substring(5).trim()
                    }
                    line.startsWith("IPA:", ignoreCase = true) -> {
                        currentPronunciation = line.substring(4).trim()
                    }
                    line.startsWith("MEANING:", ignoreCase = true) -> {
                        currentMeaning = line.substring(8).trim()
                    }
                    line.startsWith("USAGE:", ignoreCase = true) -> {
                        currentMnemonic = line.substring(6).trim()

                        // When we hit mnemonic, we should have all fields for a complete card
                        if (currentWord.isNotEmpty() && currentMeaning.isNotEmpty() && currentPronunciation.isNotEmpty()) {
                            cards.add(
                                GeneratedCard(
                                    word = currentWord,
                                    meaning = currentMeaning,
                                    pronunciation = currentPronunciation,
                                    mnemonic = currentMnemonic,
                                ),
                            )

                            // Reset for next card
                            currentWord = ""
                            currentMeaning = ""
                            currentPronunciation = ""
                        }
                    }
                }
            }

            if (cards.isNotEmpty()) {
                onSuccess(cards)
            } else {
                onError("Could not parse any language cards from GPT response")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing language cards response")
            onError("Error parsing GPT response: ${e.message}")
        }
    }

    private fun parseSinglePartialLanguageCardResponse(response: String): GeneratedCard {
        try {
            val card = GeneratedCard("", "", "")
            val lines = response.lines().map { it.trim() }

            for (line in lines) {
                when {
                    line.startsWith("WORD:", ignoreCase = true) -> {
                        card.word = line.substring(5).trim()
                    }
                    line.startsWith("IPA:", ignoreCase = true) -> {
                        card.pronunciation = line.substring(4).trim()
                    }
                    line.startsWith("MEANING:", ignoreCase = true) -> {
                        card.meaning = line.substring(8).trim()
                    }
                    line.startsWith("USAGE:", ignoreCase = true) -> {
                        card.mnemonic = line.substring(6).trim()
                    }
                }
            }

            if (card.word.isNotEmpty() || card.meaning.isNotEmpty() || card.pronunciation.isNotEmpty()) {
                return card
            } else {
                throw IllegalArgumentException("No valid fields found in the response")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing language cards response")
            throw IllegalArgumentException("Error parsing GPT response: ${e.message}")
        }
    }
}

data class LintResult(
    val completed: Boolean,
    val errorsFound: Boolean = false,
    val response: String? = null,
    val remarksPerField: GeneratedCard? = null,
)
