package com.example.agridronee;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import java.io.File;
import java.text.DecimalFormat;

public class SettingsActivity extends AppCompatActivity {

    // Switches for connectivity
    private SwitchCompat switchGPS, switchBluetooth, switchPushNotifications, switchDarkMode;

    // Switches for security
    private SwitchCompat switchBiometric, switchTwoFactor;

    // Switches for permissions
    private SwitchCompat switchCameraAccess, switchStorageAccess, switchLocationAccess;

    // Switches for data & privacy
    private SwitchCompat switchAnalytics, switchFlightData;

    // Text displays
    private TextView cacheSize, downloadPath, languageValue, unitsValue, mapStyleValue;

    // Clickable layouts
    private LinearLayout clearCache, downloadLocation, language, unitsOfMeasurement, mapStyleSetting;
    private LinearLayout contactSupport, faqSection, reportIssue;
    private LinearLayout termsAndConditions, privacyPolicy, licenses, dataExport;

    // Button
    private Button btnLogout;
    private ImageButton btnBack;

    // Preferences
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("AgriDroneSettings", MODE_PRIVATE);

        try {
            initializeComponents();
            loadSavedSettings();
            setupListeners();
            calculateCacheSize();
        } catch (Exception e) {
            Log.e("SettingsActivity", "❌ Error initializing components", e);
            showToast("Error loading settings");
        }
    }

    private void initializeComponents() {
        try {
            // Connectivity switches
            switchGPS = findViewById(R.id.switchGPS);
            switchBluetooth = findViewById(R.id.switchBluetooth);
            switchPushNotifications = findViewById(R.id.switchPushNotifications);
            switchDarkMode = findViewById(R.id.switchDarkMode);

            // Security switches
            switchBiometric = findViewById(R.id.switchBiometric);
            switchTwoFactor = findViewById(R.id.switchTwoFactor);

            // Permission switches
            switchCameraAccess = findViewById(R.id.switchCameraAccess);
            switchStorageAccess = findViewById(R.id.switchStorageAccess);
            switchLocationAccess = findViewById(R.id.switchLocationAccess);

            // Data & Privacy switches
            switchAnalytics = findViewById(R.id.switchAnalytics);
            switchFlightData = findViewById(R.id.switchFlightData);

            // Text displays
            cacheSize = findViewById(R.id.cacheSize);
            downloadPath = findViewById(R.id.downloadPath);
            languageValue = findViewById(R.id.languageValue);
            unitsValue = findViewById(R.id.unitsValue);
            mapStyleValue = findViewById(R.id.mapStyleValue);

            // Clickable layouts
            clearCache = findViewById(R.id.clearCache);
            downloadLocation = findViewById(R.id.downloadLocation);
            language = findViewById(R.id.language);
            unitsOfMeasurement = findViewById(R.id.unitsOfMeasurement);
            mapStyleSetting = findViewById(R.id.mapStyleSetting);
            contactSupport = findViewById(R.id.contactSupport);
            faqSection = findViewById(R.id.faqSection);
            reportIssue = findViewById(R.id.reportIssue);
            termsAndConditions = findViewById(R.id.termsAndConditions);
            privacyPolicy = findViewById(R.id.privacyPolicy);
            licenses = findViewById(R.id.licenses);
            dataExport = findViewById(R.id.dataExport);

            // Button
            btnLogout = findViewById(R.id.btnLogout);
            btnBack = findViewById(R.id.btnBack);

        } catch (Exception e) {
            Log.e("SettingsActivity", "❌ Error initializing UI components", e);
            throw e;
        }
    }

    private void loadSavedSettings() {
        // Load switch states from shared preferences
        switchGPS.setChecked(isGPSEnabled());
        switchBluetooth.setChecked(isBluetoothEnabled());
        switchPushNotifications.setChecked(prefs.getBoolean("push_notifications", true));
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        switchBiometric.setChecked(prefs.getBoolean("biometric", true));
        switchTwoFactor.setChecked(prefs.getBoolean("two_factor", false));
        switchCameraAccess.setChecked(prefs.getBoolean("camera_access", true));
        switchStorageAccess.setChecked(prefs.getBoolean("storage_access", true));
        switchLocationAccess.setChecked(prefs.getBoolean("location_access", true));
        switchAnalytics.setChecked(prefs.getBoolean("analytics", true));
        switchFlightData.setChecked(prefs.getBoolean("flight_data", true));

        // Load text values
        languageValue.setText(prefs.getString("language", "English"));
        unitsValue.setText(prefs.getString("units", "Metric"));
        mapStyleValue.setText(prefs.getString("map_style", "Satellite"));
        downloadPath.setText(prefs.getString("download_path", "Internal Storage"));
    }

    private void setupListeners() {
        try {
            // Back button
            btnBack.setOnClickListener(v -> finish());

            // Logout button
            btnLogout.setOnClickListener(v -> {
                showLogoutConfirmation();
            });

            // Connectivity switches
            switchGPS.setOnCheckedChangeListener((buttonView, isChecked) -> toggleGPS(isChecked));
            switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> toggleBluetooth(isChecked));
            switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("push_notifications", isChecked).apply();
                showToast("Push Notifications " + (isChecked ? "Enabled" : "Disabled"));
            });
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("dark_mode", isChecked).apply();
                toggleDarkMode(isChecked);
            });

            // Security switches
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("biometric", isChecked).apply();
                showToast("Biometric Login " + (isChecked ? "Enabled" : "Disabled"));
            });
            switchTwoFactor.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("two_factor", isChecked).apply();
                showToast("Two-Factor Authentication " + (isChecked ? "Enabled" : "Disabled"));
            });

            // Permission switches
            switchCameraAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("camera_access", isChecked).apply();
                showToast("Camera Access " + (isChecked ? "Enabled" : "Disabled"));
                if (isChecked) {
                    requestCameraPermission();
                }
            });
            switchStorageAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("storage_access", isChecked).apply();
                showToast("Storage Access " + (isChecked ? "Enabled" : "Disabled"));
                if (isChecked) {
                    requestStoragePermission();
                }
            });
            switchLocationAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("location_access", isChecked).apply();
                showToast("Location Access " + (isChecked ? "Enabled" : "Disabled"));
                if (isChecked) {
                    requestLocationPermission();
                }
            });

            // Data & Privacy switches
            switchAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("analytics", isChecked).apply();
                showToast("Usage Analytics " + (isChecked ? "Enabled" : "Disabled"));
            });
            switchFlightData.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("flight_data", isChecked).apply();
                showToast("Flight Data Recording " + (isChecked ? "Enabled" : "Disabled"));
            });

            // Cache & Data
            clearCache.setOnClickListener(v -> clearAppCache());
            downloadLocation.setOnClickListener(v -> showDownloadLocationDialog());

            // Display Settings
            language.setOnClickListener(v -> showLanguageDialog());
            unitsOfMeasurement.setOnClickListener(v -> showUnitsDialog());
            mapStyleSetting.setOnClickListener(v -> showMapStyleDialog());

            // Support & Help
            contactSupport.setOnClickListener(v -> openWebView("file:///android_asset/contact_support.html"));
            faqSection.setOnClickListener(v -> openWebView("file:///android_asset/faq.html"));
            reportIssue.setOnClickListener(v -> openWebView("file:///android_asset/report_issue.html"));

            // Legal
            termsAndConditions.setOnClickListener(v -> openWebView("file:///android_asset/terms_conditions.html"));
            privacyPolicy.setOnClickListener(v -> openWebView("file:///android_asset/privacy_policy.html"));
            licenses.setOnClickListener(v -> openWebView("file:///android_asset/licenses.html"));

            // Data Export
            dataExport.setOnClickListener(v -> showDataExportDialog());

        } catch (Exception e) {
            Log.e("SettingsActivity", "Error setting listeners", e);
            throw e;
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Perform logout actions
                    prefs.edit().putBoolean("is_logged_in", false).apply();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void toggleDarkMode(boolean enable) {
        if (enable) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // Recreate the activity to apply the theme
        recreate();
    }

    private void calculateCacheSize() {
        try {
            File cacheDir = getCacheDir();
            long size = getDirSize(cacheDir);
            DecimalFormat df = new DecimalFormat("#.##");
            String cacheSizeText;

            if (size > 1024 * 1024) {
                cacheSizeText = df.format(size / (1024.0 * 1024.0)) + " MB";
            } else if (size > 1024) {
                cacheSizeText = df.format(size / 1024.0) + " KB";
            } else {
                cacheSizeText = size + " B";
            }

            cacheSize.setText(cacheSizeText);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error calculating cache size", e);
            cacheSize.setText("Unknown");
        }
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir == null || !dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getDirSize(file);
                }
            }
        }
        return size;
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Chinese"};
        String currentLanguage = prefs.getString("language", "English");
        int selectedIndex = 0;

        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    prefs.edit().putString("language", languages[which]).apply();
                    languageValue.setText(languages[which]);
                    dialog.dismiss();
                    showToast("Language set to " + languages[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUnitsDialog() {
        String[] units = {"Metric", "Imperial"};
        String currentUnit = prefs.getString("units", "Metric");
        int selectedIndex = currentUnit.equals("Metric") ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Select Measurement Units")
                .setSingleChoiceItems(units, selectedIndex, (dialog, which) -> {
                    prefs.edit().putString("units", units[which]).apply();
                    unitsValue.setText(units[which]);
                    dialog.dismiss();
                    showToast("Units set to " + units[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMapStyleDialog() {
        String[] styles = {"Standard", "Satellite", "Terrain", "Hybrid"};
        String currentStyle = prefs.getString("map_style", "Satellite");
        int selectedIndex = 0;

        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(currentStyle)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Map Style")
                .setSingleChoiceItems(styles, selectedIndex, (dialog, which) -> {
                    prefs.edit().putString("map_style", styles[which]).apply();
                    mapStyleValue.setText(styles[which]);
                    dialog.dismiss();
                    showToast("Map style set to " + styles[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDownloadLocationDialog() {
        String[] locations = {"Internal Storage", "External SD Card", "Custom Location"};
        String currentLocation = prefs.getString("download_path", "Internal Storage");
        int selectedIndex = 0;

        for (int i = 0; i < locations.length; i++) {
            if (locations[i].equals(currentLocation)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Download Location")
                .setSingleChoiceItems(locations, selectedIndex, (dialog, which) -> {
                    prefs.edit().putString("download_path", locations[which]).apply();
                    downloadPath.setText(locations[which]);
                    dialog.dismiss();
                    showToast("Download location set to " + locations[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDataExportDialog() {
        String[] formats = {"CSV", "JSON", "PDF"};

        new AlertDialog.Builder(this)
                .setTitle("Export Data")
                .setItems(formats, (dialog, which) -> {
                    showToast("Exporting data in " + formats[which] + " format...");
                    // Simulate exporting
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            runOnUiThread(() -> showToast("Data exported successfully"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Open WebView for Terms & Conditions and Contact Support
    private void openWebView(String url) {
        try {
            Log.d("SettingsActivity", "Opening WebView: " + url);
            Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error opening WebView", e);
            showToast("Failed to open page");
        }
    }

    // ✅ Clear Cache
    private void clearAppCache() {
        try {
            File cacheDir = getCacheDir();
            if (deleteDir(cacheDir)) {
                showToast("Cache Cleared Successfully");
                calculateCacheSize(); // Recalculate and update cache size
            } else {
                showToast("⚠️ Cache could not be cleared");
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "❌ Error clearing cache", e);
            showToast("Error clearing cache");
        }
    }

    // ✅ Recursive Directory Deletion
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    // ✅ Check if GPS is enabled
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // ✅ Toggle GPS (Redirects to Location Settings)
    private void toggleGPS(boolean enable) {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (enable && !isEnabled) {
                showToast("Redirecting to GPS settings...");
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (!enable && isEnabled) {
                showToast("GPS can't be disabled programmatically");
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "❌ Error toggling GPS", e);
        }
    }

    // ✅ Check if Bluetooth is enabled
    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // ✅ Toggle Bluetooth (with runtime permission check)
    private void toggleBluetooth(boolean enable) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                showToast("Bluetooth not supported on this device");
                return;
            }

            // ✅ Check Bluetooth permission for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 101);
                    showToast("Bluetooth permission required!");
                    return;
                }
            }

            // ✅ Enable or Disable Bluetooth
            if (enable && !bluetoothAdapter.isEnabled()) {
                showToast("Enabling Bluetooth...");
                bluetoothAdapter.enable();
            } else if (!enable && bluetoothAdapter.isEnabled()) {
                showToast("Disabling Bluetooth...");
                bluetoothAdapter.disable();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "❌ Error toggling Bluetooth", e);
            showToast("Error toggling Bluetooth");
        }
    }

    // Request camera permission
    private void requestCameraPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 102);
            }
        }
    }

    // Request storage permission
    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 103);
            }
        }
    }

    // Request location permission
    private void requestLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 104);
            }
        }
    }

    // ✅ Show Toast Messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update GPS and Bluetooth switch states as they may have changed
        switchGPS.setChecked(isGPSEnabled());
        switchBluetooth.setChecked(isBluetoothEnabled());
    }
}