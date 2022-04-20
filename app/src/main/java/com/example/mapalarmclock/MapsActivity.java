package com.example.mapalarmclock;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.maps.android.SphericalUtil;
import com.ikovac.timepickerwithseconds.MyTimePickerDialog;

import java.io.IOException;
import java.util.List;

class ShowToastUtils {

    private static Toast mToast;

    public static void showToast(Context mContext, String text, int duration) {

        if (mToast == null) {
            mToast = Toast.makeText(mContext, text, duration);
        } else {
            mToast.setText(text);
            mToast.setDuration(duration);
        }
        mToast.show();
    }

    public static void showToast(Context mContext, int resId, int duration) {
        showToast(mContext, mContext.getResources().getString(resId), duration);
    }
}


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean isSetting = false;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient client;
    private ConnectivityManager manager;
    private NetworkInfo networkInfo;
    private int editDistanceValue = 0;
    private double selectedLat = 0, selectedLng = 0;
    private boolean isClicked = false;
    private boolean hasLocation = false;
    private LatLng nowLatLng;
    private int countdownTime = 0;
    String mAddress;
    BroadcastReceiver receiver;
    private static Intent tmp;
    private List<Address> addressList = null;
    NotificationManager notificationManager;

    private void CheckConnection() {
        manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkInfo = manager.getActiveNetworkInfo();
    }

    private void GetClickedAddress(double mLat, double mLng) {
        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocation(mLat, mLng, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Debug", addressList.toString());
        if (!addressList.isEmpty()) {
            Address address = addressList.get(0);
            mAddress = address.getAddressLine(0);

            String city = address.getLocality();
            String state = address.getAdminArea();
            String country = address.getCountryName();
            String postalCode = address.getPostalCode();
            String knownName = address.getFeatureName();

            if (mAddress != null) {
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng latlng = new LatLng(mLat, mLng);
                markerOptions.position(latlng).title(mAddress);
                mMap.addMarker(markerOptions).showInfoWindow();
                isClicked = true;
            } else {
                Toast.makeText(this, "Address Not Found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location Not Found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setNotification(double sLat, double sLng, double distance, int countdownTime){
        // Toast.makeText(getBaseContext(), String.valueOf(distance) + " " + String.valueOf(countdownTime), Toast.LENGTH_SHORT).show();
        Intent it = new Intent(getBaseContext(), CheckService.class);
        tmp = it;
        Log.d("CommandService", sLat + ":" + sLng + ":" + distance + ":" + countdownTime);
        it.putExtra("selectedLat", sLat);
        it.putExtra("selectedLng", sLng);
        it.putExtra("distance", distance);
        it.putExtra("countdownTime", countdownTime);
        startService(tmp);
    }


    private void setMapAlarmClock(double sLat, double sLng, double distance){
        // set Timer
        MyTimePickerDialog mTimePicker = new MyTimePickerDialog(this, new MyTimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(com.ikovac.timepickerwithseconds.TimePicker view, int hourOfDay, int minute, int seconds) {
                countdownTime = hourOfDay * 24 * 60 + minute * 60 + seconds;
                setNotification(sLat, sLng, distance, countdownTime);
            }
        }, 0, 0, 0, true);
        mTimePicker.setTitle("設定倒數時間");
        mTimePicker.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);


        Intent intent = getIntent();
        if (intent.hasExtra("isSetting")) {
            stopService(tmp);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* TODO: We need money
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        // Initialize the SDK
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling fetchPlace()).
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // Create a RectangularBounds object.
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(-33.880490, 151.184363),
                new LatLng(-33.858754, 151.229596));
        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(bounds)
                //.setLocationRestriction(bounds)
                .setOrigin(new LatLng(-33.8749937, 151.2041382))
                .setCountries("AU", "NZ")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
//                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                Log.i("Search", prediction.getPlaceId());
                Log.i("Search", prediction.getPrimaryText(null).toString());
            }
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e("Search", "Place not found: " + apiException.getStatusCode());
            }
        });

         */
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Initial googleMap & circleOptions
        mMap = googleMap;
        CircleOptions circleOptions = new CircleOptions();
        final EditText textBarValue = (EditText) findViewById(R.id.rangeValue);

        // Set GPS Button
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.d("DEBUG", "You already open the permission");
        }
        else{
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
        mMap.setMyLocationEnabled(true);

        // Get Address
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mMap.clear();
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(latLng.latitude, latLng.longitude))
                        .zoom(15)
                        .bearing(0)
                        .build();
                mMap.addMarker(new MarkerOptions().position(latLng).title("Marker Here"));
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                if (textBarValue.getText().toString().matches("") != true) {
                    // Center Point
                    circleOptions.center(latLng);
                    // Radius of the circle
                    circleOptions.radius(Integer.parseInt(textBarValue.getText().toString()));
                    // Border color of the circle
                    circleOptions.strokeWidth(2);
                    circleOptions.strokeColor(Color.BLACK);
                    circleOptions.fillColor(0x30ff0000);
                    // Adding the circle to the GoogleMap
                    mMap.addCircle(circleOptions);
                }
                CheckConnection();
                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                if (networkInfo.isConnected() && networkInfo.isAvailable()) {
                    GetClickedAddress(selectedLat, selectedLng);
                }
                else{
                    Toast.makeText(getBaseContext(), "網路出現異常，請重新嘗試", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Location Button
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Log.d("DEBUG", "clicked");
                client = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                }
                client.getLastLocation();
                Task<Location> task = client.getLastLocation();
                task.addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null){
                            hasLocation = true;
                            nowLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(nowLatLng).title("Marker Here"));
                            // WTF Google: https://developers.google.com/maps/documentation/android-sdk/views?hl=zh-tw#updating_the_camera_view
                            // animate: https://stackoverflow.com/questions/41550684/android-google-maps-animate-the-camera-slowly-to-my-location
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nowLatLng, 15));
                        }
                        else{
                            Toast.makeText(getBaseContext(), "定位失敗", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return false;
            }
        });

        // SeekBar
        SeekBar seekBar = (SeekBar) findViewById(R.id.rangeBar);
        seekBar.setMax(1000);

        // SeekBar Value
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("state", String.valueOf(hasLocation) + String.valueOf(isClicked));
                if (hasLocation && isClicked) {
                    mMap.clear();
                    LatLng selectedPoint = new LatLng(selectedLat, selectedLng);
                    GetClickedAddress(selectedLat, selectedLng);
                    mMap.addMarker(new MarkerOptions().position(selectedPoint).title("Marker Here"));
                    editDistanceValue = progress;
                    textBarValue.setText(String.valueOf(progress));
                    // Center Point
                    circleOptions.center(selectedPoint);
                    // Radius of the circle
                    circleOptions.radius(progress);
                    // Border color of the circle
                    circleOptions.strokeWidth(2);
                    circleOptions.strokeColor(Color.BLACK);
                    circleOptions.fillColor(0x30ff0000);
                    // Adding the circle to the GoogleMap
                    mMap.addCircle(circleOptions);
                }
                else if (hasLocation == false){
                    ShowToastUtils.showToast(getBaseContext(), "您尚未定位",  Toast.LENGTH_SHORT);
                }
                else if (isClicked == false) {
                    ShowToastUtils.showToast(getBaseContext(), "請長按選取目標地點", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // textBar Value
        textBarValue.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                editDistanceValue = Integer.parseInt(textBarValue.getText().toString());
                seekBar.setProgress(editDistanceValue);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Setting the Countdown
        Button btnSet = findViewById(R.id.BtnSet);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClicked && hasLocation) {
                    // Waiting to get selected point
                    // TODO: Alarm Clock
                    // setMapAlarmClock(selectedLat, selectedLng, Integer.parseInt(textBarValue.getText().toString()));

                    LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location GPSLocation = null;
                    if (locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        } else {
                            // TODO: get location permission failed
                        }
                        GPSLocation = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        LatLng nowLocation = new LatLng(GPSLocation.getLatitude(), GPSLocation.getLongitude());
                        LatLng selected = new LatLng(selectedLat, selectedLng);
                        double now_distance = SphericalUtil.computeDistanceBetween(selected, nowLocation);
                        ShowToastUtils.showToast(getBaseContext(), String.format("選取地點: (%s, %s) 距離：%s 公尺", Math.round(selectedLng*10.00/10.00), Math.round(selectedLat*10.00/10.00), Math.round(now_distance * 1.0) / 1.0), Toast.LENGTH_SHORT);
                    }
                }
                else if(hasLocation == false){
                    ShowToastUtils.showToast(getBaseContext(), "您沒有現在的定位位置，請按下定位按鈕", Toast.LENGTH_SHORT);
                }
                else if(isClicked == false){
                    ShowToastUtils.showToast(getBaseContext(), "請選取任一地點", Toast.LENGTH_SHORT);
                }
            }
        });
    }


}