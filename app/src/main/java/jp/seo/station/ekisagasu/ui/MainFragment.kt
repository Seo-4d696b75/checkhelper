package jp.seo.station.ekisagasu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.utils.AppFragment
import jp.seo.station.ekisagasu.viewmodel.MainViewModel

/**
 * @author Seo-4d696b75
 * @version 2021/01/11.
 */
class MainFragment : AppFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let { ctx ->

            getService { service ->
                service.message("main-fragment connected")
                val viewModel = MainViewModel.getFactory(service).create(MainViewModel::class.java)
                view.findViewById<View>(R.id.fab_exit).setOnClickListener {
                    activity?.finish()
                }
                val fabStart = view.findViewById<FloatingActionButton>(R.id.fab_start)
                val imgStart = ContextCompat.getDrawable(ctx, R.drawable.ic_play)
                val imgStop = ContextCompat.getDrawable(ctx, R.drawable.ic_pause)
                viewModel.running.observe(viewLifecycleOwner) {
                    fabStart.setImageDrawable(
                        if (it) imgStop else imgStart
                    )
                }
                fabStart.setOnClickListener {
                    viewModel.toggleStart()
                }
            }
        }
    }

}
