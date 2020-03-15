package jp.ac.u_tokyo.t.seo.station.app;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jp.ac.u_tokyo.t.seo.station.app.Station.LineAccessor;

/**
 * @author Seo-4d696b75
 * @version 2018/11/04.
 */

class StationGroup{

    static int getGroupKey(int dLongitude, int dLatitude){
        return (dLongitude + 1800) * 10000 + dLatitude + 900;
    }

    StationGroup(int dLongitude, int dLatitude, JSONArray list, LineAccessor accessor) throws JSONException{

        mLongitude = dLongitude / 10.0;
        mLatitude = dLatitude / 10.0;
        mKey = getGroupKey(dLongitude, dLatitude);

        if ( list != null ){
            mStationList = new ArrayList<>(list.length());
            for ( int i = 0; i < list.length(); i++ ){
                mStationList.add(new Station(list.getJSONObject(i), accessor));
            }
        }else{
            mStationList = new ArrayList<>();
        }
    }

    synchronized void setVoronoiData(JSONArray list) throws JSONException{
        if ( list == null ){
            if ( mStationList.size() != 0 ){
                throw new RuntimeException(String.format(Locale.US,"Error : voronoi data null (%.1f,%.1f)", mLongitude, mLatitude));
            }
            mHasVoronoi = true;
            return;
        }
        if ( list.length() != mStationList.size() ) throw new RuntimeException();
        mPolygonGroup = new ArrayList<>(list.length());
        for ( int i=0 ; i<list.length() ; i++ ){
            JSONObject data = list.getJSONObject(i);
            Station s = mStationList.get(i);
            if ( s.code != data.getInt("code") ) throw new RuntimeException();
            mPolygonGroup.add(new StationMapFragment.StationArea(s, data.getJSONObject("voronoi")));
        }
        mHasVoronoi = true;
    }

    synchronized void releaseVoronoiData(){
        mPolygonGroup = null;
        mHasVoronoi = false;
    }

    List<Station> mStationList;
    List<StationMapFragment.StationArea> mPolygonGroup;
    final double mLongitude;
    final double mLatitude;
    final int mKey;

    private boolean mHasVoronoi = false;

    boolean hasVoronoi(){
        return mHasVoronoi;
    }

}
