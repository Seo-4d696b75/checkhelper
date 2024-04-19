package com.seo4d696b75.android.ekisagasu.ui.top

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.LineNameAdapter
import com.seo4d696b75.android.ekisagasu.ui.databinding.FragmentTopBinding
import com.seo4d696b75.android.ekisagasu.ui.top.line.LineFragmentDirections
import com.seo4d696b75.android.ekisagasu.ui.top.line.LineSelectDialogDirections
import com.seo4d696b75.android.ekisagasu.ui.top.line.LineSelectType
import com.seo4d696b75.android.ekisagasu.ui.top.station.StationFragmentDirections
import com.seo4d696b75.android.ekisagasu.ui.utils.AnimationHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.LinkedList

/**
 * @author Seo-4d696b75
 * @version 2021/01/11.
 */
@AndroidEntryPoint
class TopFragment : Fragment() {
    private val viewModel: TopViewModel by viewModels()

    private lateinit var binding: FragmentTopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTopBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressWarnings("ClickableViewAccessibility")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        val ctx = requireContext()
        val navigationHost = binding.subNavHost

        // 駅の登録路線リスト
        val adapter = LineNameAdapter(ctx)
        binding.listLineNames.also {
            it.adapter = adapter
            it.setOnItemClickListener { _, position ->
                val line = adapter.getLine(position)
                val action = LineFragmentDirections.gotoLineScreenFromTop(line.code)
                navigationHost.findNavController().navigate(action)
            }
        }
        viewModel.nearestStation
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .filterNotNull()
            .onEach { adapter.submitList(it.lines) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // 探索が終了したらRadarFragmentに遷移する
        viewModel.isRunning
            .filter { !it }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                navigationHost.findNavController().navigate(R.id.back_radar_screen)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // クリックで駅詳細へ遷移する
        binding.stationNameMain.setOnClickListener {
            viewModel.nearestStation.value?.let { n ->
                val action = StationFragmentDirections.gotoStationScreenFromTop(n.station.code)
                navigationHost.findNavController().navigate(action)
            }
        }

        // animated floating buttons
        val res = ctx.resources
        val fabMenu =
            AnimationHolder<FloatingActionButton>(
                binding.fabMore,
                res.getDimensionPixelSize(R.dimen.fab_menu_x),
                res.getDimensionPixelSize(R.dimen.fab_menu_y),
            )
        val fabExit =
            AnimationHolder<FloatingActionButton>(
                binding.fabExit,
                res.getDimensionPixelSize(R.dimen.fab_exit_x),
                res.getDimensionPixelSize(R.dimen.fab_exit_y),
            )
        val fabSelectLine =
            AnimationHolder<FloatingActionButton>(
                binding.fabSelectLine,
                res.getDimensionPixelSize(R.dimen.fab_select_line_x),
                res.getDimensionPixelSize(R.dimen.fab_select_line_y),
            )
        val fabPredict =
            AnimationHolder<FloatingActionButton>(
                binding.fabPredict,
                res.getDimensionPixelSize(R.dimen.fab_predict_x),
                res.getDimensionPixelSize(R.dimen.fab_predict_y),
            )
        val fabTimer =
            AnimationHolder<FloatingActionButton>(
                binding.fabTimer,
                res.getDimensionPixelSize(R.dimen.fab_timer_x),
                res.getDimensionPixelSize(R.dimen.fab_timer_y),
            )
        val fabMap =
            AnimationHolder<FloatingActionButton>(
                binding.fabMap,
                res.getDimensionPixelSize(R.dimen.fab_map_x),
                res.getDimensionPixelSize(R.dimen.fab_map_y),
            )
        val animateFab: (Boolean) -> Unit = { expand ->
            if (fabMap.visibility != expand) {
                val list: MutableList<Animator> = LinkedList()
                list.add(fabMap.animate(expand))
                list.add(fabTimer.animate(expand))
                list.add(fabExit.animate(expand, false))
                list.add(fabMenu.animate(!expand, true))
                val running = viewModel.isRunning.value
                if ((expand && running) || (!expand && fabSelectLine.visibility)) {
                    list.add(
                        fabSelectLine.animate(
                            expand,
                        ),
                    )
                }
                if ((expand && running) || (!expand && fabPredict.visibility)) {
                    list.add(
                        fabPredict.animate(
                            expand,
                        ),
                    )
                }
                AnimatorSet().apply {
                    playTogether(list)
                    duration = 300L
                    addListener(
                        onStart = {
                            if (expand) {
                                fabMap.visibility = true
                                fabTimer.visibility = true
                                fabSelectLine.visibility = running
                                fabPredict.visibility = running
                            } else {
                                fabMenu.visibility = true
                            }
                        },
                        onEnd = {
                            if (!expand) {
                                fabMap.visibility = false
                                fabTimer.visibility = false
                                fabSelectLine.visibility = false
                                fabPredict.visibility = false
                            } else {
                                fabMenu.visibility = false
                            }
                        },
                    )
                    start()
                }
            }
        }

        // Event
        viewModel.event
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                when (it) {
                    is TopFragmentEvent.ToggleMenu -> animateFab(it.open)
                    is TopFragmentEvent.ShowMap -> {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.map_url)),
                            )
                        startActivity(intent)
                    }

                    is TopFragmentEvent.SelectLine -> {
                        val action = LineSelectDialogDirections.showLineSelectDialog(LineSelectType.Current)
                        view.findNavController().navigate(action)
                    }

                    is TopFragmentEvent.StartNavigation -> {
                        val action = LineSelectDialogDirections.showLineSelectDialog(LineSelectType.Navigation)
                        view.findNavController().navigate(action)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // 他の部分をタップしたらMenuを閉じる
        binding.fabContainer.setOnTouchListener { _, _ ->
            viewModel.closeMenu()
            false
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu_main, menu)
    }
}
