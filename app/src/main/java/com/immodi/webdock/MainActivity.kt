package com.immodi.webdock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.immodi.webdock.ui.WebDockApp
import com.immodi.webdock.ui.theme.WebDockAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as App).container

        setContent {
            WebDockAppTheme(container) {
                WebDockApp(container)
            }
        }
    }
}
