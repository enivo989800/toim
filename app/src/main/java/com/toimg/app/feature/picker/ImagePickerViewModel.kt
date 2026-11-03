package com.toimg.app.feature.picker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toimg.app.data.repository.LocalImageRepositoryImpl
import com.toimg.app.domain.model.FolderItem
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.ScanStatus
import com.toimg.app.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImagePickerUiState(
    val images: List<ImageItem> = emptyList(),
    val folders: List<FolderItem> = emptyList(),
    val selectedFolder: String? = null,
    val selectedImageIds: Set<String> = emptySet(),
    val scanStatus: ScanStatus = ScanStatus.Idle,
    val permissionGranted: Boolean = false,
    val isScanning: Boolean = false,
    val totalDbCount: Int = 0,
    val hashedDbCount: Int = 0,
    val isHashing: Boolean = false,
    val hashProgress: String = "",
    val filterDuplicatesOnly: Boolean = false
) {
    val duplicateImageIds: Set<String>
        get() = images.filter { !it.sha256.isNullOrBlank() }
            .groupBy { it.sha256 }
            .filter { it.value.size > 1 }
            .flatMap { entry -> entry.value.map { it.id } }
            .toSet()

    val filteredImages: List<ImageItem>
        get() {
            var list = if (selectedFolder == null) images else images.filter { it.folderName == selectedFolder }
            if (filterDuplicatesOnly) {
                list = list.filter { duplicateImageIds.contains(it.id) }
            }
            return list
        }

    val selectedCount: Int
        get() = selectedImageIds.size
}

class ImagePickerViewModel(
    private val repository: ImageRepository = LocalImageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagePickerUiState())
    val uiState: StateFlow<ImagePickerUiState> = _uiState.asStateFlow()

    fun initDbObserver(context: Context) {
        viewModelScope.launch {
            repository.observeImagesFromDb(context).collect { dbImages ->
                val folders = extractFolders(dbImages, repository.getSavedFolders(context))
                _uiState.update { state ->
                    state.copy(
                        images = dbImages,
                        folders = folders
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observeDatabaseStats(context).collect { (total, hashed) ->
                _uiState.update { state ->
                    state.copy(
                        totalDbCount = total,
                        hashedDbCount = hashed
                    )
                }
            }
        }
    }

    fun updatePermissionStatus(granted: Boolean, context: Context) {
        _uiState.update { it.copy(permissionGranted = granted) }
        initDbObserver(context)
        if (granted && _uiState.value.images.isEmpty()) {
            startFullScan(context)
        }
    }

    fun startFullScan(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanStatus = ScanStatus.Scanning(0, "媒体库")) }
            repository.scanMediaStoreImages(context).collect { list ->
                val folders = extractFolders(list, repository.getSavedFolders(context))
                _uiState.update { state ->
                    state.copy(
                        images = list,
                        folders = folders,
                        isScanning = false,
                        scanStatus = ScanStatus.Success(list.size, folders.size)
                    )
                }
            }
            startHashComputation(context)
        }
    }

    fun startHashComputation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isHashing = true, hashProgress = "计算图片 Hash 指纹中...") }
            repository.computeMissingHashes(context) { current, total ->
                _uiState.update { it.copy(hashProgress = "已处理 $current / $total 张图片 Hash") }
            }
            _uiState.update { it.copy(isHashing = false, hashProgress = "Hash 计算完成") }
        }
    }

    fun addFolderUri(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val name = doc?.name ?: "SAF目录"
            repository.saveAuthorizedFolder(context, treeUri, name)

            _uiState.update { it.copy(isScanning = true, scanStatus = ScanStatus.Scanning(0, name)) }

            repository.scanFolderUri(context, treeUri).collect { scannedList ->
                val updatedImages = (_uiState.value.images + scannedList).distinctBy { it.id }
                val folders = extractFolders(updatedImages, repository.getSavedFolders(context))
                _uiState.update { state ->
                    state.copy(
                        images = updatedImages,
                        folders = folders,
                        isScanning = false,
                        scanStatus = ScanStatus.Success(updatedImages.size, folders.size)
                    )
                }
            }
            startHashComputation(context)
        }
    }

    fun addPickedUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newItems = uris.mapIndexed { index, uri ->
                ImageItem(
                    id = uri.toString(),
                    uri = uri,
                    name = "Picked_Image_${System.currentTimeMillis()}_$index.jpg",
                    path = uri.path ?: "",
                    size = 0L,
                    dateModified = System.currentTimeMillis() / 1000,
                    mimeType = "image/jpeg",
                    folderName = "直接选择",
                    folderPath = "PhotoPicker"
                )
            }
            val merged = (_uiState.value.images + newItems).distinctBy { it.id }
            val selected = _uiState.value.selectedImageIds + newItems.map { it.id }
            val folders = extractFolders(merged, repository.getSavedFolders(context))

            _uiState.update { state ->
                state.copy(
                    images = merged,
                    selectedImageIds = selected,
                    folders = folders,
                    scanStatus = ScanStatus.Success(merged.size, folders.size)
                )
            }
            startHashComputation(context)
        }
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

    fun toggleFilterDuplicatesOnly() {
        _uiState.update { it.copy(filterDuplicatesOnly = !it.filterDuplicatesOnly) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredImages.map { it.id }.toSet()
            state.copy(selectedImageIds = allIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedImageIds = emptySet()) }
    }

    fun setFilterFolder(folderName: String?) {
        _uiState.update { it.copy(selectedFolder = folderName) }
    }

    fun removeFolder(context: Context, folder: FolderItem) {
        repository.removeAuthorizedFolder(context, folder.id)
        loadSavedFolders(context)
    }

    private fun loadSavedFolders(context: Context) {
        val saved = repository.getSavedFolders(context)
        _uiState.update { it.copy(folders = saved) }
    }

    private fun extractFolders(images: List<ImageItem>, savedFolders: List<FolderItem>): List<FolderItem> {
        val counts = images.groupBy { it.folderName }.mapValues { it.value.size }
        val autoFolders = counts.map { (name, count) ->
            FolderItem(
                id = name,
                name = name,
                imageCount = count,
                isAuthorized = true
            )
        }
        val combined = (autoFolders + savedFolders).distinctBy { it.name }
        return combined
    }
}
