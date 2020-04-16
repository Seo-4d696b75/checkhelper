package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customdialog.CompatFragment;
import jp.ac.u_tokyo.t.seo.customview.CustomNumberPicker;

/**
 * @author Seo-4d696b75
 * @version 2018/04/25.
 */

public class SettingFragment extends CompatFragment implements DataDialog.DataUpdateResult{

    private MainActivity mContext;
    private StationService mService;

    private TextView mTextDataVersion;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if ( context instanceof MainActivity ){
            mContext = (MainActivity)context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater, container, state);
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle state){
        mContext.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                mService = service;
                init(view);
            }
        });

    }

    private void init(View view){
        final Switch notification = (Switch)view.findViewById(R.id.switchNotify);
        notification.setChecked(mService.isNotifyUpdate());

        final Switch forceNotify = (Switch)view.findViewById(R.id.switchForceNotify);
        forceNotify.setChecked(mService.isNotifyForce());

        final Switch stationaryNotify = (Switch)view.findViewById(R.id.switchNotifyStationary);
        stationaryNotify.setChecked(mService.isKeepNotification());

        notification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setNotify(isChecked);
                if ( !isChecked ){
                    forceNotify.setChecked(false);
                    stationaryNotify.setChecked(false);
                }
            }
        });
        forceNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setForceNotify(isChecked);
                if ( isChecked && !mService.isNotifyUpdate() ){
                    notification.setChecked(true);
                }
            }
        });
        stationaryNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setKeepNotification(isChecked);
                if ( isChecked && !mService.isNotifyUpdate() ){
                    notification.setChecked(true);
                }
            }
        });
        Switch prefecture = (Switch)view.findViewById(R.id.switchDisplayPrefecture);
        prefecture.setChecked(mService.isNotifyPrefecture());
        prefecture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setNotifyPrefecture(isChecked);
            }
        });

        CustomNumberPicker interval = (CustomNumberPicker)view.findViewById(R.id.numberInterval);
        interval.setDisplayedValue(mService.getInterval());
        interval.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                mService.setInterval(newVal, true);
            }
        });
        CustomNumberPicker radar = (CustomNumberPicker)view.findViewById(R.id.numberRadar);
        radar.setDisplayedValue(mService.getRadarNum());
        radar.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                mService.setRadarNum(newVal);
            }
        });
        final CustomNumberPicker meter = (CustomNumberPicker)view.findViewById(R.id.numberVibrateMeter);
        meter.setDisplayedValue(mService.getVibrateMeter());
        meter.setEnabled(mService.isVibrate() && mService.isVibrateApproach());
        meter.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                mService.setVibrateMeter(newVal);
            }
        });
        final Switch vibrateApproach = (Switch)view.findViewById(R.id.switchApproachVibrate);
        vibrateApproach.setChecked(mService.isVibrateApproach());
        vibrateApproach.setEnabled(mService.isVibrate());
        vibrateApproach.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setVibrateApproach(isChecked);
                meter.setEnabled(isChecked);
            }
        });
        Switch vibration = (Switch)view.findViewById(R.id.switchVibrate);
        vibration.setChecked(mService.isVibrate());
        vibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setVibrate(isChecked);
                vibrateApproach.setEnabled(isChecked);
                meter.setEnabled(isChecked && mService.isVibrateApproach());
            }
        });
        mTextDataVersion = (TextView)view.findViewById(R.id.textDataVersion);
        showDataVersion();
        final View dataUpdate = view.findViewById(R.id.imageDataVersion);
        dataUpdate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mService.checkDataVersion(new StationService.DataVersionResult(){
                    @Override
                    public void onUpToDate(){
                        Toast.makeText(mContext, getString(R.string.data_version_latest), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLatestFound(long version, String path, String size){
                        DataDialog dialog = DataDialog.getInstance(version, path, size, false);
                        dialog.show(getCompatFragmentManager(), null);
                    }

                    @Override
                    public void onCheckFailure(){
                        Toast.makeText(mContext, getString(R.string.data_version_check_fail), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        dataUpdate.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v){
                DataDialog dialog = DataDialog.getInstance();
                dialog.show(getCompatFragmentManager(), null);
                return true;
            }
        });

        Switch night = (Switch)view.findViewById(R.id.switchNight);
        night.setChecked(mService.isNightMood());
        night.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                mService.setNightMood(isChecked);
            }
        });
        SeekBar brightness = (SeekBar)view.findViewById(R.id.seekBrightness);
        final View screen = view.findViewById(R.id.viewSampleTextScreen);
        brightness.setMax(255);
        brightness.setProgress(mService.getBrightness());
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                Drawable back = new ColorDrawable((255 - progress) << 24);
                screen.setBackground(back);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                mService.setBrightness(seekBar.getProgress());
            }
        });
    }


    @Override
    public void onUpdateRequired(boolean required, long version, String path){
        if ( required ){
            DataDialog dialog = DataDialog.getInstance(version, path);
            dialog.show(getCompatFragmentManager(), null);
        }
    }

    @Override
    public void onDateUpdate(long version, boolean success){
        showDataVersion();
        if ( success ){
            Toast.makeText(getContext(), "データの更新が完了しました", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getContext(), "データの更新に失敗しました", Toast.LENGTH_LONG).show();
        }
    }

    private void showDataVersion(){
        if ( mService != null ){
            String message = String.format(Locale.US, getString(R.string.data_version_format), mService.getDataVersion());
            mTextDataVersion.setText(message);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        if ( mService != null ){
            mService.saveSetting();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mService = null;
        mContext = null;
        mTextDataVersion = null;
    }

}
