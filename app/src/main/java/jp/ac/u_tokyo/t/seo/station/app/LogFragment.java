package jp.ac.u_tokyo.t.seo.station.app;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * @author Seo-4d696b75
 * @version 2018/05/02.
 */

public class LogFragment extends Fragment implements StationService.StatusChangeListener{

    private MainActivity mContext;
    private StationService mService;

    private Spinner mTypeSpinner;
    private ListView mLogList;
    private LogAdapter mAdapter;

    private LogType mCurrentType;


    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if ( context instanceof MainActivity ){
            mContext = (MainActivity)context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle state){
        mContext.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                mService = service;
                initialize(view);
            }
        });
    }


    private void initialize(View view){

        LogType[] LOG_TYPE = new LogType[]{
                new LogType(LogHolder.LOG_STATION, getString(R.string.log_type_station), "Station"),
                new LogType(LogHolder.LOG_LOCATION, getString(R.string.log_type_location), "Location"),
                new LogType(LogHolder.LOG_FILTER_GEO, getString(R.string.log_type_geo), "Geo"),
                new LogType(LogHolder.LOG_SYSTEM, getString(R.string.log_type_system), "System"),
                new LogType(LogHolder.LOG_FILTER_ALL, getString(R.string.log_type_all), "All")
        };

        mService.setOnStatusChangeListener(this);
        mLogList = (ListView)view.findViewById(R.id.listViewLog);
        mLogList.setFastScrollEnabled(true);
        mLogList.setFastScrollAlwaysVisible(true);
        Button writeButton = (Button)view.findViewById(R.id.buttonWriteLog);
        writeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                writeLog();
            }
        });

        mTypeSpinner = view.findViewById(R.id.spinnerLogType);
        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                Object item = parent.getItemAtPosition(position);
                if ( item instanceof LogType ){
                    LogType type = (LogType)item;
                    mCurrentType = type;
                    mAdapter = new LogAdapter(
                            mService.getLogHolder().getLogList(type.filter),
                            mContext.getApplicationContext()
                    );
                    mLogList.setAdapter(mAdapter);
                    mLogList.setSelection(mAdapter.getCount());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){

            }
        });
        mTypeSpinner.setAdapter(
                new LogTypeAdapter(mContext.getApplicationContext(), LOG_TYPE)
        );
        mTypeSpinner.setSelection(0, false);
        mCurrentType = LOG_TYPE[0];
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mService.setOnStatusChangeListener(null);
        mService = null;
        mContext = null;
        mAdapter = null;
        mLogList.setAdapter(null);
        mLogList = null;
        mTypeSpinner.setAdapter(null);
        mTypeSpinner.setOnItemSelectedListener(null);
        mTypeSpinner = null;
    }

    @Override
    public void onMessage(LogHolder.ServiceLog log){
        if ( mAdapter != null && mCurrentType.included(log.mType) ){
            mAdapter.add(log);
        }
    }

    private void writeLog(){
        List<LogHolder.ServiceLog> list = mService.getLogHolder().getLogList(mCurrentType.filter);
        StringBuilder builder = new StringBuilder();
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date());
        String type = mCurrentType.symbol;
        builder.append(getString(R.string.app_name));
        builder.append("\nlog type : ");
        builder.append( type);
        builder.append("\ntime : ");
        builder.append(time);
        for ( LogHolder.ServiceLog log : list ){
            builder.append("\n");
            builder.append(log.toString());
        }
        String fileName = String.format(Locale.US, "%sLog_%s.txt", type, time);
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(
                directory,
                fileName
        );
        try{
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(builder.toString());
            writer.close();

            Toast.makeText(mContext, String.format(Locale.US, getString(R.string.log_save_success), fileName), Toast.LENGTH_SHORT).show();
        }catch( IOException e){
            mService.onError("fail to write log.", e);
            Toast.makeText(mContext, getString(R.string.log_save_fail), Toast.LENGTH_SHORT).show();
        }


    }

    private static class LogType {

        private LogType(int typeFilter, String name, String symbol){
            this.filter = typeFilter;
            this.name = name;
            this.symbol = symbol;
        }

        final int filter;
        final String name;
        final String symbol;

        boolean included(int type){
            return (type & filter ) != 0x0;
        }

    }

    private static class LogTypeAdapter extends ArrayAdapter<LogType>{

        private LayoutInflater mInflater;

        private LogTypeAdapter(@NonNull Context context, @NonNull LogType[] objects){
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull  ViewGroup parent){
            LogType type = getItem(position);
            if ( convertView == null ){
                convertView = mInflater.inflate(android.R.layout.simple_spinner_item, null, false);
            }
            TextView text = (TextView)convertView;
            if ( type != null ){
                text.setText(type.name);
            }
            return text;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LogType type = getItem(position);
            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_spinner_dropdown, null, false);
            }
            TextView text = (TextView)convertView;
            if ( type != null ){
                text.setText(type.name);
            }
            return text;
        }
    }


    private static class LogAdapter extends ArrayAdapter<LogHolder.ServiceLog>{

        private LayoutInflater mInflater;

        private LogAdapter(List<LogHolder.ServiceLog> list, Context context){
            super(context, 0, list);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull  ViewGroup parent){
            LogHolder.ServiceLog log = getItem(position);
            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_log, null, false);
            }
            TextView time = convertView.findViewById(R.id.textLogTime);
            TextView message = convertView.findViewById(R.id.textLogMessage);
            if ( log != null ){
                time.setText(log.mTime);
                message.setText(log.mMessage);
            }
            return convertView;
        }

    }
}
