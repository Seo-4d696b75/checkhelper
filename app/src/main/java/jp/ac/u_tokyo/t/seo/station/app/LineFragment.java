package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
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

import jp.ac.u_tokyo.t.seo.station.app.RadarFragment.FragmentContext;

/**
 * @author Seo-4d696b75
 * @version 2018/11/02.
 */

public class LineFragment extends Fragment{

    interface LineFragmentCallback extends FragmentContext{
        void onStationSelected(Station station);
        void onRequestLineOnMap(Line line);
        void onRequestPolylineShown(Line line);
        void onRequestPolylineRemoved();
    }

    private static final String KEY_LINE_CODE = "line_code";

    private LineFragmentCallback mCallback;
    private StationService mService;
    private Context mContext;

    private View mLineSymbol;
    private TextView mLineName;
    private TextView mLinePhonetic;
    private ListView mStationList;
    private View mProgressBar;
    private View mButtonShowOnMap;
    private View mButtonDelete;

    private StationAdapter mAdapter;
    private int mLineCode;
    private Line mLine;

    private int mType;
    private boolean mShowInitial;
    private static final String KEY_TYPE = "fragment_type";
    private static final String KEY_SHOW_ON_MAP = "show_on_map";
    private static final int TYPE_MAIN = 0;
    private static final int TYPE_MAP = 1;

    private static String KEY_FRAGMENT_TAG = "fragment_tag";


    static LineFragment getInstanceForMain(Line line){
        LineFragment fragment = new LineFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, TYPE_MAIN);
        args.putInt(KEY_LINE_CODE, line.mCode);
        args.putString(KEY_FRAGMENT_TAG, String.format(Locale.US, "LineFragment<line:%s,type:main>@%x", line.mName, System.identityHashCode(fragment)));
        fragment.setArguments(args);
        //fragment.setTargetFragment(parent, 0);
        return fragment;
    }

    static LineFragment getInstanceForMap(Line line, boolean show){
        LineFragment fragment = new LineFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TYPE, TYPE_MAP);
        args.putInt(KEY_LINE_CODE, line.mCode);
        args.putBoolean(KEY_SHOW_ON_MAP, show);
        args.putString(KEY_FRAGMENT_TAG, String.format(Locale.US, "LineFragment<line:%s,type:map>@%x", line.mName, System.identityHashCode(fragment)));
        fragment.setArguments(args);
        //fragment.setTargetFragment(parent, 0);
        return fragment;

    }

    String getDefaultTag(){
        return getArguments().getString(KEY_FRAGMENT_TAG);
    }


    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        mContext = context;

        Bundle args = getArguments();
        if ( args == null || !args.containsKey(KEY_LINE_CODE) || !args.containsKey(KEY_TYPE)){
            Log.e("LineFragment", "args not found");
            return;
        }
        mLineCode = args.getInt(KEY_LINE_CODE);
        mType = args.getInt(KEY_TYPE);
        mShowInitial = mType == TYPE_MAP && args.getBoolean(KEY_SHOW_ON_MAP);

        Fragment fragment = getParentFragment();
        if ( fragment != null ){
            if ( fragment instanceof LineFragmentCallback ){
                mCallback = (LineFragmentCallback)fragment;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        super.onCreateView(inflater, container, state);
        if ( mType == TYPE_MAIN ){
            return inflater.inflate(R.layout.fragment_line, container, false);
        }else if ( mType == TYPE_MAP ){
            return inflater.inflate(R.layout.fragment_line_map, container, false);
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
                    init(view);
                }
            }
        });
    }

    private void init(View view){

        mProgressBar = view.findViewById(R.id.progressLineFragment);
        mButtonDelete = view.findViewById(R.id.imageMenuDelete);
        mButtonShowOnMap = view.findViewById(R.id.buttonLineMap);

        if ( mType == TYPE_MAP ){
            mButtonDelete.setClickable(false);
            mButtonShowOnMap.setClickable(false);
            mButtonDelete.setVisibility(mShowInitial ? View.VISIBLE : View.GONE);
            mButtonShowOnMap.setVisibility(mShowInitial ? View.GONE : View.VISIBLE);
        }


        mLineSymbol = view.findViewById(R.id.viewLineSymbol);


        mLineName = (TextView)view.findViewById(R.id.textLineName);
        mLinePhonetic = (TextView)view.findViewById(R.id.textLinePhonetic);




        mStationList = (ListView)view.findViewById(R.id.listStations);

        mStationList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                if ( mAdapter != null ) mCallback.onStationSelected(mAdapter.getItem(position));
            }
        });

        mButtonDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mType == TYPE_MAIN ){
                    mCallback.onRequestFragmentRemoved();
                }else if ( mType == TYPE_MAP  ){
                    mCallback.onRequestPolylineRemoved();
                    mButtonDelete.setVisibility(View.GONE);
                    mButtonShowOnMap.setVisibility(View.VISIBLE);
                }

            }
        });
        mButtonShowOnMap.findViewById(R.id.buttonLineMap).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mLine == null ) return;
                if ( mType == TYPE_MAIN){
                    mCallback.onRequestLineOnMap(mLine);
                }else if ( mType == TYPE_MAP ){
                    mCallback.onRequestPolylineShown(mLine);
                    mButtonDelete.setVisibility(View.VISIBLE);
                    mButtonShowOnMap.setVisibility(View.GONE);
                }
            }
        });

        new AsyncTask<Integer, Void, Line>(){

            @Override
            protected void onPreExecute(){
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Line doInBackground(Integer... params){
                if ( mService == null ) return null;
                return mService.getLine(params[0], true);
            }

            @Override
            protected void onPostExecute(Line line){
                if ( line == null ) return;
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(12f);
                //be sure not to add alpha channel
                drawable.setColor(0xFF000000 | line.mColor);
                mLineSymbol.setBackground(drawable);
                float nameLength = mLineName.getPaint().measureText(line.mName);
                float phoneticLength = mLinePhonetic.getPaint().measureText(line.mNameKana);
                int width = (int)Math.max(nameLength, phoneticLength);
                mLineName.getLayoutParams().width = width;
                mLinePhonetic.getLayoutParams().width = width;
                mLineName.setText(line.mName);
                mLinePhonetic.setText(line.mNameKana);
                Station[] list = line.getStationList();
                mAdapter = new StationAdapter(mContext, list);
                mProgressBar.setVisibility(View.GONE);
                mStationList.setAdapter(mAdapter);
                mLine = line;
                mButtonDelete.setClickable(true);
                mButtonShowOnMap.setClickable(true);
            }

        }.execute(mLineCode);

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mStationList.setAdapter(null);
        mAdapter = null;
        mService = null;
        mContext = null;
        mCallback = null;
    }

    private class StationAdapter extends ArrayAdapter<Station>{


        private LayoutInflater mInflater;


        private StationAdapter(Context context, Station[] stations){
            super(context, 0, stations);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent){

            if ( convertView == null ){
                convertView = mInflater.inflate(
                        mType == TYPE_MAIN ? R.layout.cell_station_item : R.layout.cell_station_item_small,
                        null, false
                );
            }
            View view = convertView;
            Station station = getItem(position);
            if ( station != null ){
                TextView name = (TextView)view.findViewById(R.id.cellTextStationName);
                name.setText(station.name);

            }
            return view;
        }

    }


}
