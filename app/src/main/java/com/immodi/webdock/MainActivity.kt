package com.immodi.webdock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.immodi.webdock.ui.home.HomeScreen
import com.immodi.webdock.ui.theme.WebDockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WebDockTheme {
                HomeScreen()
            }
        }
    }
}
