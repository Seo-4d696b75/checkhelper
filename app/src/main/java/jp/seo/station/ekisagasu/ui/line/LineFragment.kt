package jp.seo.station.ekisagasu.ui.line

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
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentLineBinding
import jp.seo.station.ekisagasu.ui.common.StationAdapter
import jp.seo.station.ekisagasu.ui.station.StationFragmentDirections
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/19.
 */
class LineFragment : Fragment() {

    private val viewModel: LineViewModel by viewModels()

    private lateinit var binding: FragmentLineBinding

    private val line: Line by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_line,
            container,
            false,
        )
        viewModel.setUiState(line)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        StationAdapter(requireContext(), viewModel.stations).let {
            binding.listLineDetailStations.adapter = it
            binding.listLineDetailStations.setOnItemClickListener { _, _, position, _ ->
                val station = it.getItem(position)!!.station
                val action = StationFragmentDirections.actionGlobalStationFragment()
                findNavController().navigate(action)
            }
        }
        viewModel.event
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is LineFragmentEvent.CloseDetail -> {
                        findNavController().navigate(R.id.action_global_to_radarFragment)
                    }
                    is LineFragmentEvent.ShowMap -> {
                        // TODO
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

    }
}