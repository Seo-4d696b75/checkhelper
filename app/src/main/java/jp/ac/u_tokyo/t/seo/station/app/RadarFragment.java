package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;

import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/10/31.
 */

public class RadarFragment extends Fragment implements StationService.StationCallback{

    public static final String FRAGMENT_TAG = "RadarFragment";

    static RadarFragment getInstance(){
        RadarFragment fragment = new RadarFragment();
        //fragment.setTargetFragment(parent, 0);
        return fragment;
    }

    private RadarFragmentCallback mCallback;
    private StationService mService;
    private Context mContext;

    private TextView mTextRadar;
    private ListView mListStations;
    private StationAdapter mAdapter;

    interface RadarFragmentCallback extends FragmentContext{
        void onStationSelected(Station station);
    }

    interface FragmentContext{
        void getService(MainActivity.OnServiceConnectedListener listener);
        void onRequestFragmentRemoved();
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mContext = context;
        Fragment parent = getParentFragment();
        if ( parent != null && parent instanceof RadarFragmentCallback){
            mCallback = (RadarFragmentCallback)parent;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater, container, state);
        return inflater.inflate(R.layout.fragment_radar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle state){


        mTextRadar = (TextView)view.findViewById(R.id.textRadarNum);
        mListStations = (ListView)view.findViewById(R.id.listRadarStations);

        mCallback.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                if ( mContext == null ) return;
                mService = service;
                mService.setCallback(RadarFragment.this);
                mTextRadar.setText(String.format(Locale.US, "X%d", mService.getRadarNum()));
                if ( mService.hasExplorerInitialized() ){
                    show();
                }
            }
        });



    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mTextRadar = null;
        mListStations.setAdapter(null);
        mListStations.setOnItemClickListener(null);
        mListStations = null;
        mAdapter = null;
        if ( mService != null ) mService.removeCallback(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mService = null;
        mCallback = null;
        mContext = null;

    }


    @Override
    public void onLocationUpdate(double longitude, double latitude){
        if ( mAdapter != null ){
            mAdapter.onLocationUpdate(longitude, latitude);
        }
    }

    @Override
    public void onStationUpdate(@Nullable Station station){
        if ( station == null ) return;
        if ( mService == null ) return;
        if ( mAdapter == null ){
            show();
        }
    }

    private void show(){
        if ( mListStations == null ) return;
        mAdapter = new StationAdapter(mService, mContext);
        mListStations.setAdapter(mAdapter);
        mListStations.setVisibility(View.VISIBLE);
        mListStations.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                mCallback.onStationSelected(mService.getNearStation(position));
            }
        });
    }

    @Override
    public void onLineUpdate(@Nullable Line line){

    }

    @Override
    public void onSearchStop(String mes){
        mAdapter = null;
        mListStations.setAdapter(null);
        mListStations.setOnItemClickListener(null);
        mListStations.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResolutionRequired(ResolvableApiException exception){

    }

    private static class StationAdapter extends BaseAdapter{

        private LayoutInflater mInflater;
        private double mLongitude, mLatitude;
        private DistanceRuler mRuler;

        private final int mSize;
        private final StationService mService;

        private StationAdapter(StationService service, Context context){
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLongitude = service.getLongitude();
            mLatitude = service.getLatitude();
            mService = service;
            mSize = service.getRadarNum();
            mRuler = service.mDisplayRuler;
        }

        private void onLocationUpdate(double longitude, double latitude){
            mLongitude = longitude;
            mLatitude = latitude;
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount(){
            return mSize;
        }

        @Override
        public Object getItem(int position){
            return mService.getNearStation(position);
        }

        @Override
        public long getItemId(int position){
            return mService.getNearStation(position).code;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent){
            Station station = mService.getNearStation(position);
            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_station, null, false);
            }
            View view = convertView;
            TextView index = (TextView)view.findViewById(R.id.textCellStationIndex);
            TextView distance = (TextView)view.findViewById(R.id.textCellStationDistance);
            TextView name = (TextView)view.findViewById(R.id.textCellStationName);
            TextView line = (TextView)view.findViewById(R.id.textCellStationLine);
            index.setText(String.valueOf(position + 1));
            distance.setText(DistanceRuler.formatDistance(mRuler.measureDistance(station, mLongitude, mLatitude)));
            name.setText(station.name);
            line.setText(station.getLinesName());

            return view;
        }


    }

}
