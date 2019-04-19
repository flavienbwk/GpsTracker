package com.berwick.gpstracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.provider.Settings.Secure;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import com.berwick.gpstracker.LocationDeserializer;
import com.berwick.gpstracker.LocationSerializer;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationService";
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY = "ACTION_PLAY";

    private String defaultUploadWebsite;

    private boolean currentlyProcessingLocation = false;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultUploadWebsite = getString(R.string.default_upload_website);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // if we are currently trying to get a location and the alarm manager has called this again,
        // no need to start processing a new location.
        if (!currentlyProcessingLocation) {
            Log.i(TAG, "Location processed");
            currentlyProcessingLocation = true;
            startTracking();
        } else {
            Log.i(TAG, "currentlyProcessingLocation");
        }

        if (intent != null && null != intent.getAction()) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    Toast.makeText(getApplicationContext(), "Foreground service is stopped.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_PLAY:
                    Toast.makeText(getApplicationContext(), "You click Play button.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_PAUSE:
                    Toast.makeText(getApplicationContext(), "You click Pause button.", Toast.LENGTH_LONG).show();
                    break;
            }
        }

        if (null == intent || null == intent.getAction()) {
            String source = null == intent ? "intent" : "action";
            Log.e(TAG, source + " was null, flags=" + flags + " bits=" + Integer.toBinaryString(flags));
            //return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);

        //return START_NOT_STICKY;
    }

    /* Used to build and start foreground service. */
    private void startForegroundService() {
        Log.d(TAG, "Start foreground service.");

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Create notification builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Make notification show big text.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("Music player implemented by foreground service.");
        bigTextStyle.bigText("Android foreground service is a android service which can run in foreground always, it can be controlled by user via notification.");
        builder.setStyle(bigTextStyle);

        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setFullScreenIntent(pendingIntent, true);

        Log.d(TAG, "...");
        // Add Play button intent in notification.
        Intent playIntent = new Intent(this, LocationService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);
        NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", pendingPlayIntent);
        builder.addAction(playAction);

        // Add Pause button intent in notification.
        Intent pauseIntent = new Intent(this, LocationService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pendingPrevIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        NotificationCompat.Action prevAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pendingPrevIntent);
        builder.addAction(prevAction);

        // Start foreground service.
        Log.d(TAG, "Started foreground service.");
        startForeground(START_NOT_STICKY, builder.build());
    }

    private void stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    private void startTracking() {
        Log.d(TAG, "startTracking");

        // if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
        /*
         else {
            Log.e(TAG, "unable to connect to google play services.");
        }
        */
    }

    protected void sendLocationDataToWebsite(final Location location) {
        // formatted for mysql datetime format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date(location.getTime());

        // Informing the user
        Log.i(TAG_FOREGROUND_SERVICE, "[" + date + "] " + getString(R.string.query_sending));
        Toast.makeText(this, "[" + date + "] " + getString(R.string.query_sending), Toast.LENGTH_LONG).show();

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        float totalDistanceInMeters = sharedPreferences.getFloat("totalDistanceInMeters", 0f);

        boolean firstTimeGettingPosition = sharedPreferences.getBoolean("firstTimeGettingPosition", true);

        if (firstTimeGettingPosition) {
            editor.putBoolean("firstTimeGettingPosition", false);
        } else {
            Location previousLocation = new Location("");
            previousLocation.setLatitude(sharedPreferences.getFloat("previousLatitude", 0f));
            previousLocation.setLongitude(sharedPreferences.getFloat("previousLongitude", 0f));

            float distance = location.distanceTo(previousLocation);
            totalDistanceInMeters += distance;
            editor.putFloat("totalDistanceInMeters", totalDistanceInMeters);
        }

        editor.putFloat("previousLatitude", (float) location.getLatitude());
        editor.putFloat("previousLongitude", (float) location.getLongitude());
        editor.apply();

        final RequestParams requestParams = new RequestParams();
        requestParams.put("latitude", Double.toString(location.getLatitude()));
        requestParams.put("longitude", Double.toString(location.getLongitude()));

        Double speedInMilesPerHour = location.getSpeed() * 2.2369;
        requestParams.put("speed", Integer.toString(speedInMilesPerHour.intValue()));

        try {
            requestParams.put("date", URLEncoder.encode(dateFormat.format(date), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }

        requestParams.put("locationmethod", location.getProvider());

        if (totalDistanceInMeters > 0) {
            requestParams.put("distance", String.format("%.1f", totalDistanceInMeters / 1609)); // in miles,
        } else {
            requestParams.put("distance", "0.0"); // in miles
        }

        String deviceId = Secure.getString(this.getContentResolver(),
                Secure.ANDROID_ID);

        requestParams.put("deviceid", deviceId);
        requestParams.put("username", sharedPreferences.getString("userName", ""));
        requestParams.put("phonenumber", sharedPreferences.getString("appID", "")); // uuid
        requestParams.put("sessionid", sharedPreferences.getString("sessionID", "")); // uuid

        Double accuracyInFeet = location.getAccuracy() * 3.28;
        requestParams.put("accuracy", Integer.toString(accuracyInFeet.intValue()));

        Double altitudeInFeet = location.getAltitude() * 3.28;
        requestParams.put("extrainfo", Integer.toString(altitudeInFeet.intValue()));

        requestParams.put("eventtype", "android");

        Float direction = location.getBearing();
        requestParams.put("direction", Integer.toString(direction.intValue()));

        final String uploadWebsite = sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite);

        LoopjHttpClient.post(uploadWebsite, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - success", uploadWebsite, requestParams, responseBody, headers, statusCode, null);
                removeUserLocation(location);
                stopSelf();
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] errorResponse, Throwable e) {
                LoopjHttpClient.debugLoopJ(TAG, "sendLocationDataToWebsite - failure", uploadWebsite, requestParams, errorResponse, headers, statusCode, e);
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.i(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());
            SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
            Log.i(TAG, "intervalInMinutes : " + sharedPreferences.getInt("intervalInMinutes", -1) + " minutes.");

            // we have our desired accuracy of 500 meters so lets quit this service,
            // onDestroy will be called and stop our location updates
            if (location.getAccuracy() < 500.0f) {
                stopLocationUpdates();
                saveUserLocation(location);

                // Loop through the array of locations
                SharedPreferences sharedLocations = this.getSharedPreferences("com.berwick.gpstracker.locations", Context.MODE_PRIVATE);

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Location.class, new LocationDeserializer());
                gsonBuilder.registerTypeAdapter(Location.class, new LocationSerializer());
                Gson gson = gsonBuilder.create();
                String json = sharedLocations.getString("locations", null);
                Type type = new TypeToken<ArrayList<Location>>() {
                }.getType();
                ArrayList<Location> locations = gson.fromJson(json, type);

                Log.i(TAG, locations.size() + " locations pending to be sent on top of the current location.");
                for (int i = 0; i < locations.size(); i++) {
                    sendLocationDataToWebsite(locations.get(i));
                }
            } else {
                Log.e(TAG, "Accuracy not satisfying yet : " + location.getAccuracy() + ".");
            }

        } else {
            Log.e(TAG, "Impossible to get the location.");
        }
    }

    private boolean saveUserLocation(Location location) {
        Log.i(TAG, "Saving location : " + location.getTime());
        SharedPreferences sharedLocations = this.getSharedPreferences("com.berwick.gpstracker.locations", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor_locations = sharedLocations.edit();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Location.class, new LocationDeserializer());
        gsonBuilder.registerTypeAdapter(Location.class, new LocationSerializer());
        Gson gson = gsonBuilder.create();
        String json = sharedLocations.getString("locations", null);
        Type type = new TypeToken<ArrayList<Location>>() {
        }.getType();
        ArrayList<Location> locationsList;
        if (json == null) {
            locationsList = new ArrayList<>();
        } else {
            locationsList = gson.fromJson(json, type);
        }

        locationsList.add(location);
        String json_save = gson.toJson(locationsList);
        editor_locations.putString("locations", json_save);
        editor_locations.apply();
        return true;
    }

    private boolean removeUserLocation(Location location) {
        Log.i(TAG, "Removal location : " + location.getTime());
        SharedPreferences sharedLocations = this.getSharedPreferences("com.berwick.gpstracker.locations", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor_locations = sharedLocations.edit();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Location.class, new LocationDeserializer());
        gsonBuilder.registerTypeAdapter(Location.class, new LocationSerializer());
        Gson gson = gsonBuilder.create();
        String json = sharedLocations.getString("locations", null);
        Type type = new TypeToken<ArrayList<Location>>() {
        }.getType();
        ArrayList<Location> locationsList = gson.fromJson(json, type);

        Iterator<Location> itr = locationsList.iterator();
        Log.i(TAG, "Checking among " + locationsList.size() + " locations.");
        while (itr.hasNext()) {
            Location loc = (Location) itr.next();
            if (loc != null) {
                Log.i(TAG, "Actual : " + loc.getTime());
                Log.i(TAG, "Check : " + location.getTime());
                if (loc.getTime() == location.getTime()) {
                    Log.i(TAG, "Removed location : " + location.getTime());
                    itr.remove();
                }
            }
        }

        String json_save = gson.toJson(locationsList);
        editor_locations.putString("locations", json_save);
        editor_locations.apply();
        Log.i(TAG, "Finished checks for removal location.");
        return true;
    }

    private void stopLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // milliseconds
        locationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            Log.i(TAG, "Requesting location updates...");
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } catch (SecurityException se) {
            Log.e(TAG, getString(R.string.location_permission));
            Toast.makeText(getApplicationContext(), R.string.location_permission, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");

        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient connection has been suspended.");
    }
}