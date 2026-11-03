package com.toimg.app.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toimg.app.core.ui.components.PlaceholderPanel

@Composable
fun ImageWorkspaceScreen(onPreviewComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("图片移动工作区", style = MaterialTheme.typography.headlineSmall)
        PlaceholderPanel(
            title = "01 · 图片预览区",
            description = "后续显示用户选择的图片、缩略图和必要的图片信息。",
        )
        PlaceholderPanel(
            title = "02 · 目标位置区",
            description = "后续根据最终需求呈现目标位置或目标画布。",
        )
        PlaceholderPanel(
            title = "03 · 操作控制区",
            description = "后续接入移动、撤销、确认等真实操作。本阶段只保留界面位置。",
        )
        Text(
            text = "当前按钮仅用于验证页面跳转，不会执行图片移动。",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onPreviewComplete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("预览完成页面")
        }
    }
}
