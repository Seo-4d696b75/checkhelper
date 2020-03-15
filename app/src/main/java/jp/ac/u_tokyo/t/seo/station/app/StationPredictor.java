package jp.ac.u_tokyo.t.seo.station.app;

import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;



/**
 * @author Seo-4d696b75
 * @version 2019/02/16.
 */

class StationPredictor {

    StationPredictor(StationService service){
        mExplorer = new StationKdTree(service, "hoge");
        mService = service;
    }

    interface StationPredictCallback{
        void onStationPredicted(Station station, int sec);
    }

    void setCallback(StationPredictCallback callback){
        mCallback = callback;
    }

    void release(){
        mExplorer.stop();
        mExplorer.release();
        mExplorer = null;
        mCallback = null;
        mLine = null;
        mService = null;
        for ( StationPrediction p : mPrediction ) p.release();
    }

    private StationService mService;
    private StationKdTree mExplorer;
    private StationPredictCallback mCallback;
    private Line mLine;

    void setLine(Line line){
        if ( line == null ){
            mLine = null;
            mExplorer.stop();
            mFragmentJunction = null;
            mPolylineFragments = null;
            mPrediction = null;
            return;
        }
        if ( !line.hasDetails() ){
            throw new IllegalArgumentException("no line details at StationPredictor#setLine(Line)");
        }
        mLine = line;
        mFragmentJunction = new HashMap<>();
        mPolylineFragments = new LinkedList<>();
        mPrediction = new LinkedList<>();
        for ( Line.PolylineSegment segment : line.getSegments() ){
            PolylineFragment fragment = new PolylineFragment(segment);
            setPolylineFragment(segment.mStart, fragment);
            setPolylineFragment(segment.mEnd, fragment);
            mPolylineFragments.add(fragment);
        }
        mExplorer.stop();
        mExplorer.setSearchProperty(1, 0);
    }

    private void setPolylineFragment(String tag, PolylineFragment fragment){
        List<PolylineFragment> list = null;
        if ( mFragmentJunction.containsKey(tag) ){
            list = mFragmentJunction.get(tag);
        }else{
            list = new LinkedList<>();
            mFragmentJunction.put(tag, list);
        }
        list.add(fragment);
    }

    void setMaxPredictionTime(int sec){
        mMaxSec = sec;
    }

    private int mMaxSec = 10;
    private final int[] TIME = {1, 2, 3, 5, 10, 20, 30};

    private Map<String, List<PolylineFragment>> mFragmentJunction;
    private List<PolylineFragment> mPolylineFragments;

    private PolylineResolver mResolver;
    private Station mCurrentStation;
    private List<StationPrediction> mPrediction;
    private List<StationPrediction> mNextPrediction;


    private final float SPEED_THRESHOLD = 2f;
    private final float DISTANCE_THRESHOLD = 1f;

    void onLocationUpdate(Location location, @NonNull Station station){
        if ( mLine == null )
            throw new IllegalStateException("line not initialized yet at #onLocationUpdate()");
        if ( mResolver == null ){
            initialize(location);
            return;
        }
        if ( !location.hasSpeed() ){
            mService.log("hasSpeed() -> false");
            return;
        }
        Log.d("Speed", String.format(Locale.US, "%fm/s", location.getSpeed()));
        if ( mCurrentStation == null || !mCurrentStation.equals(station) ){
            mCurrentStation = station;
        }
        float margin = 1000f;
        mNextPrediction = new LinkedList<>();
        mResolver.update(location, margin);

        for ( StationPrediction prediction : mNextPrediction ){
            StationPrediction old = getSameStation(mPrediction, prediction.station);
            if ( old == null ){
                prediction.schedule();
                mPrediction.add(prediction);
            }else{
                old.update(prediction);
            }
        }

    }

    private final StationPredictCallback receiver = new StationPredictCallback(){
        @Override
        public void onStationPredicted(final Station station, final int sec){
            if ( mCallback == null ) return;
            mService.getMainHandler().post(new Runnable(){
                @Override
                public void run(){
                    mCallback.onStationPredicted(station, sec);
                }
            });
        }
    };

    private void initialize(Location location){
        NearestPoint nearest = null;
        double minValue = Double.MAX_VALUE;
        for ( PolylineFragment fragment : mPolylineFragments ){
            NearestPoint n = fragment.findNearestPoint(location.getLongitude(), location.getLatitude());
            if ( minValue > n.squareDistance ){
                minValue = n.squareDistance;
                nearest = n;
            }
        }
        mResolver = new PolylineResolver(nearest, location);
        mPrediction = new LinkedList<>();
    }

    private void onPredicted(StationPrediction prediction){
        if ( canAdded(prediction) ) mNextPrediction.add(prediction);
    }

    private boolean canAdded(StationPrediction prediction){
        for ( Iterator<StationPrediction> iterator = mNextPrediction.iterator() ; iterator.hasNext() ; ){
            StationPrediction s = iterator.next();
            if ( s.station.equals(prediction.station) ){
                if ( prediction.time < s.time ){
                    iterator.remove();
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private StationPrediction getSameStation(List<StationPrediction> list, Station station){
        for ( StationPrediction p : list ){
            if ( p.station.equals(station)) return p;
        }
        return null;
    }

    private class StationPrediction{

        private StationPrediction(Station station, long time, StationPrediction next){
            this.station = station;
            this.time = time;
            this.duration = next == null ? -1 : next.time - time;
        }

        final Station station;
        private long time;
        private long duration;
        private Timer timer;

        void update(StationPrediction latest){
            if ( !station.equals(latest.station) )return;
            this.time = latest.time;
            this.duration = latest.duration;
            schedule();
        }

        void schedule(){
            long now = System.currentTimeMillis();
            if ( now > time ) return;
            if ( timer != null ) timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask(){
                @Override
                public void run(){
                    long now = System.currentTimeMillis();
                    for ( int t : TIME ){
                        if (  t <= mMaxSec && Math.abs(now + t - time) < 100 ){
                            receiver.onStationPredicted(station, t);
                            break;
                        }
                    }
                }
            }, (time-now)%1000, 1000);
        }

        void release(){
            if ( timer != null ){
                timer.cancel();
                timer = null;
            }
        }

    }

    private class PolylineResolver{

        private PolylineResolver(NearestPoint n, Location point){
            mCurrent = n;
            mLastPoint = point;
        }

        private NearestPoint mCurrent;
        private Location mLastPoint;
        private List<PredictedLine> mLines;

        void update(Location location, float meter){
            float speed = location.getSpeed();
            if ( speed > SPEED_THRESHOLD && mLastPoint.distanceTo(location) > DISTANCE_THRESHOLD ){
                //int direction = mCurrent.estimateDirection(location, mLastPoint);
                if ( mLines == null ){
                    //onDirectionChanged(direction);
                    //initialize
                }
                mLastPoint = location;
            }else{
                return;
            }
            for ( ListIterator<PredictedLine> iterator = mLines.listIterator(); iterator.hasNext(); ){
                PredictedLine p = iterator.next();
                p.resolveNext(iterator, meter);
            }
            NearestPoint min = null;
            for ( PredictedLine line : mLines ){
                NearestPoint n = line.update(location);
                if ( min == null || n.squareDistance < min.squareDistance ){
                    min = n;
                }
            }
            if ( mLines.size() > 1 && min != null ){
                for ( Iterator<PredictedLine> iterator = mLines.iterator(); iterator.hasNext(); ){
                    PredictedLine line = iterator.next();
                    if ( line.point.squareDistance > min.squareDistance * 4 ) iterator.remove();
                    //TODO 現在の区間が同じ場合でも削除する
                }
            }
            /*
            if ( mLines.size() > 1 ){
                Set<PredictedLine> set = new LinkedHashSet<>(mLines);
                mLines = new LinkedList<>();
                mLines.addAll(set);
            }*/
            mCurrent = min;
            for ( PredictedLine line : mLines ) line.predict();
        }



    }


    private class PredictedLine{

        private PredictedLine(NearestPoint point, int direction){
            if ( Math.abs(direction) != 1 ) throw new IllegalArgumentException("Invalid direction");
            this.point = point;
            PolylineFragment fragment = point.fragment;
            fragments = new LinkedList<>();
            fragments.add(fragment);
            this.index = direction > 0 ? point.startIndex : point.endIndex;
            this.currentDirection = direction;
            this.lastDirection = direction;
            distance = fragment.remainDistance(index + direction, direction);
            predictStation();
        }

        private PredictedLine(PredictedLine old, PolylineFragment fragment, String tag){
            this.point = old.point;
            this.fragments = new LinkedList<>();
            this.fragments.addAll(old.fragments);
            this.fragments.add(fragment);
            this.distance = old.distance + fragment.mDistance;
            this.currentDirection = old.currentDirection;
            this.index = old.index;
            this.lastDirection = fragment.mStartTag.equals(tag) ? 1 : -1;
            this.next1 = old.next1;
            this.next2 = old.next2;
        }

        private NearestPoint point;
        private List<PolylineFragment> fragments;
        private int currentDirection;
        private int index;
        private int lastDirection;
        private float distance;

        private Location lastLocation;
        private PredictedStation next1, next2;

        private class PredictedStation{

            PredictedStation(Station station, float distance){
                this.station = station;
                this.distance = distance;
            }

            final Station station;
            float distance;

            @Override
            public String toString(){
                return String.format(
                        Locale.US,
                        "%s in %.0fm",
                        station.name, distance
                );
            }
        }

        private class PointIterator{
            private PointIterator(){
                fragmentIndex = 0;
                direction = PredictedLine.this.currentDirection;
                this.index = PredictedLine.this.index;
                current = new LatLng(point.lat, point.lon);
                previous = null;
                fragment = fragments.get(0);
                array = fragment.mPoints;
                distance = 0f;
                elapsed = -point.distance(-direction);
            }

            private int index;
            private int fragmentIndex;
            private PolylineFragment fragment;
            private LatLng[] array;
            private int direction;
            private LatLng current;
            private LatLng previous;
            private float distance;
            private float elapsed;


            boolean hasNext(){
                return fragmentIndex < fragments.size() - 1 || (index < array.length-1 && index > 0);
            }

            LatLng next(){
                index += direction;
                if ( index >= array.length || index < 0 ){
                    String tag = direction > 0 ? fragment.mEndTag : fragment.mStartTag;
                    fragment = fragments.get(++fragmentIndex);
                    array = fragment.mPoints;
                    if ( fragment.mStartTag.equals(tag) ){
                        index = 1;
                        direction = 1;
                    }else{
                        index = array.length - 2;
                        direction = -1;
                    }
                }
                elapsed += distance;
                distance = fragment.mDistanceArray[direction > 0 ? index-1 : index];

                previous = current;
                current = array[index];
                return current;
            }

            LatLng getCurrent(){
                return current;
            }

            LatLng getPrevious(){
                return previous;
            }

            int getStartIndex(){
                return direction > 0 ? index - 1 : index;
            }

            int getEndIndex(){
                return direction > 0 ? index : index + 1;
            }

            int getDirection(){
                return direction;
            }

            float getDistance(){
                return distance;
            }

            float getElapsedDistance(){
                return elapsed;
            }

            PolylineFragment getCurrentFragment(){
                return fragment;
            }
        }


        void resolveNext(ListIterator<PredictedLine> iterator, float meter){
            if ( meter <= distance ) return;
            iterator.remove();
            PolylineFragment fragment = fragments.get(fragments.size() - 1);
            String tag = lastDirection > 0 ? fragment.mEndTag : fragment.mStartTag;
            for ( PolylineFragment f : mFragmentJunction.get(tag) ){
                if ( fragment.isConnectedSmooth(f, tag) ){
                    PredictedLine next = new PredictedLine(this, f, tag);
                    iterator.add(next);
                    iterator.previous();
                }
            }
        }

        NearestPoint update(Location location){
            LatLng p = new LatLng(location.getLatitude(), location.getLongitude());
            // iterate polyline to find out nearest point
            NearestPoint min = null;
            double minDistance = Double.MAX_VALUE;
            float elapsed = 0f;
            int nextDirection = 0;
            for ( PointIterator pointIterator = new PointIterator(); pointIterator.hasNext(); ){
                pointIterator.next();
                NearestPoint n = new NearestPoint(
                        pointIterator.getStartIndex(),
                        pointIterator.getEndIndex(),
                        p,
                        pointIterator.getCurrentFragment()
                );
                if ( n.squareDistance < minDistance ){
                    minDistance = n.squareDistance;
                    min = n;
                    elapsed = pointIterator.getElapsedDistance();
                    nextDirection = pointIterator.getDirection();
                }else if ( n.squareDistance > minDistance * 4 ){
                    break;
                }
            }
            if ( min == null ) throw new RuntimeException("#update > fail");
            // remove fragment if need
            for ( Iterator<PolylineFragment> iterator = fragments.iterator(); iterator.hasNext(); ){
                PolylineFragment fragment = iterator.next();
                if ( fragment.equals(min.fragment) ){
                    break;
                }else{
                    iterator.remove();
                }
            }
            point = min;
            index = nextDirection > 0 ? min.startIndex : min.endIndex;
            currentDirection = nextDirection;
            distance = min.fragment.remainDistance(index + currentDirection, currentDirection);
            for ( PolylineFragment fragment : fragments ){
                if ( fragment.equals(min.fragment) ) continue;
                distance += fragment.mDistance;
            }
            // predict next stations
            elapsed += min.distance(-nextDirection);
            if ( next1 != null ){
                next1.distance -= elapsed;
                if ( next1.distance < 0 ) next1 = null;
            }
            if ( next2 != null ) {
                next2.distance -= elapsed;
                if ( next2.distance < 0 ) next2 = null;
            }
            if ( next1 == null ){
                next1 = next2;
                next2 = null;
            }
            lastLocation = location;
            return min;
        }

        void predict(){
            predictStation();
            // callback
            StationPrediction prediction = null;
            long time = lastLocation.getTime();
            float speed = lastLocation.getSpeed();
            if ( next2 != null ){
                prediction = new StationPrediction(next2.station, time + (long)(next2.distance/speed*1000), null);
                onPredicted(prediction);
            }
            if ( next1 != null ){
                prediction = new StationPrediction(next1.station, time + (long)(next1.distance/speed*1000), prediction);
                onPredicted(prediction);
            }
        }


        private void predictStation(){
            if ( next1 != null && next2 != null ) return;
            Station current = next1 == null ? mCurrentStation : next1.station;
            for ( PointIterator iterator = new PointIterator(); iterator.hasNext(); ){
                LatLng p = iterator.next();
                mExplorer.updateLocation(p.longitude, p.latitude);
                if ( !mExplorer.hasInitialized() ) break;
                Station station = mExplorer.getCurrentStation();
                if ( !station.equals(current) ){
                    // detect boundary
                    LatLng pre = iterator.getPrevious();
                    LatLng b = detectBoundary(pre, p, 5);
                    PredictedStation s = new PredictedStation(station, iterator.getElapsedDistance() + measureDistance(pre, b));
                    if ( next1 == null ){
                        next1 = s;
                        current = station;
                    }else{
                        next2 = s;
                        break;
                    }
                }
            }

        }

        private LatLng detectBoundary(LatLng from, LatLng to, int level){
            LatLng mid = new LatLng((from.latitude + to.latitude) / 2, (from.longitude + to.longitude) / 2);
            if ( level <= 0 ) return mid;
            mExplorer.updateLocation(mid.longitude, mid.latitude);
            if ( !mExplorer.hasInitialized() ) return mid;
            if ( mExplorer.getCurrentStation().equals(mCurrentStation) ){
                return detectBoundary(mid, to, level - 1);
            }else{
                return detectBoundary(from, to, level - 1);
            }
        }

        LatLng predict(float meter){
            float d = point.remainDistance();
            if ( d > meter ){
                double index = meter / d;
                double lon = (1 - index) * point.lon + index * point.end.longitude;
                double lat = (1 - index) * point.lat + index * point.end.latitude;
                return new LatLng(lat, lon);
            }
            int direction = currentDirection;
            int i = index;
            String tag = null;
            for ( PolylineFragment fragment : fragments ){
                LatLng[] array = fragment.mPoints;
                if ( tag != null ){
                    if ( fragment.mStartTag.equals(tag) ){
                        i = 0;
                        direction = 1;
                    }else{
                        i = array.length - 2;
                        direction = -1;
                    }
                }
                for ( ; i < array.length - 1 && i >= 0; i += direction ){
                    d += fragment.mDistanceArray[i];
                    if ( d > meter ){
                        double index = (d - meter) / fragment.mDistanceArray[i];
                        if ( direction < 0 ) index = 1 - index;
                        LatLng from = array[i];
                        LatLng to = array[i + 1];
                        double lon = (1 - index) * from.longitude + index * to.longitude;
                        double lat = (1 - index) * from.latitude + index * to.latitude;
                        return new LatLng(lat, lon);
                    }
                }
                tag = direction > 0 ? fragment.mEndTag : fragment.mStartTag;
            }
            throw new IllegalStateException("#predict > margin not enough");
        }

        @Override
        public boolean equals(Object obj){
            if ( obj instanceof PredictedLine ){
                PredictedLine p = (PredictedLine)obj;
                return this.point.equals(p.point) && this.fragments.equals(p.fragments);
            }
            return false;
        }

        @Override
        public int hashCode(){
            int hash = 17;
            hash = hash * 31 + point.hashCode();
            hash = hash * 31 + fragments.hashCode();
            return hash;
        }
    }

    private static class PolylineFragment{

        final int STEP = 50;

        private PolylineFragment(Line.PolylineSegment segment){
            mStartTag = segment.mStart;
            mEndTag = segment.mEnd;
            mPoints = segment.mPoints;
            float distance = 0f;
            LatLng previous = mPoints[0];
            mDistanceArray = new float[mPoints.length - 1];
            for ( int i = 1; i < mPoints.length; i++ ){
                LatLng p = mPoints[i];
                float d = measureDistance(p, previous);
                distance += d;
                mDistanceArray[i - 1] = d;
                previous = p;
            }
            this.mDistance = distance;
        }


        final String mStartTag, mEndTag;
        final LatLng[] mPoints;
        final float mDistance;
        final float[] mDistanceArray;

        NearestPoint findNearestPoint(double lon, double lat){
            LatLng p = new LatLng(lat, lon);
            double minValue = Double.MAX_VALUE;
            int minStart = -1;
            int minEnd = -1;
            for ( int start = 0; start < mPoints.length; start += STEP ){
                int end = start + STEP;
                if ( end >= mPoints.length ) end = mPoints.length - 1;
                if ( start == end ) break;
                NearestPoint n = new NearestPoint(start, end, p, this);
                if ( n.squareDistance < minValue ){
                    minValue = n.squareDistance;
                    minStart = start;
                    minEnd = end;
                }
            }
            minValue = Double.MAX_VALUE;
            NearestPoint nearest = null;
            for ( int i = minStart; i < minEnd; i++ ){
                NearestPoint n = new NearestPoint(i, i + 1, p, this);
                if ( n.squareDistance < minValue ){
                    minValue = n.squareDistance;
                    nearest = n;
                }
            }
            return nearest;
        }

        boolean isConnectedSmooth(PolylineFragment other, String tag){
            LatLng v1 = null, v2 = null, v3 = null;
            if ( mStartTag.equals(tag) ){
                v2 = mPoints[0];
                v1 = mPoints[1];
            }else if ( mEndTag.equals(tag) ){
                v2 = mPoints[mPoints.length - 1];
                v1 = mPoints[mPoints.length - 2];
            }
            if ( other.mStartTag.equals(tag) ){
                v3 = other.mPoints[1];
            }else if ( other.mEndTag.equals(tag) ){
                v3 = other.mPoints[other.mPoints.length - 2];
            }
            double v = (v1.longitude - v2.longitude) * (v3.longitude - v2.longitude)
                    + (v1.latitude - v2.latitude) * (v3.latitude - v2.latitude);
            return v > 0;
        }

        float distanceBetween(int index1, int index2){
            int low = Math.min(index1, index2);
            int up = Math.max(index1, index2);
            float d = 0f;
            for ( int i = low; i < up; i++ ) d += mDistanceArray[i];
            return d;
        }

        float remainDistance(int index, int direction){
            if ( direction > 0 ){
                return distanceBetween(index, mPoints.length - 1);
            }else{
                return distanceBetween(0, index);
            }
        }

        @Override
        public boolean equals(Object obj){
            if ( obj instanceof PolylineFragment ){
                PolylineFragment other = (PolylineFragment)obj;
                if ( mStartTag.equals(other.mStartTag) && mEndTag.equals(other.mEndTag) ){
                    if ( mPoints.length == other.mPoints.length ){
                        int i = mPoints.length / 2;
                        return mPoints[i].equals(other.mPoints[i]);
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode(){
            int hash = 17;
            hash = 31 * hash + mStartTag.hashCode();
            hash = 31 * hash + mEndTag.hashCode();
            hash = 31 * hash + mPoints.length;
            LatLng p = mPoints[mPoints.length / 2];
            long b = Double.doubleToLongBits(p.latitude);
            hash = 31 * hash + (int)(b ^ (b >> 32));
            b = Double.doubleToLongBits(p.longitude);
            hash = 31 * hash + (int)(b ^ (b >> 32));
            return hash;
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "{tag:%s-%s,distance:%.0fm,size:%d}",
                    mStartTag, mEndTag, mDistance, mPoints.length
            );
        }
    }

    private static class NearestPoint{

        private NearestPoint(int startIndex, int endIndex, LatLng point, PolylineFragment fragment){
            if ( startIndex >= endIndex ) throw new IllegalArgumentException("Index order inverted");
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.fragment = fragment;
            start = fragment.mPoints[startIndex];
            end = fragment.mPoints[endIndex];
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
            lon = (1 - index) * start.longitude + index * end.longitude;
            lat = (1 - index) * start.latitude + index * end.latitude;
            squareDistance = Math.pow(lon - point.longitude, 2) + Math.pow(lat - point.latitude, 2);
        }

        final int startIndex, endIndex;
        final LatLng start, end;
        final PolylineFragment fragment;
        final double index;
        final double squareDistance;
        final double lon, lat;
        final boolean isOnEdge;


        int estimateDirection(Location current, Location last){
            double v = (current.getLongitude() - last.getLongitude()) * (end.longitude - start.longitude)
                    + (current.getLatitude() - last.getLatitude()) * (end.latitude - start.latitude);
            return v >= 0 ? 1 : -1;
        }

        float remainDistance(){
            return measureDistance(lat, lon, end.latitude, end.longitude);
        }

        float distance(int direction){
            float d = measureDistance(start, end);
            if ( direction > 0 ){
                return d * (1-(float)index);
            }else{
                return d * (float)index;
            }
        }

        @Override
        public boolean equals(Object obj){
            if ( obj instanceof NearestPoint ){
                NearestPoint n = (NearestPoint)obj;
                return fragment.equals(n.fragment)
                        && this.index == n.index
                        && this.startIndex == n.startIndex
                        && this.endIndex == n.endIndex;
            }
            return false;
        }

        @Override
        public int hashCode(){
            int hash = 17;
            hash = hash * 31 + startIndex;
            hash = hash * 31 + endIndex;
            long b = Double.doubleToLongBits(index);
            hash = hash * 31 + (int)(b ^ (b >> 32));
            return hash;
        }

        @Override
        public String toString(){
            return String.format(
                    Locale.US,
                    "lat/lon:(%.6f,%.6f) index:%d-%d in %s",
                    lat, lon, startIndex, endIndex, fragment.toString()
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
