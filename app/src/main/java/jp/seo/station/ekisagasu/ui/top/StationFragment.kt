package jp.seo.station.ekisagasu.ui.top

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentStationBinding
import jp.seo.station.ekisagasu.ui.common.LineAdapter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
@AndroidEntryPoint
class StationFragment : Fragment() {
    private val viewModel: StationViewModel by viewModels()

    private lateinit var binding: FragmentStationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        val context = requireContext()

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.lines
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .filter { it.isNotEmpty() }
            .onEach {
                val adapter = LineAdapter(context, it)
                binding.listStationDetailLines.adapter = adapter
                binding.listStationDetailLines.setOnItemClickListener { _, _, position, _ ->
                    val line = adapter.getItem(position)!!
                    val action = StationFragmentDirections.actionStationFragmentToLineFragment(line.code)
                    findNavController().navigate(action)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.event
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is StationFragmentEvent.CloseDetail -> {
                        findNavController().navigate(R.id.action_global_to_radarFragment)
                    }
                    is StationFragmentEvent.ShowMap -> {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.map_url) + "?station=${viewModel.arg.stationCode}"),
                            )
                        startActivity(intent)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
