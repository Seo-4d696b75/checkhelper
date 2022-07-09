package jp.seo.station.ekisagasu.ui.station

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.databinding.FragmentStationBinding
import jp.seo.station.ekisagasu.ui.AppFragment
import jp.seo.station.ekisagasu.ui.common.LineAdapter
import jp.seo.station.ekisagasu.ui.common.StationNameView
import jp.seo.station.ekisagasu.utils.parseColorCode
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
@AndroidEntryPoint
class StationFragment : AppFragment() {

    private val viewModel: StationViewModel by viewModels()

    private lateinit var binding: FragmentStationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_station,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->
            val name = view.findViewById<StationNameView>(R.id.station_name_detail)
            val location = view.findViewById<TextView>(R.id.text_station_detail_location)
            val prefecture =
                view.findViewById<TextView>(R.id.text_station_detail_prefecture)
            val lines = view.findViewById<ListView>(R.id.list_station_detail_lines)

            mainViewModel.stationInDetail.observe(viewLifecycleOwner) {
                it?.let { station ->
                    name.setStation(station)
                    location.text = String.format("E%.6f N%.6f", station.lng, station.lat)
                    prefecture.text = prefectureRepository.getName(station.prefecture)
                }
            }
            mainViewModel.linesInDetail.observe(viewLifecycleOwner) {
                lines.adapter = LineAdapter(ctx, it)
            }

            view.findViewById<View>(R.id.button_station_detail_delete).setOnClickListener {
                findNavController().navigate(R.id.action_global_to_radarFragment)
            }
            view.findViewById<View>(R.id.button_station_detail_show_map)
                .setOnClickListener {
                    //TODO
                }
            lines.setOnItemClickListener { parent, view, position, id ->
                mainViewModel.showLineInDetail(position)
                findNavController().navigate(R.id.action_global_lineFragment)
            }
        }
    }


}
