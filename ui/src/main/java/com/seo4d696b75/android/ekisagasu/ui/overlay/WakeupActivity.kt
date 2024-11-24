package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.ui.databinding.ActivityWakeupBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WakeupActivity : AppCompatActivity() {

    @Inject
    lateinit var screenRepository: ScreenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (screenRepository.isScreenLocked) {
            // SecureなLockScreenが存在する場合
            // ユーザ操作でないと解除できない 適当な画面を表示し続けて解除を促す
            window.setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
            val binding = ActivityWakeupBinding.inflate(layoutInflater)
            binding.containerWakeup.setOnClickListener {
                finish()
            }
            setContentView(binding.root)
        } else {
            // SecureなLockScreenが無い場合
            // このActivityで画面点灯＆直前のアプリを表示できたので破棄
            finish()
        }
    }
}
