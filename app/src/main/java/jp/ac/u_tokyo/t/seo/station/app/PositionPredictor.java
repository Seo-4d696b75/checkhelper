package jp.ac.u_tokyo.t.seo.station.app;

import android.app.Service;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.internal.ResourceUtils;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.ac.u_tokyo.t.seo.station.app.Line.PolylineSegment;
import jp.ac.u_tokyo.t.seo.station.diagram.BasePoint;
import jp.ac.u_tokyo.t.seo.station.diagram.Edge;
import jp.ac.u_tokyo.t.seo.station.diagram.Point;


/**
 * @author Seo-4d696b75
 * @version 2019/02/16.
 */

class PositionPredictor{

    PositionPredictor(StationService service, @NonNull Line line){
        target = line;
        mExplorer = new StationKdTree(service, "line");
        mService = service;

        if ( !line.hasPolyline() ){
            throw new AppException("Polyline not set, but prediction required. line:" + line.mCode);
        }

        mLine = line;
        mFragmentJunction = new HashMap<>();
        mPolylineFragments = new LinkedList<>();
        mPrediction = new LinkedList<>();
        for ( Line.PolylineSegment segment : line.getSegments() ){
            setPolylineFragment(segment.mStart, segment);
            setPolylineFragment(segment.mEnd, segment);
            mPolylineFragments.add(segment);
        }
        mExplorer.stop();
        mExplorer.setSearchProperty(mRadarSize, 0);

        mCursors = new LinkedList<>();
        mPrediction = new LinkedList<>();
        mEstimation = new KalmanFilter();
    }

    static class PredictionResult{

        private PredictionResult(int size){
            mPredictions = new StationPrediction[size];
            this.size = size;
        }

        private StationPrediction[] mPredictions;

        final int size;

        Station getStation(int index){
            return mPredictions[index].station;
        }

        float getDistance(int index){
            return mPredictions[index].distance;
        }

    }

    interface StationPredictCallback{

        void onApproachStations(@NonNull PredictionResult result);
    }

    void setCallback(StationPredictCallback callback){
        mCallback = callback;
    }

    void release(){
        mExplorer.stop();
        mExplorer.release();
        mExplorer = null;
        mCallback = null;
        mService = null;
        for ( PolylineCursor p : mCursors ) p.release();
        mPrediction = null;
        mCursors = null;
    }

    private StationService mService;
    private StationKdTree mExplorer;
    private StationPredictCallback mCallback;
    private Line mLine;

    final Line target;


    private void setPolylineFragment(String tag, PolylineSegment fragment){
        List<PolylineSegment> list = null;
        if ( mFragmentJunction.containsKey(tag) ){
            list = mFragmentJunction.get(tag);
        }else{
            list = new LinkedList<>();
            mFragmentJunction.put(tag, list);
        }
        list.add(fragment);
    }


    void setMaxPredictionCnt(int cnt){
        mMaxPrediction = cnt;
    }

    private int mMaxPrediction = 2;

    private Map<String, List<PolylineSegment>> mFragmentJunction;
    private List<PolylineSegment> mPolylineFragments;

    private List<PolylineCursor> mCursors;
    private Station mCurrentStation;
    private List<StationPrediction> mPrediction;

    private Location mLastLocation;
    private final float DISTANCE_THRESHOLD = 5f;
    private KalmanFilter mEstimation;

    private long mUpdateTime;

    private int mRadarSize = 20;

    void onLocationUpdate(Location location, @NonNull Station station){
        mUpdateTime = System.currentTimeMillis();
        if ( mCursors.isEmpty() ){
            initialize(location);
            return;
        }
        if ( mCurrentStation == null || !mCurrentStation.equals(station) ){
            mCurrentStation = station;
        }
        // Update each cursors
        List<PolylineCursor> list = new LinkedList<>();
        for ( PolylineCursor p : mCursors ) p.update(location, list);

        // Filter cursors
        if ( mCursors.size() > 1 ) filterCursors(list, 2);

        mCursors = list;
        Log.d("update", String.format("cursor size: %d, speed: %.0fkm/h", list.size(), list.get(0).state.speed * 3.6));


        if ( mLastLocation.distanceTo(location) < DISTANCE_THRESHOLD ) return;
        mLastLocation = location;

        // prediction の集計
        List<StationPrediction> resolved = new LinkedList<>();
        List<StationPrediction> predictions = new LinkedList<>();
        for ( PolylineCursor p : list ){
            predictions.clear();
            p.predict(predictions);
            // 駅の重複がないように、重複するならより近い距離を採用
            for ( StationPrediction prediction : predictions ){
                StationPrediction same = getSameStation(resolved, prediction.station);
                if ( same == null ){
                    resolved.add(prediction);
                }else{
                    same.compareDistance(prediction);
                }
            }
        }


        // 距離に関して駅をソート
        Collections.sort(resolved);
        mPrediction = resolved;
        int size = Math.min(mMaxPrediction, mPrediction.size());
        // 結果オブジェクトにまとめる
        PredictionResult result = new PredictionResult(size);
        String date = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date(mUpdateTime));
        Log.d("predict", date + " station size: " + mPrediction.size());
        for ( int i = 0; i < size; i++ ){
            StationPrediction s = mPrediction.get(i);
            result.mPredictions[i] = s;
            Log.d("predict", String.format(Locale.US, "[%d] %.0fm %s", i, s.distance, s.station.name));
        }

        mCallback.onApproachStations(result);


    }


    private void initialize(Location location){
        if ( mLine == null ) return;
        NearestPoint nearest = null;
        PolylineSegment segment = null;
        for ( PolylineSegment fragment : mPolylineFragments ){
            NearestPoint n = fragment.findNearestPoint(location.getLongitude(), location.getLatitude());
            if ( nearest == null || nearest.distance > n.distance ){
                nearest = n;
                segment = fragment;
            }
        }
        mCursors.add(new PolylineCursor(segment, nearest, location));
        mLastLocation = location;
    }


    private StationPrediction getSameStation(List<StationPrediction> list, Station station){
        for ( StationPrediction p : list ){
            if ( p.station.equals(station) ) return p;
        }
        return null;
    }

    private class StationPrediction implements Comparable<StationPrediction>{

        private StationPrediction(Station station, float distance){
            this.station = station;
            this.distance = distance;
        }

        final Station station;
        float distance;

        @Override
        public int compareTo(StationPrediction o){
            return Float.compare(this.distance, o.distance);
        }

        void compareDistance(StationPrediction other){
            this.distance = Math.min(this.distance, other.distance);
        }
    }

    private double filterCursors(List<PolylineCursor> list, double threshold){

        double minDistance = Double.MAX_VALUE;
        for ( PolylineCursor cursor : list )
            minDistance = Math.min(minDistance, cursor.nearest.distance);
        for ( Iterator<PolylineCursor> iterator = list.iterator(); iterator.hasNext(); ){
            if ( iterator.next().nearest.distance > minDistance * threshold ){
                iterator.remove();
            }
        }
        return minDistance;
    }

    private class PolylineCursor{

        /**
         * 初期化
         */
        PolylineCursor(PolylineSegment segment, NearestPoint nearest, Location location){
            this.nearest = nearest;
            // initialize node-node graph and start end node
            new EndNode(segment, this);
            this.state = KalmanFilter.Sample.initialize(
                    mUpdateTime,
                    nearest.distanceFrom(),
                    location.hasSpeed() ? location.getSpeed() : 0.0,
                    Math.max(location.getAccuracy(), nearest.distance)
            );
            this.pathLengthSign = 1;
            this.pathPosAtStart = 0;
            this.pathPosAtNearest = nearest.distanceFrom();
        }

        /**
         * 新しい最近傍点でカーソルを新規生成
         *
         * @param start
         * @param end
         * @param nearest      最近傍点 on edge start-end
         * @param pathPosition 符号付経路上の距離@startノード
         */
        private PolylineCursor(PolylineNode start, PolylineNode end, NearestPoint nearest, double pathPosition, PolylineCursor old){
            this.nearest = nearest;
            this.start = start;
            this.end = end;
            if ( !start.point.equals(nearest.start) || !end.point.equals(nearest.end) ){
                throw new IllegalArgumentException();
            }
            this.pathPosAtStart = pathPosition;
            this.pathLengthSign = old.pathLengthSign;
            this.pathPosAtNearest = pathPosition + nearest.distanceFrom() * this.pathLengthSign;
            this.state = old.state;
            this.isSignDecided = old.isSignDecided;
        }

        /**
         * 以前のカーソルから新しい現在位置に対し探索を開始するカーソルを得る
         *
         * @param old      以前のカーソル
         * @param forward  向き
         * @param location 現在位置
         */
        private PolylineCursor(PolylineCursor old, boolean forward, Location location){
            if ( forward ){
                this.start = old.start;
                this.end = old.end;
                this.pathLengthSign = old.pathLengthSign;
                this.pathPosAtStart = old.pathPosAtStart;
            }else{
                this.start = old.end;
                this.end = old.start;
                this.pathLengthSign = -1 * old.pathLengthSign;
                this.pathPosAtStart = old.pathPosAtStart + old.pathLengthSign * old.nearest.edgeDistance;
            }
            this.state = old.state;

            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            this.nearest = new NearestPoint(start.point, end.point, position);
            this.isSignDecided = old.isSignDecided;
            this.pathPosAtNearest = this.pathPosAtStart + this.nearest.distanceFrom() * this.pathLengthSign;
        }

        private NearestPoint nearest;
        private PolylineNode start, end;
        // start -> end 方向を正方向とし、startを原点とする1次元座標で測定する
        private KalmanFilter.Sample state;
        private int pathLengthSign;
        private double pathPosAtStart, pathPosAtNearest;
        private boolean isSignDecided = false;

        void initPosition(PolylineNode from, PolylineNode to){
            start = from;
            end = to;
        }

        void update(Location location, Collection<PolylineCursor> callback){

            // 1. Search for the nearest point on this polyline to the given location in all the directions
            List<PolylineCursor> list1 = new LinkedList<>();
            List<PolylineCursor> list2 = new LinkedList<>();
            double v1 = searchForNearest(location, start, end, new PolylineCursor(this, true, location), list1, this.pathPosAtStart + nearest.edgeDistance * pathLengthSign);
            double v2 = searchForNearest(location, end, start, new PolylineCursor(this, false, location), list2, this.pathPosAtStart);
            List<PolylineCursor> found = v1 <= v2 ? list1 : list2;

            // 2. Estimate current position using Kalman filter
            //    so that high-freq. noise should be removed
            for ( PolylineCursor cursor : found ){
                KalmanFilter.Sample next = mEstimation.update(
                        cursor.pathPosAtNearest,
                        Math.max(cursor.nearest.distance, location.getAccuracy()),
                        mUpdateTime, cursor.state
                );
                // 3. Check estimated position
                boolean reverse = next.speed * cursor.pathLengthSign < 0;
                if ( cursor.isSignDecided && next.speed < 10 ){
                    // 一度進み始めた方向はそう変化しないはず
                    reverse = cursor.pathLengthSign * this.pathLengthSign < 0;
                }
                if ( !cursor.isSignDecided && next.speed > 10 ){
                    cursor.isSignDecided = true;
                }
                if ( reverse ){
                    // direction reversed
                    PolylineNode tmp = cursor.start;
                    cursor.start = cursor.end;
                    cursor.end = tmp;
                    cursor.pathLengthSign *= -1;
                    cursor.nearest = new NearestPoint(cursor.start.point, cursor.end.point, location);
                }
                if ( cursor.pathLengthSign * this.pathLengthSign < 0 ){
                    Log.d("predict", "direction changed");
                }
                cursor.state = next;
            }
            callback.addAll(found);
        }

        /**
         * 指定された方向へ探索して最近傍点を探す　枝分かれがある場合は枝分かれの数だけ探す
         */
        private double searchForNearest(Location location, PolylineNode previous, PolylineNode current, PolylineCursor min, Collection<PolylineCursor> results, double pathPosition){
            NeighborIterator iterator = current.iterator(previous);
            double minDist = Double.MAX_VALUE;
            if ( iterator.hasNext() ){
                for ( ; iterator.hasNext(); ){
                    PolylineNode next = iterator.next();
                    NearestPoint near = new NearestPoint(current.point, next.point, new LatLng(location.getLatitude(), location.getLongitude()));

                    if ( near.distance > min.nearest.distance * 2 ){
                        // 探索終了
                        if ( !results.contains(min) ) results.add(min);
                        minDist = Math.min(minDist, min.nearest.distance);
                    }else{
                        if ( near.distance < min.nearest.distance ){
                            min = new PolylineCursor(
                                    current, next, near,
                                    pathPosition,
                                    min
                            );
                        }
                        // 深さ優先探索
                        double v = searchForNearest(location, current, next, min, results, pathPosition + iterator.distance() * min.pathLengthSign);
                        minDist = Math.min(minDist, v);
                    }
                }
            }else{
                // 路線の終点 -> 探索終了
                if ( !results.contains(min) ) results.add(min);
                minDist = min.nearest.distance;
            }
            return minDist;
        }

        void predict(Collection<StationPrediction> result){
            mExplorer.updateLocation(this.nearest.closedPoint.longitude, this.nearest.closedPoint.latitude);
            if ( !mExplorer.hasInitialized() ) return;
            Station current = mExplorer.getCurrentStation();
            int cnt = mMaxPrediction;
            if ( !current.equals(mCurrentStation) ){
                result.add(new StationPrediction(current, 0f));
                cnt--;
            }
            searchForStation(start, this.nearest.closedPoint, end, current, 0, cnt, result);
        }

        private void searchForStation(PolylineNode previous, LatLng start, PolylineNode end, Station current, float pathLength, int cnt, Collection<StationPrediction> result){
            boolean loop = true;
            while ( loop ){
                // check start < end
                if ( start.equals(end.point) ){
                    break;
                }
                StationMapFragment.StationArea area = current.mAreaData;
                LatLng a = area.points[area.hasEnclosed ? area.points.length - 1 : 0];
                int i = area.hasEnclosed ? 0 : 1;
                loop = false;
                Edge e1 = new Edge(
                        new BasePoint(start.longitude, start.latitude),
                        new BasePoint(end.point.longitude, end.point.latitude)
                );
                for ( ; i < area.points.length; i++ ){
                    LatLng b = area.points[i];
                    // 1. Check whether edge start-end goes over boundary a-b
                    Edge e2 = new Edge(
                            new BasePoint(a.longitude, a.latitude),
                            new BasePoint(b.longitude, b.latitude)
                    );
                    Point intersection = e1.getIntersection(e2);
                    // 2. If so, detect the intersection and add to prediction list
                    if ( intersection != null &&
                            (intersection.getX() - start.longitude) * (end.point.longitude - start.longitude) +
                                    (intersection.getY() - start.latitude) * (end.point.latitude - start.latitude) > 0 ){
                        // Calc coordinate of another station
                        double index = ((current.longitude - b.longitude) * (a.longitude - b.longitude) + (current.latitude - b.latitude) * (a.latitude - b.latitude))
                                / (Math.pow(a.longitude - b.longitude, 2) + Math.pow(a.latitude - b.latitude, 2));
                        double x = (1 - index) * b.longitude + index * a.longitude;
                        double y = (1 - index) * b.latitude + index * a.latitude;
                        double lng = 2 * x - current.longitude;
                        double lat = 2 * y - current.latitude;
                        Station next = null;
                        // Search for which station was detected
                        for ( int code : current.next ){
                            Station neighbor = mService.getStation(code);
                            if ( measureDistance(neighbor.latitude, neighbor.longitude, lat, lng) < 1 ){
                                next = neighbor;
                                break;
                            }
                        }
                        if ( next == null ) throw new RuntimeException();
                        // Update for next station
                        float dist = measureDistance(start.latitude, start.longitude, intersection.getY(), intersection.getX());
                        StationPrediction prediction = new StationPrediction(next, pathLength + dist);
                        result.add(prediction);
                        if ( next.equals(mCurrentStation) ){
                            Log.d("predict", "same station");
                        }
                        if ( --cnt <= 0 ) return;
                        pathLength += dist;
                        index = 1.0 / measureDistance(intersection.getY(), intersection.getX(), start.latitude, start.longitude);
                        start = new LatLng(
                                (1 + index) * intersection.getY() - index * start.latitude,
                                (1 + index) * intersection.getX() - index * start.longitude
                        );
                        current = next;
                        loop = true;
                        break;
                    }
                    a = b;
                }
            }

            for ( NeighborIterator iterator = end.iterator(previous); iterator.hasNext(); ){
                PolylineNode next = iterator.next();
                searchForStation(end, end.point, next, current, pathLength + measureDistance(start, end.point), cnt, result);
            }
        }


        void release(){
            start.release();
            end.release();
            start = null;
            end = null;
        }

        @Override
        public boolean equals(Object obj){
            if ( obj instanceof PolylineCursor ){
                PolylineCursor other = (PolylineCursor)obj;
                return (this.start.equals(other.start) && this.end.equals(other.end)) ||
                        (this.start.equals(other.end) && this.end.equals(other.start));
            }
            return false;
        }
    }


    private static abstract class PolylineNode{

        PolylineNode(LatLng point){
            this.point = point;
        }

        final LatLng point;

        abstract NeighborIterator iterator(PolylineNode previous);

        abstract void release();

        abstract void setNext(PolylineNode next, float distance);

        @Override
        public boolean equals(Object o){
            if ( o instanceof PolylineNode ){
                PolylineNode node = (PolylineNode)o;
                return node.point.equals(this.point);
            }
            return false;
        }


    }

    private interface NeighborIterator{
        boolean hasNext();

        PolylineNode next();

        float distance();
    }

    private class EndNode extends PolylineNode{

        private EndNode(LatLng point, PolylineSegment segment, String tag){
            super(point);
            this.segment = segment;
            this.tag = tag;
            size = 0;
            hasChecked = false;
        }

        EndNode(PolylineSegment segment, PolylineCursor cursor){
            this(segment.mPoints[0], segment, segment.mStart);
            expand(segment, cursor);
        }

        final PolylineSegment segment;
        final String tag;

        private PolylineNode[] next = new PolylineNode[3];
        private float[] distance = new float[3];
        private int size;
        private boolean hasChecked;
        private boolean hasReleased;

        @Override
        void setNext(PolylineNode next, float distance){
            if ( size >= 3 ){
                throw new RuntimeException();
            }else{
                this.next[size] = next;
                this.distance[size] = distance;
                size++;
            }
        }

        private void expand(PolylineSegment segment, PolylineCursor cursor){
            boolean forward = segment.mStart.equals(tag);
            int start = forward ? 1 : segment.mPoints.length - 2;
            int end = forward ? segment.mPoints.length - 1 : 0;
            int dir = forward ? 1 : -1;
            PolylineNode previous = this;
            int index = 0;
            for ( int i = start; i != end + dir; i += dir ){
                PolylineNode node = (i == end) ?
                        new EndNode(segment.mPoints[end], segment, forward ? segment.mEnd : segment.mStart)
                        : new MiddleNode(segment.mPoints[i], index++);
                float distance = measureDistance(previous.point, node.point);
                previous.setNext(node, distance);
                node.setNext(previous, distance);
                if ( cursor != null ){
                    if ( cursor.nearest.start.equals(previous.point) && cursor.nearest.end.equals(node.point) ){
                        cursor.initPosition(previous, node);
                    }else if ( cursor.nearest.start.equals(node.point) && cursor.nearest.end.equals(previous.point) ){
                        cursor.initPosition(node, previous);
                    }
                }
                previous = node;
            }
        }

        @Override
        NeighborIterator iterator(PolylineNode previous){
            if ( size <= 0 ) throw new RuntimeException();
            if ( !hasChecked ){
                for ( PolylineSegment segment : mFragmentJunction.get(tag) ){
                    if ( segment.equals(this.segment) ) continue;
                    expand(segment, null);
                }
                hasChecked = true;
            }
            return new JunctionIterator(previous);
        }

        private class JunctionIterator implements NeighborIterator{

            JunctionIterator(PolylineNode previous){
                this.previous = previous;
                searchForNext();
            }

            final PolylineNode previous;
            int index = -1;
            int nextIndex = -1;

            void searchForNext(){
                nextIndex++;
                while ( nextIndex < size ){
                    PolylineNode node = next[nextIndex];
                    LatLng v1 = previous.point;
                    LatLng v2 = EndNode.this.point;
                    LatLng v3 = node.point;
                    double v = (v1.longitude - v2.longitude) * (v3.longitude - v2.longitude)
                            + (v1.latitude - v2.latitude) * (v3.latitude - v2.latitude);
                    if ( !node.equals(previous) && v < 0 ) break;
                    nextIndex++;
                }
            }


            @Override
            public boolean hasNext(){
                return nextIndex < size;
            }

            @Override
            public PolylineNode next(){
                if ( nextIndex >= size ){
                    throw new NoSuchElementException();
                }else{
                    index = nextIndex;
                    searchForNext();
                    return next[index];
                }
            }

            @Override
            public float distance(){
                if ( index < 0 ){
                    throw new NoSuchElementException();
                }else{
                    return distance[index];
                }
            }
        }

        @Override
        void release(){
            if ( !hasReleased ){
                hasReleased = true;
                for ( int i = 0; i < size; i++ ) next[i].release();
                next = null;
                distance = null;
                hasChecked = false;
                size = -1;
            }
        }

        @Override
        public boolean equals(Object o){
            if ( o instanceof EndNode ){
                EndNode e = (EndNode)o;
                return e.tag.equals(this.tag) && e.point.equals(this.point);
            }
            return false;
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US, "EndNode{lat/lon:(%.6f,%.6f), tag:%s, size:%s}",
                    point.latitude, point.longitude,
                    tag,
                    hasChecked ? String.valueOf(size) : "??"
            );
        }
    }

    private static class MiddleNode extends PolylineNode{

        MiddleNode(LatLng point, int index){
            super(point);
            this.index = index;
        }

        private PolylineNode next1, next2;
        private float distance1, distance2;
        private boolean hasReleased;
        final int index;

        @Override
        void setNext(PolylineNode next, float distance){
            if ( next1 == null ){
                next1 = next;
                distance1 = distance;
            }else if ( next2 == null ){
                next2 = next;
                distance2 = distance;
            }else{
                throw new RuntimeException();
            }
        }

        @Override
        NeighborIterator iterator(PolylineNode previous){
            final boolean former = previous.equals(next2);
            if ( !former && !previous.equals(next1) ){
                throw new RuntimeException();
            }
            return new NeighborIterator(){

                private boolean hasIterated = false;

                @Override
                public boolean hasNext(){
                    return !hasIterated;
                }

                @Override
                public PolylineNode next(){
                    if ( hasIterated ){
                        throw new NoSuchElementException();
                    }else{
                        hasIterated = true;
                        return former ? next1 : next2;
                    }
                }

                @Override
                public float distance(){
                    if ( hasIterated ){
                        return former ? distance1 : distance2;
                    }else{
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        void release(){
            if ( !hasReleased ){
                hasReleased = true;
                next1.release();
                next2.release();
                next1 = null;
                next2 = null;
            }
        }

        @Override
        public boolean equals(Object o){
            return o instanceof PolylineNode && ((PolylineNode)o).point.equals(this.point);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US, "MiddleNode{lat/lon:(%.6f,%.6f), index:%d}",
                    point.latitude, point.longitude, index
            );
        }
    }


    static class NearestPoint{

        NearestPoint(LatLng start, LatLng end, Location point){
            this(start, end, new LatLng(point.getLatitude(), point.getLongitude()));
        }

        NearestPoint(LatLng start, LatLng end, LatLng point){
            this.start = start;
            this.end = end;
            double v1 = (point.longitude - start.longitude) * (end.longitude - start.longitude) + (point.latitude - start.latitude) * (end.latitude - start.latitude);
            double v2 = (point.longitude - end.longitude) * (start.longitude - end.longitude) + (point.latitude - end.latitude) * (start.latitude - end.latitude);
            if ( v1 >= 0 && v2 >= 0 ){
                isOnEdge = true;
                index = v1 / (Math.pow(start.longitude - end.longitude, 2) + Math.pow(start.latitude - end.latitude, 2));
            }else if ( v1 < 0 ){
                isOnEdge = false;
                index = 0;
            }else{
                isOnEdge = false;
                index = 1;
            }
            double lon = (1 - index) * start.longitude + index * end.longitude;
            double lat = (1 - index) * start.latitude + index * end.latitude;
            closedPoint = new LatLng(lat, lon);
            distance = measureDistance(closedPoint, point);
            edgeDistance = measureDistance(start, end);
        }


        final LatLng start, end;
        final double index;
        final float distance;
        final float edgeDistance;
        final LatLng closedPoint;
        final boolean isOnEdge;

        float distanceFrom(){
            return edgeDistance * (float)index;
        }

        float distanceTo(){
            return edgeDistance * (1 - (float)index);
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "lat/lon:(%.6f,%.6f) - %.2fm",
                    closedPoint.latitude, closedPoint.longitude, distance
            );
        }

    }

    private static float measureDistance(LatLng p1, LatLng p2){
        return measureDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
    }

    private static float measureDistance(double lat1, double lon1, double lat2, double lon2){
        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        return result[0];
    }

}
