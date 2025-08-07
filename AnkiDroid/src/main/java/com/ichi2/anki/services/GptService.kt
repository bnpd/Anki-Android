package com.ichi2.anki.services

import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.models.responses.ResponseCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration

/**
 * Service for interacting with OpenAI's GPT API
 * Uses OpenAI credentials from app preferences
 */
class GptService {
    companion object {
        private const val DEFAULT_MODEL = "gpt-5-mini"
    }

    /**
     * Data class to hold OpenAI credentials from preferences
     */
    private data class OpenAiCredentials(
        val apiKey: String,
        val organization: String,
        val project: String,
    )

    /**
     * Get OpenAI credentials from app preferences
     */
    private fun getOpenAiCredentials(): OpenAiCredentials {
        val sharedPrefs = AnkiDroidApp.sharedPrefs()
        val context = AnkiDroidApp.instance

        val apiKey = sharedPrefs.getString(context.getString(R.string.pref_openai_api_key), "") ?: ""
        val organization = sharedPrefs.getString(context.getString(R.string.pref_openai_organization_key), "") ?: ""
        val project = sharedPrefs.getString(context.getString(R.string.pref_openai_project_key), "") ?: ""

        return OpenAiCredentials(
            apiKey = apiKey.trim(),
            organization = organization.trim(),
            project = project.trim(),
        )
    }

    /**
     * Create OpenAI client with credentials from preferences
     */
    private fun createOpenAIClient(): OpenAIClientAsync? {
        val credentials = getOpenAiCredentials()
        if (credentials.apiKey.isEmpty()) {
            return null
        }

        return OpenAIOkHttpClientAsync
            .builder()
            .apiKey(credentials.apiKey)
            .timeout(Duration.ofSeconds(60))
            .apply {
                if (credentials.organization.isNotEmpty()) {
                    organization(credentials.organization)
                }
                if (credentials.project.isNotEmpty()) {
                    project(credentials.project)
                }
            }.build()
    }

    /**
     * Send a prompt to GPT and return the response
     * Uses OpenAI credentials from app preferences
     *
     * @param prompt The text prompt to send to GPT
     * @param model The GPT model to use (default: gpt-5-mini)
     * @return The GPT response text, or error if credentials are missing/invalid
     */
    suspend fun sendPrompt(
        prompt: String,
        model: String = DEFAULT_MODEL,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val client =
                    createOpenAIClient()
                        ?: return@withContext Result.failure(
                            IllegalStateException("OpenAI API key not configured"),
                        )

                // Build the response creation parameters
                val responseCreateParams =
                    ResponseCreateParams
                        .builder()
                        .input(prompt)
                        .model(model)
                        .build()

                Timber.d("Sending GPT request with model: $model")

                // Use CompletableFuture.get() to await the async response in the coroutine
                val response = client.responses().create(responseCreateParams).get()

                val contentBuilder = StringBuilder()

                response.output().forEach { item ->
                    item.message().ifPresent { message ->
                        message.content().forEach { content ->
                            content.outputText().ifPresent { outputText ->
                                val text = outputText.text()
                                contentBuilder.append(text)
                                Timber.d("Received content: $text")
                            }
                        }
                    }
                }

                if (contentBuilder.toString().isBlank()) {
                    Timber.w("Received empty response from GPT")
                    return@withContext Result.failure(
                        RuntimeException("Received empty response from GPT"),
                    )
                }

                val content = contentBuilder.toString().trim()
                Timber.d("Received GPT response with ${content.length} characters")
                Result.success(content)
            } catch (e: Exception) {
                Timber.e(e, "Error communicating with GPT API")
                Result.failure(e)
            }
        }
}
