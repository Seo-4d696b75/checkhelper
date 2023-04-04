package jp.seo.station.ekisagasu.ui.setting

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.databinding.FragmentSettingBinding
import jp.seo.station.ekisagasu.ui.overlay.OverlayViewHolder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@AndroidEntryPoint
class SettingFragment : Fragment() {

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
            viewModel.updateState { it.copy(locationUpdateInterval = value) }
        }
        binding.numberRadar.setOnValueChangedListener { _, _, value ->
            viewModel.updateState { it.copy(searchK = value) }
        }

        binding.switchNotification.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isPushNotification = checked) }
        }
        binding.switchForceNotify.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isPushNotificationForce = checked) }
        }
        binding.switchKeepNotification.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isKeepNotification = checked) }
        }
        binding.switchDisplayPrefecture.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isShowPrefectureNotification = checked) }
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isVibrate = checked) }
        }
        binding.switchVibrateApproach.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isVibrateWhenApproach = checked) }
        }

        binding.numberVibrateMeter.setOnValueChangedListener { _, _, value ->
            viewModel.updateState { it.copy(vibrateDistance = value) }
        }

        binding.switchNight.setOnCheckedChangeListener { _, checked ->
            viewModel.updateState { it.copy(isNightMode = checked) }
        }

        binding.dropdownNightTimeout.apply {
            val context = requireContext()
            val values = context.resources
                .getIntArray(R.array.night_mode_switch_timeout)
                .toTypedArray()
                .map { timeout ->
                    NightModeTimeout(
                        timeout = timeout,
                        text = if (timeout == 0) {
                            context.getString(R.string.setting_mes_night_switch_always)
                        } else {
                            context.getString(R.string.setting_mes_night_switch, timeout)
                        }
                    )
                }
            val text = values.map { it.text }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, text)
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val value = values[position].timeout
                viewModel.updateState { it.copy(nightModeTimeout = value) }
            }
            viewModel.state
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .map { it.nightModeTimeout }
                .distinctUntilChanged()
                .onEach { timeout ->
                    values.find { it.timeout == timeout }?.let {
                        setText(it.text, false)
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }

        binding.seekBrightness.also {
            it.valueFrom = OverlayViewHolder.MIN_BRIGHTNESS
            it.valueTo = 255f
            it.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateState { it.copy(nightModeBrightness = value) }
                }
            }
        }

        viewModel.state
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .map { it.nightModeBrightness }
            .distinctUntilChanged()
            .onEach {
                binding.viewSampleBrightness.background =
                    ColorDrawable((255 - it.roundToInt()).shl(24))
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.buttonUpdateData.setOnClickListener {
            viewModel.checkLatestData()
        }
    }
}

private data class NightModeTimeout(
    val timeout: Int,
    val text: String,
)
