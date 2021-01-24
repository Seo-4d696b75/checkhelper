package jp.seo.station.ekisagasu.ui

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.utils.AnimationHolder
import jp.seo.station.ekisagasu.utils.onChanged
import jp.seo.station.ekisagasu.utils.parseColorCode
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel.SearchState
import java.util.*
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/11.
 */
@AndroidEntryPoint
class MainFragment : AppFragment() {

    @Inject
    lateinit var prefectureRepository: PrefectureRepository

    private val activityViewModel: ActivityViewModel by lazy {
        ActivityViewModel.getInstance(
            requireActivity(),
            requireContext(),
            stationRepository,
            userRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    @SuppressWarnings("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->

            val fabStart = view.findViewById<FloatingActionButton>(R.id.fab_start)
            val imgStart = ContextCompat.getDrawable(ctx, R.drawable.ic_play)
            val imgStop = ContextCompat.getDrawable(ctx, R.drawable.ic_pause)
            val runAnimation = view.findViewById<AnimationView>(R.id.animation_view)
            appViewModel.isRunning.observe(viewLifecycleOwner) {
                fabStart.setImageDrawable(
                    if (it) imgStop else imgStart
                )
                runAnimation.runAnimation(it)
                Log.d("MainFragment", "running: $it")
            }
            fabStart.setOnClickListener {
                appViewModel.isRunning.value?.let { running ->
                    appViewModel.setSearchState(!running)
                }
            }

            val mainContainer = view.findViewById<ViewGroup>(R.id.container_station)
            val waitMessage = view.findViewById<View>(R.id.text_wait_message)
            val startingMessage =
                view.findViewById<View>(R.id.container_starting_search_message)
            appViewModel.state.observe(viewLifecycleOwner) {
                Log.d("MainFragment", "state: $it")
                mainContainer.visibility =
                    if (it == SearchState.RUNNING) View.VISIBLE else View.INVISIBLE
                waitMessage.visibility =
                    if (it == SearchState.STOPPED) View.VISIBLE else View.GONE
                startingMessage.visibility =
                    if (it == SearchState.STARTING) View.VISIBLE else View.GONE
            }

            val stationName = view.findViewById<StationNameView>(R.id.station_name_main)
            val prefecture = view.findViewById<TextView>(R.id.text_station_prefecture)
            val distance = view.findViewById<TextView>(R.id.text_distance)
            val lineNames = view.findViewById<HorizontalListView>(R.id.list_line_names)
            val adapter = LineNamesAdapter(ctx)
            lineNames.adapter = adapter
            mainViewModel.nearestStation.observe(viewLifecycleOwner) {
                it?.let { s ->
                    stationName.setStation(s.station)
                    prefecture.text = prefectureRepository.getName(s.station.prefecture)
                    distance.text = formatDistance(s.distance)
                    adapter.data = s.lines
                }
            }
            val navigationHost = view.findViewById<View>(R.id.sub_nav_host)
            appViewModel.isRunning.onChanged(viewLifecycleOwner) {
                if (!it) navigationHost.findNavController()
                    .navigate(R.id.action_global_to_radarFragment)
            }
            stationName.setOnClickListener {
                mainViewModel.nearestStation.value?.let { n ->
                    mainViewModel.showStationInDetail(n.station)

                    navigationHost.findNavController().navigate(R.id.action_global_stationFragment)
                }
            }
            adapter.setOnItemSelectedListener { view, data, position ->
                Log.d("Line", "selected: $data")
                mainViewModel.showLineInDetail(data)
                navigationHost.findNavController().navigate(R.id.action_global_lineFragment)
            }
            val selectedLine = view.findViewById<TextView>(R.id.text_selected_line)
            mainViewModel.selectedLine.observe(viewLifecycleOwner) {
                selectedLine.text = it?.name ?: getString(R.string.no_selected_line)
            }

            // animated floating buttons
            val res = ctx.resources
            val fabMenu = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_more),
                res.getDimensionPixelSize(R.dimen.fab_menu_x),
                res.getDimensionPixelSize(R.dimen.fab_menu_y)
            )
            val fabExit = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_exit),
                res.getDimensionPixelSize(R.dimen.fab_exit_x),
                res.getDimensionPixelSize(R.dimen.fab_exit_y)
            )
            val fabSelectLine = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_select_line),
                res.getDimensionPixelSize(R.dimen.fab_select_line_x),
                res.getDimensionPixelSize(R.dimen.fab_select_line_y)
            )
            val fabPredict = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_predict),
                res.getDimensionPixelSize(R.dimen.fab_predict_x),
                res.getDimensionPixelSize(R.dimen.fab_predict_y)
            )
            val fabTimer = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_timer),
                res.getDimensionPixelSize(R.dimen.fab_timer_x),
                res.getDimensionPixelSize(R.dimen.fab_timer_y)
            )
            val fabFixTimer = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_fix_timer),
                res.getDimensionPixelSize(R.dimen.fab_fix_timer_x),
                res.getDimensionPixelSize(R.dimen.fab_fix_timer_y)
            )
            val fabMap = AnimationHolder<FloatingActionButton>(
                view.findViewById(R.id.fab_map),
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
                    val running = appViewModel.isRunning.value ?: false
                    val selected = mainViewModel.selectedLine.value != null
                    if ((expand && running) || (!expand && fabSelectLine.visibility)) list.add(
                        fabSelectLine.animate(
                            expand
                        )
                    )
                    if ((expand && selected) || (!expand && fabPredict.visibility)) list.add(
                        fabPredict.animate(
                            expand
                        )
                    )
                    AnimatorSet().apply {
                        playTogether(list)
                        duration = 300L
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator?) {
                                if (expand) {
                                    fabMap.visibility = true
                                    fabTimer.visibility = true
                                    fabFixTimer.visibility = true
                                    fabSelectLine.visibility = running
                                    fabPredict.visibility = selected
                                } else {
                                    fabMenu.visibility = true
                                }
                            }

                            override fun onAnimationEnd(animation: Animator?) {
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

                            override fun onAnimationCancel(animation: Animator?) {}

                            override fun onAnimationRepeat(animation: Animator?) {}
                        })
                        start()
                    }
                }
            }

            val fabScreen = view.findViewById<View>(R.id.fab_container)
            fabScreen.setOnTouchListener { v, event ->
                // close floating button menu if any
                if (fabMap.visibility) {
                    animateFab(false)
                }
                false
            }
            fabMenu.view.setOnClickListener {
                animateFab(true)
            }
            fabExit.view.setOnClickListener {
                appViewModel.finish()
            }
            fabSelectLine.view.setOnClickListener {
                activityViewModel.requestDialog(LineDialog.DIALOG_SELECT_CURRENT)
                animateFab(false)
            }
            fabPredict.view.setOnClickListener {
                activityViewModel.requestDialog(LineDialog.DIALOG_SELECT_PREDICTION)
                animateFab(false)
            }

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
