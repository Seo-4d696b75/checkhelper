package jp.ac.u_tokyo.t.seo.station.app;

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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.ac.u_tokyo.t.seo.station.app.Line.PolylineSegment;
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
    }

    static class PredictionResult {

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
        if ( mLastLocation.distanceTo(location) < DISTANCE_THRESHOLD ) return;
        mLastLocation = location;

        mExplorer.updateLocation(location.getLongitude(), location.getLatitude());
        double farthest = mService.mInternalRuler.measureDistance(mExplorer.getNearStation(mRadarSize-1), location.getLongitude(), location.getLatitude());

        List<PolylineCursor> list = new LinkedList<>();
        for ( PolylineCursor p : mCursors ) p.update(location, list, mUpdateTime);
        if ( mCursors.size() > 1 ) reduceElement(list, 2);
        mCursors = list;
        Log.d("update", "complete > cursor size: " + list.size());

        List<StationPrediction> resolved = new LinkedList<>();
        List<StationPrediction> predictions = new LinkedList<>();
        for ( PolylineCursor p : list ) p.predict(predictions);
        for ( StationPrediction prediction : predictions ){
            StationPrediction same = getSameStation(resolved, prediction.station);
            if ( same == null ){
                resolved.add(prediction);
            }else{
                same.compareDistance(prediction);
            }
        }
        Collections.sort(resolved);
        mPrediction = resolved;
        int size = Math.min(mMaxPrediction, mPrediction.size());
        PredictionResult result = new PredictionResult(size);
        String date = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date(mUpdateTime));
        Log.d("predict", date + " station size: " + mPrediction.size());
        for ( int i = 0; i < size; i++ ){
            StationPrediction s = mPrediction.get(i);
            result.mPredictions[i] = s;
            Log.d("predict", String.format(Locale.US, "[%d] %.0fm %s", i, s.distance, s.station.name));
        }

        if ( size > 0 && farthest < mPrediction.get(size-1).distance ){
            mExplorer.setSearchProperty(++mRadarSize, 0);
            mService.log("prediction > radar size incremented:" + mRadarSize);
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
        mCursors.add(new PolylineCursor(segment, nearest));
        mLastLocation = location;
    }


    private StationPrediction getSameStation(List<StationPrediction> list, Station station){
        for ( StationPrediction p : list ){
            if ( p.station.equals(station) ) return p;
        }
        return null;
    }

    private void onDirectionChanged(){

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

    private double reduceElement(List<PolylineCursor> list, double threshold){
        double min = Double.MAX_VALUE;
        for ( PolylineCursor p : list ) min = Math.min(min, p.nearest.distance);
        double sum = 0.0;
        for ( ListIterator<PolylineCursor> iterator = list.listIterator(); iterator.hasNext(); ){
            PolylineCursor p = iterator.next();
            if ( p.nearest.distance > min * threshold ){
                iterator.remove();
            }else{
                sum += p.nearest.distance;
            }
        }
        return sum / list.size();
    }

    private class PolylineCursor{

        PolylineCursor(PolylineSegment segment, NearestPoint nearest){
            this.nearest = nearest;
            new EndNode(segment, this);
        }

        private PolylineCursor(PolylineCursor old, Location location, PolylineNode start, PolylineNode end, NearestPoint nearest){
            this.nearest = nearest;
            this.start = start;
            this.end = end;
            if ( !start.point.equals(nearest.start) || !end.point.equals(nearest.end) ){
                throw new IllegalArgumentException();
            }
            //float rate = (float)Math.exp(-location.getAccuracy()/10.0);
            //this.estimatedSpeed = old.estimatedSpeed * (1f-rate) + location.getSpeed() * rate;
            //this.forward = start.point.equals(nearest.start);
            //this.estimatedSpeed = location.getSpeed();
            this.time = old.time;
        }

        private PolylineCursor(PolylineCursor old, boolean forward, Location location){
            this.start = forward ? old.start : old.end;
            this.end = forward ? old.end : old.start;
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            this.nearest = new NearestPoint(start.point, end.point, position);
            //float rate = (float)Math.exp(-location.getAccuracy()/100.0);
            //this.estimatedSpeed = old.estimatedSpeed * (1f-rate) + location.getSpeed() * rate;
            //this.estimatedSpeed = location.getSpeed();
            this.time = old.time;
        }

        private NearestPoint nearest;
        private PolylineNode start, end;
        //private float estimatedSpeed;

        void initPosition(PolylineNode from, PolylineNode to){
            start = from;
            end = to;
        }

        void update(Location location, Collection<PolylineCursor> callback, long time){
            this.time = time;
            // iterate depth-first strategy
            // forward
            List<PolylineCursor> forward = new LinkedList<>();
            iteratePolyline(location, start, end, new PolylineCursor(this, true, location), forward);
            // backward
            List<PolylineCursor> backward = new LinkedList<>();
            iteratePolyline(location, end, start, new PolylineCursor(this, false, location), backward);
            // cut too long path
            List<PolylineCursor> list = null;
            if ( reduceElement(forward, 2) <= reduceElement(backward, 2) ){
                list = forward;
                Log.d("update", String.format(Locale.US, "forward %s size:%d", mCurrentStation.name, list.size()));
            }else{
                onDirectionChanged();
                list = backward;
                Log.d("update", String.format(Locale.US, "backward %s size:%d", mCurrentStation.name, list.size()));
            }
            callback.addAll(list);
        }


        private void iteratePolyline(Location location, PolylineNode previous, PolylineNode next, PolylineCursor min, Collection<PolylineCursor> results){
            NeighborIterator iterator = next.iterator(previous);
            if ( iterator.hasNext() ){
                for ( ; iterator.hasNext(); ){
                    PolylineCursor m = min;
                    PolylineNode neighbor = iterator.next();
                    NearestPoint near = new NearestPoint(next.point, neighbor.point, new LatLng(location.getLatitude(), location.getLongitude()));
                    if ( near.distance < m.nearest.distance ){
                        m = new PolylineCursor(this, location, next, neighbor, near);
                    }else if ( near.distance > m.nearest.distance * 2 && !results.contains(m)){
                        results.add(m);
                        break;
                    }
                    iteratePolyline(location, next, neighbor, m, results);

                }
            }else{
                results.add(min);
            }
        }

        private long time;

        void predict(Collection<StationPrediction> result){
            candidates = new LinkedList<>();
            iteratePolyline(start, nearest.closedPoint, end, mCurrentStation, 0, nearest.distanceTo(), mMaxPrediction);
            result.addAll(candidates);
            candidates = null;
        }


        private List<StationPrediction> candidates;

        private void iteratePolyline(PolylineNode pre, LatLng start, PolylineNode end, Station current, float elapsed, float distance, int cnt){
            final float threshold = 100;
            int iteration = (int)Math.ceil(distance / threshold);
            LatLng previous = start;
            for ( int i = 1; i <= iteration; i++ ){
                double index = (double)i / iteration;
                double lng = end.point.longitude * index + start.longitude * (1.0 - index);
                double lat = end.point.latitude * index + start.latitude * (1.0 - index);
                LatLng next = new LatLng(lat, lng);
                mExplorer.updateLocation(lng, lat);
                if ( !mExplorer.hasInitialized() ) return;
                Station station = mExplorer.getCurrentStation();
                if ( !station.equals(current) ){
                    if ( station.equals(mCurrentStation) ){
                        Log.e("predict", "??");
                    }
                    LatLng b = detectBoundary(previous, next, station, 5);
                    candidates.add(new StationPrediction(station, elapsed + measureDistance(start, b)));
                    current = station;
                    if ( --cnt == 0 ) return;
                }
                previous = next;
            }
            for ( NeighborIterator iterator = end.iterator(pre); iterator.hasNext(); ){
                PolylineNode next = iterator.next();
                iteratePolyline(end, end.point, next, current, elapsed + distance, iterator.distance(), cnt);
            }
        }

        private LatLng detectBoundary(LatLng from, LatLng to, Station detected, int level){
            LatLng mid = new LatLng((from.latitude + to.latitude) / 2, (from.longitude + to.longitude) / 2);
            if ( level <= 0 ) return mid;
            mExplorer.updateLocation(mid.longitude, mid.latitude);
            if ( !mExplorer.hasInitialized() ) return mid;
            if ( mExplorer.getCurrentStation().equals(detected) ){
                return detectBoundary(from, mid, detected, level - 1);
            }else{
                return detectBoundary(mid, to, detected, level - 1);
            }
        }

        void release(){
            start.release();
            end.release();
            start = null;
            end = null;
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
