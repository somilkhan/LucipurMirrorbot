package com.kyant.backdrop.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.player.PlayerScreen
import com.kyant.backdrop.catalog.player.PlayerStyle

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PlayerScreen(rawResId = R.raw.sample_bw, style = PlayerStyle.Glass)
        }
    }
}
