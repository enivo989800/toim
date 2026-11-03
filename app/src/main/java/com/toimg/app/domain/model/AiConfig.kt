package com.toimg.app.domain.model

data class AiConfig(
    val apiKey: String = "",
    val baseUrl: String = DEFAULT_BASE_URL,
    val modelName: String = DEFAULT_MODEL,
    val providerType: String = PROVIDER_GEMINI
) {
    val maskedApiKey: String
        get() {
            if (apiKey.isBlank()) return "未设置 API Key"
            return if (apiKey.length > 8) {
                "${apiKey.take(4)}...${apiKey.takeLast(4)}"
            } else {
                "********"
            }
        }

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        const val PROVIDER_GEMINI = "GEMINI_DIRECT"
        const val PROVIDER_OPENAI = "OPENAI_COMPATIBLE"
    }
}

data class TestConnectionResult(
    val isSuccess: Boolean,
    val statusCode: Int = 0,
    val latencyMs: Long = 0L,
    val modelName: String = "",
    val message: String = "",
    val rawResponse: String = ""
)
