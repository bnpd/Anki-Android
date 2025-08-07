package com.ichi2.anki.utils

import com.ichi2.anki.services.GptService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
     * @param model GPT model to use (default: gpt-4o-mini)
     */
    fun askGpt(
        prompt: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        model: String = "gpt-4o-mini",
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result =
                    gptService.sendPrompt(
                        prompt = prompt,
                        model = model,
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
     * Send a prompt to GPT synchronously (use only from background threads)
     * Uses OpenAI credentials from app preferences
     *
     * @param prompt The text to send to GPT
     * @param model GPT model to use
     * @return Result containing the response or error
     */
    suspend fun askGptSync(
        prompt: String,
        model: String = "gpt-4o-mini",
    ): Result<String> =
        withContext(Dispatchers.IO) {
            gptService.sendPrompt(
                prompt = prompt,
                model = model,
            )
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
     * Generate multiple flashcards for a topic
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
        val prompt =
            """
            Create $count flashcards for the topic: "$topic"
            
            Please respond with each flashcard in the following format:
            CARD 1:
            FRONT: [question or term]
            BACK: [answer or definition]
            
            CARD 2:
            FRONT: [question or term]
            BACK: [answer or definition]
            
            And so on... Make them educational and varied.
            """.trimIndent()

        askGpt(
            prompt = prompt,
            onSuccess = { response ->
                parseMultipleFlashcardsResponse(response, onSuccess, onError)
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
}
