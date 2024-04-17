package jp.seo.station.ekisagasu.ui.permission

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
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

@BindingAdapter("permissionRequestGuideImg")
fun setPermissionRequestGuideImg(
    view: ImageView,
    rationale: PermissionRationale?,
) {
    val drawable = when (rationale) {
        null -> null
        is PermissionRationale.LocationPermission -> if (rationale.showSystemRequestDialog) {
            null
        } else {
            ResourcesCompat.getDrawable(view.context.resources, R.drawable.location_permission, null)
        }

        is PermissionRationale.NotificationPermission -> if (rationale.showSystemRequestDialog) {
            null
        } else {
            ResourcesCompat.getDrawable(view.context.resources, R.drawable.notification_permission, null)
        }

        PermissionRationale.NotificationChannel -> ResourcesCompat.getDrawable(
            view.context.resources,
            R.drawable.notification_permission,
            null,
        )

        PermissionRationale.DrawOverlay -> ResourcesCompat.getDrawable(
            view.context.resources,
            R.drawable.draw_overlay_setting,
            null,
        )
    }
    view.setImageDrawable(drawable)
    view.visibility = if (drawable == null) View.GONE else View.VISIBLE
}

@BindingAdapter("permissionRequestGuideCaption")
fun setPermissionRequestGuideCaption(
    view: TextView,
    rationale: PermissionRationale?,
) {
    val visible = when (rationale) {
        null -> false
        is PermissionRationale.LocationPermission -> !rationale.showSystemRequestDialog
        is PermissionRationale.NotificationPermission -> !rationale.showSystemRequestDialog

        PermissionRationale.NotificationChannel -> true

        PermissionRationale.DrawOverlay -> true
    }
    if (visible) {
        view.setText(R.string.dialog_guide_image_caption)
        view.visibility = View.VISIBLE
    } else {
        view.visibility = View.GONE
    }
}
