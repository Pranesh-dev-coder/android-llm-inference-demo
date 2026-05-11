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
}