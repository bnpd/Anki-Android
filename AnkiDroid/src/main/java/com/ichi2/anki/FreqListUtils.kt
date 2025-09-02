package com.ichi2.anki

import timber.log.Timber

data class Word(
    val freq: Int, // line number (1-based)
    val word: String,
    val ipa: String?,
    val meaning: String?,
    val example: String?,
)

object FreqListUtils {
    /**
     * Loads a frequency list from assets/freqLists/{lang}.tsv into a HashMap.
     * Each line: word\t[ipa]\t[meaning]\t[example]
     * @param context Context
     * @param lang Language code or name (e.g., "Thai")
     * @return HashMap mapping word to Word object
     */
    fun loadFreqList(
        context: android.content.Context,
        lang: String,
    ): HashMap<String, Word> {
        val freqMap = HashMap<String, Word>()
        val assetPath = "freqLists/$lang.tsv"
        try {
            context.assets.open(assetPath).bufferedReader().useLines { lines ->
                lines.forEachIndexed { idx, line ->
                    val parts = line.split('\t')
                    val word = parts.getOrNull(0)?.trim() ?: return@forEachIndexed
                    val ipa = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                    val meaning = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                    val example = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
                    freqMap[word] =
                        Word(
                            freq = idx + 1, // 1-based rank
                            word = word,
                            ipa = ipa,
                            meaning = meaning,
                            example = example,
                        )
                }
            }
        } catch (e: Exception) {
            // Log or handle error as needed
            Timber.e(e, "Error loading freq list: $assetPath")
        }
        return freqMap
    }
}
