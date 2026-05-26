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
     * Finds and compiles multiple highly relevant context chunks, using hybrid scoring
     * (Semantic Cosine Similarity + Keyword Frequency) and packages them under a safe token budget.
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

    fun findBestContextChunks(
        query: String,
        chunks: List<String>,
        maxContextLengthChars: Int = 5000
    ): String{
        val words = query.lowercase().trim().split(Regex("\\W+")).filter { it.isNotEmpty() }
        val isGreeting = words.any { it in setOf("hello", "hi", "hey", "greetings", "good morning", "good evening") }
        if (isGreeting || words.size <= 1) return ""

        val embedder = textEmbedder ?: return fallbackMultiKeywordSearch(query, chunks, maxContextLengthChars)

        try {
            val queryEmbedding = embedder.embed(query).embeddingResult().embeddings().first()
            val scoredChunks = mutableListOf<Pair<String, Double>>()

            for (chunk in chunks ){
                // 1. Semantic Embedding Similarity Score
                val chunkEmbedding = embedder.embed(chunk).embeddingResult().embeddings().first()
                val semanticScore = TextEmbedder.cosineSimilarity(queryEmbedding, chunkEmbedding).toDouble()



                // 2. Local Keyword Frequency Matching (BM25 Fallback)
                val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
                val chunkWords = chunk.lowercase().split(Regex("\\W+"))
                val matchCount = queryWords.count { it in chunkWords }
                val keywordScore = if (queryWords.isNotEmpty()) matchCount.toDouble() / queryWords.size else 0.0

                // 3. Hybrid Reranking (Weighted sum: 70% Semantic, 30% Keyword overlap)
                val hybridScore = (semanticScore * 0.70) + (keywordScore * 0.30)

                // Filter out low relevance noise (threshold 0.32)
                if (hybridScore >= 0.32) {
                    scoredChunks.add(Pair(chunk, hybridScore))
                }

            }
            val sortedChunks = scoredChunks.sortedByDescending { it.second }

            // Compress into our maximum character budget
            val compressedBuilder = StringBuilder()
            var currentLength = 0

            for ((chunkText, _) in sortedChunks) {
                if (currentLength + chunkText.length > maxContextLengthChars) break
                compressedBuilder.append("📄 [Context Source]: ").append(chunkText.trim()).append("\n\n")
                currentLength += chunkText.length
            }
            return compressedBuilder.toString().trim()
        }catch (e: Exception){
            return fallbackMultiKeywordSearch(query, chunks, maxContextLengthChars)
        }
    }

    private fun fallbackMultiKeywordSearch(query: String, chunks: List<String>, maxChars: Int): String{
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        if (queryWords.isEmpty()) return ""

        val scored = chunks.map { chunk ->
            val chunkWords = chunk.lowercase().split(Regex("\\W+")).toSet()
            val matches = queryWords.count { it in chunkWords }
            Pair(chunk, matches)
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        val compressedBuilder = StringBuilder()
        var currentLength = 0

        for ((chunkText, _) in scored) {
            if (currentLength + chunkText.length > maxChars) break
            compressedBuilder.append("📄 [Context Source]: ").append(chunkText.trim()).append("\n\n")
            currentLength += chunkText.length
        }
        return compressedBuilder.toString()

    }

}

