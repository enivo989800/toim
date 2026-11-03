package com.toimg.app.feature.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toimg.app.core.ui.components.MoveHistoryDialog
import com.toimg.app.core.ui.components.OnboardingDialog
import com.toimg.app.core.util.BackgroundBatchManager
import com.toimg.app.data.local.ToImgDatabase
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    onSelectImages: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenCategories: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("toimg_app_prefs", Context.MODE_PRIVATE) }

    var showOnboarding by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var interruptedCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var showResumeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasSeen = prefs.getBoolean("has_seen_onboarding", false)
        if (!hasSeen) {
            showOnboarding = true
        }
        val count = BackgroundBatchManager.checkAndResumeInterrupted(context)
        if (count > 0) {
            interruptedCount = count
            showResumeDialog = true
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // App Title & Tagline
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ToImg 图片整理",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "本地索引 · AI 批量理解 · 逻辑归类 · 零数据丢失安全移动",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Quick Entry: Onboarding & History buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showOnboarding = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_onboarding_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("概念说明与指引", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showHistory = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_history_button")
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("操作与移动历史", fontSize = 12.sp)
            }
        }

        // Card 1: Scanning & Indexing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "1. 本地图片检索与扫描",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "零权限选择 / SAF 目录授权 / Room 索引去重",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "通过标准的 Android 零权限 Photo Picker 或授权扫描本地相册目录，存入 Room 数据库并计算 SHA-256 去重指纹。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onSelectImages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_picker_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("挑选/扫描本地图片")
                }
            }
        }

        // Card 2: AI Workspace
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "2. 批量 AI 识别工作区",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "串行批次调度 / 结构化 JSON 校验 / 智能理解卡片",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = "启动严格串行 AI 视觉识别，提取 3-10 个中文关键词与一句话描述，持久化绑定到图片元数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onOpenWorkspace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_workspace_button")
                ) {
                    Text("进入 AI 批量识别工作区")
                }
            }
        }

        // Card 3: Categories & Search
        if (onOpenCategories != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "3. 智能逻辑分类与搜索",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "多维标签聚类 / 模糊全文搜索 / 跨目录归组",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Text(
                        text = "基于 AI 关键词自动聚合标签分类网格，支持按标签、描述和路径全盘语义搜索（不改变任何原文件物理位置）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onOpenCategories,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_categories_button")
                    ) {
                        Text("查看 AI 智能分类与搜索")
                    }
                }
            }
        }
    }

    if (showOnboarding) {
        OnboardingDialog(
            onDismiss = {
                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                showOnboarding = false
            }
        )
    }

    if (showHistory) {
        MoveHistoryDialog(
            context = context,
            onDismiss = { showHistory = false }
        )
    }

    if (showResumeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = {
                Text(text = "发现未完成的后台任务", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "检测到上次会话有 $interruptedCount 张图片在后台 AI 识别中断。是否继续在后台恢复执行？")
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showResumeDialog = false
                        scope.launch {
                            val interrupted = ToImgDatabase.getInstance(context).imageDao().getImagesByStatus("PROCESSING")
                            if (interrupted.isNotEmpty()) {
                                BackgroundBatchManager.startBatchRecognition(context, interrupted)
                            }
                        }
                    }
                ) {
                    Text("继续后台识别")
                }
            },

            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showResumeDialog = false }
                ) {
                    Text("忽略")
                }
            }
        )
    }
}

