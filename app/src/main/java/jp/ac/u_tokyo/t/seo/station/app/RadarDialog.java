package jp.ac.u_tokyo.t.seo.station.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import jp.ac.u_tokyo.t.seo.customdialog.CustomDialog;

/**
 * @author Seo-4d696b75
 * @version 2018/06/28.
 */

public class RadarDialog extends CustomDialog{

    private StationService mService;
    private StationKdTree mExplorer;

    private final static String KEY_LONGITUDE = "lon";
    private final static String KEY_LATITUDE = "lat";

    static RadarDialog getInstance(double lon, double lat){
        RadarDialog dialog = new RadarDialog();
        Bundle args = new Bundle();
        args.putDouble(RadarDialog.KEY_LONGITUDE, lon);
        args.putDouble(RadarDialog.KEY_LATITUDE, lat);
        dialog.setArguments(args);
        return dialog;
    }

    interface RadarCallback{
        StationKdTree getExplorer();
        void onStationSelected(Station station);
    }

    private RadarCallback mListener;

    @Override
    protected View onInflateView(LayoutInflater inflater, int id){
        return inflater.inflate(R.layout.dialog_radar, null, false);
    }

    @Override
    protected void onCreateContentView(@NonNull View view){

        Fragment fragment = getParentFragment();
        if ( fragment != null && fragment instanceof RadarCallback ){
            mListener = (RadarCallback)fragment;
            mExplorer = mListener.getExplorer();
        }


        Activity activity = getActivity();
        if ( activity instanceof MainActivity ){
            mService = ((MainActivity)activity).getService();
        }

        if ( mService == null || mExplorer == null){
            Log.e("LineDialog","service or explorer not found");
            dismiss();
            return;
        }




        ListView listView = (ListView)view.findViewById(R.id.listRadarStations);
        Bundle args = getArguments();
        if ( args != null ){
            double lon = args.getDouble(KEY_LONGITUDE);
            double lat = args.getDouble(KEY_LATITUDE);
            listView.setAdapter(new StationAdapter(mExplorer, activity.getApplicationContext(), lon, lat));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                    Station item = (Station)parent.getItemAtPosition(position);
                    mListener.onStationSelected(item);
                    dismiss();
                }
            });
        }


    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mService = null;
        mExplorer = null;
        mListener = null;
    }

    private static class StationAdapter extends ArrayAdapter<Station>{

        private LayoutInflater mInflater;
        private final double mLongitude, mLatitude;
        private DistanceRuler mRuler;


        private StationAdapter(StationKdTree explorer, Context context, double lon, double lat){
            super(context, 0, explorer.getNearStations());
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLongitude = lon;
            mLatitude = lat;
            mRuler = DistanceRuler.getInstance(true);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent){
            Station station = getItem(position);
            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_station, null, false);
            }
            View view = convertView;
            if ( station != null ){

                TextView index = (TextView)view.findViewById(R.id.textCellStationIndex);
                TextView distance = (TextView)view.findViewById(R.id.textCellStationDistance);
                TextView name = (TextView)view.findViewById(R.id.textCellStationName);
                TextView line = (TextView)view.findViewById(R.id.textCellStationLine);
                index.setText(String.valueOf(position + 1));
                double meter = mRuler.measureDistance(station, mLongitude, mLatitude);
                distance.setText(DistanceRuler.formatDistance(meter));
                name.setText(station.name);
                line.setText(station.getLinesName());
            }
            return view;
        }

    }


}
