package com.meridians.scribolt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.meridians.scribolt.ui.theme.ScriboltTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScriboltTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ScriboltApp()
                }
            }
        }
    }
}