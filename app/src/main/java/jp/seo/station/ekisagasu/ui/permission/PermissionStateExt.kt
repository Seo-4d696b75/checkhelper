package jp.seo.station.ekisagasu.ui.permission

import android.app.Activity
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionState

/**
 * 権限の根拠を説明するUIを表示する必要があるか
 *
 * 初回リクエスト時（権限が一度も拒否されていない場合）のみ`false`
 */
val PermissionState.NotGranted.shouldShowRationale: Boolean
    get() = this.hasDenied

// TODO ユーザーが設定画面から直接（アプリ経由せず）操作する or「毎回確認する」を選択した場合に想定外の挙動になる
/**
 * システムの権限リクエストダイアログを表示できるか
 *
 * 権限を複数回拒否 or 「今後表示しない」を押下済みの場合に相当
 */
fun PermissionState.NotGranted.canShowSystemRequestDialog(activity: Activity): Boolean {
    // 権限が１度拒否された and (複数回拒否 or 「今後表示しない」押下されていない)
    val rationale = activity.shouldShowRequestPermissionRationale(permission)
    // １度も拒否されていない or １度だけ拒否された場合のみ表示できる
    return !hasDenied || rationale
}
