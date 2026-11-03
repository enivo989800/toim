package com.toimg.app.core.util

import android.content.Context
import android.net.Uri
import com.toimg.app.data.remote.AiVisionClient
import com.toimg.app.data.remote.ImagePayload
import com.toimg.app.data.local.ToImgDatabase
import com.toimg.app.data.local.entity.ImageEntity
import com.toimg.app.data.repository.AiConfigRepository
import com.toimg.app.data.repository.LocalImageRepositoryImpl
import com.toimg.app.domain.model.BatchProgressState
import com.toimg.app.domain.model.RecognitionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lifecycle-resilient Application-scoped manager for batch AI recognition.
 * Survives screen navigation and allows resuming interrupted tasks after process kills.
 */
object BackgroundBatchManager {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentBatchJob: Job? = null

    private val _progressState = MutableStateFlow(BatchProgressState())
    val progressState: StateFlow<BatchProgressState> = _progressState.asStateFlow()

    private val repository = LocalImageRepositoryImpl()

    fun isRunning(): Boolean = _progressState.value.isRunning

    /**
     * Starts or resumes batch recognition for the given images in the background.
     */
    fun startBatchRecognition(
        context: Context,
        imagesToProcess: List<ImageEntity>,
        onFinished: (() -> Unit)? = null
    ) {
        if (isRunning() || imagesToProcess.isEmpty()) return

        currentBatchJob?.cancel()
        currentBatchJob = applicationScope.launch {
            val db = ToImgDatabase.getInstance(context)
            val aiConfig = AiConfigRepository(context).getAiConfig()

            _progressState.value = BatchProgressState(
                totalCount = imagesToProcess.size,
                completedCount = 0,
                failedCount = 0,
                isRunning = true,
                statusText = "后台批量识别任务已启动..."
            )

            // Mark all items as PROCESSING in Room for crash recovery
            imagesToProcess.forEach { img ->
                db.imageDao().updateStatus(img.id, "PROCESSING")
            }

            var successCount = 0
            var failCount = 0

            imagesToProcess.forEachIndexed { index, imageEntity ->
                if (!_progressState.value.isRunning) return@forEachIndexed

                _progressState.value = _progressState.value.copy(
                    completedCount = successCount,
                    failedCount = failCount,
                    currentBatchIndex = index + 1,
                    totalBatches = imagesToProcess.size,
                    currentImageName = imageEntity.name,
                    statusText = "正在后台分析 (${index + 1}/${imagesToProcess.size}): ${imageEntity.name}"
                )

                try {
                    val base64 = ImageCompressUtil.compressUriToBase64(context, Uri.parse(imageEntity.uriString), maxDimension = 1024)
                    if (base64 != null) {
                        val payload = ImagePayload(
                            imageId = imageEntity.id,
                            base64Jpeg = base64,
                            fileName = imageEntity.name
                        )
                        val results = AiVisionClient().recognizeBatch(aiConfig, listOf(payload))
                        val result = results.firstOrNull() ?: RecognitionResult(
                            imageId = imageEntity.id,
                            keywords = emptyList(),
                            description = "未返回识别结果",
                            modelName = aiConfig.modelName,
                            isSuccess = false
                        )
                        repository.saveRecognitions(context, listOf(result))
                        if (result.isSuccess) {
                            successCount++
                            db.imageDao().updateStatus(imageEntity.id, "RECOGNIZED")
                        } else {
                            failCount++
                            db.imageDao().updateStatus(imageEntity.id, "FAILED")
                        }
                    } else {
                        failCount++
                        db.imageDao().updateStatus(imageEntity.id, "FAILED")
                    }
                } catch (e: Exception) {
                    failCount++
                    db.imageDao().updateStatus(imageEntity.id, "FAILED")
                }

            }

            _progressState.value = _progressState.value.copy(
                completedCount = successCount,
                failedCount = failCount,
                isRunning = false,
                statusText = "后台识别完成：成功 $successCount 张，失败 $failCount 张"
            )

            withContext(Dispatchers.Main) {
                onFinished?.invoke()
            }
        }
    }

    fun stopBatch() {
        currentBatchJob?.cancel()
        currentBatchJob = null
        _progressState.value = _progressState.value.copy(
            isRunning = false,
            statusText = "后台任务已暂停"
        )
    }

    /**
     * Checks if there are interrupted tasks from a previous session and resumes them.
     */
    suspend fun checkAndResumeInterrupted(context: Context): Int = withContext(Dispatchers.IO) {
        val db = ToImgDatabase.getInstance(context)
        val interrupted = db.imageDao().getImagesByStatus("PROCESSING")
        if (interrupted.isNotEmpty() && !isRunning()) {
            startBatchRecognition(context, interrupted)
        }
        interrupted.size
    }
}
