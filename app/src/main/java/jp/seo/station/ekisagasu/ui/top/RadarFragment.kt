package jp.seo.station.ekisagasu.ui.top

import android.content.Context
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seo4d696b75.android.ekisagasu.data.search.NearStation
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.CellStationRadarBinding
import jp.seo.station.ekisagasu.databinding.FragmentRadarBinding
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
                val action =
                    StationFragmentDirections.actionGlobalStationFragment(data.station.code)
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

    private class NearStationComparator : DiffUtil.ItemCallback<NearStation>() {
        override fun areItemsTheSame(
            oldItem: NearStation,
            newItem: NearStation,
        ): Boolean {
            return oldItem.station.id == newItem.station.id && oldItem.distance == newItem.distance
        }

        override fun areContentsTheSame(
            oldItem: NearStation,
            newItem: NearStation,
        ): Boolean {
            return oldItem == newItem
        }
    }

    private class RadarViewHolder(val binding: CellStationRadarBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class RadarAdapter(context: Context) : ListAdapter<NearStation, RadarViewHolder>(NearStationComparator()) {
        private val inflater = LayoutInflater.from(context)
        var onItemClickListener: ((NearStation) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RadarViewHolder {
            val binding =
                DataBindingUtil.inflate<CellStationRadarBinding>(
                    inflater,
                    R.layout.cell_station_radar,
                    parent,
                    false,
                )
            return RadarViewHolder(binding)
        }

        override fun onBindViewHolder(
            holder: RadarViewHolder,
            position: Int,
        ) {
            val near = getItem(position)
            holder.binding.index = (position + 1).toString()
            holder.binding.near = near
            holder.binding.root.setOnClickListener {
                onItemClickListener?.invoke(near)
            }
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).station.code.toLong()
        }
    }
}
