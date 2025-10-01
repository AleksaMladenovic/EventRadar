package com.eventradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.eventradar.navigation.graphs.RootNavGraph
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.theme.EventRadarAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventRadarAppTheme {
                RootNavGraph()
            }
        }
    }
}

