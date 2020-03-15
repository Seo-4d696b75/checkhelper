package jp.ac.u_tokyo.t.seo.station.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author Seo-4d696b75
 * @version 2018/05/27.
 */

class GPSService{

    GPSService(StationService service){
        mService = service;
        mLocationClient = LocationServices.getFusedLocationProviderClient(mService);
        mSettingClient = LocationServices.getSettingsClient(mService);
        mLocationCallback = new CallBackReference(this);
    }

    interface GPSCallback{
        void onLocationUpdate(Location location);

        void onResolutionRequired(ResolvableApiException exception);

        void onGPSStop(String mes);
    }

    private static class CallBackReference extends LocationCallback{

        private GPSService mReference;

        private CallBackReference(GPSService service){
            mReference = service;
        }

        @Override
        public void onLocationResult(LocationResult locationResult){
            super.onLocationResult(locationResult);

            if ( mReference != null ) mReference.onLocationResult(locationResult);
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability){
            super.onLocationAvailability(locationAvailability);

            if ( mReference != null ) mReference.onLocationAvailability(locationAvailability);
        }
    }

    private boolean mRunning = false;
    private int mMinInterval;
    private List<GPSRequest> mGPSCallback;
    private StationService mService;
    private FusedLocationProviderClient mLocationClient;
    private CallBackReference mLocationCallback;
    private SettingsClient mSettingClient;
    private Location mLatestLocation;

    double getLongitude(){
        return mLatestLocation == null ? 0 : mLatestLocation.getLongitude();
    }

    double getLatitude(){
        return mLatestLocation == null ? 0 : mLatestLocation.getLatitude();
    }

    boolean hasValidLocation(){
        return mLatestLocation != null;
    }


    private void onLocationResult(LocationResult locationResult){
        //super.onLocationResult(locationResult);
        Location location = locationResult.getLastLocation();
        if ( location != null ){
            mLatestLocation = location;
            //log(String.format(Locale.US, "onLocationChanged > (%.4f,%.4f)", location.getLongitude(), location.getLatitude()));
            for ( GPSCallback callback : mGPSCallback ){
                callback.onLocationUpdate(location);
            }
        }
    }


    private void onLocationAvailability(LocationAvailability locationAvailability){
        Log.d("onLocationAvailability", " isLocationAvailable =  " + locationAvailability.isLocationAvailable());
    }


    void release(){
        mService = null;
        if ( mRunning ){
            mLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(new OnCompleteListener<Void>(){
                @Override
                public void onComplete(@NonNull Task<Void> task){
                    for ( GPSCallback callback : mGPSCallback ){
                        callback.onGPSStop("GPS service released");
                    }
                    mRunning = false;
                    release();
                }
            });
            return;
        }
        if ( mGPSCallback != null ){
            mGPSCallback.clear();
            mGPSCallback = null;
        }
        mLocationCallback.mReference = null;
        mLocationCallback = null;
    }

    private static class GPSRequest implements GPSCallback{

        GPSRequest(GPSCallback callback, int min){
            mCallback = callback;
            mMinTime = min;
        }

        final GPSCallback mCallback;
        private int mMinTime;

        @Override
        public void onLocationUpdate(Location location){
            mCallback.onLocationUpdate(location);
        }

        @Override
        public void onResolutionRequired(ResolvableApiException exception){
            mCallback.onResolutionRequired(exception);
        }

        @Override
        public void onGPSStop(String mes){
            mCallback.onGPSStop(mes);
        }
    }

    private boolean checkCallback(GPSCallback callback, int min){
        if ( mGPSCallback == null ){
            mGPSCallback = new LinkedList<>();
            return true;
        }else{
            for ( GPSRequest item : mGPSCallback ){
                if ( item.mCallback == callback ){
                    item.mMinTime = min;
                    return false;
                }
            }
            return true;
        }
    }

    private int addCallback(GPSCallback callback, int min){
        if ( checkCallback(callback, min) ){
            GPSRequest request = new GPSRequest(callback, min);
            if ( hasValidLocation() ){
                request.onLocationUpdate(mLatestLocation);
            }
            mGPSCallback.add(request);
        }
        min = Integer.MAX_VALUE;
        for ( GPSRequest item : mGPSCallback ){
            min = Math.min(min, item.mMinTime);
        }
        return min;
    }

    void requestGPSUpdate(final GPSCallback callback, int minInterval){
        if ( callback == null || minInterval < 1 ){
            return;
        }
        minInterval = addCallback(callback, minInterval);
        if ( mRunning ){
            if ( minInterval != mMinInterval ){
                log(String.format(Locale.US, "requestGPSUpdate > minIntervalChanged %d->%d", mMinInterval, minInterval));
                mMinInterval = minInterval;
                mLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(new OnCompleteListener<Void>(){
                    @Override
                    public void onComplete(@NonNull Task<Void> task){
                        mRunning = false;
                        requestGPSUpdate(callback);
                    }
                });
            }
        }else{
            mMinInterval = minInterval;
            requestGPSUpdate(callback);
        }
    }

    private void requestGPSUpdate(final GPSCallback callback){
        log(String.format(Locale.US, "requestGPSUpdate > minInterval:%ds", mMinInterval));

        final LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(mMinInterval * 1000);
        request.setFastestInterval(mMinInterval * 1000);
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(request).build();
        mSettingClient.checkLocationSettings(settingsRequest).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>(){
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse){
                if ( ContextCompat.checkSelfPermission(mService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ){
                    mLocationClient.requestLocationUpdates(request, mLocationCallback, Looper.myLooper());
                    mRunning = true;
                }else{
                    onError("start > permission denied", "予想外のパーミッション拒否");
                }
            }
        }).addOnFailureListener(new OnFailureListener(){
            @Override
            public void onFailure(@NonNull Exception e){
                ApiException exception = (ApiException)e;
                if ( exception.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED ){
                    ResolvableApiException resolvableApiException = (ResolvableApiException)exception;
                    callback.onResolutionRequired(resolvableApiException);
                }
                onError(e.getMessage(), "GPSを使用できません");
            }
        });
    }

    void removeGPSUpdate(GPSCallback callback){
        for ( Iterator<GPSRequest> iterator = mGPSCallback.iterator(); iterator.hasNext(); ){
            GPSRequest next = iterator.next();
            if ( next.mCallback == callback ) iterator.remove();
        }
        if ( mGPSCallback.size() == 0 ){
            mLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(new OnCompleteListener<Void>(){
                @Override
                public void onComplete(@NonNull Task<Void> task){
                    mRunning = false;
                    mLatestLocation = null;
                }
            });
        }
    }

    private void log(String mes){
        mService.log(mes);
    }

    private void onError(String mes, String UI){
        mRunning = false;
        mService.log(mes);
        for ( GPSCallback callback : mGPSCallback ){
            callback.onGPSStop(UI);
        }
        mGPSCallback.clear();
    }


}
