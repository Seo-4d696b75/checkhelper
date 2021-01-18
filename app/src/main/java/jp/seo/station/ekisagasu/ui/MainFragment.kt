package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.HorizontalListView
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel.SearchState
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/11.
 */
@AndroidEntryPoint
class MainFragment : AppFragment() {

    @Inject
    lateinit var prefectureRepository: PrefectureRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->

            view.findViewById<View>(R.id.fab_exit).setOnClickListener {
                activity?.finish()
            }
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
            adapter.setOnItemSelectedListener { view, data, position ->
                Log.d("Line", "selected: $data")
            }
            val selectedLine = view.findViewById<TextView>(R.id.text_selected_line)
            mainViewModel.selectedLine.observe(viewLifecycleOwner) {
                selectedLine.text = it?.name ?: getString(R.string.no_selected_line)
            }

        }
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
            val color = data.color?.substring(1)?.toInt(16) ?: 0xcccccc
            background.setColor(color.or(0xff000000.toInt()))
            symbol.background = background
        }

    }
}
