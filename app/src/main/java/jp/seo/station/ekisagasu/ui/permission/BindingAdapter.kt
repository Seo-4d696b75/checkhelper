package jp.seo.station.ekisagasu.ui.permission

import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.seo.station.ekisagasu.R

@BindingAdapter("permissionRationale")
fun setPermissionRationale(
    view: TextView,
    rationale: PermissionRationale?,
) {
    view.text = when (rationale) {
        null -> ""
        is PermissionRationale.LocationPermission -> view.context.getString(R.string.dialog_location_permission_rationale)
        is PermissionRationale.NotificationPermission -> view.context.getString(R.string.dialog_notification_permission_rationale)
        PermissionRationale.NotificationChannel -> view.context.getString(R.string.dialog_notification_permission_rationale)
        PermissionRationale.DrawOverlay -> view.context.getString(R.string.dialog_draw_overlay_rationale)
    }
}

@BindingAdapter("permissionRequest")
fun setPermissionRequestDescription(
    view: TextView,
    rationale: PermissionRationale?,
) {
    view.text = when (rationale) {
        null -> ""
        is PermissionRationale.LocationPermission -> if (rationale.showSystemRequestDialog) {
            view.context.getString(R.string.dialog_location_permission_request_dialog)
        } else {
            view.context.getText(R.string.dialog_location_permission_request_setting)
        }

        is PermissionRationale.NotificationPermission -> if (rationale.showSystemRequestDialog) {
            view.context.getString(R.string.dialog_notification_permission_request_dialog)
        } else {
            view.context.getString(R.string.dialog_notification_permission_request_setting)
        }

        PermissionRationale.NotificationChannel -> view.context.getString(R.string.dialog_notification_channel_request)
        PermissionRationale.DrawOverlay -> view.context.getString(R.string.dialog_draw_overlay_request)
    }
}
