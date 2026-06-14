package com.constructiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.constructiontracker.ui.navigation.AppNavigation
import com.constructiontracker.ui.theme.ConstructionTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConstructionTrackerTheme {
                AppNavigation()
            }
        }
    }
}
