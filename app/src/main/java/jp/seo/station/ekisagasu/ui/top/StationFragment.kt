package jp.seo.station.ekisagasu.ui.top

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentStationBinding
import jp.seo.station.ekisagasu.ui.common.LineAdapter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
@AndroidEntryPoint
class StationFragment : Fragment() {

    private val viewModel: StationViewModel by viewModels()

    private val stationCode: Int by lazy {
        requireArguments().getInt("stationCode")
    }

    private lateinit var binding: FragmentStationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_station,
            container,
            false
        )
        viewModel.setUiState(stationCode)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        LineAdapter(context, viewModel.lines).let {
            binding.listStationDetailLines.adapter = it
            binding.listStationDetailLines.setOnItemClickListener { _, _, position, _ ->
                val line = it.getItem(position)!!
                val action = LineFragmentDirections.actionGlobalLineFragment(line.code)
                findNavController().navigate(action)
            }
        }

        viewModel.event
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is StationFragmentEvent.CloseDetail -> {
                        findNavController().navigate(R.id.action_global_to_radarFragment)
                    }
                    is StationFragmentEvent.ShowMap -> {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.map_url) + "?station=${viewModel.station?.id}")
                        )
                        startActivity(intent)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
