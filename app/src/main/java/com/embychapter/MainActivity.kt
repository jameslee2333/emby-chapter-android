package com.embychapter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.embychapter.ui.navigation.AppNavigation
import com.embychapter.ui.theme.EmbyChapterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmbyChapterTheme {
                AppNavigation()
            }
        }
    }
}
