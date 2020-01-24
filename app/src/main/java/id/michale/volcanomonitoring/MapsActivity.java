package id.michale.volcanomonitoring;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location userLocation, lastLocation;
    private double latitudeGunung = 0d;
    private double longitudeGunung = 0d;

    TextView jarak, radiusBahaya;

//    private float lastZoom=12f;

    private float radius;

    DataProsess dataProses = new DataProsess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        jarak=findViewById(R.id.jarak);
        radiusBahaya=findViewById(R.id.radius_bahaya);
        userLocation = new Location(LocationManager.PASSIVE_PROVIDER);
        lastLocation = new Location(LocationManager.PASSIVE_PROVIDER);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            longitudeGunung = extras.getDouble("long_gunung", 0);
            latitudeGunung = extras.getDouble("lat_gunung", 0);
//            userLocation=extras.getParcelable("user_location");

            if (extras.getParcelable("user_location") != null) {
                userLocation = extras.getParcelable("user_location");
            } else {
                userLocation = lastLocation;
            }

            radius = extras.getInt("radius", 0);
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        this.registerReceiver(dataProses, new IntentFilter("UpdateVolcanoLocationUser"));

    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(dataProses);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.registerReceiver(dataProses, new IntentFilter("UpdateVolcanoLocationUser"));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        LatLng user=null;
        if (userLocation!=null) {
            user = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(user).title("Your Position").icon(bitmapDescriptorFromVector(this, R.drawable.ic_location_on_black_24dp)));
        }
        LatLng gunung = new LatLng(latitudeGunung, longitudeGunung);
        mMap.addMarker(new MarkerOptions().position(gunung).title("Volcano Position").icon(iconGunungSelector(radius)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gunung, 12f));
        if (radius > 0&&radius<99) {
            mMap.addCircle(new CircleOptions().center(gunung).radius(radius * 1000).fillColor(0x55FF0000).strokeColor(0x00000000));
//            if (user!=null) {
//                mMap.addPolyline(new PolylineOptions().add(user)
//                        .add(gunung)
//                        .color(Color.rgb(0x00, 0x00, 0x00))
//                        .width(2f));
//            }
        }
        lastLocation = userLocation;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        }
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = null;
        if (bitmap != null) {
            canvas = new Canvas(bitmap);
        }
        if (canvas != null) {
            vectorDrawable.draw(canvas);
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private class DataProsess extends BroadcastReceiver {
        //String data;

        @Override
        public void onReceive(Context context, Intent intent) {
            //get data
            latitudeGunung = intent.getDoubleExtra("lat_gunung", 0);
            longitudeGunung = intent.getDoubleExtra("long_gunung", 0);
            Log.d("LANG GUNUNG", "onReceive: " + latitudeGunung);
            if (intent.getParcelableExtra("user_location") != null) {
                userLocation = intent.getParcelableExtra("user_location");
            } else {
                userLocation = lastLocation;
            }

            radius = intent.getIntExtra("radius", 0);
            if (radius<99) {
                radiusBahaya.setText(String.format(Locale.US, "Radius Bahaya %.2f KM.", radius));
            }else {
                radiusBahaya.setText("Radius Bahaya -- KM.");
            }
            mMap.clear();
            LatLng gunung = new LatLng(latitudeGunung, longitudeGunung);

            Location a = new Location(LocationManager.PASSIVE_PROVIDER);
            a.setLongitude(longitudeGunung);
            a.setLatitude(latitudeGunung);
            LatLng user = null;
            if (userLocation != null) {
                Log.d("DISTANCE", "onReceive: " + userLocation.distanceTo(a));
                jarak.setText(String.format(Locale.US, "Jarak Pengguna %.2f KM.",userLocation.distanceTo(a)/1000));
                user = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(user).title("Your Position").icon(bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_location_on_black_24dp)));
            }
            mMap.addMarker(new MarkerOptions().position(gunung).title("Volcano Position").icon(iconGunungSelector(radius)));

            if (radius > 0&&radius<99) {
                mMap.addCircle(new CircleOptions().center(gunung).radius(radius * 1000).fillColor(0x55FF0000).strokeColor(0x00000000));
                if (user!=null) {
                    mMap.addPolyline(new PolylineOptions().add(user)
                            .add(gunung)
                            .color(Color.rgb(0xff, 0x12, 0xf0))
                            .width(2f));
                }
            }
            lastLocation = userLocation;


        }


    }

    private BitmapDescriptor iconGunungSelector(float rad) {
        if (rad <= 0) {
            return bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_normal);
        } else if (rad == 3) {
            return bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_waspada);
        } else if (rad == 6) {
            return bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_siaga);
        } else if (rad > 9) {
            return bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_awas);
        } else {
            return bitmapDescriptorFromVector(MapsActivity.this, R.drawable.ic_unknown);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent=new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
