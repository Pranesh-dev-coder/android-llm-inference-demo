package com.google.mediapipe.examples.llminference.ui.theme

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder


object TextChunker {

    private var textEmbedder: TextEmbedder? = null

    fun chunkText(text: String, chunkSize: Int = 500, overlap: Int = 50): List<String>{
        if (text.length <= chunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length){
            val end  = minOf(start + chunkSize, text.length)
            chunks.add(text.substring(start, end))

            start += (chunkSize - overlap)

            if (start >= text.length || chunkSize <= overlap) break


        }
        return chunks
    }

    fun initEmbedder(context: Context, modelPath: String){
        if (textEmbedder != null) return
        val baseOptions = BaseOptions.builder().setModelAssetPath(modelPath).build()
        val options = TextEmbedder.TextEmbedderOptions.builder().setBaseOptions(baseOptions).build()
        textEmbedder = TextEmbedder.createFromOptions(context, options)

    }

    /**
     * A simple "Keyword Relevance" search.
     * Finds the chunk that has the most words in common with the user's query.
     */
    fun findBestChunk(query: String, chunks: List<String>): String {
        // Skip context retrieval for simple greetings or ultra-short queries
        val words = query.lowercase().trim().split(Regex("\\W+")).filter { it.isNotEmpty() }
        val isGreeting = words.any { it in setOf("hello", "hi", "hey", "greetings", "howdy", "ello", "good morning", "good afternoon", "good evening") }
        if (isGreeting || words.size <= 1) {
            return ""
        }

        val embedder  = textEmbedder ?: return fallbackKeywordSearch(query, chunks)

        try {
            val queryEmbedding = embedder.embed(query).embeddingResult().embeddings().first()

            var bestChunk = ""
            var highestSimilarity = -1.0

            for (chunk in chunks){
                val chunkEmbedding = embedder.embed(chunk).embeddingResult().embeddings().first()
                val similarity = TextEmbedder.cosineSimilarity(queryEmbedding, chunkEmbedding).toDouble()

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity
                    bestChunk = chunk
                }
            }
            // Only inject context if similarity threshold is met (e.g., >= 0.35)
            return if (highestSimilarity >= 0.35) bestChunk else ""
        } catch (e: Exception) {
            return fallbackKeywordSearch(query, chunks)
        }
    }

    private fun fallbackKeywordSearch(query: String, chunks: List<String>): String {
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        if (queryWords.isEmpty()) return ""
        
        var maxMatches = 0
        var bestChunk = ""
        
        for (chunk in chunks) {
            val chunkWords = chunk.lowercase().split(Regex("\\W+")).toSet()
            val matches = queryWords.count { it in chunkWords }
            if (matches > maxMatches) {
                maxMatches = matches
                bestChunk = chunk
            }
        }
        // Only return context if there is at least one keyword match
        return if (maxMatches > 0) bestChunk else ""
    }
}

