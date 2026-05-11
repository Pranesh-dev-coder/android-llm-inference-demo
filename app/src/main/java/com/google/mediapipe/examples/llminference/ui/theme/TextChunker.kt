package com.google.mediapipe.examples.llminference.ui.theme

object TextChunker {

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

    /**
     * A simple "Keyword Relevance" search.
     * Finds the chunk that has the most words in common with the user's query.
     */
    fun findBestChunk(query: String, chunks: List<String>): String {
        val queryWords = query.lowercase()
            .split(Regex("\\W+")) // Split by non-word characters
            .filter { it.length > 3 } // Ignore small words like "the", "a", "is"
            .toSet()

        if (queryWords.isEmpty()) return chunks.firstOrNull() ?: ""

        return chunks.maxByOrNull { chunk ->
            val chunkWords = chunk.lowercase().split(Regex("\\W+")).toSet()
            queryWords.count { it in chunkWords }
        } ?: chunks.firstOrNull() ?: ""
    }
}