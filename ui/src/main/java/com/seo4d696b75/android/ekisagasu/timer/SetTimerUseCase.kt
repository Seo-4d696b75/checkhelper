package com.seo4d696b75.android.ekisagasu.timer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import android.widget.Toast
import com.seo4d696b75.android.ekisagasu.ui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetTimerUseCase @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {
    private val timerDurationMillis = 5 * 60 * 1000L
    private var previousTimerTimestamp = -timerDurationMillis

    operator fun invoke() {
        val current = SystemClock.elapsedRealtime()
        if (current - previousTimerTimestamp < timerDurationMillis) {
            Toast.makeText(context, context.getString(R.string.timer_wait_message), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_MESSAGE, context.getString(R.string.timer_title))
            .putExtra(AlarmClock.EXTRA_LENGTH, 300)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            Toast.makeText(context, context.getString(R.string.timer_set_message), Toast.LENGTH_SHORT).show()
            previousTimerTimestamp = current
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "Failed to set timer")
        }
    }
}
