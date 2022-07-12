package jp.seo.station.ekisagasu.ui.top

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NearStation
import jp.seo.station.ekisagasu.databinding.FragmentRadarBinding
import jp.seo.station.ekisagasu.search.formatDistance
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class RadarFragment : Fragment() {

    private val viewModel: RadarViewModel by viewModels()

    private lateinit var binding: FragmentRadarBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_radar,
            container,
            false,
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val adapter = StationAdapter(context).apply {
            setOnItemSelectedListener { _, data, _ ->
                val action = StationFragmentDirections.actionGlobalStationFragment(data.station)
                findNavController().navigate(action)
            }
        }
        binding.listRadar.also {
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            it.layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            it.adapter = adapter
        }

        viewModel.radarList.observe(viewLifecycleOwner) {
            adapter.data = it
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
