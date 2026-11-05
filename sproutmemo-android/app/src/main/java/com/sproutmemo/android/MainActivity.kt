package com.sproutmemo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.sproutmemo.android.ui.navigation.AppNavigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.ui.theme.SproutMemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val tokenManager = ApiClient.getTokenManager(applicationContext)
        com.sproutmemo.android.data.remote.NetworkProbeManager.init(applicationContext)

        setContent {
            val themeId by tokenManager.themeIdFlow.collectAsState(initial = 8)
            SproutMemoTheme(themeId = themeId) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.sproutmemo.android.data.remote.NetworkProbeManager.probeAndSelectBestUrl(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.sproutmemo.android.data.remote.NetworkProbeManager.stopMonitoring()
    }
}
