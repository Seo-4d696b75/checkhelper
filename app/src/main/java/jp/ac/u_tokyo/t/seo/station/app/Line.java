package jp.ac.u_tokyo.t.seo.station.app;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/11/03.
 */

class Line{

    static final int COLOR_DEFAULT = 0xCCCCCC;
    static final String COLOR_DEFAULT_STRING = "#CCCCCC";

    interface StationAccessor{
        @NonNull
        Station[] getStations(int[] id);
    }

    Line(JSONObject data) throws JSONException{
        mCode = data.getInt("code");
        mName = data.getString("name");
        mNameKana = data.getString("name_kana");
        mStationSize = data.getInt("station_size");
        if ( data.has("color") ){
            String value = data.getString("color");
            mColorString = value;
            mColor = Integer.parseInt(value.substring(1, value.length()), 16);
        }else{
            mColor = COLOR_DEFAULT;
            mColorString = COLOR_DEFAULT_STRING;
        }
    }

    Line(int code){
        mCode = code;
        mName = "unknown";
        mNameKana = "unknown";
        mStationSize = 0;
        mColor = COLOR_DEFAULT;
        mColorString = COLOR_DEFAULT_STRING;
        mStationList = null;
    }

    final String mName;
    final int mCode;
    final int mStationSize;
    final String mNameKana;
    /**
     * This value has no alpha channel
     */
    final int mColor;
    final String mColorString;

    void setDetails(JSONObject data, StationAccessor accessor) throws JSONException{
        JSONArray array = data.getJSONArray("station_list");
        int[] code = new int[array.length()];
        for ( int i=0 ; i<array.length() ; i++ ){
            JSONObject item = array.getJSONObject(i);
            code[i] = item.getInt("code");
        }
        mStationList = accessor.getStations(code);
        if ( data.has("polyline_list")){
            array = data.getJSONArray("polyline_list");
            final int size = array.length();
            mLineSegments = new PolylineSegment[size];
            for ( int i = 0; i < size; i++ ){
                mLineSegments[i] = new PolylineSegment(array.getJSONObject(i));
            }
        }
        mHasDetails = true;
    }

    void releaseDetails(){
        mStationList = null;
        mHasDetails = false;
    }

    boolean hasDetails(){
        return mHasDetails;
    }

    boolean hasPolyline(){
        return mHasDetails && mLineSegments != null;
    }

    //details
    private boolean mHasDetails = false;
    private Station[] mStationList;
    private PolylineSegment[] mLineSegments;

    @NonNull Station[] getStationList(){
        if ( !mHasDetails ) throw new IllegalStateException("Details not set yet. line:" + mCode);
        return Arrays.copyOf(mStationList, mStationSize);
    }

    @NonNull PolylineSegment[] getSegments(){
        if ( !mHasDetails || mLineSegments == null ) throw new IllegalStateException("Polyline not set. line:" + mCode);
        return mLineSegments;
    }

    void drawPolyline(GoogleMap map){
        if ( !mHasDetails ) throw new IllegalStateException("Details not set yet");
        for ( PolylineSegment segment : mLineSegments ){
            segment.drawPolyline(map);
        }
    }

    void removePolyline(){
        if ( !mHasDetails ) throw new IllegalStateException("Details not set yet");
        for ( PolylineSegment segment : mLineSegments ){
            segment.removePolyline();
        }
    }

    @Override
    public boolean equals(Object other){
        return other instanceof Line && ((Line)other).mCode == mCode;
    }

    @Override
    public String toString(){
        return String.format(Locale.US, "Line{name:%s code:%d}", mName, mCode);
    }

    static class PolylineSegment {

        private final double SCALE_BIAS = 100000.0;

        private PolylineSegment(JSONObject data) throws JSONException{
            mStart = data.getString("start");
            mEnd = data.getString("end");
            int lng = (int)(data.getDouble("lng") * SCALE_BIAS);
            int lat = (int)(data.getDouble("lat") * SCALE_BIAS);
            JSONArray deltaX = data.getJSONArray("delta_lng");
            JSONArray deltaY = data.getJSONArray("delta_lat");
            if ( deltaX.length() != deltaY.length() ) throw new AppException("delta-lng/lat size mismatch");
            final int size = deltaX.length();
            mPoints = new LatLng[size];
            for ( int i=0 ; i<size ; i++){
                lng += deltaX.getInt(i);
                lat += deltaY.getInt(i);
                mPoints[i] = new LatLng(lat/SCALE_BIAS, lng/SCALE_BIAS);
            }
        }

        final String mStart, mEnd;
        final LatLng[] mPoints;

        private Polyline mPolyline;

        void drawPolyline(GoogleMap map){
            if ( mPolyline != null ) mPolyline.remove();
            PolylineOptions options = new PolylineOptions()
                    .add(mPoints)
                    .color(Color.RED)
                    .width(5f)
                    .geodesic(false);
            mPolyline = map.addPolyline(options);
        }

        void removePolyline(){
            if ( mPolyline != null ){
                mPolyline.remove();
                mPolyline = null;
            }
        }

        @Override
        public boolean equals(Object obj){
            if ( obj instanceof PolylineSegment ){
                PolylineSegment other = (PolylineSegment)obj;
                if ( mStart.equals(other.mStart) && mEnd.equals(other.mEnd) ){
                    if ( mPoints.length == other.mPoints.length ){
                        int i = mPoints.length / 2;
                        return mPoints[i].equals(other.mPoints[i]);
                    }
                }
            }
            return false;
        }

        PositionPredictor.NearestPoint findNearestPoint(double lon, double lat){
            final int STEP = 50;
            LatLng p = new LatLng(lat, lon);
            double minValue = Double.MAX_VALUE;
            int minStart = -1;
            int minEnd = -1;
            for ( int start = 0; start < mPoints.length; start += STEP ){
                int end = start + STEP;
                if ( end >= mPoints.length ) end = mPoints.length - 1;
                if ( start == end ) break;
                PositionPredictor.NearestPoint n = new PositionPredictor.NearestPoint(mPoints[start], mPoints[end], p);
                if ( n.distance < minValue ){
                    minValue = n.distance;
                    minStart = start;
                    minEnd = end;
                }
            }
            minValue = Double.MAX_VALUE;
            PositionPredictor.NearestPoint nearest = null;
            for ( int i = minStart; i < minEnd; i++ ){
                PositionPredictor.NearestPoint n = new PositionPredictor.NearestPoint(mPoints[i], mPoints[i+1], p);
                if ( n.distance < minValue ){
                    minValue = n.distance;
                    nearest = n;
                }
            }
            return nearest;
        }

    }

}
