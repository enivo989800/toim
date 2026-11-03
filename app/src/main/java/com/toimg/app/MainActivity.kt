package com.toimg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toimg.app.core.ui.theme.ToImgTheme
import com.toimg.app.navigation.ToImgNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToImgTheme {
                ToImgNavHost()
            }
        }
    }
}
