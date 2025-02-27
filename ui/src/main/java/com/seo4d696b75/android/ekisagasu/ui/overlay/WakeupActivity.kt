package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.ui.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WakeupActivity : ComponentActivity() {

    @Inject
    lateinit var screenRepository: ScreenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        if (screenRepository.isScreenLocked) {
            // SecureなLockScreenが存在する場合
            // ユーザ操作でないと解除できない 適当な画面を表示し続けて解除を促す
            window.setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
            setContent {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { finish() },
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(100.dp),
                    )
                }
            }
        } else {
            // SecureなLockScreenが無い場合
            // このActivityで画面点灯＆直前のアプリを表示できたので破棄
            finish()
        }
    }
}
