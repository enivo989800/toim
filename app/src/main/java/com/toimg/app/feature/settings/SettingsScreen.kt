package com.toimg.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import android.widget.Toast

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toimg.app.domain.model.AiConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadConfig(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "AI 视觉服务配置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "配置用于图片识别与分类整理的视觉 AI 模型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Provider Type Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. AI 服务接入模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.providerType == AiConfig.PROVIDER_GEMINI,
                        onClick = { viewModel.updateProviderType(AiConfig.PROVIDER_GEMINI) },
                        label = { Text("Google Gemini 官方 REST") },
                        leadingIcon = {
                            if (uiState.providerType == AiConfig.PROVIDER_GEMINI) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("provider_gemini_chip")
                    )

                    FilterChip(
                        selected = uiState.providerType == AiConfig.PROVIDER_OPENAI,
                        onClick = { viewModel.updateProviderType(AiConfig.PROVIDER_OPENAI) },
                        label = { Text("OpenAI 兼容代理") },
                        leadingIcon = {
                            if (uiState.providerType == AiConfig.PROVIDER_OPENAI) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("provider_openai_chip")
                    )
                }
            }
        }

        // Model Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. 选择视觉模型 (Model)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetModels = if (uiState.providerType == AiConfig.PROVIDER_GEMINI) {
                        listOf("gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.0-flash")
                    } else {
                        listOf("gpt-4o-mini", "gpt-4o", "claude-3-5-sonnet")
                    }

                    presetModels.forEach { model ->
                        FilterChip(
                            selected = uiState.modelName == model,
                            onClick = { viewModel.updateModelName(model) },
                            label = { Text(model) },
                            modifier = Modifier.testTag("model_chip_$model")
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.modelName,
                    onValueChange = { viewModel.updateModelName(it) },
                    label = { Text("自定义模型标识") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("model_input")
                )
            }
        }

        // Endpoint & Key Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. 密钥与 Base URL 端点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("输入您的 API Key") },
                    visualTransformation = if (uiState.isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { viewModel.toggleKeyVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "切换可见性"
                                )
                            }
                            if (uiState.apiKey.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearApiKey(context) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Text(
                    text = "当前掩码: ${uiState.maskedKeyDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = uiState.baseUrl,
                    onValueChange = { viewModel.updateBaseUrl(it) },
                    label = { Text("Base URL 端点") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_url_input")
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.testConnectivity(context) },
                enabled = !uiState.isTesting && uiState.apiKey.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("test_connection_button")
            ) {
                if (uiState.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("连接测试中...")
                } else {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("测试服务连接")
                }
            }

            Button(
                onClick = { viewModel.saveSettings(context) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存配置")
            }
        }

        AnimatedVisibility(visible = uiState.saveMessage != null) {
            uiState.saveMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Test Result Card
        uiState.testResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isSuccess) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (result.isSuccess) "连接成功 (HTTP ${result.statusCode})" else "连接失败",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${result.latencyMs} ms",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (result.isSuccess) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Text("支持图像输入", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Text("JSON 结构化驱动", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Text("模型: ${result.modelName}", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    if (result.rawResponse.isNotBlank()) {
                        Text(
                            text = "响应片段: ${result.rawResponse}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Section 4: Smart Rules Engine
        var showAddRuleDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "4. 智能归档规则引擎",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddRuleDialog = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增规则", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "开启规则后，系统根据图片识别关键词自动匹配归档文件夹，实现自动化规整。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                uiState.smartRules.forEach { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = rule.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(text = "➔ ${rule.targetFolderName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(text = "触发词: ${rule.keywordTrigger}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { isChecked ->
                                        viewModel.toggleRule(context, rule.id, isChecked)
                                    }
                                )
                                IconButton(onClick = { viewModel.deleteRule(context, rule.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除规则", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: User Guide, Audit Logs & App Info
        var showGuide by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var showHistory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "5. 指引、安全审计与版本",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGuide = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("使用指南与概念", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showHistory = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("移动操作审计历史", fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ToImg 移动智能整理 v0.2.0 (全功能版)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("已完成 A(撤回)、B(规则)、C(OCR与敏感图)、D(标签编辑与合并)、2(后台断点续传)", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("核心引擎: Room 状态机 + 双 SHA-256 核验 + 自动归档引擎", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        if (showAddRuleDialog) {
            AddRuleDialog(
                onDismiss = { showAddRuleDialog = false },
                onConfirm = { name, triggers, folder ->
                    viewModel.addRule(context, name, triggers, folder)
                    showAddRuleDialog = false
                    Toast.makeText(context, "已成功添加智能规则：$name", Toast.LENGTH_SHORT).show()
                }
            )
        }


        if (showGuide) {
            com.toimg.app.core.ui.components.OnboardingDialog(
                onDismiss = { showGuide = false }
            )
        }

        if (showHistory) {
            com.toimg.app.core.ui.components.MoveHistoryDialog(
                context = context,
                onDismiss = { showHistory = false }
            )
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, triggers: String, folder: String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var triggers by remember { mutableStateOf("") }
    var targetFolder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "新增智能归档规则", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("规则名称 (例如：工作票据归档)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = triggers,
                    onValueChange = { triggers = it },
                    label = { Text("触发关键词 (逗号分隔)") },
                    placeholder = { Text("发票, 收据, 账单, 报销") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetFolder,
                    onValueChange = { targetFolder = it },
                    label = { Text("归档目标文件夹名") },
                    placeholder = { Text("Pictures/财务票据") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ruleName.isNotBlank() && triggers.isNotBlank() && targetFolder.isNotBlank()) {
                        onConfirm(ruleName.trim(), triggers.trim(), targetFolder.trim())
                    }
                },
                enabled = ruleName.isNotBlank() && triggers.isNotBlank() && targetFolder.isNotBlank()
            ) {
                Text("保存规则")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

