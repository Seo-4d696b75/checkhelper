package com.seo4d696b75.android.ekisagasu.ui.utils

/**
 * メートル単位の距離を文字列表現に変換
 */
val Float.formatDistance: String
    get() =
        if (this < 1000.0) {
            String.format("%.0fm", this)
        } else if (this < 10000.0) {
            String.format("%.2fkm", this / 1000.0)
        } else if (this < 100000.0) {
            String.format("%.1fkm", this / 1000.0)
        } else {
            String.format("%.0fkm", this / 1000.0)
        }
