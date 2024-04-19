package com.seo4d696b75.android.ekisagasu.ui.top.radar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.seo4d696b75.android.ekisagasu.ui.databinding.FragmentRadarBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRadarBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        val context = requireContext()
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = RadarAdapter(context).apply {
            onItemClickListener = { data ->
                val action = RadarFragmentDirections.gotoStationScreen(data.station.code)
                findNavController().navigate(action)
            }
            setHasStableIds(true)
        }
        binding.listRadar.also {
            it.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL,
                ),
            )
            it.layoutManager =
                LinearLayoutManager(context).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
            it.adapter = adapter
        }

        viewModel.radarList
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { adapter.submitList(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
