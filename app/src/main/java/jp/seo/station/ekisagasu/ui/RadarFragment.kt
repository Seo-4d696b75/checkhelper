package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NearStation
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.viewmodel.MainViewModel

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
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
            getService { service ->
                val viewModel = MainViewModel.getFactory(service).create(MainViewModel::class.java)
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
                viewModel.radarList.observe(viewLifecycleOwner) {
                    adapter.data = it
                }
                adapter.setOnItemSelectedListener { view, data, pos ->
                    viewModel.showStationInDetail(data.station)
                    findNavController().navigate(R.id.action_radarFragment_to_stationFragment)
                }
                val radarNum = view.findViewById<TextView>(R.id.text_radar_num)
                viewModel.radarNum.observe(viewLifecycleOwner) {
                    radarNum.text = String.format("x%d", it)
                }
            }
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
