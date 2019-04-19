package com.berwick.gpstracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.location.Location;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.DialogInterface;

import com.google.gson.Gson;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Arrays;
import java.util.ArrayList;

public class GpsTrackerActivity extends AppCompatActivity {

    private static String[] PERMISSIONS_ARR = {
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    };

    private static final String TAG = "GpsTrackerActivity";

    // use the websmithing defaultUploadWebsite for testing and then check your
    // location with your browser here: https://www.websmithing.com/gpstracker/displaymap.php
    private String defaultUploadWebsite;

    private EditText txtUserName;
    private EditText txtWebsite;
    private Button trackingButton;

    private boolean currentlyTracking;
    private RadioGroup intervalRadioGroup;
    private int intervalInMinutes = 1;
    private AlarmManager alarmManager;
    private Intent gpsTrackerIntent;
    private PendingIntent pendingIntent;
    private ArrayList<Location> locationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_tracker);
        loadUserLocations();

        setTitle("GpsTracker FB");
        Intent intent = new Intent(GpsTrackerActivity.this, LocationService.class);
        intent.setAction(LocationService.ACTION_START_FOREGROUND_SERVICE);
        startService(intent);

        /*
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        */

        defaultUploadWebsite = getString(R.string.default_upload_website);

        txtWebsite = (EditText) findViewById(R.id.txtWebsite);
        txtUserName = (EditText) findViewById(R.id.txtUserName);
        intervalRadioGroup = (RadioGroup) findViewById(R.id.intervalRadioGroup);
        trackingButton = (Button) findViewById(R.id.trackingButton);
        txtUserName.setImeOptions(EditorInfo.IME_ACTION_DONE);

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        currentlyTracking = sharedPreferences.getBoolean("currentlyTracking", false);

        boolean firstTimeLoadingApp = sharedPreferences.getBoolean("firstTimeLoadingApp", true);
        if (firstTimeLoadingApp) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstTimeLoadingApp", false);
            editor.putString("appID", UUID.randomUUID().toString());
            editor.apply();
        }


        intervalRadioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        saveInterval();
                    }
                });

        trackingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                trackLocation(view);
            }
        });
    }

    private void checkPhoneStatePermission() {
        for (int i = 0; i < PERMISSIONS_ARR.length; i++) {
            int permissionCheck = ContextCompat.checkSelfPermission(
                    this, PERMISSIONS_ARR[i]);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSIONS_ARR[i])) {
                    showExplanation("Permission Needed", "Please allow access to your geolocation.", PERMISSIONS_ARR[i], 1);
                } else {
                    requestPermission(PERMISSIONS_ARR[i], 1);
                }
            }
        }
    }

    private void saveInterval() {
        if (currentlyTracking) {
            Toast.makeText(getApplicationContext(), R.string.user_needs_to_restart_tracking, Toast.LENGTH_LONG).show();
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (intervalRadioGroup.getCheckedRadioButtonId()) {
            case R.id.i1:
                editor.putInt("intervalInMinutes", 1);
                break;
            case R.id.i2:
                editor.putInt("intervalInMinutes", 2);
                break;
            case R.id.i3:
                editor.putInt("intervalInMinutes", 3);
                break;
            case R.id.i5:
                editor.putInt("intervalInMinutes", 5);
                break;
            case R.id.i10:
                editor.putInt("intervalInMinutes", 10);
                break;
            case R.id.i15:
                editor.putInt("intervalInMinutes", 15);
                break;
        }

        editor.apply();
    }

    private void startAlarmManager() {
        Log.d(TAG, "startAlarmManager");

        Context context = getBaseContext();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                intervalInMinutes * 60000, // 60000 = 1 minute
                pendingIntent);
    }

    private void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager");

        Context context = getBaseContext();
        Intent gpsTrackerIntent = new Intent(context, GpsTrackerAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, gpsTrackerIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    // called when trackingButton is tapped
    protected void trackLocation(View v) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!saveUserSettings()) {
            return;
        }

        if (!checkIfGooglePlayEnabled()) {
            return;
        }

        checkPhoneStatePermission();

        if (currentlyTracking) {
            cancelAlarmManager();

            currentlyTracking = false;
            editor.putBoolean("currentlyTracking", false);
            editor.putString("sessionID", "");
        } else {
            startAlarmManager();

            currentlyTracking = true;
            editor.putBoolean("currentlyTracking", true);
            editor.putFloat("totalDistanceInMeters", 0f);
            editor.putBoolean("firstTimeGettingPosition", true);
            editor.putString("sessionID", UUID.randomUUID().toString());
        }

        editor.apply();
        setTrackingButtonState();
    }

    private boolean loadUserLocations() {
        Log.i(TAG, "Loading previous locations...");
        SharedPreferences sharedLocations = this.getSharedPreferences("com.berwick.gpstracker.locations", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor_locations = sharedLocations.edit();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Location.class, new LocationDeserializer());
        gsonBuilder.registerTypeAdapter(Location.class, new LocationSerializer());
        Gson gson = gsonBuilder.create();
        String json = sharedLocations.getString("locations", null);
        Type type = new TypeToken<ArrayList<Location>>() {
        }.getType();

        if (json != null)
            locationsList = gson.fromJson(json, type);

        if (locationsList == null) {
            locationsList = new ArrayList<Location>();
        } else {
            /*
            SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.activity_gps_tracker, from, to);
            ListView androidListView = (ListView) findViewById(R.id.list_view);
            androidListView.setAdapter(simpleAdapter);
            */
        }
        return true;
    }

    private boolean saveUserSettings() {
        if (textFieldsAreEmptyOrHaveSpaces()) {
            return false;
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (intervalRadioGroup.getCheckedRadioButtonId()) {
            case R.id.i1:
                editor.putInt("intervalInMinutes", 1);
                break;
            case R.id.i2:
                editor.putInt("intervalInMinutes", 2);
                break;
            case R.id.i3:
                editor.putInt("intervalInMinutes", 3);
                break;
            case R.id.i5:
                editor.putInt("intervalInMinutes", 5);
                break;
            case R.id.i10:
                editor.putInt("intervalInMinutes", 10);
                break;
            case R.id.i15:
                editor.putInt("intervalInMinutes", 15);
                break;
        }

        editor.putString("userName", txtUserName.getText().toString().trim());
        editor.putString("defaultUploadWebsite", txtWebsite.getText().toString().trim());

        editor.apply();

        return true;
    }

    private boolean textFieldsAreEmptyOrHaveSpaces() {
        String tempUserName = txtUserName.getText().toString().trim();
        String tempWebsite = txtWebsite.getText().toString().trim();

        if (tempWebsite.length() == 0 || hasSpaces(tempWebsite) || tempUserName.length() == 0 || hasSpaces(tempUserName)) {
            Toast.makeText(this, R.string.textfields_empty_or_spaces, Toast.LENGTH_LONG).show();
            return true;
        }

        return false;
    }

    private boolean hasSpaces(String str) {
        return ((str.split(" ").length > 1) ? true : false);
    }

    private void displayUserSettings() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.berwick.gpstracker.prefs", Context.MODE_PRIVATE);
        intervalInMinutes = sharedPreferences.getInt("intervalInMinutes", 1);

        switch (intervalInMinutes) {
            case 1:
                intervalRadioGroup.check(R.id.i1);
                break;
            case 5:
                intervalRadioGroup.check(R.id.i5);
                break;
            case 15:
                intervalRadioGroup.check(R.id.i15);
                break;
        }

        txtWebsite.setText(sharedPreferences.getString("defaultUploadWebsite", defaultUploadWebsite));
        txtUserName.setText(sharedPreferences.getString("userName", ""));
    }

    private boolean checkIfGooglePlayEnabled() {

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            return true;
        } else {

            Log.e(TAG, String.valueOf(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)));
            Log.e(TAG, "Unable to connect to google play services.");
            Toast.makeText(getApplicationContext(), R.string.google_play_services_unavailable, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void setTrackingButtonState() {
        if (currentlyTracking) {
            trackingButton.setBackgroundResource(R.drawable.green_tracking_button);
            trackingButton.setTextColor(Color.BLACK);
            trackingButton.setText(R.string.tracking_is_on);
        } else {
            trackingButton.setBackgroundResource(R.drawable.red_tracking_button);
            trackingButton.setTextColor(Color.WHITE);
            trackingButton.setText(R.string.tracking_is_off);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission successfully granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        displayUserSettings();
        setTrackingButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}