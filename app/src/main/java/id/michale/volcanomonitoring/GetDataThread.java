package id.michale.volcanomonitoring;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

//add notification from service;

public class GetDataThread extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private InputStream iStream = null;
    private HttpURLConnection urlConnection = null;
    private String strUrl = "https://api.thingspeak.com/channels/757226/feeds.json?api_key=6ZFKPIZE6H9DXPXS&results=6";

    private final String KEY_DATA_UPDATE = "UpdateVolcanoStatus";
    private final String KEY_LOCATION_UPDATE = "UpdateVolcanoLocationUser";

    Intent broadCastIntent = new Intent(KEY_DATA_UPDATE);
    Intent locationBroadcast = new Intent(KEY_LOCATION_UPDATE);
//    Intent broadCastLocationUser=new Intent(KEY_LOCATION_UPDATE);

    String lastUpdateString;

    final String TAG = "LOCATION_REQ";

    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;

    private double latGunung, longGunung;

    private Location userLocation, lastLocation;
    private LocationCallback locationCallback;

    LocationRequest locationRequest = new LocationRequest();
    private int radius = 0;

    private double suhu = -2000, kelembapan = 2000;
    private long gempa;

    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient googleApiClient;

    public GetDataThread() {

    }

    public GetDataThread(Context ApplicationContext) {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        lastLocation = new Location(LocationManager.PASSIVE_PROVIDER);
        userLocation = new Location(LocationManager.PASSIVE_PROVIDER);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && suhu != -2000) {
                    Location d = new Location(LocationManager.PASSIVE_PROVIDER);
                    for (Location location : locationResult.getLocations()) {
                        d = location;
                    }
                    if (d == null) {
                        d = locationResult.getLastLocation();
                    }
                    locationBroadcast.putExtra("user_location", d);
                    locationBroadcast.putExtra("radius", radius);
                    userLocation = locationResult.getLastLocation();
                    Log.d(TAG, "onLocationResult: " + d);
                    sendBroadcast(locationBroadcast);
                    if ((userLocation.getLatitude() != lastLocation.getLatitude()) && (userLocation.getLongitude() != lastLocation.getLongitude())) {
                        logicalBuild();
                    }
                    lastLocation.setLatitude(userLocation.getLatitude());
                    lastLocation.setLongitude(userLocation.getLongitude());

                }
            }
        };

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Volcano Monitoring";
            String description = "Volcano Status Warning";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("VOLCANO_NOTIFIFICATION", name, importance);
            channel.setDescription(description);
            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void notificationBuild(String mainMessage, String status, String erupsi, int radi) {
        Intent mapIntent = new Intent(this, MapsActivity.class);

        Log.d("yogi service", "notificationBuild: " + longGunung);

        mapIntent.putExtra("lat_gunung", latGunung);
        mapIntent.putExtra("long_gunung", longGunung);
        mapIntent.putExtra("radius", radi);
        if (userLocation != null) {
            mapIntent.putExtra("user_location", userLocation);
        }
//        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(this, "VOLCANO_NOTIFIFICATION");
        notificationBuilder.setSmallIcon(R.drawable.ic_awas)
                .setContentTitle("Volcano App Warning")
                .setContentText(mainMessage)
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine(mainMessage)
                        .addLine(String.format(Locale.US, "Status: %s.", status))
                        .addLine(radSelector(radi))
                        .addLine(String.format(Locale.US, "Erupsi: %s.", erupsi)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(0, notificationBuilder.build());
    }

    String radSelector(int rad) {
        if (rad >= 99) {
            return "Radius bahaya: -- KM.";
        } else {
            return String.format(Locale.US, "Radius bahaya: %d KM.", rad);
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(10000);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        googleApiClient.connect();


        startTimer();

        return Service.START_STICKY;
    }

    private Timer timer;
    private TimerTask timerTask;

    private void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        timer.schedule(timerTask, 1000, 1000); //
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    // Creating an http connection to communicate with url
                    urlConnection = (HttpURLConnection) url.openConnection();

                    // Connecting to url
                    urlConnection.connect();

                    // Reading data from url
                    iStream = urlConnection.getInputStream();

                    BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                    StringBuilder sb = new StringBuilder();

                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    br.close();
                    String a = sb.toString();
                    broadCastIntent.putExtra("data", a);
                    sendBroadcast(broadCastIntent);
//                    Log.d("SERVICE", "run: "+a);
                    if (!a.equals(lastUpdateString)) {
                        lastUpdateString = a;
//                        notificationBuild();


                        //json decoder
                        Log.d(TAG, "run: ENCODER");
                        JSONObject jsonObject1 = new JSONObject(a);
                        JSONObject jsonObject = jsonObject1.getJSONObject("channel");
                        latGunung = jsonObject.getDouble("latitude");
                        longGunung = jsonObject.getDouble("longitude");
                        JSONArray jsonArray = jsonObject1.getJSONArray("feeds");
                        JSONObject data = jsonArray.getJSONObject(jsonArray.length() - 1);
                        suhu = data.getDouble("field1");
                        kelembapan = 100 * (1023 - data.getDouble("field2")) / 1023;
                        gempa = data.getLong("field3");
//                        Log.d(TAG, "run: " + suhu + " | " + kelembapan);
//                        notificationBuild("data", "no", 12);
                        locationBroadcast.putExtra("lat_gunung", latGunung);
                        locationBroadcast.putExtra("long_gunung", longGunung);
                        sendBroadcast(locationBroadcast);
                        logicalBuild();
                    }


                    Log.d("THREAD", "run: thread-run");

                } catch (Exception e) {
                    Log.d("network", e.toString());
                }
            }
        };
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void logicalBuild() {

        float distance = 0;
        if (userLocation != null) {
            distance = distanceCalculate(latGunung, longGunung, userLocation.getLatitude(), userLocation.getLongitude());
            Log.d(TAG, "logicalBuild distance:" + distance);
        }
        if (suhu <= 32 && kelembapan >= 15) {
            //normal
            radius = 0;
        } else if (suhu <= 32 && kelembapan < 15) {
            //waspada
            radius = 3;
            if (userLocation != null) {
                if (distance <= 3) {
                    if (gempa <= 0) {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Waspada", "Tidak", radius);
                    } else {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Waspada", "Ya", radius);
                    }
                } else if (distance > 3) {
                    if (gempa <= 0) {
                        notificationBuild("Terjadi perubahan status gunung ke Waspada", "Waspada", "Tidak", radius);
                    } else {
                        notificationBuild("Terjadi perubahan status gunung ke Waspada", "Waspada", "Ya", radius);
                    }
                }

            }

        } else if ((suhu > 32 && suhu <= 37) && (kelembapan >= 10 && kelembapan <= 14)) {
            //waspada
            radius = 3;
            if (userLocation != null) {
                if (distance < 3) {
                    if (gempa <= 0) {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Waspada", "Tidak", radius);
                    } else {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Waspada", "Ya", radius);
                    }
                } else {
                    if (gempa <= 0) {
                        notificationBuild("Terjadi perubahan status gunung ke Waspada", "Waspada", "Tidak", radius);
                    } else {
                        notificationBuild("Terjadi perubahan status gunung ke Waspada", "Waspada", "Ya", radius);
                    }
                }

            }
        } else if ((suhu > 37 && suhu <= 39) && (kelembapan >= 5 && kelembapan <= 9)) {
            //siaga
            radius = 6;
            if (userLocation != null) {
                if (distance < 6) {
                    if (gempa <= 0) {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Siaga", "Tidak", radius);
                    } else {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Siaga", "Ya", radius);
                    }
                } else {
                    if (gempa <= 0) {
                        notificationBuild("Terjadi perubahan status gunung ke Siaga", "Siaga", "Tidak", radius);
                    } else {
                        notificationBuild("Terjadi perubahan status gunung ke Siaga", "Siaga", "Ya", radius);
                    }
                }

            }
        } else if ((suhu > 39) && (kelembapan <= 4)) {
            //awas
            radius = 9;
            if (userLocation != null) {
                if (distance < 9) {
                    if (gempa <= 0) {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Awas", "Tidak", radius);
                    } else {
                        notificationBuild(String.format(Locale.US, "Anda berada pada radius yang berbahaya %d KM.", radius), "Awas", "Ya", radius);
                    }
                } else {
                    if (gempa <= 0) {
                        notificationBuild("Terjadi perubahan status gunung ke Awas", "Awas", "Tidak", radius);
                    } else {
                        notificationBuild("Terjadi perubahan status gunung ke Awas", "Awas", "Ya", radius);
                    }
                }

            }
        } else {
            radius = 99;
            notificationBuild("Waspada! terjadi aktivitas vulkanik", "Unknown", "Unknown", radius);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {

            if (iStream != null) {
                iStream.close();
            }

            stoptimertask();

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (urlConnection != null) {
            urlConnection.disconnect();
        }


        Log.d("GetDataThread", "onDestroy: Service Stoped");
    }

    private float distanceCalculate(double latGunung, double longGunung, double latUSer, double longUser) {
        double sinDLat = Math.sin((Math.PI * (latUSer - latGunung)) / 180);
        double sinDLong = Math.sin((Math.PI * (longUser - longGunung)) / 180);
        double conLat = Math.cos((Math.PI * latGunung) / 180) * Math.cos((Math.PI * latUSer) / 180);
        double a = (sinDLat * sinDLat) + conLat * (sinDLong * sinDLong);
        double c = Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        final long EARTH_MEAN_RADIUS = 6371009;
        return (float) EARTH_MEAN_RADIUS * (float) c / 1000; //km
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location != null && suhu != -200000) {
            locationBroadcast.putExtra("user_location", location);
            locationBroadcast.putExtra("radius", radius);
            sendBroadcast(locationBroadcast);
            Log.d(TAG, "onLocationChanged: " + location);
            logicalBuild();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            Log.d(TAG, "== Error On onConnected() Permission not granted");
            //Permission not granted by user so cancel the further execution.

            return;
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, );
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        Log.d(TAG, "Connected to Google API");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "suspend to connect to Google API");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");
    }
}
