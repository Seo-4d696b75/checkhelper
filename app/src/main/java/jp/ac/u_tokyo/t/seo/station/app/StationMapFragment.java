package jp.ac.u_tokyo.t.seo.station.app;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jp.ac.u_tokyo.t.seo.customdialog.CompatFragment;
import jp.ac.u_tokyo.t.seo.station.diagram.HighVoronoi;
import jp.ac.u_tokyo.t.seo.station.diagram.Point;
import jp.ac.u_tokyo.t.seo.station.diagram.Polygon;
import jp.ac.u_tokyo.t.seo.station.diagram.Rectangle;

/**
 * @author Seo-4d696b75
 * @version 2018/05/31.
 */

public class StationMapFragment extends CompatFragment implements
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        GPSService.GPSCallback,  OnMapReadyCallback,
        StationFragment.StationFragmentCallback, RadarDialog.RadarCallback, LineFragment.LineFragmentCallback,
        GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveListener{

    final static String KEY_STATION = "station_request";
    final static String KEY_LINE = "line_request";

    private final String MAP_FRAGMENT_TAG = "googleMap_fragment";

    private final String KEY_IS_SHOWN = "key_fragment_shown";
    private final String KEY_CAMERA_RECT = "key_camera_projection";

    private MainActivity mContext;
    private StationService mService;
    private StationKdTree mExplorer;

    private Bundle mSavedState;

    private boolean mHasCreated = false;

    private GoogleMap mMap;
    private GoogleApiClient mApiClient;
    private LocationSource.OnLocationChangedListener mLocationListener;

    private final float ZOOM_THRESHOLD = 6f;
    private Set<StationArea> mPolygonGroups;
    private double mMapRadius;
    private LatLng mMapCenter;

    private View mMessageView;
    private TextView mMessageText;


    private boolean mFragmentShown = false;
    private final String TRANSACTION_TAG = "transaction_tag";


    private Marker mPinedMarker;
    private Line mRailwayPolyline;

    private DisplayState mDisplayVoronoi = DisplayState.GONE;

    @Override
    public void getService(MainActivity.OnServiceConnectedListener listener){
        mContext.getService(listener);
    }

    @Override
    public void onRequestFragmentRemoved(){
        FragmentManager manager = getCompatFragmentManager();
        manager.popBackStack(TRANSACTION_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mFragmentShown = false;

    }

    private enum DisplayState{
        GONE,
        PREPARE,
        SHOWN
    }

    private VoronoiTask mVoronoiTask;
    private boolean mDisplayPolyline;
    private boolean mDisplayBoundary;
    private com.google.android.gms.maps.model.Polygon[] mVoronoiPolygons;

    private boolean mHasFocused = false;
    private float mScreenWidth = 360, mScreenHeight = 600;

    public static StationMapFragment getInstance(){
        return new StationMapFragment();
    }

    public static StationMapFragment getInstance(Station station){
        StationMapFragment fragment = new StationMapFragment();
        Bundle args = new Bundle();
        args.putInt(StationMapFragment.KEY_STATION, station.code);
        fragment.setArguments(args);
        return fragment;
    }

    public static StationMapFragment getInstance(Line line){
        StationMapFragment fragment = new StationMapFragment();
        Bundle args = new Bundle();
        args.putInt(StationMapFragment.KEY_LINE, line.mCode);
        fragment.setArguments(args);
        return fragment;
    }

    public static StationMapFragment getInstance(Fragment parent){
        StationMapFragment fragment = new StationMapFragment();
        if ( parent instanceof StationMapFragment ){
            fragment.setArguments(parent.getArguments());
        }
        return fragment;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if ( context instanceof MainActivity ){
            mContext = (MainActivity)context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle state){
        super.onViewCreated(view, state);

        if ( mHasCreated ) return;
        mHasCreated = true;
        //以下生成時一回のみ実行

        mContext.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                mService = service;
                SupportMapFragment mapFragment = null;
                if ( state == null ){
                    mapFragment = SupportMapFragment.newInstance();
                    FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
                    transaction.add(R.id.mapContainer, mapFragment, MAP_FRAGMENT_TAG);
                    transaction.commit();
                    mFragmentShown = false;
                    mSavedState = null;
                }else{
                    Fragment fragment = getChildFragmentManager().findFragmentById(R.id.mapContainer);
                    if ( fragment instanceof SupportMapFragment ){
                        mapFragment = (SupportMapFragment)fragment;
                    }else{
                        throw new RuntimeException("SupportMapFragment not found");
                    }
                    mFragmentShown = state.getBoolean(KEY_IS_SHOWN, false);
                    mSavedState = state;
                }
                mRootContainer = view.findViewById(R.id.mapContainer);
                mRootContainer.getViewTreeObserver().addOnGlobalLayoutListener(LAYOUT_LISTENER);

                mApiClient = new GoogleApiClient.Builder(mContext.getApplicationContext())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(StationMapFragment.this)
                        .addOnConnectionFailedListener(StationMapFragment.this)
                        .build();
                mApiClient.connect();
                mapFragment.getMapAsync(StationMapFragment.this);
                mService.requestLocationUpdate(StationMapFragment.this, 5);
                //mPolyLineGroups = new LinkedList<>();
                mPolygonGroups = new HashSet<>();

                mMessageView = view.findViewById(R.id.mapMessageContainer);
                mMessageText = (TextView)view.findViewById(R.id.textMapMessage);


                mExplorer = new StationKdTree(mService,"map");
                mExplorer.setSearchProperty(mService.getRadarNum(), 0);

            }
        });

    }
    
    private View mRootContainer;
    private final ViewTreeObserver.OnGlobalLayoutListener LAYOUT_LISTENER = new ViewTreeObserver.OnGlobalLayoutListener(){
        @Override
        public void onGlobalLayout(){
            float density = getResources().getDisplayMetrics().density;
            mScreenHeight = mRootContainer.getHeight() / density;
            mScreenWidth = mRootContainer.getWidth() / density;
            mRootContainer.getViewTreeObserver().removeOnGlobalLayoutListener(LAYOUT_LISTENER);
        }
    };

    private boolean removeVoronoi(){
        if ( mDisplayVoronoi == DisplayState.PREPARE && mVoronoiTask != null && mVoronoiTask.cancel(true) ){
            mVoronoiTask = null;
            updateDisplayedLine();
            return true;
        }
        if ( mDisplayVoronoi == DisplayState.SHOWN && mVoronoiPolygons != null ){
            for ( com.google.android.gms.maps.model.Polygon item : mVoronoiPolygons ){
                if ( item != null ) item.remove();
            }
            mVoronoiPolygons = null;
            mDisplayVoronoi = DisplayState.GONE;
            updateDisplayedLine();
            return true;
        }
        return false;
    }

    private final int[] VORONOI_COLOR = new int[]{
            Color.BLUE,
            Color.GREEN,
            Color.RED,
            Color.YELLOW
    };


    private static class VoronoiTask extends AsyncTask<Void, Integer, Rectangle>{

        PolygonOptions[] builders;
        Polygon polygon;
        Station station;
        StationMapFragment parent;
        StationKdTree mExplorer;
        int mRadarNum;

        private VoronoiTask(StationMapFragment parent, Station station){
            this.station = station;
            this.parent = parent;
            this.mExplorer = parent.mExplorer;
            mRadarNum = parent.mService.getRadarNum();
        }

        @Override
        protected void onPreExecute(){
            parent.mMessageView.setVisibility(View.VISIBLE);
            parent.mMessageText.setText(String.format(Locale.US, "%s 0/%d", parent.getString(R.string.message_calc_voronoi), mRadarNum));
            parent.removeAllLine();
        }

        @Override
        protected Rectangle doInBackground(Void... params){
            // calculating voronoi area takes long time, must not be called in UI thread.
            builders = new PolygonOptions[mRadarNum];
            parent.mVoronoiPolygons = new com.google.android.gms.maps.model.Polygon[mRadarNum];
            Rectangle container = new Rectangle(127, 46, 146, 26);
            HighVoronoi<StationPoint> diagram = new HighVoronoi<>(container.getContainer());
            diagram.solve(mRadarNum, new StationPoint(station), new HighVoronoi.PointProvider<StationPoint>(){
                @Override
                public Collection<StationPoint> getNeighbors(StationPoint point){
                    List<StationPoint> list = new ArrayList<>(point.next.length);
                    for ( int i=0 ; i<point.next.length ; i++ ){
                        list.add(parent.mService.getStationPoint(point.next[i]));
                    }
                    return list;
                }
            }, new HighVoronoi.ResultCallback(){
                @Override
                public void onResolved(int index, Polygon points, long time){
                    polygon = points;
                    PolygonOptions builder = new PolygonOptions();
                    for ( Point p : polygon ){
                        builder.add(new LatLng(p.getY(), p.getX()));
                    }
                    builder.geodesic(false);
                    builder.strokeWidth(3f);
                    if ( index == mRadarNum - 1 ){
                        builder.fillColor(0x22000000);
                        builder.strokeColor(Color.BLACK);
                    }else{
                        builder.strokeColor(parent.VORONOI_COLOR[index % 4]);
                    }
                    builder.clickable(true);
                    builder.zIndex(-index);
                    builders[index] = builder;
                    publishProgress(index);
                }

                @Override
                public void onCompleted(Polygon[] results, long time){

                }
            });
            return polygon.getRectBound();
        }

        @Override
        protected void onProgressUpdate(Integer... param){
            int index = param[0];
            parent.mVoronoiPolygons[index] = parent.mMap.addPolygon(builders[index]);
            parent.mVoronoiPolygons[index].setTag(index);
            parent.mMessageText.setText(String.format(Locale.US, "%s%d/%d", parent.getString(R.string.message_calc_voronoi), index, mRadarNum));
        }

        @Override
        protected void onPostExecute(Rectangle rect){
            parent.mMessageView.setVisibility(View.INVISIBLE);
            parent.mDisplayVoronoi = DisplayState.SHOWN;
            parent.mVoronoiTask = null;

            Point center = rect.getCenter();
            parent.focusAt(center.getX(), center.getY(), (float)(Math.log(360 / rect.getWidth()) / Math.log(2)), true);

        }

        @Override
        protected void onCancelled(){
            // This task may take a long time, so accepts being cancelled
            parent.mMessageView.setVisibility(View.INVISIBLE);
            for ( com.google.android.gms.maps.model.Polygon item : parent.mVoronoiPolygons ){
                if ( item != null ) item.remove();
            }
            parent.mVoronoiPolygons = null;
            parent.mDisplayVoronoi = DisplayState.GONE;
            parent.mVoronoiTask = null;
        }
    }

    private void showRadarVoronoi(Station station){
        if ( mDisplayVoronoi != DisplayState.GONE ) return;
        mDisplayVoronoi = DisplayState.PREPARE;

        mVoronoiTask = new VoronoiTask(this, station);
        mVoronoiTask.execute();
    }

    private void showRadarDialog(double lon, double lat){
        DialogFragment dialog = RadarDialog.getInstance(lon, lat);
        dialog.show(getCompatFragmentManager(), null);
    }

    private void focusAt(double longitude, double latitude, boolean animate){
        focusAt(longitude, latitude, 15f, animate);
    }

    private void focusAt(double longitude, double latitude, float zoom, boolean animate){
        LatLng location = new LatLng(latitude, longitude);
        CameraPosition position = new CameraPosition.Builder()
                .target(location)
                .zoom(zoom)
                .build();
        if ( animate ) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
        }else{
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
        Log.d("GoogleMap", String.format(Locale.US, "(%.6f,%.6f) zoom:%.1f", longitude, latitude, zoom));
        //double size = 360 * Math.pow(2, -15);
        //mExplorer.updateArea(longitude - size/2, latitude + size/2, longitude + size/2, latitude - size/2);
    }

    private void focusAt(Station station){
        removeVoronoi();
        onRequestPolylineRemoved();
        removePinedMarker();

        focusAt(station.longitude, station.latitude, true);
        mExplorer.updateLocation(station.longitude, station.latitude);

        FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
        StationFragment fragment = StationFragment.getInstanceForMap(station);
        if ( mFragmentShown ){
            transaction.replace(R.id.fragmentContainer, fragment, fragment.getDefaultTag());
            transaction.addToBackStack(null);
        }else{
            mFragmentShown = true;
            transaction.add(R.id.fragmentContainer, fragment, fragment.getDefaultTag());
            transaction.addToBackStack(TRANSACTION_TAG);
        }
        transaction.commit();
    }

    private void focusAt(Line line, boolean showPolyline){

        removeVoronoi();
        onRequestPolylineRemoved();
        removePinedMarker();
        FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
        LineFragment fragment = LineFragment.getInstanceForMap(line, showPolyline);
        if ( mFragmentShown ){
            transaction.replace(R.id.fragmentContainer, fragment, fragment.getDefaultTag());
            transaction.addToBackStack(null);
        }else{
            mFragmentShown = true;
            transaction.add(R.id.fragmentContainer, fragment, fragment.getDefaultTag());
            transaction.addToBackStack(TRANSACTION_TAG);
        }
        transaction.commit();
        if ( showPolyline ) onRequestPolylineShown(line);
    }

    @Override
    public void onMapReady(GoogleMap googleMap){
        if ( mContext == null ) return;
        if ( ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
            mMap = googleMap;
            mMap.setLocationSource(new LocationSource(){
                @Override
                public void activate(OnLocationChangedListener onLocationChangedListener){
                    mLocationListener = onLocationChangedListener;
                }

                @Override
                public void deactivate(){
                    mLocationListener = null;
                }
            });
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
                @Override
                public boolean onMyLocationButtonClick(){
                    if ( mService.hasValidLocation() ){
                        focusAt(mService.getLongitude(), mService.getLatitude(), true);
                    }
                    return true;
                }
            });
            mMap.setOnCameraIdleListener(this);
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
                @Override
                public void onMapClick(LatLng latLng){
                    if ( mDisplayVoronoi == DisplayState.SHOWN ){
                        removeVoronoi();
                        return;
                    }
                    mExplorer.updateLocation(latLng.longitude, latLng.latitude);
                    Station station = mExplorer.getCurrentStation();
                    Log.d("station click", station.toString());
                    focusAt(station);
                }
            });
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){
                @Override
                public void onMapLongClick(LatLng latLng){
                    removeVoronoi();
                    onRequestPolylineRemoved();
                    removePinedMarker();
                    mExplorer.updateLocation(latLng.longitude, latLng.latitude);
                    Station station = mExplorer.getCurrentStation();
                    Log.d("long click", station.toString());
                    focusAt(latLng.longitude, latLng.latitude, true);
                    String location = String.format(Locale.US, "N%.4f E%.4f", latLng.longitude, latLng.latitude);
                    mPinedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
                    Fragment fragment = StationFragment.getInstanceForMap(station, latLng.longitude, latLng.latitude);
                    if ( mFragmentShown ){
                        transaction.replace(R.id.fragmentContainer, fragment);
                        transaction.addToBackStack(null);
                    }else{
                        mFragmentShown = true;
                        transaction.add(R.id.fragmentContainer, fragment);
                        transaction.addToBackStack(TRANSACTION_TAG);
                    }
                    transaction.commit();
                }
            });
            mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener(){
                @Override
                public void onCameraMoveStarted(int i){
                    //要確認
                    /*
                    * マップ画面上における詳細データフラグメントの挙動
                    * マップ移動開始時に
                    * 駅ボロノイ図：表示まま
                    * その以外駅：消す
                    * 路線ポリライン：表示まま
                    * それ以外路線：消す
                    */
                    if ( i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                        if ( mFragmentShown && mDisplayVoronoi == DisplayState.GONE && !mDisplayPolyline ){
                            onRequestFragmentRemoved();
                        }
                        if ( mPinedMarker != null ){
                            mPinedMarker.remove();
                            mPinedMarker = null;
                        }
                    }

                }
            });
            mMap.setOnCameraMoveListener(this);
            mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener(){
                @Override
                public void onPolygonClick(com.google.android.gms.maps.model.Polygon polygon){
                    if ( mDisplayVoronoi == DisplayState.SHOWN && mVoronoiPolygons != null ){
                        Log.d("onPolygonClick", "index:" + polygon.getTag());

                    }
                }
            });

            if ( mSavedState == null ){
                // initial fragment transaction if needed
                Bundle args = getArguments();
                if ( args != null ){
                    if ( args.containsKey(KEY_STATION) ){
                        Station station = mService.getStation(args.getInt(KEY_STATION));
                        focusAt(station);
                        mHasFocused = true;
                    }else if ( args.containsKey(KEY_LINE) ){
                        Line line = mService.getLine(args.getInt(KEY_LINE), true);
                        focusAt(line, true);
                        mHasFocused = true;
                    }
                }
            }else{
                LatLngBounds rect = mSavedState.getParcelable(KEY_CAMERA_RECT);
                if ( rect != null ){
                    LatLng center = rect.getCenter();
                    double width = rect.northeast.longitude - rect.southwest.longitude;
                    double zoom = Math.log(360.0 / width * mScreenWidth / 256) / Math.log(2);
                    focusAt(center.longitude, center.latitude, (float)zoom, false);
                    mHasFocused = true;
                }
            }

            if ( !mHasFocused ){
                if (  mService.hasValidLocation() ){
                    focusAt(mService.getLongitude(), mService.getLatitude(), false);
                    mHasFocused = true;
                }else{
                    focusAt(137, 34, 4.5f, false);
                }
            }

        }
    }


    private final double BOUNDARY_MARGIN = 1.0;
    
    private static class CameraUpdateTask extends AsyncTask<Void,Void,Boolean>{

        
        CameraUpdateTask(StationMapFragment parent, LatLngBounds cameraBounds){
            mParent = parent;
            mCameraBounds = cameraBounds;
            north = mCameraBounds.northeast.latitude;
            east = mCameraBounds.northeast.longitude;
            south = mCameraBounds.southwest.latitude;
            west = mCameraBounds.southwest.longitude;
            BOUNDARY_MARGIN = parent.BOUNDARY_MARGIN;
        }
        
        StationMapFragment mParent;
        LatLngBounds mCameraBounds;
        final double BOUNDARY_MARGIN;
        double north, south, west, east;

        private void updateDisplayLine(){
            mParent.mMapCenter = new LatLng((north + south)/2, (east + west)/2);
            mParent.mMapRadius = mParent.mService.mInternalRuler.measure(west,south,east,north) / 2;
            mParent.updateDisplayedLine();
        }

        @Override
        protected void onPreExecute(){
            updateDisplayLine();
        }

        @Override
        protected Boolean doInBackground(Void... voids){
            // this operation may take a long time due to IO.
            // so must be not called in UI thread.
            StationKdTree explorer = mParent.mExplorer;
            explorer.setSearchProperty(mParent.mService.getRadarNum(), mParent.mMapRadius * ( 1.0 + BOUNDARY_MARGIN));
            explorer.updateLocation((west+east)/2, (north+south)/2);
            return explorer.hasInitialized();
        }

        @Override
        protected void onPostExecute(Boolean result){
            if ( result ){
                List<StationArea> tmp = new LinkedList<>();
                for ( Station station : mParent.getExplorer().getNearStations() ){
                    if ( station.mAreaData != null ){
                        mParent.mPolygonGroups.remove(station.mAreaData);
                        tmp.add(station.mAreaData);
                    }
                }
                for ( StationArea area : mParent.mPolygonGroups ){
                    area.removePolygon();
                }
                mParent.mPolygonGroups.clear();
                mParent.mPolygonGroups.addAll(tmp);
                updateDisplayLine();
            }
            mParent = null;
        }
        
    }

    @Override
    public void onCameraIdle(){
        float zoom = mMap.getCameraPosition().zoom;
        if ( zoom >= ZOOM_THRESHOLD ){
            LatLngBounds rect = mMap.getProjection().getVisibleRegion().latLngBounds;
            new CameraUpdateTask(this, rect).execute();
        }
    }


    @Override
    public void onCameraMove(){
        LatLngBounds rect = mMap.getProjection().getVisibleRegion().latLngBounds;
        double north = rect.northeast.latitude;
        double east = rect.northeast.longitude;
        double south = rect.southwest.latitude;
        double west = rect.southwest.longitude;
        //TODO
    }


    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mApiClient.disconnect();
        mService.removeLocationUpdate(this);
        mExplorer.release();
        mMap = null;
        for ( StationArea s : mPolygonGroups ) s.removePolygon();
        mPolygonGroups.clear();

        mContext = null;
        mVoronoiTask = null;
    }

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putBoolean(KEY_IS_SHOWN, mFragmentShown);

        if ( mMap != null ){
            LatLngBounds rect = mMap.getProjection().getVisibleRegion().latLngBounds;
            state.putParcelable(KEY_CAMERA_RECT, rect);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle){

    }

    @Override
    public void onConnectionSuspended(int i){

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){

    }

    @Override
    public void onLocationUpdate(Location location){
        if ( mLocationListener != null ){
            mLocationListener.onLocationChanged(location);
        }
        if ( !mHasFocused && mMap != null ){
            focusAt(location.getLongitude(), location.getLatitude(), true);
            mHasFocused = true;
        }
    }

    @Override
    public void onResolutionRequired(ResolvableApiException exception){
        try{
            exception.startResolutionForResult(mContext, mContext.PERMISSION_REQUEST_SETTING);
        }catch( IntentSender.SendIntentException e ){
            e.printStackTrace();
        }
    }

    @Override
    public void onGPSStop(String mes){

    }

    private void removePinedMarker(){
        if ( mPinedMarker != null ){
            mPinedMarker.remove();
            mPinedMarker = null;
        }
    }

    private synchronized void updateDisplayedLine(){
        if ( mDisplayVoronoi == DisplayState.SHOWN ) return;
        boolean display = mMap.getCameraPosition().zoom >= ZOOM_THRESHOLD && mPolygonGroups.size() <= 500;

        for ( StationArea area : mPolygonGroups ){
            if ( display ){
                area.drawPolygon(mMap);
            }else{
                area.removePolygon();
            }
        }
        mDisplayBoundary = display;
    }

    private synchronized void removeAllLine(){
        for ( StationArea item : mPolygonGroups ) item.removePolygon();
    }

    @Override
    public void onLineSelected(Line line){
        focusAt(line, false);
    }

    @Override
    public StationKdTree getExplorer(){
        return mExplorer;
    }

    @Override
    public void onStationSelected(Station station){
        focusAt(station);
    }

    @Override
    public void onRequestLineOnMap(Line line){
        //nothing to do
    }

    @Override
    public void onRequestPolylineShown(Line line){
        if ( !line.hasDetails() ){
            Log.e("Error", "line details not initialized : " + line.mName);
            return;
        }
        if ( !line.hasPolyline() ){
            Toast.makeText(getContext(), mService.getString(R.string.message_no_polyline), Toast.LENGTH_SHORT).show();
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for ( Line.PolylineSegment segment : line.getSegments() ){
            for ( LatLng point : segment.mPoints ){
                builder.include(point);
            }
        }
        LatLngBounds bounds = builder.build();
        LatLng center = bounds.getCenter();
        double width = bounds.northeast.longitude - bounds.southwest.longitude;
        double height = bounds.northeast.latitude - bounds.southwest.latitude;
        double zoom = Math.log(0.8 * Math.min(360.0 / width * mScreenWidth / 256, 180.0 / height * mScreenHeight / 256)) / Math.log(2);
        focusAt(center.longitude, center.latitude, (float)zoom, true);
        line.drawPolyline(mMap);
        mRailwayPolyline = line;
        mDisplayPolyline = true;
    }

    @Override
    public void onRequestPolylineRemoved(){
        if ( mRailwayPolyline != null ){
            mRailwayPolyline.removePolyline();
            mRailwayPolyline = null;
        }
        mDisplayPolyline = false;
    }

    @Override
    public void onRequestVoronoiShown(Station station){
        showRadarVoronoi(station);
    }

    @Override
    public void onRequestRadarShown(double lon, double lat){
        showRadarDialog(lon, lat);
    }

    @Override
    public void onRequestVoronoiRemoved(){
        removeVoronoi();
    }

    @Override
    public void onRequestStationOnMap(Station station){
        //nothing to do
    }

    static class StationArea{

        StationArea(Station station, JSONObject data) throws JSONException{
            this.code = station.code;
            this.lat = station.latitude;
            this.lng = station.longitude;
            double lng = data.getDouble("lng");
            double lat = data.getDouble("lat");
            JSONArray deltaX = data.getJSONArray("delta_lng");
            JSONArray deltaY = data.getJSONArray("delta_lat");
            if ( deltaX.length() != deltaY.length() ) throw new AppException("list size mismatch");
            this.points = new LatLng[deltaX.length()];
            final double scale = 1.0 / 1000000;
            bottom = Double.MAX_VALUE;
            top = -Double.MAX_VALUE;
            right = -Double.MAX_VALUE;
            left = Double.MAX_VALUE;
            for ( int i = 0; i < deltaX.length() ; i++ ){
                lng += (deltaX.getInt(i) * scale);
                lat += (deltaY.getInt(i) * scale);
                this.points[i] = new LatLng(lat, lng);
                bottom = Math.min(bottom, lat);
                top = Math.max(top, lat);
                right = Math.max(right, lng);
                left = Math.min(left, lng);
            }
            hasEnclosed = !data.has("enclosed") || data.getBoolean("enclosed");
        }

        double bottom, top, right, left;
        final double lat,lng;
        final int code;

        final LatLng[] points;
        final boolean hasEnclosed;

        private com.google.android.gms.maps.model.Polygon polygon;

        void removePolygon(){
            if ( polygon != null ){
                polygon.remove();
                polygon = null;
            }
        }

        void drawPolygon(GoogleMap map){
            if ( polygon == null ){
                PolygonOptions options = new PolygonOptions()
                        .add(points)
                        .strokeColor(Color.BLUE)
                        .strokeWidth(3f)
                        .geodesic(false);
                polygon = map.addPolygon(options);
            }
        }

        boolean isDisplayed(){
            return polygon != null;
        }

        boolean isInside(double east, double north, double west, double south){
            return Math.abs((left+right)-(east+west)) <= right-left + east-west &&
                    Math.abs((top-bottom)-(north-south)) <= top-bottom + north-south;
        }

        @Override
        public int hashCode(){
            return code;
        }

        @Override
        public boolean equals(Object obj){
            return obj instanceof StationArea && ((StationArea)obj).code == this.code;
        }
    }


}
