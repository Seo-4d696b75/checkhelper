package com.seo4d696b75.android.ekisagasu.data.log

import com.seo4d696b75.android.ekisagasu.data.database.AppRebootLog

data class LogTarget(val target: AppRebootLog?, val since: Long, val until: Long = Long.MAX_VALUE,)
