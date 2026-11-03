package com.toimg.app.feature.result

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
fun MoveResultScreen(onBackHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("流程页面已连通", style = MaterialTheme.typography.headlineSmall)
        PlaceholderPanel(
            title = "结果展示区域（待接入）",
            description = "后续显示真实操作结果、成功或失败状态，以及必要的恢复入口。当前仅代表页面流程到达此处。",
        )
        Button(
            onClick = onBackHome,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("返回首页")
        }
    }
}
