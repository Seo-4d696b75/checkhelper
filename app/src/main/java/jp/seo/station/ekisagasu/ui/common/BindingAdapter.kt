package jp.seo.station.ekisagasu.ui.common

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.model.*
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType
import jp.seo.station.ekisagasu.ui.top.AnimationView
import jp.seo.station.ekisagasu.utils.*
import java.util.*

@BindingAdapter("searchRunning")
fun setSearchState(view: FloatingActionButton, running: Boolean) {
    view.setImageResource(
        if (running) R.drawable.ic_pause else R.drawable.ic_play
    )
}

@BindingAdapter("running")
fun setAnimationState(view: AnimationView, running: Boolean) {
    view.runAnimation(running)
}

@BindingAdapter("nearestStationName")
fun setNearestStationName(view: StationNameView, s: Station?) {
    s?.let {
        view.setStation(s)
    }
}

@BindingAdapter("stationDistance")
fun setNearestStationDistance(view: TextView, n: NearStation?) {
    view.text = n?.let { formatDistance(it.distance) } ?: ""
}

@BindingAdapter("stationName")
fun setStationName(view: TextView, n: NearStation?) {
    view.text = n?.station?.name
}

@BindingAdapter("linesName")
fun setLinesName(view: TextView, n: NearStation?) {
    view.text = n?.let { it.getLinesName() } ?: ""
}

@BindingAdapter("selectedLineName")
fun setSelectedLineName(view: TextView, line: Line?) {
    view.text = line?.name ?: view.context.getString(R.string.no_selected_line)
}

@BindingAdapter("stationLocation")
fun setStationLocation(view: TextView, s: Station?) {
    view.text = s?.let { String.format("E%.6f N%.6f", it.lng, it.lat) } ?: ""
}

@BindingAdapter("lineName")
fun setLineName(view: TextView, line: Line?) {
    view.text = line?.name ?: ""
}

@BindingAdapter("lineNameKana")
fun setLineNameKana(view: TextView, line: Line?) {
    view.text = line?.nameKana ?: ""
}

@BindingAdapter("lineSymbol")
fun setLineSymbol(view: TextView, line: Line?) {
    view.text = line?.symbol ?: ""
    view.background = GradientDrawable().apply {
        cornerRadius = 8f
        shape = GradientDrawable.RECTANGLE
        setColor(parseColorCode(line?.color))
    }
    view.setTextColor(
        line?.color?.let {
            if (getVFromColorCode(it) < 200) Color.WHITE else Color.BLACK
        } ?: Color.BLACK
    )
}

@BindingAdapter("lineSymbolColor")
fun setSimpleLineSymbol(view: TextView, line: Line?) {
    view.background = GradientDrawable().apply {
        cornerRadius = 4f
        shape = GradientDrawable.RECTANGLE
        setColor(parseColorCode(line?.color))
    }
}

@BindingAdapter("stationSize")
fun setStationSize(view: TextView, line: Line?) {
    view.text = line?.let {
        String.format("%d駅", it.stationSize)
    } ?: ""
}

@BindingAdapter("searchK")
fun setSearchK(view: TextView, k: Int?) {
    view.text = k?.let {
        String.format("x%d", it)
    } ?: ""
}

@BindingAdapter("dataUpdateProgress")
fun setDataUpdateProgress(view: TextView, progress: DataUpdateProgress?) {
    view.text = when (progress) {
        is DataUpdateProgress.Download -> {
            view.context.getString(R.string.update_state_download, progress.percent)
        }
        is DataUpdateProgress.Save -> {
            view.context.getString(R.string.update_state_save)
        }
        else -> ""
    }
}

@BindingAdapter("logTarget")
fun setLogFilter(view: TextView, log: LogTarget?) {
    view.text = log?.target?.let {
        String.format(
            "%s～%s",
            formatTime(TIME_PATTERN_DATETIME, it.start),
            formatTime(TIME_PATTERN_DATETIME, it.finish)
        )
    } ?: ""
}

@BindingAdapter("dataVersion")
fun setDataVersion(view: TextView, version: DataVersion?) {
    view.text = version?.let {
        view.context.getString(R.string.text_data_version, it.version)
    } ?: ""
}

@BindingAdapter("dataVersion")
fun setDataVersion(view: TextView, info: DataLatestInfo?) {
    view.text = info?.let {
        view.context.getString(R.string.text_data_version, it.version)
    } ?: ""
}

@BindingAdapter("dataSize")
fun setDataSize(view: TextView, info: DataLatestInfo?) {
    view.text = info?.let {
        view.context.getString(R.string.text_data_size, it.fileSize())
    } ?: ""
}

@BindingAdapter("dataUpdatedAt")
fun setDataUpdatedAt(view: TextView, version: DataVersion?) {
    view.text = version?.let {
        view.context.getString(
            R.string.text_data_updated_at,
            formatTime(TIME_PATTERN_DATETIME, it.timestamp)
        )
    } ?: ""
}

@BindingAdapter("confirmDataUpdate")
fun setConfirmDataUpdateMessage(view: TextView, type: DataUpdateType?) {
    view.text = when (type) {
        DataUpdateType.Init -> view.context.getString(R.string.dialog_message_init_data)
        DataUpdateType.Latest -> view.context.getString(R.string.dialog_message_latest_data)
        else -> ""
    }
}

@BindingAdapter("formatDateMilliSec")
fun setFormatDateMilliSec(view: TextView, date: Date?) {
    view.text = date?.let {
        formatTime(TIME_PATTERN_MILLI_SEC, it)
    } ?: ""
}

@BindingAdapter("formatDate")
fun setFormatDateTime(view: TextView, date: Date?) {
    view.text = date?.let {
        formatTime(TIME_PATTERN_DATETIME, it)
    } ?: ""
}

@BindingAdapter("formatAppRunningDuration")
fun setDuration(view: TextView, log: AppRebootLog?) {
    view.text = log?.finish?.let {
        val duration = ((it.time - log.start.time) / 1000L).toInt()
        formatTime(view.context, duration)
    } ?: ""
}

@BindingAdapter("visible")
fun setVisible(view: View, visible: Boolean?) {
    view.visibility = if (visible == true) View.VISIBLE else View.INVISIBLE
}
