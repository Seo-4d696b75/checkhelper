package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.viewmodel.LogViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Seo-4d696b75
 * @version 2020/12/21.
 */
class LogFragment : AppFragment() {

    private val filters: Array<LogFilter> = arrayOf(
        LogFilter(AppLog.FILTER_ALL, "ALL"),
        LogFilter(AppLog.TYPE_SYSTEM, "System"),
        LogFilter(AppLog.FILTER_GEO, "Geo"),
        LogFilter(AppLog.TYPE_STATION, "Station")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getService { s ->
            context?.let { ctx ->
                val model = LogViewModel.getFactory(s.userRepository)
                    .create(LogViewModel::class.java)
                val list: RecyclerView = view.findViewById(R.id.list_log)
                val spinner: Spinner = view.findViewById(R.id.spinner_filter_log)
                spinner.adapter = LogTypeAdapter(ctx, filters)
                spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                    override fun onItemSelected(parent: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                        parent?.getItemAtPosition(position)?.let {
                            if ( it is LogFilter ){
                                model.setFilter(it.filter)
                            }
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }

                }
                val adapter = LogAdapter(ctx, ArrayList())
                list.layoutManager =
                    LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
                list.adapter = adapter
                model.logs.observe(viewLifecycleOwner, { list ->
                    adapter.logs = list
                    adapter.notifyDataSetChanged()
                })
            }
        }
    }
}

data class LogFilter(
    val filter: Int,
    val name: String,
)

class LogTypeAdapter(
    context: Context,
    values: Array<LogFilter>
) : ArrayAdapter<LogFilter>(context, 0, values) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val value = getItem(position)
        val view =
            convertView ?: inflater.inflate(android.R.layout.simple_spinner_item, null, false)
        if (value != null && view is TextView) {
            view.text = value.name
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val value = getItem(position)
        val view = convertView ?: inflater.inflate(R.layout.cell_spinner_filter_log, null, false)
        if (value != null && view is TextView) {
            view.text = value.name
        }
        return view
    }

}

class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val time: TextView = view.findViewById(R.id.text_log_time)
    val message: TextView = view.findViewById(R.id.text_log_message)
}

class LogAdapter(context: Context, var logs: List<AppLog>) :
    RecyclerView.Adapter<LogViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = inflater.inflate(R.layout.cell_list_log, null, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.time.text = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(log.timestamp)
        holder.message.text = log.message
    }

    override fun getItemCount(): Int {
        return logs.size
    }

}
