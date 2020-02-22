package id.michale.volcanomonitoring;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import id.yogiastawan.customui.AwesomeStatusEruptionView;
import id.yogiastawan.customui.AwesomeStatusView;

public class MainActivity extends AppCompatActivity {

    AwesomeStatusView suhu;
    AwesomeStatusView kelembapan;
    AwesomeStatusView tremor;
    AwesomeStatusEruptionView statusGunung;
    LineChart chart;

    DataProcess dataProcess = new DataProcess();

    JSONObject jsonObject, dataAfterProcess;
    JSONArray jsonArrayData;

    String dataValue;

    ArrayList<String> xVal = new ArrayList<>(6);
    ArrayList<Entry> yValSuhu = new ArrayList<>(6);
    ArrayList<Entry> yValKelembapan = new ArrayList<>(6);
    ArrayList<ILineDataSet> lineDataSet;

    XAxis xAxis;
    LineData lineData;
    YAxis yAxis;

    GetDataThread getDataThread;
    Intent serviceIntent;

    Context ctx;

    int lastI;

    TextView lastSyncTime;

    MainActivityModel mainActivityModel;

    Toolbar toolBar;
    ActionBarDrawerToggle actionBarDrawerToggle;
    DrawerLayout drawerLayout;

    NavigationView navigationView;

    int radius = 0;
    double lat_gunung;
    double long_gunung;

    String lastDataReceive;

    private static final int PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent mapIntent = new Intent(MainActivity.this, MapsActivity.class);
                mapIntent.putExtra("radius", radius);
                mapIntent.putExtra("lat_gunung", lat_gunung);
                mapIntent.putExtra("long_gunung", long_gunung);
                if (item.getItemId() == R.id.menu_map) {
                    startActivity(mapIntent);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            }
        });

        //setup action bar
        lastSyncTime = findViewById(R.id.lastSync);
        toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolBar, 0, 0);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        //state view model
        mainActivityModel = ViewModelProviders.of(this).get(MainActivityModel.class);

        suhu = findViewById(R.id.suhu);
        suhu.setLabel(getString(R.string.temperature));
        kelembapan = findViewById(R.id.kelembapan);
        kelembapan.setLabel(getString(R.string.humidity));
        tremor = findViewById(R.id.gempa);
        tremor.setLabel(getString(R.string.getaran));
        statusGunung = findViewById(R.id.status);
        chart = findViewById(R.id.chart);

        //chart setting
        chart.setPinchZoom(true);
        chart.setBorderWidth(1f);
        chart.setBorderColor(Color.BLACK);
        chart.setBackgroundColor(Color.argb(85, 0xBE, 0xBE, 0xBE));
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);

        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        xAxis = chart.getXAxis();
        xAxis.setLabelRotationAngle(45 * (float) Math.PI / 180);

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setGranularity(1f);
        yAxis = chart.getAxisLeft();
        chart.getAxisRight().setEnabled(false);

        //for count number of data chart
        lastI = 0;

        //restore state if state not null
        if (mainActivityModel.getTempValue() != null) {
            lastSyncTime.setText(mainActivityModel.getLastSync());
            suhu.setValue(mainActivityModel.getTempValue());
            suhu.setStatus(mainActivityModel.getTempStatus());
            kelembapan.setValue(mainActivityModel.getHumiValue());
            kelembapan.setStatus(mainActivityModel.getHumiStatus());
            tremor.setValue(mainActivityModel.getVibratValue());
            tremor.setStatus(mainActivityModel.getVibratStatus());
            statusGunung.setStatus(mainActivityModel.getEruptStatus());
            switch (mainActivityModel.getEruptSymbol()) {
                case 0:
                    //normal
                    statusGunung.setValue(getDrawable(R.drawable.ic_normal));
                    break;
                case 1:
                    //waspada
                    statusGunung.setValue(getDrawable(R.drawable.ic_waspada));
                    break;
                case 2:
                    //siaga
                    statusGunung.setValue(getDrawable(R.drawable.ic_siaga));
                    break;
                case 3:
                    //awas
                    statusGunung.setValue(getDrawable(R.drawable.ic_awas));
                    break;
                case 4:
                    statusGunung.setValue(getDrawable(R.drawable.ic_unknown));
                    break;
            }
            xVal = mainActivityModel.getChartXVal();
            yValSuhu = mainActivityModel.getChartYValSuhu();
            yValKelembapan = mainActivityModel.getChartYValKelembapan();
            setData();
        }

        //check google play
        if (!isGooglePlayServiceAvailable()) {
            Toast.makeText(this, "Google Play Service is not avaliable. Map and location will not work properly", Toast.LENGTH_LONG).show();
        }

        //check internet connection
        checkConnectionInternet();

        //check permission
        if (!checkPermission()) {
            requestPermission();

            if (!checkBackgroundLocationPermission()) {
                requestPermissionBackLocation();
            }
        } else {
            requestPermission();
            requestPermissionBackLocation();
        }

        //start service if not already running
        ctx = this;
        getDataThread = new GetDataThread(getContext());
        serviceIntent = new Intent(getContext(), getDataThread.getClass());
        if (isMyServiceNotRunning(getDataThread.getClass())) {
            startService(serviceIntent);
            Log.d("MainActivity", "onCreate: service start");
        }

        this.registerReceiver(dataProcess, new IntentFilter("UpdateVolcanoStatus"));
        Log.d("MainActivity", "onCreate: receive data");
    }

    private boolean isGooglePlayServiceAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int state = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (state != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(state)) {
                googleApiAvailability.getErrorDialog(this, state, 2404).show();
            }
            return false;
        }
        return true;
    }

    private boolean checkPermission() {
        int fineLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkBackgroundLocationPermission() {
        int backLocationPermission = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            backLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        return backLocationPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissionBackLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSIONS_REQUEST_CODE);
        }
    }

    private void checkConnectionInternet() {
        Log.d("INTERNET", "oncek: ncek internet");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (connectivityManager != null) {
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if (info == null || !info.isConnectedOrConnecting()) {
                    noInternetConnectionPrompt();
                    Log.d("INTERNET", "checkConnectionInternet: no connection");
                }
            }


        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        noInternetConnectionPrompt();
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        noInternetConnectionPrompt();
                    }
                });
            }
        }
    }

    private void noInternetConnectionPrompt() {
//        AlertDialog dialog = null;
        Log.d("INTERNET", "onUnavailable: dialog");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        dialogBuilder.setTitle("No Internet Connection")
                .setMessage("Please turn off plan mode, and enable data connection or connect to wifi!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialog.hide();
                        //dialog.dismiss();
                        checkConnectionInternet();

                    }
                })
                .setIcon(getDrawable(R.drawable.ic_portable_wifi_off_black_24dp));
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        stopService(new Intent(MainActivity.this, GetDataThread.class));
        Log.d("LIFE CYCLE", "onPause: pause");
        //unregisterReceiver(dataProcess);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyServiceNotRunning(getDataThread.getClass())) {
            startService(new Intent(MainActivity.this, GetDataThread.class));
        }
        Log.d("LIFE CYCLE", "onResume: resume");

//        this.registerReceiver(dataProcess,new IntentFilter("UpdateVolcanoStatus"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopService(serviceIntent);
        Log.d("LIFE CYCLE", "onDestroy: destroy");
        //unregisterReceiver(dataProcess);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(dataProcess);
        Log.d("LIFE CYCLE", "onStop: stop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isMyServiceNotRunning(getDataThread.getClass())) {
            startService(new Intent(MainActivity.this, GetDataThread.class));
        }
        this.registerReceiver(dataProcess, new IntentFilter("UpdateVolcanoStatus"));
        Log.d("LIFE CYCLE", "onRestart: restart");
    }




    /*
     * Private class broadcaster receiver
     * to receiving data from background service broadcaster
     * */

    private class DataProcess extends BroadcastReceiver {
        String data;

        @Override
        public void onReceive(Context context, Intent intent) {
            //get data
            data = intent.getStringExtra("data");
            //send data to activity view
            if (data != null) {
                rawDataProcessing(data);
            }

        }

        @SuppressLint("DefaultLocale")
        void rawDataProcessing(String rawData) {
            if (!rawData.equals(lastDataReceive)) {
                Log.d("DataProcess", "rawDataProcessing: " + rawData);
                String c = "";
                try {
                    jsonObject = new JSONObject(rawData);
                    JSONObject jsonObject2 = jsonObject.getJSONObject("channel");
                    lat_gunung = jsonObject2.getDouble("latitude");
                    long_gunung = jsonObject2.getDouble("longitude");
                    jsonArrayData = jsonObject.getJSONArray("feeds");
                    Log.d("DataProcess", "rawDataProcessing: " + jsonArrayData.length());
                    for (int i = 0; i < jsonArrayData.length(); i++) {
                        dataAfterProcess = jsonArrayData.getJSONObject(i);
                        c = dataAfterProcess.getString("created_at");
                        final String mSubstring = c.substring(c.length() - 9, c.length() - 4);
                        if (xVal.size() == 6) {
                            xVal.set(i, mSubstring);
                            yValSuhu.set(i, new Entry((float) i, (float) dataAfterProcess.getDouble("field1")));
                            yValKelembapan.set(i, new Entry((float) i, 100 * (1023 - (float) dataAfterProcess.getDouble("field2")) / 1023));
                        } else if (xVal.size() < 6) {
                            xVal.add(i, mSubstring);
                            yValSuhu.add(i, new Entry((float) i, (float) dataAfterProcess.getDouble("field1")));
                            yValKelembapan.add(i, new Entry((float) i, 100 * (1023 - (float) dataAfterProcess.getDouble("field2")) / 1023));
                        }
                    }

//                Log.d("XAVAL", "rawDataProcessing: " + yValSuhu);
                    setData();
                    //for monitoring
                    dataValue = jsonArrayData.getJSONObject(5).get("field1").toString();
                    suhu.setValue(dataValue);
//                Log.d("DataProccess", "rawDataProcessing: " + dataValue);
                    kelembapan.setValue(String.format(Locale.US, "%.1f", 100 * (1023 - (float) jsonArrayData.getJSONObject(5).getDouble("field2")) / 1023));
                    tremor.setValue(jsonArrayData.getJSONObject(5).getString("field3"));
                    String first = c.substring(0, c.indexOf('T'));
                    String second = c.substring(c.indexOf('T') + 1, c.indexOf('+'));
                    lastSyncTime.setText(String.format("%s %s", first, second));
                    try {
                        if (Long.parseLong(tremor.getValue()) >= 20000) {
                            tremor.setStatus(getString(R.string.tremor));
                        } else if (Long.parseLong(tremor.getValue()) < 20000) {
                            tremor.setStatus(getString(R.string.normal));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, String.format("%s Error: %s", R.string.tremor, e.toString()), Toast.LENGTH_LONG).show();
                    }

//                        Log.d("KELEMBAPAN", "rawDataProcessing: kelembapan " + Float.parseFloat(kelembapan.getValue()));

                    try {
                        if (Float.parseFloat(suhu.getValue()) > 32 && Float.parseFloat(suhu.getValue()) <= 37) {
                            suhu.setStatus(getString(R.string.waspada));
                        } else if (Float.parseFloat(suhu.getValue()) > 37 && Float.parseFloat(suhu.getValue()) <= 39) {
                            suhu.setStatus(getString(R.string.siaga));
                        } else if (Float.parseFloat(suhu.getValue()) > 39) {
                            suhu.setStatus(getString(R.string.awas));
                        } else if (Float.parseFloat(suhu.getValue()) <= 32) {
                            suhu.setStatus(getString(R.string.normal));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, String.format("%s Error: %s", R.string.temperature, e.toString()), Toast.LENGTH_LONG).show();
                    }

                    try{
                        if (Float.parseFloat(kelembapan.getValue()) >= 35f) {
                            kelembapan.setStatus(getString(R.string.basah));
                        } else if (Float.parseFloat(kelembapan.getValue()) >= 15f && Float.parseFloat(kelembapan.getValue()) < 35f) {
                            kelembapan.setStatus(getString(R.string.lembab));
                        } else if (Float.parseFloat(kelembapan.getValue()) < 15f) {
                            kelembapan.setStatus(getString(R.string.kering));
                        }
                    }catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, String.format("%s Error: %s",R.string.humidity, e.toString()),Toast.LENGTH_LONG).show();
                    }

                    //logical eruption
                    try{
                        if (Float.parseFloat(suhu.getValue()) <= 32 && Float.parseFloat(kelembapan.getValue()) >= 15) {
//                    Log.d("IF", "rawDataProcessing: "+getString(R.string.eruption_no));
                            suhu.setStatus(getString(R.string.normal));
                            statusGunung.setStatus(getString(R.string.eruption_no));
                            statusGunung.setLabel(getString(R.string.normal));
                            statusGunung.setValue(getDrawable(R.drawable.ic_normal));
                            mainActivityModel.setEruptSymbol(0);
                            radius = 0;
//                    Log.d("PARSE", "rawDataProcessing: "+String.format(Locale.US,kelembapan.getValue()));
                        } else if (Float.parseFloat(suhu.getValue()) <= 32 && Float.parseFloat(kelembapan.getValue()) < 15) {
                            statusGunung.setStatus(getString(R.string.eruption_no));
                            statusGunung.setLabel(getString(R.string.waspada));
                            statusGunung.setValue(getDrawable(R.drawable.ic_waspada));
                            mainActivityModel.setEruptSymbol(1);
                            radius = 3;
                            if (Long.parseLong(tremor.getValue()) >= 20000) {
                                statusGunung.setStatus(getString(R.string.eruption_yes));
                            }
                        } else if ((Float.parseFloat(suhu.getValue()) > 32 && Float.parseFloat(suhu.getValue()) <= 37) && (Float.parseFloat(kelembapan.getValue()) >= 10 && Float.parseFloat(kelembapan.getValue()) <= 14)) {
                            statusGunung.setLabel(getString(R.string.waspada));
                            statusGunung.setValue(getDrawable(R.drawable.ic_waspada));
                            statusGunung.setStatus(getString(R.string.eruption_no));
                            mainActivityModel.setEruptSymbol(1);
                            radius = 3;
                            if (Long.parseLong(tremor.getValue()) >= 20000) {
                                statusGunung.setStatus(getString(R.string.eruption_yes));
                            }
                        } else if ((Float.parseFloat(suhu.getValue()) > 37 && Float.parseFloat(suhu.getValue()) <= 39) && (Float.parseFloat(kelembapan.getValue()) <= 9 && Float.parseFloat(kelembapan.getValue()) >= 5)) {
                            statusGunung.setLabel(getString(R.string.siaga));
                            statusGunung.setStatus(getString(R.string.eruption_no));
                            statusGunung.setValue(getDrawable(R.drawable.ic_siaga));
                            mainActivityModel.setEruptSymbol(2);
                            radius = 6;
                            if (Long.parseLong(tremor.getValue()) > -20000) {
                                statusGunung.setStatus(getString(R.string.eruption_yes));
                            }
                        } else if ((Float.parseFloat(suhu.getValue()) > 39) && (Float.parseFloat(kelembapan.getValue()) <= 4)) {
                            statusGunung.setLabel(getString(R.string.awas));
                            statusGunung.setStatus(getString(R.string.eruption_no));
                            statusGunung.setValue(getDrawable(R.drawable.ic_awas));
                            mainActivityModel.setEruptSymbol(3);
                            radius = 9;
                            if (Long.parseLong(tremor.getValue()) >= 20000) {
                                statusGunung.setStatus(getString(R.string.eruption_yes));
                            }
                        } else {
//                    suhu.setStatus(getString(R.string.normal));
                            statusGunung.setStatus(getString(R.string.unknown));
                            statusGunung.setLabel(getString(R.string.unknown));
                            mainActivityModel.setEruptSymbol(4);
                            statusGunung.setValue(getDrawable(R.drawable.ic_unknown));
                            radius = 0;
                        }
                    }catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, String.format("Error: %s", e.toString()),Toast.LENGTH_LONG).show();
                    }


                    //save state
                    mainActivityModel.setTempValue(suhu.getValue());
                    mainActivityModel.setTempStatus(suhu.getStatus());
                    mainActivityModel.setHumiValue(kelembapan.getValue());
                    mainActivityModel.setHumiStatus(kelembapan.getStatus());
                    mainActivityModel.setVibratValue(tremor.getValue());
                    mainActivityModel.setVibratStatus(tremor.getStatus());
                    mainActivityModel.setChartXVal(xVal);
                    mainActivityModel.setChartYValSuhu(yValSuhu);
                    mainActivityModel.setChartYValKelembapan(yValKelembapan);
                    mainActivityModel.setEruptStatus(statusGunung.getStatus());
                    mainActivityModel.setLastSync(lastSyncTime.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            lastDataReceive = rawData;
        }
    }

    /*
     * set chart value
     * when there are data receiving from background service
     * */
    private void setData() {
        LineDataSet mSuhu, mKelembapan;

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                try {
                    return xVal.get((int) value);
                } catch (Exception ex) {
                    return "";
                }
            }
        });
        mSuhu = new LineDataSet(yValSuhu, "Suhu");
        mKelembapan = new LineDataSet(yValKelembapan, "Kelembapan");
        mSuhu.setFillAlpha(0);
        mKelembapan.setFillAlpha(0);
        mSuhu.setCircleColor(Color.CYAN);
        mKelembapan.setCircleColor(Color.MAGENTA);
        mSuhu.setColor(Color.CYAN);
        mKelembapan.setColor(Color.MAGENTA);
        mSuhu.setLineWidth(1f);
        mKelembapan.setLineWidth(1f);
        mSuhu.setCircleRadius(3f);
        mKelembapan.setCircleRadius(3f);
        mSuhu.setDrawFilled(false);
        mKelembapan.setDrawFilled(false);
        mSuhu.setDrawCircleHole(false);
        mKelembapan.setDrawCircleHole(false);
        mSuhu.setValueTextSize(9f);
        mKelembapan.setValueTextSize(9f);
        mSuhu.setValueTextColor(Color.BLUE);
        mKelembapan.setValueTextColor(Color.RED);
        lineDataSet = new ArrayList<>();
        lineDataSet.add(mSuhu);
        lineDataSet.add(mKelembapan);
        lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    /*
     * function to check is background service already running or not.
     * */

    public Context getContext() {
        return ctx;
    }

    private boolean isMyServiceNotRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("isMyServiceNotRunning?", true + "");
                return false;
            }
        }
        Log.i("isMyServiceNotRunning?", false + "");
        return true;
    }

}
