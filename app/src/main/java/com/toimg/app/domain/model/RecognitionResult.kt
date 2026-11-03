package com.toimg.app.domain.model

data class RecognitionResult(
    val imageId: String,
    val keywords: List<String>,
    val description: String,
    val modelName: String,
    val isSuccess: Boolean = true,
    val ocrText: String? = null,
    val isSensitive: Boolean = false,
    val errorMessage: String? = null,
    val recognizedAt: Long = System.currentTimeMillis()
) {
    val keywordsSummary: String
        get() = keywords.joinToString(", ")
}

data class BatchProgressState(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val currentBatchIndex: Int = 0,
    val totalBatches: Int = 0,
    val currentImageName: String = "",
    val statusText: String = "",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)
