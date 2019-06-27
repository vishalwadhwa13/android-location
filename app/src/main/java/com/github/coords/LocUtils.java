package com.github.coords;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public class LocUtils {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest lreq = new LocationRequest();
    public static final int REQUEST_CHECK_SETTINGS = 13;
    private boolean requestingLocationUpdates = false;
    private LocationCallback locationCallback;
    private Context ctx;
    private static final String TAG = "LocUtils";

    public LocUtils(Context ctx) {
        this.ctx = ctx;
        // get config object
    }


    private FusedLocationProviderClient initFusedLocationProvider() {
        return LocationServices.getFusedLocationProviderClient(this.ctx);
    }

    public boolean getPerms() {
        if (ActivityCompat.checkSelfPermission(this.ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) this.ctx,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return false;
        }

        return true;

    }

    @SuppressLint("MissingPermission")
    private void _locnGet(final LocationUpdCallback lupdCbk) {
        if (fusedLocationClient == null) fusedLocationClient = initFusedLocationProvider();
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            //send result
                            lupdCbk.locn(new LocationResponse( false, "" , location));
                        } else {
                            lupdCbk.locn(new LocationResponse( true, "Unable to get location", null));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // send error
                        lupdCbk.locn(new LocationResponse(true, "unable to get location", null));
                    }
                });
    }


    @SuppressLint("MissingPermission")
    public void getLocation(final LocationUpdCallback lupdCbk) {

        if (!getPerms()) return;
        this._locnGet(lupdCbk);

    }


    interface LocationUpdCallback {
        void locn(LocationResponse lrsp);
    }

    interface LocationSettingUpdCallback {
        void status(LocationSettingsResponse lsrsp);
    }

    class LocationSettingsResponse {
        private boolean error;
        private String errorMsg;
        private LocationRequest lreq;

        public LocationSettingsResponse(boolean error, String errorMsg, LocationRequest lreq) {
            this.error = error;
            this.errorMsg = errorMsg;
            this.lreq = lreq;
        }


        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public String toString() {
            return "LocationSettingsResponse{" +
                    "error=" + error +
                    ", errorMsg='" + errorMsg + '\'' +
                    ", lreq=" + (lreq != null ? lreq.toString(): null) +
                    '}';
        }

        public LocationRequest getLreq() {
            return lreq;
        }
    }

    class LocationResponse {
        private boolean error;
        private String errorMsg;
        private Location locn;

        public LocationResponse(boolean error, String errorMsg, Location locn) {
            this.error = error;
            this.errorMsg = errorMsg;
            this.locn = locn;
        }



        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public Location getLocn() {
            return locn;
        }

        @Override
        public String toString() {
            return "LocationResponse{" +
                    "error=" + error +
                    ", errorMsg='" + errorMsg + '\'' +
                    ", locn=" + locn +
                    '}';
        }
    }



    public void enableLocation(Map<String, Object> config, final LocationSettingUpdCallback lsupd) {
        if (!getPerms()) return;

        lreq = LocationRequest.create();
        Log.d(TAG, "setParams: lreq" + lreq.toString());

        if (config.containsKey("fastestUpdateInterval")) lreq.setFastestInterval((Long) config.get("fastestUpdateInterval"));
        if (config.containsKey("updateInterval")) lreq.setInterval((Long) config.get("updateInterval"));
        if (config.containsKey("priority")) lreq.setPriority((Integer) config.get("priority"));

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(lreq);

        builder.setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this.ctx);


        Task<com.google.android.gms.location.LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task
                .addOnSuccessListener(new OnSuccessListener<com.google.android.gms.location.LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(com.google.android.gms.location.LocationSettingsResponse locationSettingsResponse) {
                        lsupd.status(new LocationSettingsResponse(false, "Location is already enabled.", lreq));
                        Log.d(TAG, "onSuccess: ");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        lsupd.status(new LocationSettingsResponse(false, "Location not enabled.", null));
                        // failed
                        Log.d(TAG, "onFailure: " + e.toString());
                        if (e instanceof ResolvableApiException) {

                            try {
//                                Log.d(TAG, "onFailure: ");
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult((Activity) LocUtils.this.ctx,
                                        REQUEST_CHECK_SETTINGS);
                                Log.d(TAG, "onFailure: try");
                            } catch (IntentSender.SendIntentException sendEx) {
                                Log.d(TAG, "onFailure: catch");
                            }
                        }
                    }
                });


    }

    @SuppressLint("MissingPermission")
    public void registerForLocationUpdates(LocationRequest lr, final LocationCallback lcbk) {
        requestingLocationUpdates = true;
        if (fusedLocationClient == null) fusedLocationClient = initFusedLocationProvider();
        locationCallback = lcbk;
        lreq = lr;
        fusedLocationClient.requestLocationUpdates(
                lreq,
                lcbk,
                null
        );


//        this._setParams(lreqOpts, new LocationSettingUpdCallback() {
//            @Override
//            public void status(LocationSettingsResponse lsrsp) {
//                if (!lsrsp.isError()) {
//
//                } else {
//                    Toast.makeText(ctx, "could not request location.", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    public void unregisterForLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            
        }
//        lreq = null;
//        locationCallback = null;
        requestingLocationUpdates = false;
    }
}
