package jp.seo.station.ekisagasu.ui

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.NearStation
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.ui.common.StationNameView
import jp.seo.station.ekisagasu.ui.top.AnimationView

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
    n?.let {
        view.text = formatDistance(it.distance)
    }
}

@BindingAdapter("lineName")
fun setLineName(view: TextView, line: Line?) {
    view.text = line?.name ?: view.context.getString(R.string.no_selected_line)
}
