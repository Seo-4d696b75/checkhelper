package com.seo4d696b75.android.ekisagasu.ui.vibrator

import android.app.Service
import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

internal interface Vibration {
    operator fun invoke(pattern: LongArray)

    companion object {
        fun fromContext(context: Context): Vibration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            VibrationImpl(manager)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
            LegacyVibrationImpl(vibrator)
        }
    }
}

private class LegacyVibrationImpl(val vibrator: Vibrator) : Vibration {
    override operator fun invoke(pattern: LongArray) {
        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class VibrationImpl(val manager: VibratorManager) : Vibration {
    override operator fun invoke(pattern: LongArray) {
        val effect = VibrationEffect.createWaveform(pattern, -1)
        val vibration = CombinedVibration.createParallel(effect)
        val attr = VibrationAttributes
            .Builder()
            .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
            .build()
        manager.vibrate(vibration, attr)
    }
}
