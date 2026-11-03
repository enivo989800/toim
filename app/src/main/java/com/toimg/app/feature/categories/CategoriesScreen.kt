package com.toimg.app.feature.categories

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.toimg.app.domain.model.CategoryItem
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.RecognitionResult

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onGoToWorkspaceOrResult: () -> Unit,
    viewModel: CategoriesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showMergeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("搜索关键词、描述或路径...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空搜索")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("categories_search_input")
        )

        // Logic View Notice
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "逻辑分类说明：分类由 AI 关键词自动聚合。同一个图片可归属多个标签，原文件物理位置保持不变。",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Active Search Results
        if (uiState.searchQuery.isNotBlank()) {
            Text(
                text = "搜索结果 (${uiState.searchResults.size} 张)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到匹配 '${uiState.searchQuery}' 的图片", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.searchResults, key = { it.id }) { item ->
                        val rec = uiState.recognitions[item.id]
                        SearchResultCard(item = item, recognition = rec, context = context)
                    }
                }
            }
        } else if (uiState.selectedCategory != null) {
            // Category Detail View
            val category = uiState.selectedCategory!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.selectCategory(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回分类")
                    }
                    Text(
                        text = "${category.displayName} (${category.imageCount}张)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showMergeDialog = true }) {
                        Text("🔀 合并", fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.selectAllInDetail() }) {
                        Text("全选", fontSize = 12.sp)
                    }
                    if (uiState.selectedImageIds.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("清空", fontSize = 12.sp)
                        }
                    }
                }

            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.categoryDetailImages, key = { it.id }) { item ->
                    val isSelected = uiState.selectedImageIds.contains(item.id)
                    val rec = uiState.recognitions[item.id]
                    CategoryDetailTile(
                        item = item,
                        recognition = rec,
                        isSelected = isSelected,
                        onToggleSelect = { viewModel.toggleSelection(item.id) },
                        context = context
                    )
                }
            }

            Button(
                onClick = onGoToWorkspaceOrResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("organize_category_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (uiState.selectedImageIds.isNotEmpty()) {
                        "整理分类选中的 ${uiState.selectedImageIds.size} 张图片 ➔"
                    } else {
                        "前往工作区整理此分类 ➔"
                    }
                )
            }
        } else {
            // Main Category Cards Grid
            Text(
                text = "AI 聚合分类 (${uiState.categories.size} 个标签)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无分类结果，请先进行图片识别。", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.categories, key = { it.tag }) { category ->
                        CategoryCardItem(
                            category = category,
                            onClick = { viewModel.selectCategory(category) },
                            context = context
                        )
                    }
                }
            }
        }
    }

    if (showMergeDialog && uiState.selectedCategory != null) {
        val currentCategory = uiState.selectedCategory!!
        val otherCategories = uiState.categories.filter { it.tag != currentCategory.tag }
        MergeCategoryDialog(
            currentTag = currentCategory.tag,
            otherCategories = otherCategories,
            onDismiss = { showMergeDialog = false },
            onConfirmMerge = { targetTag ->
                viewModel.mergeCategories(context, currentCategory.tag, targetTag)
                showMergeDialog = false
                Toast.makeText(context, "已成功将 #${currentCategory.tag} 合并至 #$targetTag", Toast.LENGTH_SHORT).show()
            }
        )
    }
}


@Composable
fun CategoryCardItem(
    category: CategoryItem,
    onClick: () -> Unit,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("category_card_${category.tag}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (category.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(category.coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = category.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                // Count Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${category.imageCount} 张",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDetailTile(
    item: ImageItem,
    recognition: RecognitionResult?,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggleSelect() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onToggleSelect,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "未选择", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                }

                // Source Folder Badge Bottom Left
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(item.folderName, color = Color.White, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }

            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (recognition?.description != null) {
                    Text(text = recognition.description, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    item: ImageItem,
    recognition: RecognitionResult?,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("目录: ${item.folderName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                if (recognition?.keywordsSummary?.isNotBlank() == true) {
                    Text("#${recognition.keywordsSummary}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MergeCategoryDialog(
    currentTag: String,
    otherCategories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirmMerge: (String) -> Unit
) {
    var customTargetTag by remember { mutableStateOf("") }
    var selectedExistingTag by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "合并分类标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "将当前分类【#$currentTag】下的所有图片合并到另一个分类：",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (otherCategories.isNotEmpty()) {
                    Text(
                        text = "选择已有分类：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        otherCategories.take(12).forEach { cat ->
                            val isChosen = selectedExistingTag == cat.tag
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    selectedExistingTag = if (isChosen) null else cat.tag
                                    if (selectedExistingTag != null) customTargetTag = ""
                                },
                                label = { Text("#${cat.tag} (${cat.imageCount})", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text(
                    text = "或输入新目标分类名：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = customTargetTag,
                    onValueChange = {
                        customTargetTag = it
                        if (it.isNotBlank()) selectedExistingTag = null
                    },
                    placeholder = { Text("例如：工作票据", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val finalTarget = selectedExistingTag ?: customTargetTag.trim()
            Button(
                onClick = {
                    if (finalTarget.isNotBlank()) {
                        onConfirmMerge(finalTarget)
                    }
                },
                enabled = finalTarget.isNotBlank() && finalTarget != currentTag
            ) {
                Text("确认合并")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

