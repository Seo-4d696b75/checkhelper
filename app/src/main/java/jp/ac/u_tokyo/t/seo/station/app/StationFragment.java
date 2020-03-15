package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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


import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/10/31.
 */

public class StationFragment extends Fragment{

    private static final String KEY_STATION_CODE = "station_code";
    private static final String KEY_TYPE = "fragment_type";
    private static final String KEY_LON = "lon";
    private static final String KEY_LAT = "lat";

    private static final String KEY_FRAGMENT_TAG = "fragment_tag";

    private static final int TYPE_MAIN = 0;
    private static final int TYPE_MAP = 1;

    interface StationFragmentCallback extends RadarFragment.FragmentContext{
        void onRequestVoronoiShown(Station station);
        void onRequestRadarShown(double lon, double lat);
        void onRequestVoronoiRemoved();
        void onRequestStationOnMap(Station station);
        void onLineSelected(Line line);
    }
    
    static StationFragment getInstanceForMain(Station station){
        StationFragment fragment = new StationFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_STATION_CODE, station.code);
        args.putInt(KEY_TYPE, TYPE_MAIN);
        args.putString(KEY_FRAGMENT_TAG, String.format(Locale.US, "StationFragment<station:%s,type:main>@%x", station.name, System.identityHashCode(fragment)));
        fragment.setArguments(args);
        //fragment.setTargetFragment(parent, 0);
        return fragment;
    }
    
    static StationFragment getInstanceForMap(Station station){
        StationFragment fragment = new StationFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_STATION_CODE, station.code);
        args.putInt(KEY_TYPE, TYPE_MAP);
        args.putString(KEY_FRAGMENT_TAG, String.format(Locale.US, "StationFragment<station:%s,type:map>@%x", station.name, System.identityHashCode(fragment)));
        fragment.setArguments(args);
        //fragment.setTargetFragment(parent, 0);
        return fragment;
    }

    static StationFragment getInstanceForMap(Station station, double lon, double lat){
        StationFragment fragment = new StationFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_STATION_CODE, station.code);
        args.putInt(KEY_TYPE, TYPE_MAP);
        args.putDouble(KEY_LON, lon);
        args.putDouble(KEY_LAT, lat);
        args.putString(KEY_FRAGMENT_TAG, String.format(Locale.US, "StationFragment<station:%s,type:map,point:(%f,%f)>@%x", station.name, lon, lat, System.identityHashCode(fragment)));
        fragment.getTag();
        fragment.setArguments(args);
        //fragment.setTargetFragment(parent, 0);
        return fragment;
    }

    String getDefaultTag(){
        return getArguments().getString(KEY_FRAGMENT_TAG);
    }

    private StationFragmentCallback mCallback;
    private StationService mService;
    private Context mContext;

    private int mStationCode;
    private Station mStation;
    private int mType;

    private ListView mListList;
    private LineAdapter mAdapter;
    
    private View mButtonDelete;
    private View mButtonShowMap;
    private TextView mTextRadar;

    private double mLon, mLat;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mContext = context;

        Bundle args = getArguments();
        if ( args == null || !args.containsKey(KEY_STATION_CODE) || !args.containsKey(KEY_TYPE) ){
            Log.e("StationFragment", "argument not found");
            return;
        }

        Fragment fragment = getParentFragment();
        if ( fragment instanceof StationFragmentCallback ){
            mCallback = (StationFragmentCallback)fragment;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater, container, state);

        Bundle args = getArguments();
        mType = args.getInt(KEY_TYPE);


        if ( mType == TYPE_MAIN ){
            return inflater.inflate(R.layout.fragment_station, container, false);    
        }else if ( mType == TYPE_MAP ){
            return inflater.inflate(R.layout.fragment_station_map, container, false);
        }else{
            return null;
        }
        
    }

    @Override
    public void onViewCreated(final View view, Bundle state){
        mCallback.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                if ( mContext != null ){
                    mService = service;
                    init(view, getArguments());
                }
            }
        });
    }

    private void init(View view, Bundle args){

        mStationCode = args.getInt(KEY_STATION_CODE);
        mStation = mService.getStation(mStationCode);
        if ( mType == TYPE_MAP && args.containsKey(KEY_LON) && args.containsKey(KEY_LAT) ){
            mLon = args.getDouble(KEY_LON);
            mLat = args.getDouble(KEY_LAT);
        }else{
            mLon = mStation.longitude;
            mLat = mStation.latitude;
        }


        StationNameView name = (StationNameView)view.findViewById(R.id.stationName);
        TextView prefecture = (TextView)view.findViewById(R.id.textStationPrefecture);
        TextView location = (TextView)view.findViewById(R.id.textStationLocation);
        mListList = (ListView)view.findViewById(R.id.listLines);

        name.setStationName(mStation);

        prefecture.setText(mService.getPrefecture(mStation.prefecture));
        location.setText(String.format(Locale.US, "E%.6f N%.6f", mStation.longitude, mStation.latitude));

        mAdapter = new LineAdapter(mContext, mStation.lines);
        mListList.setAdapter(mAdapter);
        mListList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                mCallback.onLineSelected(mAdapter.getItem(position));
            }
        });

        mButtonDelete = view.findViewById(R.id.imageMenuDelete);
        mButtonShowMap = view.findViewById(R.id.buttonStationMap);


        mButtonDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mType == TYPE_MAIN){
                    mCallback.onRequestFragmentRemoved();
                }else if ( mType == TYPE_MAP ){
                    mCallback.onRequestVoronoiRemoved();
                    mButtonDelete.setVisibility(View.GONE);
                    mButtonShowMap.setVisibility(View.VISIBLE);
                }

            }
        });

        mButtonShowMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mType == TYPE_MAIN ){
                    mCallback.onRequestStationOnMap(mStation);
                }else if ( mType == TYPE_MAP){
                    mCallback.onRequestVoronoiShown(mStation);
                    mButtonDelete.setVisibility(View.VISIBLE);
                    mButtonShowMap.setVisibility(View.GONE);
                }
            }
        });

        if ( mType == TYPE_MAP ){

            view.findViewById(R.id.buttonStationRadar).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mCallback.onRequestRadarShown(mLon, mLat);
                }
            });
            mTextRadar = (TextView)view.findViewById(R.id.textStationRadarNum);
            mTextRadar.setText(String.format(Locale.US, "x%d", mService.getRadarNum()));

            double distance = mService.mDisplayRuler.measureDistance(mStation, mLon, mLat);
            View container = view.findViewById(R.id.currentLocationContainer);
            if ( distance > 10 ){
                TextView textLocation = (TextView)view.findViewById(R.id.textCurrentLocation);
                TextView textDistance = (TextView)view.findViewById(R.id.textDistance);
                textLocation.setText(String.format(Locale.US, "N%.4f E%.4f", mLat, mLon));
                textDistance.setText(String.format(Locale.US, "%.0fm", distance));
                container.setVisibility(View.VISIBLE);
                mListList.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mListList.setAdapter(null);
        mAdapter = null;
        mService = null;
        mCallback = null;
        mContext = null;
    }


    private class LineAdapter extends ArrayAdapter<Line>{


        private LayoutInflater mInflater;


        private LineAdapter(Context context, Line[] lines){
            super(context, 0, lines);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent){

            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_line, null, false);
            }
            View view = convertView;
            Line line = getItem(position);
            if ( line != null ){
                View symbol = view.findViewById(R.id.textCellLineSymbol);
                TextView name = (TextView)view.findViewById(R.id.textCellLineName);
                TextView size = (TextView)view.findViewById(R.id.textCellLineListSize);
                name.setText(line.mName);
                size.setText(String.format(Locale.US, "%d駅", line.mStationSize));
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(6f);
                //be sure not to add alpha channel
                drawable.setColor(0xFF000000 | line.mColor);
                symbol.setBackground(drawable);
            }
            return view;
        }

    }

}
