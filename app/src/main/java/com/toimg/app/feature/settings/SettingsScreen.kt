package com.toimg.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toimg.app.core.ui.components.PlaceholderPanel

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("应用设置", style = MaterialTheme.typography.headlineSmall)
        PlaceholderPanel(
            title = "设置模块占位",
            description = "后续根据产品规划加入偏好设置、权限说明、帮助信息和版本信息。",
        )
    }
}
