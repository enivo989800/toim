package com.toimg.app.feature.workspace

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toimg.app.core.util.ImageCompressUtil
import com.toimg.app.data.remote.AiVisionClient
import com.toimg.app.data.remote.ImagePayload
import com.toimg.app.data.repository.AiConfigRepository
import com.toimg.app.data.repository.LocalImageRepositoryImpl
import com.toimg.app.domain.model.AiConfig
import com.toimg.app.domain.model.BatchProgressState
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.RecognitionResult
import com.toimg.app.domain.repository.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkspaceTab {
    UNRECOGNIZED,
    RECOGNIZED,
    ALL
}

data class WorkspaceUiState(
    val images: List<ImageItem> = emptyList(),
    val recognitions: Map<String, RecognitionResult> = emptyMap(),
    val selectedImageIds: Set<String> = emptySet(),
    val currentTab: WorkspaceTab = WorkspaceTab.UNRECOGNIZED,
    val batchSize: Int = 5,
    val progress: BatchProgressState = BatchProgressState(),
    val aiConfig: AiConfig = AiConfig(),
    val selectedDetailItem: Pair<ImageItem, RecognitionResult?>? = null,
    val errorMessage: String? = null
) {
    val unrecognizedImages: List<ImageItem>
        get() = images.filter { !recognitions.containsKey(it.id) || recognitions[it.id]?.isSuccess != true }

    val recognizedImages: List<ImageItem>
        get() = images.filter { recognitions[it.id]?.isSuccess == true }

    val failedImages: List<ImageItem>
        get() = images.filter { recognitions[it.id]?.isSuccess == false }

    val currentTabImages: List<ImageItem>
        get() = when (currentTab) {
            WorkspaceTab.UNRECOGNIZED -> unrecognizedImages
            WorkspaceTab.RECOGNIZED -> recognizedImages
            WorkspaceTab.ALL -> images
        }

    val selectedCount: Int
        get() = selectedImageIds.size

    val isKeyConfigured: Boolean
        get() = aiConfig.apiKey.isNotBlank()
}

class WorkspaceViewModel(
    private val repository: ImageRepository = LocalImageRepositoryImpl()
) : ViewModel() {

    private val aiClient = AiVisionClient()
    private var recognitionJob: Job? = null

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        loadAiConfig(context)

        // Observe images from Room DB
        viewModelScope.launch {
            combine(
                repository.observeImagesFromDb(context),
                repository.observeRecognitions(context)
            ) { images, recognitions ->
                val map = recognitions.associateBy { it.imageId }
                Pair(images, map)
            }.collect { (images, recMap) ->
                _uiState.update { state ->
                    state.copy(
                        images = images,
                        recognitions = recMap
                    )
                }
            }
        }
    }

    fun loadAiConfig(context: Context) {
        val config = AiConfigRepository(context).getAiConfig()
        _uiState.update { it.copy(aiConfig = config) }
    }

    fun setTab(tab: WorkspaceTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setBatchSize(size: Int) {
        _uiState.update { it.copy(batchSize = size) }
    }

    fun toggleSelection(imageId: String) {
        _uiState.update { state ->
            val set = state.selectedImageIds.toMutableSet()
            if (set.contains(imageId)) {
                set.remove(imageId)
            } else {
                set.add(imageId)
            }
            state.copy(selectedImageIds = set)
        }
    }

    fun selectAllInCurrentTab() {
        _uiState.update { state ->
            val ids = state.currentTabImages.map { it.id }.toSet()
            state.copy(selectedImageIds = ids)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedImageIds = emptySet()) }
    }

    fun openDetail(item: ImageItem) {
        val result = _uiState.value.recognitions[item.id]
        _uiState.update { it.copy(selectedDetailItem = Pair(item, result)) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedDetailItem = null) }
    }

    fun startRecognition(context: Context, targetImages: List<ImageItem>? = null) {
        val config = _uiState.value.aiConfig
        if (config.apiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "尚未配置 AI API Key，请先进入【设置】页面填入 Key。") }
            return
        }

        val candidates = targetImages ?: run {
            if (_uiState.value.selectedImageIds.isNotEmpty()) {
                _uiState.value.images.filter { _uiState.value.selectedImageIds.contains(it.id) }
            } else {
                _uiState.value.unrecognizedImages
            }
        }

        if (candidates.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "当前没有需要识别的图片。") }
            return
        }

        val batchSize = _uiState.value.batchSize
        val batches = candidates.chunked(batchSize)

        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progress = BatchProgressState(
                        totalCount = candidates.size,
                        completedCount = 0,
                        failedCount = 0,
                        currentBatchIndex = 0,
                        totalBatches = batches.size,
                        isRunning = true,
                        statusText = "准备图片压缩与 Payload..."
                    ),
                    errorMessage = null
                )
            }

            var totalCompleted = 0
            var totalFailed = 0

            for (batchIdx in batches.indices) {
                val currentChunk = batches[batchIdx]
                val batchNum = batchIdx + 1
                val totalBatchNum = batches.size
                val chunkCount = currentChunk.size
                val progressMsg = "正在处理第 $batchNum/$totalBatchNum 批 (共 $chunkCount 张)..."

                _uiState.update { state ->
                    state.copy(
                        progress = state.progress.copy(
                            currentBatchIndex = batchNum,
                            currentImageName = currentChunk.firstOrNull()?.name ?: "",
                            statusText = progressMsg
                        )
                    )
                }

                // Compress images to Base64 in parallel for this batch
                val payloads = mutableListOf<ImagePayload>()
                for (item in currentChunk) {
                    val base64 = ImageCompressUtil.compressUriToBase64(context, item.uri, maxDimension = 1024)
                    if (base64 != null) {
                        payloads.add(
                            ImagePayload(
                                imageId = item.id,
                                base64Jpeg = base64,
                                fileName = item.name
                            )
                        )
                    } else {
                        // Image could not be compressed/read
                        totalFailed++
                        val failedResult = RecognitionResult(
                            imageId = item.id,
                            keywords = emptyList(),
                            description = "文件无法读取或已损坏",
                            modelName = config.modelName,
                            isSuccess = false,
                            errorMessage = "无法从 URI 解码图片"
                        )
                        repository.saveRecognitions(context, listOf(failedResult))
                    }
                }

                if (payloads.isNotEmpty()) {
                    val requestMsg = "已向 AI 服务 (${config.modelName}) 发起识别请求..."
                    _uiState.update { state ->
                        state.copy(
                            progress = state.progress.copy(
                                statusText = requestMsg
                            )
                        )
                    }

                    val results = aiClient.recognizeBatch(config, payloads)
                    repository.saveRecognitions(context, results)

                    results.forEach { res ->
                        if (res.isSuccess) totalCompleted++ else totalFailed++
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        progress = state.progress.copy(
                            completedCount = totalCompleted,
                            failedCount = totalFailed
                        )
                    )
                }
            }

            val finishMsg = "识别完成！成功 $totalCompleted 张，失败 $totalFailed 张。"
            _uiState.update { state ->
                state.copy(
                    progress = state.progress.copy(
                        isRunning = false,
                        statusText = finishMsg
                    ),
                    selectedImageIds = emptySet()
                )
            }
        }
    }

    fun cancelRecognition() {
        recognitionJob?.cancel()
        _uiState.update { state ->
            state.copy(
                progress = state.progress.copy(
                    isRunning = false,
                    statusText = "识别任务已取消。"
                )
            )
        }
    }

    fun retryFailed(context: Context) {
        val failed = _uiState.value.failedImages
        if (failed.isNotEmpty()) {
            startRecognition(context, failed)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateKeywords(context: Context, imageId: String, newKeywords: List<String>) {
        viewModelScope.launch {
            repository.updateKeywords(context, imageId, newKeywords)
            val currentDetail = _uiState.value.selectedDetailItem
            if (currentDetail != null && currentDetail.first.id == imageId) {
                val updatedRes = currentDetail.second?.copy(keywords = newKeywords)
                _uiState.update { it.copy(selectedDetailItem = Pair(currentDetail.first, updatedRes)) }
            }
        }
    }
}

