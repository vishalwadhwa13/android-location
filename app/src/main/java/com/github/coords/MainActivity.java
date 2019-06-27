package com.github.coords;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocUtils.LocationUpdCallback,
        LocUtils.LocationSettingUpdCallback,
        View.OnClickListener
{

    private LocationCallback locationCallback;
    private LocUtils locUtils;
    private static final String TAG = "MainActivity";
    private boolean enableUpdates = false;
    private boolean pauseUpdates = false;
//    private boolean isLocEnabled = false;
    private LocationRequest locUpdsLocReq;


    @SuppressWarnings("FieldCanBeLocal")
    private Button btnGetPerms, btnEnableLocn, btnGetLocn, btnGetLocnUpd;
    private TextView textViewLogs;

    private Map<String, Object> lreqOpts = new HashMap<String, Object>() {{
        put("updateInterval", 5000L);
        put("priority", LocationRequest.PRIORITY_HIGH_ACCURACY);
        put("fastestInterval", 1000L);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetPerms = findViewById(R.id.btn_perms);
        btnEnableLocn = findViewById(R.id.btn_enable_locn);
        btnGetLocn = findViewById(R.id.btn_get_locn);
        btnGetLocnUpd = findViewById(R.id.btn_locn_upd);


        btnGetPerms.setOnClickListener(this);
        btnEnableLocn.setOnClickListener(this);
        btnGetLocn.setOnClickListener(this);
        btnGetLocnUpd.setOnClickListener(this);

        textViewLogs = findViewById(R.id.logs);
        textViewLogs.setMovementMethod(new ScrollingMovementMethod());



        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    textViewLogs.append("\nLocation result null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    textViewLogs.append("\nLocation Update: lat: " + location.getLatitude() + ", long: " + location.getLongitude());
                }
            }
        };

        locUtils = new LocUtils(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(MainActivity.this, "Permission denied to read your location", Toast.LENGTH_SHORT).show();
                textViewLogs.append("\nPermission denied");
            }
        }
    }


    /*
     * priority: send constant
     * fastestUpdateInterval
     * updateInterval
     *
     * */

    @Override
    protected void onResume() {
        super.onResume();
        if (enableUpdates) {
            locUtils.registerForLocationUpdates(locUpdsLocReq, locationCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (enableUpdates) {
            locUtils.unregisterForLocationUpdates();
        }
    }

    @Override
    public void locn(LocUtils.LocationResponse lrsp) {
        Log.d(TAG, "locn: "+lrsp.toString());
        if (lrsp.getLocn() == null) {
            textViewLogs.append("\nLocation unavailable.");
            return;
        }
        textViewLogs.append("\nlocn: lat "+ lrsp.getLocn().getLatitude() + ", long: " + lrsp.getLocn().getLongitude());
    }

    @Override
    public void status(LocUtils.LocationSettingsResponse lsrsp) {
        textViewLogs.append("\nLocation settings: "+lsrsp.toString());
        // isLocEnabled = !lsrsp.isError();
        if (enableUpdates) {
            locUpdsLocReq = lsrsp.getLreq();
            locUtils.registerForLocationUpdates(lsrsp.getLreq(), locationCallback);
        }
//         locUtils.getLocation(this);
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: "+view.getId());
        switch (view.getId()) {
            case R.id.btn_perms:
                if (locUtils.getPerms()) {
                    textViewLogs.append("\nPermission available.");
                }
                break;

            case R.id.btn_enable_locn:
//                if (isLocEnabled) {
//                    textViewLogs.append("\nLocation is already enabled");
//                } else
                locUtils.enableLocation(
                        lreqOpts,
                        this
                );
                break;

            case R.id.btn_get_locn:
//                if (isLocEnabled) {
                    locUtils.getLocation(this);
//                } else locUtils.enableLocation(new HashMap<String, Object>(), this);
                break;

            case R.id.btn_locn_upd:

                if (!enableUpdates) {
                    enableUpdates = true;
                    locUtils.enableLocation(lreqOpts, this);
//                    locUtils.registerForLocationUpdates(locUpdsLocReq, locationCallback);
                } else {
                    locUtils.unregisterForLocationUpdates();
                }

        }
    }
}
