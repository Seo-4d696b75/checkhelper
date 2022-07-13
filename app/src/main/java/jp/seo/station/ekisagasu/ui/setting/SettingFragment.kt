package jp.seo.station.ekisagasu.ui.setting

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentSettingBinding
import jp.seo.station.ekisagasu.ui.overlay.OverlayViewHolder
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SettingFragment : AppFragment() {

    private val viewModel: SettingViewModel by viewModels()

    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_setting,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.numberTimeInterval.setOnValueChangedListener { _, _, value ->
            viewModel.state = viewModel.state.copy(locationUpdateInterval = value)
        }
        binding.numberRadar.setOnValueChangedListener { _, _, value ->
            viewModel.state = viewModel.state.copy(searchK = value)
        }

        binding.switchNotification.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isPushNotification = checked)
        }
        binding.switchForceNotify.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isPushNotificationForce = checked)
        }
        binding.switchKeepNotification.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isKeepNotification = checked)
        }
        binding.switchDisplayPrefecture.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isShowPrefectureNotification = checked)
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isVibrate = checked)
        }
        binding.switchVibrateApproach.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isVibrateWhenApproach = checked)
        }
        binding.numberVibrateMeter.setOnValueChangedListener { _, _, value ->
            viewModel.state = viewModel.state.copy(vibrateDistance = value)
        }

        binding.switchNight.setOnCheckedChangeListener { _, checked ->
            viewModel.state = viewModel.state.copy(isNightMode = checked)
        }

        binding.spinnerNightModeSwitch.also {
            val ctx = requireContext()
            val values = ctx.resources.getIntArray(R.array.night_mode_switch_timeout).toTypedArray()
            it.adapter = NightModeTimeoutAdapter(ctx, values)
            val timeout = viewModel.state.nightModeTimeout
            val index = values.indexOfFirst { it == timeout }
            if (index >= 0) it.setSelection(index)
            it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    parent.getItemAtPosition(position)?.let { value ->
                        if (value is Int) {
                            viewModel.state = viewModel.state.copy(nightModeTimeout = value)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        binding.seekBrightness.also {
            it.min = OverlayViewHolder.MIN_BRIGHTNESS
            it.max = 255
        }

        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.state =
                    viewModel.state.copy(nightModeBrightness = binding.seekBrightness.progress)
            }
        })

        viewModel.setting
            .flowWithLifecycle(lifecycle)
            .map { it.nightModeBrightness }
            .distinctUntilChanged()
            .onEach { binding.viewSampleBrightness.background = ColorDrawable((255 - it).shl(24)) }
            .launchIn(lifecycleScope)

        viewModel.dataVersion.observe(viewLifecycleOwner) {
            it?.let { data ->
                binding.textDataVersion.text =
                    getString(R.string.setting_mes_data_version, data.version)
                binding.textDataTimestamp.text = getString(
                    R.string.setting_mes_data_timestamp,
                    formatTime(TIME_PATTERN_DATETIME, data.timestamp)
                )
            }
        }
        binding.buttonUpdateData.setOnClickListener {
            viewModel.checkLatestData(requireContext())
        }

    }
}

class NightModeTimeoutAdapter(
    context: Context,
    values: Array<Int>
) : ArrayAdapter<Int>(context, 0, values) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(android.R.layout.simple_spinner_item, null)
        val value = getItem(position)
        if (value != null && view is TextView) {
            view.text = if (value == 0) {
                context.getString(R.string.setting_mes_night_switch_always)
            } else {
                String.format(
                    context.getString(R.string.setting_mes_night_switch),
                    formatTime(context, value)
                )
            }
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: inflater.inflate(android.R.layout.simple_spinner_dropdown_item, null)
        val value = getItem(position)
        if (value != null && view is TextView) {
            view.text = if (value == 0) {
                context.getString(R.string.setting_mes_night_switch_always)
            } else {
                formatTime(context, value)
            }
        }
        return view
    }
}
