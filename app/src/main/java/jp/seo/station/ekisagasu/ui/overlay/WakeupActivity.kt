package jp.seo.station.ekisagasu.ui.overlay

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import jp.seo.station.ekisagasu.databinding.ActivityWakeupBinding

class WakeupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val locked = manager.isKeyguardLocked

        Log.d("Wakeup", "locked: $locked")
        if (locked) {
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
