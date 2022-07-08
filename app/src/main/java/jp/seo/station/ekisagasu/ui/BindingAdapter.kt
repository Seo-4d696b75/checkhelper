package jp.seo.station.ekisagasu.ui

import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NearStation
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
fun setNearestStationName(view: StationNameView, s: NearStation?) {
    s?.let {
        view.setStation(s.station)
    }
}
