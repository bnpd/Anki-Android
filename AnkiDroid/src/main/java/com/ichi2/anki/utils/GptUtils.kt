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
     * Generate flashcard content using GPT
     * Uses OpenAI credentials from app preferences
     *
     * @param topic The topic for the flashcard
     * @param onSuccess Callback with front and back content
     * @param onError Callback when there's an error
     */
    fun generateFlashcard(
        topic: String,
        onSuccess: (front: String, back: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt =
            """
            Create a flashcard for the topic: "$topic"
            
            Please respond in the following format:
            FRONT: [question or term]
            BACK: [answer or definition]
            
            Make it educational and clear.
            """.trimIndent()

        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseFlashcardResponse(response, onSuccess, onError)
            },
            onError = onError,
        )
    }

    /**
     * Improve existing flashcard content using GPT
     * Uses OpenAI credentials from app preferences
     *
     * @param frontText Current front text
     * @param backText Current back text
     * @param onSuccess Callback with improved content
     * @param onError Callback when there's an error
     */
    fun improveFlashcard(
        frontText: String,
        backText: String,
        onSuccess: (front: String, back: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt =
            """
            Please improve this flashcard by making it clearer and more educational:
            
            Current front: "$frontText"
            Current back: "$backText"
            
            Please respond in the following format:
            FRONT: [improved question or term]
            BACK: [improved answer or definition]
            
            Keep the core concept but make it more engaging and easier to understand.
            """.trimIndent()

        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseFlashcardResponse(response, onSuccess, onError)
            },
            onError = onError,
        )
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
            reasoningEffort = ReasoningEffort.MINIMAL,
            serviceTier = ResponseCreateParams.ServiceTier.FLEX,
        )
    }

    /**
     * Generate multiple flashcards for a topic (legacy method for backward compatibility)
     * Uses OpenAI credentials from app preferences
     *
     * @param topic The topic for flashcards
     * @param count Number of flashcards to generate
     * @param onSuccess Callback with list of flashcard pairs
     * @param onError Callback when there's an error
     */
    fun generateMultipleFlashcards(
        topic: String,
        count: Int,
        onSuccess: (List<Pair<String, String>>) -> Unit,
        onError: (String) -> Unit,
    ) {
        // Convert to language cards and then back to pairs for compatibility
        generateMultipleLanguageCards(
            topic = topic,
            count = count,
            onSuccess = { cards ->
                val pairs =
                    cards.map { card ->
                        Pair(card.word, "${card.meaning}\n${card.pronunciation}")
                    }
                onSuccess(pairs)
            },
            onError = onError,
        )
    }

    private fun parseFlashcardResponse(
        response: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val lines = response.lines().map { it.trim() }
            var front = ""
            var back = ""

            for (line in lines) {
                when {
                    line.startsWith("FRONT:", ignoreCase = true) -> {
                        front = line.substring(6).trim()
                    }
                    line.startsWith("BACK:", ignoreCase = true) -> {
                        back = line.substring(5).trim()
                    }
                }
            }

            if (front.isNotEmpty() && back.isNotEmpty()) {
                onSuccess(front, back)
            } else {
                onError("Could not parse flashcard format from GPT response")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing flashcard response")
            onError("Error parsing GPT response: ${e.message}")
        }
    }

    private fun parseMultipleFlashcardsResponse(
        response: String,
        onSuccess: (List<Pair<String, String>>) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            val flashcards = mutableListOf<Pair<String, String>>()
            val lines = response.lines().map { it.trim() }

            var currentFront = ""
            var currentBack = ""

            for (line in lines) {
                when {
                    line.startsWith("FRONT:", ignoreCase = true) -> {
                        currentFront = line.substring(6).trim()
                    }
                    line.startsWith("BACK:", ignoreCase = true) -> {
                        currentBack = line.substring(5).trim()

                        if (currentFront.isNotEmpty() && currentBack.isNotEmpty()) {
                            flashcards.add(Pair(currentFront, currentBack))
                            currentFront = ""
                            currentBack = ""
                        }
                    }
                }
            }

            if (flashcards.isNotEmpty()) {
                onSuccess(flashcards)
            } else {
                onError("Could not parse any flashcards from GPT response")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing multiple flashcards response")
            onError("Error parsing GPT response: ${e.message}")
        }
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
                            currentMnemonic = ""
                        }
                    }
                }
            }

            // Handle case where last card doesn't have a mnemonic
            if (currentWord.isNotEmpty() && currentMeaning.isNotEmpty() && currentPronunciation.isNotEmpty()) {
                cards.add(
                    GeneratedCard(
                        word = currentWord,
                        meaning = currentMeaning,
                        pronunciation = currentPronunciation,
                        mnemonic = currentMnemonic,
                    ),
                )
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
}
