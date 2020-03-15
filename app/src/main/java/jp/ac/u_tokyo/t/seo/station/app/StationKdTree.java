package jp.ac.u_tokyo.t.seo.station.app;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2020/03/11.
 */
class StationKdTree{

    private StationService mService;
    private String mName;
    private DistanceRuler mRuler;

    StationKdTree(StationService service, String name){
        this.mService = service;
        this.mName = name;
        this.mRuler = service.mInternalRuler;

        mRoot = service.getStationTreeRoot();
    }

    interface StationUpdateCallback{
        void onDetectStation(@Nullable Station station);

        void onExploreStop(String mes);
    }


    private StationUpdateCallback mUpdateCallback;

    void setStationUpdateCallback(StationUpdateCallback callback){
        mUpdateCallback = callback;
    }


    private void log(String log){
        mService.log(String.format(Locale.US, "%s:%s", mName, log));
    }

    private void onError(String log, String mes){
        log = String.format(Locale.US, "%s:%s", mName, log);
        if ( mUpdateCallback != null ){
            mUpdateCallback.onExploreStop(log);
        }
        mInitialized = false;
        Toast.makeText(mService, mes, Toast.LENGTH_SHORT).show();
    }

    static class StationNode{

        StationNode(StationService service, JSONObject data, SparseArray<JSONObject> nodeList, int depth) throws JSONException{
            mDepth = depth;
            mCode = data.getInt("code");
            build(service, data, nodeList);
        }

        private void build(StationService service, JSONObject data, SparseArray<JSONObject> nodeList) throws JSONException {
            if ( data.has("segment") ){
                mSegmentName = data.getString("segment");
                mService = service;
            }else{
                mStation = service.getStation(mCode);
                if ( data.has("left") ){
                    JSONObject left = nodeList.get(data.getInt("left"));
                    if ( left == null ) throw new AppException("node not found. code" + data.getInt("left"));
                    mLeft = new StationNode(service, left, nodeList, mDepth+1);
                }
                if ( data.has("right") ) {
                    JSONObject right = nodeList.get(data.getInt("right"));
                    if ( right == null ) throw new AppException("node not found. code" + data.getInt("right"));
                    mRight = new StationNode(service, right, nodeList, mDepth+1);
                }
            }
        }

        void release(){
            mService = null;
            mStation = null;
            if ( mLeft != null ) mLeft.release();
            if ( mRight != null ) mRight.release();
            mLeft = null;
            mRight = null;
        }

        int mDepth;

        String mSegmentName;
        StationService mService;

        int mCode;
        Station mStation;

        StationNode mLeft, mRight;

        @NonNull
        Station getStation(){
            if ( mStation == null ){
                try{
                    JSONObject data = mService.getStationTreeSegment(mSegmentName);
                    if ( data.getInt("root") != mCode ){
                        throw new AppException("root mismatch. name:" + mSegmentName);
                    }
                    SparseArray<JSONObject> nodes = new SparseArray<>();
                    JSONArray array = data.getJSONArray("node_list");
                    for ( int i=0 ; i<array.length() ; i++ ){
                        JSONObject object = array.getJSONObject(i);
                        nodes.put(object.getInt("code"), object);
                    }
                    build(mService, nodes.get(mCode), nodes);
                    Log.d("station-node","IO has done. segment-name:" + mSegmentName);
                }catch( JSONException e ){
                    throw new AppException("fail to expand station tree. name:" + mSegmentName, e);
                }
                mSegmentName = null;
                mService = null;
            }
            return  mStation;
        }

    }

    private boolean mInitialized = false;
    private Station mCurrentStation;
    private int mSearchK = 0;
    private double mSearchRadius = 0;
    private StationNode mRoot;

    private double mSearchLat, mSearchLng;
    private List<NeighborStation> mSearchList;



    /**
     * Gets the nearest station.
     * <strong>NOTE</strong> This explorer has to be initialized. check here : {@link #hasInitialized()}
     *
     * @return station object whose distance from user position is minimum.
     */
    @NonNull
    synchronized Station getCurrentStation(){
        if ( !mInitialized ){
            throw new IllegalStateException("Explore not initialized yet");
        }
        return mCurrentStation;
    }

    synchronized boolean hasInitialized(){
        return mInitialized;
    }

    synchronized void setSearchProperty(int k, double radius){
        if ( k > mSearchK || radius > mSearchRadius ) mInitialized = false;
        if ( k != mSearchK || radius != mSearchRadius ){
            mSearchK = k;
            mSearchRadius = radius;
        }
    }


    /**
     * Gets station which is N-th closest from user position.
     *
     * @param index the index counted from 0.
     * @return N-th element of station list, whose items are sorted with the distance measured from user position.
     */
    @NonNull
    synchronized Station getNearStation(int index){
        if ( !mInitialized ) throw new IllegalStateException("Explorer not initialize yet.");
        if ( 0 <= index && index < mSearchList.size()){
            return mSearchList.get(index).mStation;
        }else{

            throw new IndexOutOfBoundsException(String.format(Locale.US, "request:%d, size:%d", index, mSearchList.size()));
        }
    }

    List<Station> getNearStations(){
        return getNearStations(mSearchList.size());
    }

    synchronized List<Station> getNearStations(int size){
        List<Station> list = new LinkedList<>();
        if ( size <= 0 || size > mSearchList.size() ){
            throw new IllegalArgumentException("list size out of bounds.");
        }else{
            for ( int i=0 ; i<size ; i++ ) list.add(mSearchList.get(i).mStation);
        }
        return list;
    }

    synchronized List<Line> getNearLines(){
        List<Line> list = new ArrayList<>();
        for ( NeighborStation n : mSearchList ){
            for ( Line line : n.mStation.lines ){
                if ( !list.contains(line) ){
                    list.add(line);
                }
            }
        }
        return list;
    }


    synchronized void stop(){
        mCurrentStation = null;
        mInitialized = false;
    }

    void release(){
        mService = null;
        mUpdateCallback = null;
        mInitialized = false;
        mRoot = null;
        mSearchList = null;
    }

    private static class NeighborStation {

        NeighborStation(Station station, double dist){
            mStation = station;
            mDist = dist;
        }

        final Station mStation;
        final double mDist;

    }


    synchronized void updateLocation(double longitude, double latitude){
        if ( mService == null ) return;
        if ( mSearchK < 1 ) throw new AppException("Explorer k not initialized.");
        long time = SystemClock.elapsedRealtimeNanos();
        mSearchLat = latitude;
        mSearchLng = longitude;
        mSearchList = new LinkedList<>();
        search(mRoot);
        mInitialized = true;
        final Station next = mSearchList.get(0).mStation;
        Log.d("update-location",String.format(Locale.US, "duration: %.3f(ms)", (SystemClock.elapsedRealtimeNanos() - time)/1000000.0));
        if ( mCurrentStation == null || !mCurrentStation.equals(next) ){
            mCurrentStation = next;
            if ( mUpdateCallback != null ){
                mUpdateCallback.onDetectStation(next);
            }
        }
    }

    void search(StationNode node){
        if ( node == null ) return;
        Station station = node.getStation();
        double d = mRuler.measure(mSearchLng, mSearchLat, station.longitude, station.latitude);
        int index = -1;
        int size = mSearchList.size();
        if ( size > 0 && d < mSearchList.get(size-1).mDist ){
            index = size - 1;
            while ( index > 0 ){
                if ( d >= mSearchList.get(index - 1).mDist ) break;
                index--;
            }
        } else if ( size == 0 ){
            index = 0;
        }
        if ( index >= 0 ){
            mSearchList.add(index, new NeighborStation(station, d));
            /*
            mSearchList に距離昇順に格納された結果に関して、
            (i) サイズが mSearchK 以上
            (ii) 距離 mSearchSqrtDist 以内はすべて含まれる
            の両条件を満たす
             */
            if ( size >= mSearchK && mSearchList.get(size).mDist > mSearchRadius ) mSearchList.remove(size);
        }

        boolean x = node.mDepth%2 == 0;
        double value = x ? mSearchLng : mSearchLat;
        double threshold = x ? station.longitude : station.latitude;
        search( value < threshold ? node.mLeft : node.mRight);
        if ( Math.abs(value - threshold) < Math.max(mSearchList.get(mSearchList.size()-1).mDist, mSearchRadius) ){
            search( value < threshold ? node.mRight : node.mLeft);
        }

    }

}
