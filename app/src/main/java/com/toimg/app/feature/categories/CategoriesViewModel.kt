package com.toimg.app.feature.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toimg.app.core.util.TagNormalizeUtil
import com.toimg.app.data.repository.LocalImageRepositoryImpl
import com.toimg.app.domain.model.CategoryItem
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.RecognitionResult
import com.toimg.app.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val images: List<ImageItem> = emptyList(),
    val recognitions: Map<String, RecognitionResult> = emptyMap(),
    val categories: List<CategoryItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: CategoryItem? = null,
    val selectedImageIds: Set<String> = emptySet(),
    val searchResults: List<ImageItem> = emptyList()
) {
    val imageMap: Map<String, ImageItem>
        get() = images.associateBy { it.id }

    val categoryDetailImages: List<ImageItem>
        get() {
            if (selectedCategory == null) return emptyList()
            return selectedCategory.imageIds.mapNotNull { imageMap[it] }
        }
}

class CategoriesViewModel(
    private val repository: ImageRepository = LocalImageRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        viewModelScope.launch {
            combine(
                repository.observeImagesFromDb(context),
                repository.observeRecognitions(context)
            ) { images, recognitions ->
                val recMap = recognitions.associateBy { it.imageId }
                val categories = TagNormalizeUtil.buildCategories(images, recMap)
                Pair(images, Pair(recMap, categories))
            }.collect { (images, pair) ->
                val recMap = pair.first
                val categories = pair.second
                _uiState.update { state ->
                    val updatedState = state.copy(
                        images = images,
                        recognitions = recMap,
                        categories = categories
                    )
                    // Re-calculate search if active query
                    val searchList = performSearch(state.searchQuery, images, recMap)
                    updatedState.copy(searchResults = searchList)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val list = performSearch(query, state.images, state.recognitions)
            state.copy(
                searchQuery = query,
                searchResults = list
            )
        }
    }

    fun selectCategory(category: CategoryItem?) {
        _uiState.update { it.copy(selectedCategory = category, selectedImageIds = emptySet()) }
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

    fun selectAllInDetail() {
        _uiState.update { state ->
            val ids = state.categoryDetailImages.map { it.id }.toSet()
            state.copy(selectedImageIds = ids)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedImageIds = emptySet()) }
    }

    fun mergeCategories(context: Context, sourceCategory: String, targetCategory: String) {
        viewModelScope.launch {
            repository.mergeCategory(context, sourceCategory, targetCategory)
            // After merging, reset selected category or re-select target
            _uiState.update { it.copy(selectedCategory = null) }
        }
    }

    private fun performSearch(
        query: String,
        images: List<ImageItem>,
        recognitions: Map<String, RecognitionResult>
    ): List<ImageItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        return images.filter { item ->
            val rec = recognitions[item.id]
            val kwMatch = rec?.keywords?.any { it.lowercase().contains(q) } == true
            val descMatch = rec?.description?.lowercase()?.contains(q) == true
            val ocrMatch = rec?.ocrText?.lowercase()?.contains(q) == true
            val nameMatch = item.name.lowercase().contains(q)
            val folderMatch = item.folderName.lowercase().contains(q)

            kwMatch || descMatch || ocrMatch || nameMatch || folderMatch
        }
    }
}

