package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NearStation
import jp.seo.station.ekisagasu.search.formatDistance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class RadarFragment : AppFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_radar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->
            val list = view.findViewById<RecyclerView>(R.id.list_radar)
            list.addItemDecoration(
                DividerItemDecoration(
                    ctx,
                    LinearLayoutManager.VERTICAL
                )
            )
            list.layoutManager = LinearLayoutManager(ctx).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            val adapter = StationAdapter(ctx)
            list.adapter = adapter
            mainViewModel.radarList.observe(viewLifecycleOwner) {
                adapter.data = it
            }
            adapter.setOnItemSelectedListener { view, data, pos ->
                mainViewModel.showStationInDetail(data.station)
                findNavController().navigate(R.id.action_global_stationFragment)
            }
            val radarNum = view.findViewById<TextView>(R.id.text_radar_num)
            mainViewModel.radarNum
                .flowWithLifecycle(lifecycle)
                .onEach {
                    radarNum.text = String.format("x%d", it)
                }
                .launchIn(lifecycleScope)
        }
    }

    private class StationAdapter(context: Context) :
        HorizontalListView.ArrayAdapter<NearStation>() {

        private val inflater = LayoutInflater.from(context)

        override fun getView(group: ViewGroup): View {
            return inflater.inflate(R.layout.cell_station_radar, group, false)
        }

        override fun onBindView(view: View, data: NearStation, position: Int) {
            view.findViewById<TextView>(R.id.text_station_cell_index).text =
                (position + 1).toString()
            view.findViewById<TextView>(R.id.text_cell_station_distance).text =
                formatDistance(data.distance)
            view.findViewById<TextView>(R.id.text_cell_station_name).text = data.station.name
            view.findViewById<TextView>(R.id.text_cell_station_lines).text = data.getLinesName()
        }

    }
}
