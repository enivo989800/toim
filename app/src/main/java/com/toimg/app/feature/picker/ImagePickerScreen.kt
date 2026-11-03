package com.toimg.app.feature.picker

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
fun ImagePickerScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("选择需要处理的图片", style = MaterialTheme.typography.headlineSmall)
        PlaceholderPanel(
            title = "图片来源区域（待接入）",
            description = "后续阶段在这里接入 Android 系统照片选择器，并展示已选图片。此版本不会申请媒体权限。",
        )
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("进入工作区预览")
        }
    }
}
