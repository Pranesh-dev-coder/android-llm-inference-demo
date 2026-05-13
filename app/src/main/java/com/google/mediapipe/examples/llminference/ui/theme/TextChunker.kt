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
        val embedder  = textEmbedder ?: return fallbackKeywordSearch(query,chunks)

        try {
            val queryEmbedding = embedder.embed(query).embeddingResult().embeddings().first()

            var bestChunk = chunks.firstOrNull() ?: ""
            var highestSimilarity = -1.0

            for (chunk in chunks){
                val chunkEmbedding = embedder.embed(chunk).embeddingResult().embeddings().first()
                // Compute conceptual closeness (1.0 is identical, -1.0 is completely opposite)
                val similarity = TextEmbedder.cosineSimilarity(queryEmbedding, chunkEmbedding).toDouble()

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity
                    bestChunk = chunk
                }
            }
            return bestChunk
        }catch (e: Exception) {
            return fallbackKeywordSearch(query, chunks)
        }
    }

    private fun fallbackKeywordSearch(query: String, chunks: List<String>): String{
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        if (queryWords.isEmpty()) return chunks.firstOrNull() ?: ""
        return chunks.maxByOrNull { chunk ->
            val chunkWords = chunk.lowercase().split(Regex("\\W+")).toSet()
            queryWords.count { it in chunkWords }
        } ?: chunks.firstOrNull() ?: ""

    }
}

