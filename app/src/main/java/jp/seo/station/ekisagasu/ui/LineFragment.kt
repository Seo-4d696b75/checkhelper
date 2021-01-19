package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.utils.getVFromColorCode
import jp.seo.station.ekisagasu.utils.parseColorCode
import jp.seo.station.ekisagasu.viewmodel.MainViewModel.StationRegister

/**
 * @author Seo-4d696b75
 * @version 2021/01/19.
 */
class LineFragment : AppFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_line, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->
            val name = view.findViewById<TextView>(R.id.text_line_detail_name)
            val kana = view.findViewById<TextView>(R.id.text_line_detail_name_kana)
            val symbol = view.findViewById<TextView>(R.id.text_line_detail_symbol)
            mainViewModel.lineInDetail.observe(viewLifecycleOwner) {
                it?.let { line ->
                    name.text = line.name
                    kana.text = line.nameKana
                    symbol.text = line.symbol ?: ""
                    symbol.background = GradientDrawable().apply {
                        cornerRadius = 8f
                        shape = GradientDrawable.RECTANGLE
                        setColor(parseColorCode(line.color))
                    }
                    symbol.setTextColor(line.color?.let { if (getVFromColorCode(it) < 200) Color.WHITE else Color.BLACK }
                        ?: Color.BLACK)
                }
            }
            val list = view.findViewById<ListView>(R.id.list_line_detail_stations)
            mainViewModel.stationRegisterList.observe(viewLifecycleOwner) {
                list.adapter = StationAdapter(ctx, it)
            }
            list.setOnItemClickListener { parent, view, position, id ->
                mainViewModel.showStationInDetail(position)
                findNavController().navigate(R.id.action_global_stationFragment)
            }
            view.findViewById<Button>(R.id.button_line_detail_delete).setOnClickListener {
                findNavController().navigate(R.id.action_global_to_radarFragment)
            }
            view.findViewById<Button>(R.id.button_line_detail_show_map).setOnClickListener {
                //TODO
            }
        }
    }


    private class StationAdapter(context: Context, stations: List<StationRegister>) :
        ArrayAdapter<StationRegister>(context, 0, stations) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.cell_station, parent, false)
            getItem(position)?.let { r ->
                view.findViewById<TextView>(R.id.text_cell_station_name).text = r.station.name
                view.findViewById<TextView>(R.id.text_cell_station_numbering).text = r.numbering
            }
            return view
        }

    }
}
