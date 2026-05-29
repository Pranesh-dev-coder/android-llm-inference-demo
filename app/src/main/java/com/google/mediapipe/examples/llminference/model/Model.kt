package com.google.mediapipe.examples.llminference.model


import com.google.mediapipe.tasks.genai.llminference.LlmInference.Backend

// NB: Make sure the filename is *unique* per model you use!
// Weight caching is currently based on filename alone.
enum class Model(
    val path: String,
    val url: String,
    val licenseUrl: String,
    val needsAuth: Boolean,
    val preferredBackend: Backend?,
    val thinking: Boolean,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val systemPrompt: String = "You are a Medical Assistant. You MUST prioritize accuracy above all else. If you are asked a medical question and the answer is not explicitly provided in the 'Relevant Context' or 'VERIFIED MEDICAL KNOWLEDGE', you MUST clearly state that you do not have enough verified information to answer safely. DO NOT guess or invent medical facts or medication uses.",
) {



    DEEPSEEK_R1_DISTILL_QWEN_1_5_B(
        path = "/data/local/tmp/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
        url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
        licenseUrl = "",
        needsAuth = false,
        preferredBackend = Backend.CPU,
        thinking = true,
        temperature = 0.6f,
        topK = 40,
        topP = 0.7f
    ),
    LLAMA_3_2_3B_INSTRUCT(
        path = "/data/local/tmp/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        url = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct/resolve/main/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        licenseUrl = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct",
        needsAuth = true,
        preferredBackend = Backend.CPU,
        thinking = true,
        temperature = 0.6f,
        topK = 64,
        topP = 0.9f,
    );






    private fun getEffectiveSystemPrompt(context: String): String {
        var prompt = systemPrompt
        if (this.thinking) {
            prompt += " Keep your <think> process extremely brief. Do not summarize previous conversation turns in your thinking. Focus ONLY on answering the user's newest question. For simple questions, finish thinking in less than 2 sentences."
            if (context.isNotEmpty()) {
                prompt += " RAG BYPASS: If the answer is directly found in the Relevant Context, skip the thinking process and just output the text."
            }
        }
        return prompt
    }

    fun createPrompt(userMessage: String, context: String, isFirstTurn: Boolean): String {
        return when (this) {

            DEEPSEEK_R1_DISTILL_QWEN_1_5_B -> {
                val prefix =
                    if (isFirstTurn) "<|im_start|>system\n${getEffectiveSystemPrompt(context)}<|im_end|>\n" else ""
                "$prefix<|im_start|>user\n$context$userMessage<|im_end|>\n<|im_start|>assistant\n"
            }


            LLAMA_3_2_3B_INSTRUCT -> {
                val prefix =
                    if (isFirstTurn) "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n${getEffectiveSystemPrompt(context)}<|eot_id|>" else ""
                "$prefix<|start_header_id|>user<|end_header_id|>\n\n$context$userMessage<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            }
        }
    }

    fun generateSlidingWindowPrompt(history: List<Pair<Boolean, String>>, newQuery: String, context: String): String {
        val sb = StringBuilder()
        when (this) {

            DEEPSEEK_R1_DISTILL_QWEN_1_5_B -> {
                sb.append("<|im_start|>system\n${getEffectiveSystemPrompt(context)}<|im_end|>\n")
                for ((isUser, msg) in history) {
                    val role = if (isUser) "user" else "assistant"
                    sb.append("<|im_start|>$role\n$msg<|im_end|>\n")
                }
                sb.append("<|im_start|>user\n$context$newQuery<|im_end|>\n<|im_start|>assistant\n")
            }

           LLAMA_3_2_3B_INSTRUCT -> {
                sb.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n${getEffectiveSystemPrompt(context)}<|eot_id|>")
                for ((isUser, msg) in history) {
                    val role = if (isUser) "user" else "assistant"
                    sb.append("<|start_header_id|>$role<|end_header_id|>\n\n$msg<|eot_id|>")
                }
                sb.append("<|start_header_id|>user<|end_header_id|>\n\n$context$newQuery<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
            }
        }
        return sb.toString()
    }
}

