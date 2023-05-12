package com.mock.gpssearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

public class MockLocationImpl {

    private LocationManager mLocationManager;
    private Handler mHandler;
    private Runnable mRunnable;

    public static float accuracy_value;

    MockLocationImpl(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mHandler = new Handler();
    }

    void startMockLocationUpdates(final double latitude, final double longitude) {
        mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                setMock(LocationManager.GPS_PROVIDER, latitude, longitude);
                setMock(LocationManager.NETWORK_PROVIDER, latitude, longitude);
                mHandler.postDelayed(mRunnable, 200);
            }
        };

        mHandler.post(mRunnable);
    }

    void stopMockLocationUpdates() {
        mHandler.removeCallbacks(mRunnable);
        mLocationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        mLocationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("NewApi")
    private void setMock(String provider, double latitude, double longitude) {
        mLocationManager.addTestProvider (provider,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                0,
                5);

        Location newLocation = new Location(provider);

        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAltitude(3F);
        newLocation.setTime(System.currentTimeMillis());
        newLocation.setSpeed(0.01F);
        newLocation.setBearing(10F);
        newLocation.setAccuracy(accuracy_value);
        newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            newLocation.setBearingAccuracyDegrees(0.01F);
            newLocation.setVerticalAccuracyMeters(0.01F);
            newLocation.setSpeedAccuracyMetersPerSecond(0.001F);
        }
        mLocationManager.setTestProviderEnabled(provider, true);

        mLocationManager.setTestProviderLocation(provider, newLocation);
    }
}
