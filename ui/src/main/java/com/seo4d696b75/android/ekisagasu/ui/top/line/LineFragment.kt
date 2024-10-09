package com.seo4d696b75.android.ekisagasu.ui.top.line

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
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.StationAdapter
import com.seo4d696b75.android.ekisagasu.ui.databinding.FragmentLineBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/19.
 */
@AndroidEntryPoint
class LineFragment : Fragment() {
    private val viewModel: LineViewModel by viewModels()

    private lateinit var binding: FragmentLineBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLineBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.stations
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .filter { it.isNotEmpty() }
            .onEach {
                val adapter = StationAdapter(requireContext(), it)
                binding.listLineDetailStations.adapter = adapter
                binding.listLineDetailStations.setOnItemClickListener { _, _, position, _ ->
                    val station = adapter.getItem(position)!!.station
                    val action = LineFragmentDirections.gotoStationScreen(station.code)
                    findNavController().navigate(action)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.event
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is LineFragmentEvent.CloseDetail -> {
                        findNavController().navigate(R.id.back_radar_screen)
                    }

                    is LineFragmentEvent.ShowMap -> {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.map_url) + "?line=${viewModel.arg.lineCode}"),
                            )
                        startActivity(intent)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
