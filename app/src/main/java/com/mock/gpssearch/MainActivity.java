package com.mock.gpssearch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener {


    private MapView mapView;
    private MapboxMap map;
    LocationEngine locationEngine;
    LocationLayerPlugin locationLayerPlugin;
    PermissionsManager permissionsManager;
    Location originLayout;

    public static NoteDatabase noteDatabase = null;
    private static final String TAG = "MainActivity";
    private static MockLocationImpl mockLocation;
    private Context mContext;
    static double longitudeText = 1.0;
    static double latitudeText = 1.0;

    static EditText longitude, latitude;

    private Intent mNotificationIntent;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private Button startButton;

    private static boolean isRunning = false;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);

        initToolbar();
        init_view();
        openDatabase();

        mapView = (MapView) findViewById(R.id.mapView);

        FloatingActionButton FAB = (FloatingActionButton) findViewById(R.id.myLocationButton);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                intializLocationLayer();

                Location lastLocation = locationEngine.getLastLocation();
                setLatLng(String.valueOf(lastLocation.getLatitude()), String.valueOf(lastLocation.getLongitude()));
                setCamerpostion(lastLocation);
            }
        });
    }


    private void initToolbar(){
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("GPS GHOST");
    }


    private void init_view(){

        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.latitude);

        startButton = findViewById(R.id.start_button);
        mContext = getApplicationContext();
        mPreferences = mContext.getSharedPreferences("NAVIGINE_FAKE_GPS", Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();

        boolean mNeedPref = mPreferences.getBoolean("NEED_PREF", true);

        mockLocation = new MockLocationImpl(this);
        mNotificationIntent = new Intent(this.getApplicationContext(), NotificationService.class);

        init();
    }


    private void init(){
        LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Location networkLoc = Objects.requireNonNull(locationManager).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (networkLoc != null && !isRunning) {
            longitudeText = networkLoc.getLongitude();
            latitudeText = networkLoc.getLatitude();
        } else if (!isRunning) {
            longitudeText = mPreferences.getFloat("LONGITUDE", 1.0f);
            latitudeText = mPreferences.getFloat("LATITUDE", 1.0f);
        }

        longitude.setText(String.valueOf(longitudeText));
        latitude.setText(String.valueOf(latitudeText));

        longitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    longitudeText = Double.parseDouble(s.toString());
                    if (longitudeText <= 180.0 && longitudeText >= -180.0) {

                        updateUI(latitudeText, longitudeText);
                        mEditor.putFloat("LONGITUDE", (float) longitudeText);
                        mEditor.apply();
                        mContext.stopService(mNotificationIntent);
                        tryToStop();
                    }
                }
            }


            @Override
            public void afterTextChanged(Editable s) { }
        });

        latitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    latitudeText = Double.parseDouble(s.toString());
                    if (latitudeText <= 90.0 && latitudeText >= -90.0) {

                        updateUI(latitudeText, longitudeText);
                        mEditor.putFloat("LATITUDE", (float) latitudeText);
                        mEditor.apply();
                        mContext.stopService(mNotificationIntent);
                        tryToStop();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        startButton.setText(isRunning ? "Stop" : "Start");
        startButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                if (!isMockLocationEnabled()) {
                    Toast.makeText(v.getContext(), "Please turn on Mock Location permission on Developer Settings", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    return;
                }

                if (isRunning) {
                    mContext.stopService(mNotificationIntent);
                    mockLocation.stopMockLocationUpdates();
                    startButton.setText("Start");
                } else {

                    mockLocation.startMockLocationUpdates(latitudeText, longitudeText);
                    startButton.setText("Stop");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mContext.startForegroundService(mNotificationIntent);
                    } else {
                        mContext.startService(mNotificationIntent);
                    }
                }
                isRunning = !isRunning;
            }
        });
    }


    private void updateUI(double lati, double longi){
        map.clear();
        map.addMarker(new MarkerOptions().position(new LatLng(lati, longi)).title("picked"));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lati, longi), 13.0));
    }

    private boolean isMockLocationEnabled() {
        boolean isMockLocation;
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (Objects.requireNonNull(opsManager).checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)== AppOpsManager.MODE_ALLOWED);
            } else {
                isMockLocation = !android.provider.Settings.Secure.getString(mContext.getContentResolver(), "mock_location").equals("0");
            }
        } catch (Exception e) {
            return false;
        }
        return isMockLocation;
    }

    static void setLatLng(String mLat, String mLng) {
        latitudeText = Double.parseDouble(mLat);
        longitudeText = Double.parseDouble(mLng);

        latitude.setText(mLat);
        longitude.setText(mLng);
    }

    static String getLat() {
        return latitude.getText().toString();
    }

    static String getLng() {
        return longitude.getText().toString();
    }

    @SuppressLint("SetTextI18n")
    void tryToStop() {
        if (isRunning) {
            mockLocation.stopMockLocationUpdates();
            startButton.setText("Start");
            isRunning = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_basic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }else if(item.getItemId() == R.id.action_add){
            //showCustomDialog();
            getHashKey();
        }else if(item.getItemId() == R.id.action_favourite){
            Intent intent = new Intent(getApplicationContext(), EventActivity.class);
            startActivity(intent);
        }else if(item.getItemId() == R.id.action_settings){
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCustomDialog(){

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_event);
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText et_name = (EditText) dialog.findViewById(R.id.et_name);
        final EditText et_latitude = (EditText) dialog.findViewById(R.id.et_latitude);
        final EditText et_longitude = (EditText) dialog.findViewById(R.id.et_longitude);

        et_latitude.setText(getLat());
        et_longitude.setText(getLng());

        ((ImageButton) dialog.findViewById(R.id.bt_close)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        ((Button) dialog.findViewById(R.id.bt_save)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(et_name.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please input this event name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveToDo(et_name.getText().toString(), et_latitude.getText().toString(), et_longitude.getText().toString());

                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }


    @Override
    public void onMapReady(MapboxMap mapboxMap) {
       /* LocationPluginActivity.this.map = map;
        enableLocationPlugin();*/
        map = mapboxMap;
        locationEnable();
        mapboxMap.getUiSettings().setZoomControlsEnabled(true);
        mapboxMap.getUiSettings().setZoomGesturesEnabled(true);
        mapboxMap.getUiSettings().setScrollGesturesEnabled(true);
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);

        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);

        map.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull @NotNull LatLng latLng) {

                setLatLng(String.valueOf(latLng.getLatitude()), String.valueOf(latLng.getLongitude()));

                updateUI(latLng.getLatitude(), latLng.getLongitude());


            }
        });
    }



    void locationEnable() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            intialLocationEngine();
          //  intializLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    void intialLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLayout = lastLocation;

            //setCamerpostion(lastLocation);

            updateUI(latitudeText, longitudeText);
        } else {
            locationEngine.addLocationEngineListener(this);
        }

    }

    void intializLocationLayer() {

        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
        @SuppressLint("MissingPermission") Location loc = locationLayerPlugin.getLastKnownLocation();

        Toast.makeText(getApplicationContext(), String.valueOf(loc), Toast.LENGTH_SHORT).show();
    }

    void setCamerpostion(Location camerpostion) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(camerpostion.getLatitude(), camerpostion.getLongitude()), 13.0));
    }

    @Override
    public void onConnected() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        updateUI(latitudeText, longitudeText);
//        if (location != null) {
//            originLayout = location;
//            updateUI(latitudeText, longitudeText);
//            //setCamerpostion(originLayout);
//        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            locationEnable();
        }

    }



    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
//        if (locationEngine != null)
//            locationEngine.requestLocationUpdates();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        if (noteDatabase != null) {
            noteDatabase.close();
            noteDatabase = null;
        }
    }

    private void saveToDo(String name, String lati, String longi){

        String sqlSave = "insert into " + NoteDatabase.TABLE_NOTE + " (name, lati, longi) values ("
                + "'" + name  + "'" + ","
                + "'" + lati  + "'" + ","
                + "'" + longi  + "')";

        NoteDatabase database = NoteDatabase.getInstance(mContext);
        database.execSQL(sqlSave);
    }

    public void openDatabase() {
        // open database
        if (noteDatabase != null) {
            noteDatabase.close();
            noteDatabase = null;
        }

        noteDatabase = NoteDatabase.getInstance(this);
        boolean isOpen = noteDatabase.open();
        if (isOpen) {
            Log.d(TAG, "Note database is open.");
        } else {
            Log.d(TAG, "Note database is not open.");
        }
    }

    private void getHashKey(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }



}



