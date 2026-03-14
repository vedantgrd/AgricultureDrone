package com.example.agridronee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private TextView batteryStatus, weatherInfo, droneStatus, temperature, areaName;
    private ImageView weatherIcon;
    private ImageButton userSettingBtn;
    private MaterialButton btnFlightLogs, btnLiveMap, btnFlightControl, btnFieldSurvey, btnCropHealth, btnPesticideSpray;
    private LinearLayout nav_home, nav_survey, nav_drone, nav_settings;
    private FloatingActionButton fabZoom;
    private GoogleMap mMap;

    private FirebaseAuth mAuth;
    private DatabaseReference droneRef, weatherRef, logsRef;

    private String loggedInEmail;
    private String loggedInUserName;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 1002;

    // Handler for delayed retry
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean mapInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate: Starting MainActivity");
            setContentView(R.layout.activity_main);

            // Check Firebase authentication first
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Log.w(TAG, "onCreate: No authenticated user, redirecting to LoginActivity");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            // Initialize views before using them
            initializeViews();

            // Get user data
            loggedInEmail = user.getEmail();
            loggedInUserName = user.getDisplayName() != null ? user.getDisplayName() : "User";
            String userId = user.getUid();

            // Initialize Firebase references
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                droneRef = database.getReference("Drones").child(userId);
                weatherRef = database.getReference("Weather").child(userId);
                logsRef = database.getReference("FlightLogs").child(userId);
                Log.d(TAG, "onCreate: Firebase database references initialized");
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Error initializing Firebase references: " + e.getMessage());
                Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show();
            }

            // Set up UI interaction
            setupClickListeners();
            setupBottomNavigation();

            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Create location request and callback
            createLocationRequest();

            // Check and request all required permissions
            checkAndRequestAllPermissions();

            // Initialize weather and Firebase listeners
            loadWeatherData();
            setupFirebaseListeners();

            // Initialize map
            initializeMap();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Critical error in MainActivity initialization: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting application: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            Log.d(TAG, "Location update received: " + currentLocation.latitude + ", " + currentLocation.longitude);
                            updateMapWithLocation(currentLocation);
                        }
                        break;
                    }
                }
            }
        };
    }

    private void initializeViews() {
        try {
            batteryStatus = findViewById(R.id.batteryStatus);
            weatherInfo = findViewById(R.id.weatherInfo);
            droneStatus = findViewById(R.id.drone_status);
            temperature = findViewById(R.id.temperature);
            areaName = findViewById(R.id.areaName);
            weatherIcon = findViewById(R.id.weatherIcon);
            userSettingBtn = findViewById(R.id.usersetting);
            btnFlightLogs = findViewById(R.id.btnFlightLogs);
            btnLiveMap = findViewById(R.id.btnLiveMap);
            btnFlightControl = findViewById(R.id.btnFlightControl);
            btnFieldSurvey = findViewById(R.id.btnFieldSurvey);
            btnCropHealth = findViewById(R.id.btnCropHealth);
            btnPesticideSpray = findViewById(R.id.btnPesticideSpray);
            nav_home = findViewById(R.id.nav_home);
            nav_survey = findViewById(R.id.nav_survey);
            nav_drone = findViewById(R.id.nav_drone);
            nav_settings = findViewById(R.id.nav_settings);

            Log.d(TAG, "initializeViews: All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initializeViews: Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading UI components", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeMap() {
        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapFragment);

            if (mapFragment == null) {
                Log.e(TAG, "initializeMap: Map fragment is null!");
                Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
                return;
            }

            mapFragment.getMapAsync(this);
            Log.d(TAG, "initializeMap: Map initialization requested");
        } catch (Exception e) {
            Log.e(TAG, "initializeMap: Error initializing map: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFirebaseListeners() {
        try {
            droneRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            Integer battery = snapshot.child("battery").getValue(Integer.class);
                            Boolean connected = snapshot.child("connected").getValue(Boolean.class);
                            String signal = snapshot.child("signal").getValue(String.class);
                            Double lat = snapshot.child("latitude").getValue(Double.class);
                            Double lng = snapshot.child("longitude").getValue(Double.class);

                            // Use safe defaults if values are null
                            int batteryLevel = battery != null ? battery : 0;
                            boolean isConnected = connected != null ? connected : false;
                            String signalStrength = signal != null ? signal : "Unknown";

                            updateDroneUI(batteryLevel, isConnected, signalStrength, lat, lng);
                        } else {
                            // Create default drone data if it doesn't exist
                            createDefaultDroneData();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: Error processing drone data: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "setupFirebaseListeners: Drone data fetch failed: " + error.getMessage());
                    Toast.makeText(MainActivity.this, "Failed to load drone data", Toast.LENGTH_SHORT).show();
                }
            });

            weatherRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            String temp = snapshot.child("temperature").getValue(String.class);
                            String desc = snapshot.child("description").getValue(String.class);
                            String icon = snapshot.child("icon").getValue(String.class);

                            if (temp != null && desc != null && icon != null) {
                                temperature.setText(temp);
                                weatherInfo.setText(desc);
                                weatherIcon.setImageResource(getWeatherIcon(icon));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: Error processing weather data: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "setupFirebaseListeners: Weather data fetch failed: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setupFirebaseListeners: Error setting up Firebase listeners: " + e.getMessage(), e);
        }
    }

    private void createDefaultDroneData() {
        try {
            // Create default data structure for a new user
            droneRef.child("battery").setValue(85);
            droneRef.child("connected").setValue(false);
            droneRef.child("signal").setValue("Strong");

            // Default to Mumbai coordinates if we can't get user location
            droneRef.child("latitude").setValue(19.0760);
            droneRef.child("longitude").setValue(72.8777);

            Log.d(TAG, "createDefaultDroneData: Created default drone data");
        } catch (Exception e) {
            Log.e(TAG, "createDefaultDroneData: Error creating default drone data: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            userSettingBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
            });

            btnLiveMap.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, LiveMap.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Live Map: " + e.getMessage());
                }
            });

            btnFlightControl.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, FlightControlActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Flight Control: " + e.getMessage());
                    sendDroneCommand("start_flight");
                }
            });

            btnFlightLogs.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, FlightLogsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Flight Logs: " + e.getMessage());
                }
            });

            btnFieldSurvey.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Survey Activity: " + e.getMessage());
                    sendDroneCommand("start_survey");
                }
            });
            btnCropHealth.setOnClickListener(v -> openWebView("file:///android_asset/sample_report.html"));
            btnPesticideSpray.setOnClickListener(v -> sendDroneCommand("spray_pesticide"));

            if (fabZoom != null) {
                fabZoom.setOnClickListener(v -> {
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.zoomIn());
                    } else {
                        Log.w(TAG, "fabZoom: Map is not initialized");
                        showToast("Map not loaded yet");
                    }
                });
            } else {
                Log.e(TAG, "setupClickListeners: fabZoom is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "setupClickListeners: Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void openWebView(String url) {
        try {
            Log.d("SettingsActivity", "Opening WebView: " + url);
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error opening WebView", e);
            showToast("Failed to open page");
        }
    }

    private void sendDroneCommand(String command) {
        try {
            droneRef.child("command").setValue(command)
                    .addOnSuccessListener(aVoid -> {
                        String actionMessage;
                        switch (command) {
                            case "start_flight":
                                actionMessage = "Flight started";
                                break;
                            case "start_survey":
                                actionMessage = "Survey started";
                                break;
                            case "analyze_crop":
                                actionMessage = "Crop analysis started";
                                break;
                            case "spray_pesticide":
                                actionMessage = "Spraying started";
                                break;
                            default:
                                actionMessage = "Command sent";
                        }
                        showToast(actionMessage);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "sendDroneCommand: Failed to send command: " + e.getMessage());
                        showToast("Failed to send command to drone");
                    });
        } catch (Exception e) {
            Log.e(TAG, "sendDroneCommand: Exception: " + e.getMessage(), e);
            showToast("Error sending command");
        }
    }

    private void setupBottomNavigation() {
        try {
            nav_home.setOnClickListener(v -> showToast("You are already on Home screen"));

            nav_survey.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Navigating to SurveyActivity");
                    Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Survey: " + e.getMessage());
                    showToast("Error opening survey");
                }
            });

            nav_drone.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(MainActivity.this, FlightControlActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Drone Control: " + e.getMessage());
                    showToast("Drone Control Coming Soon");
                }
            });

            nav_settings.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Settings: " + e.getMessage());
                    showToast("Settings Coming Soon");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setupBottomNavigation: Error setting up bottom navigation: " + e.getMessage(), e);
        }
    }

    private void updateDroneUI(int batteryLevel, boolean isConnected, String signalStrength, Double latitude, Double longitude) {
        try {
            batteryStatus.setText("Battery: " + batteryLevel + "%");
            droneStatus.setText("Drone Status: " + (isConnected ? "Connected" : "Ready") + " | Signal: " + signalStrength);

            if (mMap != null && latitude != null && longitude != null) {
                LatLng droneLocation = new LatLng(latitude, longitude);
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(droneLocation)
                        .title("Drone Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(droneLocation, 15f));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateDroneUI: Error updating drone UI: " + e.getMessage(), e);
        }
    }

    private void checkAndRequestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check all required permissions
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    ALL_PERMISSIONS_REQUEST_CODE
            );
        } else {
            // All permissions already granted
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
        } else {
            // No permissions granted
            allGranted = false;
        }

        if (allGranted) {
            Log.d(TAG, "onRequestPermissionsResult: All requested permissions granted");
            startLocationUpdates();
            if (mMap != null) {
                enableMyLocation();
            }
        } else {
            Log.w(TAG, "onRequestPermissionsResult: Some permissions denied");
            Toast.makeText(this, "Location permissions are required for map functionality", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

            // Also get the last known location immediately
            getLastLocation();
        }
    }

    private void getLastLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "getLastLocation: Location: " + currentLocation.latitude + ", " + currentLocation.longitude);

                        if (mMap != null) {
                            updateMapWithLocation(currentLocation);
                        } else {
                            Log.w(TAG, "getLastLocation: Map is null, will update when ready");
                            // Store location and update map when it's ready
                            handler.postDelayed(() -> {
                                if (mMap != null) {
                                    updateMapWithLocation(currentLocation);
                                }
                            }, 1000);
                        }
                    } else {
                        Log.w(TAG, "getLastLocation: No last known location available");
                        // Use default location if no location is available
                        LatLng defaultLocation = new LatLng(19.0760, 72.8777);
                        if (mMap != null) {
                            updateMapWithLocation(defaultLocation);
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "getLastLocation: Failed to get location: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Location services error", Toast.LENGTH_SHORT).show();

                    // Use default location on failure
                    LatLng defaultLocation = new LatLng(19.0760, 72.8777);
                    if (mMap != null) {
                        updateMapWithLocation(defaultLocation);
                    }
                });
            } else {
                Log.w(TAG, "getLastLocation: Location permission not granted");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } catch (Exception e) {
            Log.e(TAG, "getLastLocation: Exception: " + e.getMessage(), e);
        }
    }

    private void updateMapWithLocation(LatLng location) {
        try {
            if (mMap == null) {
                Log.e(TAG, "updateMapWithLocation: Map is null");
                return;
            }

            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Current Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

            // Enable my location after setting marker
            enableMyLocation();

            // Update drone reference location
            droneRef.child("latitude").setValue(location.latitude);
            droneRef.child("longitude").setValue(location.longitude);

            Log.d(TAG, "updateMapWithLocation: Map updated with location: " + location);
        } catch (Exception e) {
            Log.e(TAG, "updateMapWithLocation: Error updating map: " + e.getMessage(), e);
        }
    }

    private void enableMyLocation() {
        try {
            if (mMap != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                Log.d(TAG, "enableMyLocation: My location enabled on map");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "enableMyLocation: SecurityException: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "enableMyLocation: Exception: " + e.getMessage(), e);
        }
    }

    private void loadWeatherData() {
        String apiKey = "d506f9acad88f650874e103bd9fe8902";
        String city = "Mumbai";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" + apiKey;

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL weatherUrl = new URL(url);
                connection = (HttpURLConnection) weatherUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject main = jsonResponse.getJSONObject("main");
                    JSONArray weatherArray = jsonResponse.getJSONArray("weather");
                    JSONObject weather = weatherArray.getJSONObject(0);

                    double temp = main.getDouble("temp");
                    String weatherDescription = weather.getString("description");
                    String iconCode = weather.getString("icon");

                    String temperatureText = temp + "°C";
                    String weatherMessage = Character.toUpperCase(weatherDescription.charAt(0)) + weatherDescription.substring(1);

                    runOnUiThread(() -> {
                        try {
                            temperature.setText(temperatureText);
                            weatherInfo.setText(weatherMessage);
                            areaName.setText("Area: MUMBAI");
                            weatherIcon.setImageResource(getWeatherIcon(iconCode));
                        } catch (Exception e) {
                            Log.e(TAG, "loadWeatherData: Error updating UI: " + e.getMessage(), e);
                        }
                    });

                    // Update Firebase
                    weatherRef.child("temperature").setValue(temperatureText);
                    weatherRef.child("description").setValue(weatherMessage);
                    weatherRef.child("icon").setValue(iconCode);
                } else {
                    Log.e(TAG, "loadWeatherData: HTTP error code: " + responseCode);
                    runOnUiThread(() -> showToast("Weather data not available"));
                }
            } catch (Exception e) {
                Log.e(TAG, "loadWeatherData: Exception: " + e.getMessage(), e);
                runOnUiThread(() -> showToast("Failed to load weather data"));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private int getWeatherIcon(String iconCode) {
        switch (iconCode) {
            case "01d": return R.drawable.ic_sunny;
            case "01n": return R.drawable.ic_night;
            case "02d": return R.drawable.ic_few_clouds;
            case "02n": return R.drawable.ic_few_clouds;
            case "03d": case "03n": return R.drawable.ic_cloudy;
            case "04d": case "04n": return R.drawable.ic_cloudy;
            case "09d": case "09n": return R.drawable.ic_rainy;
            case "10d": return R.drawable.ic_rain_sun;
            case "10n": return R.drawable.ic_rain_night;
            case "11d": case "11n": return R.drawable.ic_thunderstorm;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_mist;
            default: return R.drawable.ic_unknown;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        mapInitialized = true;

        try {
            // Configure map settings
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);

            // Set initial position to default location (Mumbai)
            LatLng defaultLocation = new LatLng(19.0760, 72.8777);
            mMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("Default Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

            // Try to get actual location if permissions are granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "onMapReady: Location permission not granted, requesting...");
                checkAndRequestAllPermissions();
            }
        } catch (Exception e) {
            Log.e(TAG, "onMapReady: Error configuring map: " + e.getMessage(), e);
            Toast.makeText(this, "Error configuring map", Toast.LENGTH_SHORT).show();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

        // Try to initialize map again if it failed the first time
        if (!mapInitialized && mMap == null) {
            handler.postDelayed(this::initializeMap, 1000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop location updates to save battery
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Final cleanup
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        Log.d(TAG, "onDestroy: MainActivity destroyed");
    }
}