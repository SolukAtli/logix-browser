package com.logix.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.logix.browser.chromiumbridge.ContentViewHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Faz-1 shell: hosts the temporary [ContentViewHost]. The omnibox/tab UI
 * (Faz-2) will drive navigation through `ChromiumEngineManager`.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContentViewHost(
                    url = START_URL,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    companion object {
        const val START_URL = "https://www.google.com"
    }
}
