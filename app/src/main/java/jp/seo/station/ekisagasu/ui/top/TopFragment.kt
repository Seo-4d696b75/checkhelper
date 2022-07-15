package jp.seo.station.ekisagasu.ui.top

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentTopBinding
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.ui.dialog.LineDialogDirections
import jp.seo.station.ekisagasu.ui.dialog.LineDialogType
import jp.seo.station.ekisagasu.utils.AnimationHolder
import jp.seo.station.ekisagasu.utils.parseColorCode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2021/01/11.
 */
@AndroidEntryPoint
class TopFragment : Fragment() {

    private val viewModel: TopViewModel by viewModels()

    private lateinit var binding: FragmentTopBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_top,
            container,
            false,
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressWarnings("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        val ctx = requireContext()
        val navigationHost = binding.subNavHost

        // 駅の登録路線リスト
        val adapter = LineNamesAdapter(ctx).also {
            it.setOnItemSelectedListener { _, data, _ ->
                val action = LineFragmentDirections.actionGlobalLineFragment(data.code)
                navigationHost.findNavController().navigate(action)
            }
        }
        binding.listLineNames.adapter = adapter
        viewModel.nearestStation
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .filterNotNull()
            .onEach { adapter.data = it.lines }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // 探索が終了したらRadarFragmentに遷移する
        viewModel.isRunning
            .filter { !it }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach {
                navigationHost.findNavController()
                    .navigate(R.id.action_global_to_radarFragment)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        // クリックで駅詳細へ遷移する
        binding.stationNameMain.setOnClickListener {
            viewModel.nearestStation.value?.let { n ->
                val action = StationFragmentDirections.actionGlobalStationFragment(n.station.code)
                navigationHost.findNavController().navigate(action)
            }
        }

        // animated floating buttons
        val res = ctx.resources
        val fabMenu = AnimationHolder<FloatingActionButton>(
            binding.fabMore,
            res.getDimensionPixelSize(R.dimen.fab_menu_x),
            res.getDimensionPixelSize(R.dimen.fab_menu_y)
        )
        val fabExit = AnimationHolder<FloatingActionButton>(
            binding.fabExit,
            res.getDimensionPixelSize(R.dimen.fab_exit_x),
            res.getDimensionPixelSize(R.dimen.fab_exit_y)
        )
        val fabSelectLine = AnimationHolder<FloatingActionButton>(
            binding.fabSelectLine,
            res.getDimensionPixelSize(R.dimen.fab_select_line_x),
            res.getDimensionPixelSize(R.dimen.fab_select_line_y)
        )
        val fabPredict = AnimationHolder<FloatingActionButton>(
            binding.fabPredict,
            res.getDimensionPixelSize(R.dimen.fab_predict_x),
            res.getDimensionPixelSize(R.dimen.fab_predict_y)
        )
        val fabTimer = AnimationHolder<FloatingActionButton>(
            binding.fabTimer,
            res.getDimensionPixelSize(R.dimen.fab_timer_x),
            res.getDimensionPixelSize(R.dimen.fab_timer_y)
        )
        val fabFixTimer = AnimationHolder<FloatingActionButton>(
            binding.fabFixTimer,
            res.getDimensionPixelSize(R.dimen.fab_fix_timer_x),
            res.getDimensionPixelSize(R.dimen.fab_fix_timer_y)
        )
        val fabMap = AnimationHolder<FloatingActionButton>(
            binding.fabMap,
            res.getDimensionPixelSize(R.dimen.fab_map_x),
            res.getDimensionPixelSize(R.dimen.fab_map_y)
        )
        val animateFab: (Boolean) -> Unit = { expand ->
            if (fabMap.visibility != expand) {
                val list: MutableList<Animator> = LinkedList()
                list.add(fabMap.animate(expand))
                list.add(fabTimer.animate(expand))
                list.add(fabFixTimer.animate(expand))
                list.add(fabExit.animate(expand, false))
                list.add(fabMenu.animate(!expand, true))
                val running = viewModel.isRunning.value
                if ((expand && running) || (!expand && fabSelectLine.visibility)) list.add(
                    fabSelectLine.animate(
                        expand
                    )
                )
                if ((expand && running) || (!expand && fabPredict.visibility)) list.add(
                    fabPredict.animate(
                        expand
                    )
                )
                AnimatorSet().apply {
                    playTogether(list)
                    duration = 300L
                    addListener(
                        onStart = {
                            if (expand) {
                                fabMap.visibility = true
                                fabTimer.visibility = true
                                fabFixTimer.visibility = true
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
                                fabFixTimer.visibility = false
                                fabSelectLine.visibility = false
                                fabPredict.visibility = false
                            } else {
                                fabMenu.visibility = false
                            }
                        }
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
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.map_url))
                        )
                        startActivity(intent)
                    }
                    is TopFragmentEvent.SelectLine -> {
                        val action =
                            LineDialogDirections.actionGlobalLineDialog(LineDialogType.Current)
                        view.findNavController().navigate(action)
                    }
                    is TopFragmentEvent.StartNavigation -> {
                        val action =
                            LineDialogDirections.actionGlobalLineDialog(LineDialogType.Navigation)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    private class LineNamesAdapter(context: Context) : HorizontalListView.ArrayAdapter<Line>() {

        private val inflater = LayoutInflater.from(context)

        override fun getView(group: ViewGroup): View {
            return inflater.inflate(R.layout.cell_line_small, group, false)
        }

        override fun onBindView(view: View, data: Line, position: Int) {
            val name = view.findViewById<TextView>(R.id.text_cell_line_name)
            val symbol = view.findViewById<TextView>(R.id.text_cell_line_symbol)
            name.text = data.name
            val background = GradientDrawable()
            background.shape = GradientDrawable.RECTANGLE
            background.cornerRadius = 4f
            background.setColor(parseColorCode(data.color))
            symbol.background = background
        }

    }
}
