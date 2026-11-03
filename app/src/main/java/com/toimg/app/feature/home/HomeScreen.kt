package com.toimg.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toimg.app.core.ui.components.PlaceholderPanel

@Composable
fun HomeScreen(
    onSelectImages: () -> Unit,
    onOpenWorkspace: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "图片整理，从这里开始",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "先搭好完整流程，再逐步接入真实图片能力。",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        PlaceholderPanel(
            title = "第一阶段框架",
            description = "首页、选择图片、图片工作区、结果页和设置页已建立导航骨架。当前不读取、不修改设备中的图片。",
        )
        Button(
            onClick = onSelectImages,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始图片流程")
        }
        OutlinedButton(
            onClick = onOpenWorkspace,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("直接预览工作区")
        }
    }
}
