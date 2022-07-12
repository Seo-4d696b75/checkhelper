package jp.seo.station.ekisagasu.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.model.NearStation
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.ui.common.StationNameView
import jp.seo.station.ekisagasu.ui.top.AnimationView
import jp.seo.station.ekisagasu.utils.getVFromColorCode
import jp.seo.station.ekisagasu.utils.parseColorCode

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

@BindingAdapter("searchK")
fun setSearchK(view: TextView, k: Int?) {
    view.text = k?.let {
        String.format("x%d", it)
    } ?: ""
}
