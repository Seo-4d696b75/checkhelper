package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.android.widget.CustomNumberPicker
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.formatTime
import jp.seo.station.ekisagasu.viewmodel.ActivityViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@AndroidEntryPoint
class SettingFragment : AppFragment() {

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
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    @Inject
    lateinit var appStateRepository: AppStateRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val notify = view.findViewById<SwitchCompat>(R.id.switch_notification)
        userRepository.isNotify.value?.let { notify.isChecked = it }
        val forceNotify = view.findViewById<SwitchCompat>(R.id.switch_force_notify)
        userRepository.isNotifyForce.value?.let { forceNotify.isChecked = it }
        val keepNotification = view.findViewById<SwitchCompat>(R.id.switch_keep_notification)
        userRepository.isKeepNotification.value?.let { keepNotification.isChecked = it }

        notify.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isNotify.value = isChecked
            if (!isChecked) {
                forceNotify.isChecked = false
                keepNotification.isChecked = false
            }
        }
        forceNotify.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isNotifyForce.value = isChecked
            if (isChecked) {
                notify.isChecked = true
            }
        }
        keepNotification.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isKeepNotification.value = isChecked
            if (isChecked) {
                notify.isChecked = true
            }
        }

        val prefecture = view.findViewById<SwitchCompat>(R.id.switch_display_prefecture)
        userRepository.isNotifyPrefecture.value?.let { prefecture.isChecked = it }
        prefecture.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isNotifyPrefecture.value = isChecked
        }

        val interval = view.findViewById<CustomNumberPicker>(R.id.number_time_interval)
        userRepository.gpsUpdateInterval.value?.let { interval.displayedValue = it }
        interval.setOnValueChangedListener { picker, oldVal, newVal ->
            userRepository.gpsUpdateInterval.value = newVal
        }

        val radar = view.findViewById<CustomNumberPicker>(R.id.number_radar)
        userRepository.searchK.value?.let { radar.displayedValue = it }
        radar.setOnValueChangedListener { picker, oldVal, newVal ->
            userRepository.searchK.value = newVal
        }

        val vibrate = view.findViewById<SwitchCompat>(R.id.switch_vibrate)
        val vibrateApproach = view.findViewById<SwitchCompat>(R.id.switch_vibrate_approach)
        val vibrateDistance = view.findViewById<CustomNumberPicker>(R.id.number_vibrate_meter)
        userRepository.isVibrateApproach.value?.let { vibrateApproach.isChecked = it }
        userRepository.vibrateDistance.value?.let { vibrateDistance.displayedValue = it }
        userRepository.isVibrate.value?.let {
            vibrate.isChecked = it
            vibrateApproach.isEnabled = it
            vibrateDistance.isEnabled = it
        }
        vibrate.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isVibrate.value = isChecked
            vibrateApproach.isEnabled = isChecked
            vibrateDistance.isEnabled = isChecked && vibrateApproach.isChecked
        }
        vibrateApproach.setOnCheckedChangeListener { buttonView, isChecked ->
            userRepository.isVibrateApproach.value = isChecked
            vibrateDistance.isEnabled = isChecked
        }
        vibrateDistance.setOnValueChangedListener { picker, oldVal, newVal ->
            userRepository.vibrateDistance.value = newVal
        }

        val night = view.findViewById<SwitchCompat>(R.id.switch_night)

        appStateRepository.nightMode
            .flowWithLifecycle(lifecycle)
            .onEach { night.isChecked = it }
            .launchIn(lifecycleScope)

        night.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                appStateRepository.setNightMode(isChecked)
            }
        }

        val nightTimeout = view.findViewById<Spinner>(R.id.spinner_night_mode_switch)
        context?.let { ctx ->
            val values = ctx.resources.getIntArray(R.array.night_mode_switch_timeout).toTypedArray()
            nightTimeout.adapter = NightModeTimeoutAdapter(ctx, values)
            userRepository.nightModeTimeout.value?.let { timeout ->
                val index = values.indexOfFirst { it == timeout }
                if (index >= 0) nightTimeout.setSelection(index)
            }
            nightTimeout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    parent?.getItemAtPosition(position)?.let {
                        if (it is Int) {
                            userRepository.nightModeTimeout.value = it
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val brightness = view.findViewById<SeekBar>(R.id.seek_brightness)
        val screen = view.findViewById<View>(R.id.view_sample_brightness)
        brightness.max = 255
        brightness.min = OverlayViewHolder.MIN_BRIGHTNESS
        userRepository.brightness.value?.let {
            brightness.progress = it
            screen.background = ColorDrawable((255 - it).shl(24))
        }
        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val back = ColorDrawable((255 - progress).shl(24))
                screen.background = back
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userRepository.brightness.value = brightness.progress
            }

        })

        val version = view.findViewById<TextView>(R.id.text_data_version)
        val timestamp = view.findViewById<TextView>(R.id.text_data_timestamp)
        val checkVersion = view.findViewById<Button>(R.id.button_update_data)
        stationRepository.dataVersion.observe(viewLifecycleOwner) {
            it?.let { data ->
                version.text = String.format(
                    "%s: %d",
                    getString(R.string.setting_mes_data_version),
                    data.version
                )
                timestamp.text = String.format(
                    "%s: %s",
                    getString(R.string.setting_mes_data_timestamp),
                    formatTime(TIME_PATTERN_DATETIME, data.timestamp)
                )
            }
        }
        checkVersion.setOnClickListener {
            activityViewModel.checkLatestData(requireContext())
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
