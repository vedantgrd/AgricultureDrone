package com.example.agridronee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class SurveyActivity extends AppCompatActivity {

    private static final String TAG = "SurveyActivity";

    // Header components
    private ImageButton backButton, filterReports;

    // Report card buttons
    private MaterialButton btnViewDetails, btnViewDetails2;

    // Bottom navigation
    private LinearLayout navHome, navSurvey, navDrone, navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "onCreate: Starting SurveyActivity");
            setContentView(R.layout.activity_survey);

            initializeViews();
            setupListeners();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error in SurveyActivity initialization: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading survey: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            // Header
            backButton = findViewById(R.id.backButton);
            filterReports = findViewById(R.id.filterReports);

            // Report card buttons
            btnViewDetails = findViewById(R.id.btnViewDetails);
            btnViewDetails2 = findViewById(R.id.btnViewDetails2);

            // Bottom navigation
            navHome = findViewById(R.id.nav_home);
            navSurvey = findViewById(R.id.nav_survey);
            navDrone = findViewById(R.id.nav_drone);
            navSettings = findViewById(R.id.nav_settings);

            Log.d(TAG, "initializeViews: All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initializeViews: Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading UI components", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        try {
            // Header buttons
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                finish(); // Return to previous activity (likely MainActivity)
            });

            filterReports.setOnClickListener(v -> {
                Log.d(TAG, "Filter reports clicked");
                showToast("Filter functionality coming soon");
                // TODO: Implement filter dialog or activity
            });

            // Report card buttons
            btnViewDetails.setOnClickListener(v -> openWebView("file:///android_asset/wheat_survey.html"));

            btnViewDetails2.setOnClickListener(v -> openWebView("file:///android_asset/corn_survey.html"));

            // Bottom navigation
            navHome.setOnClickListener(v -> {
                Log.d(TAG, "Navigating to MainActivity");
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });

            navSurvey.setOnClickListener(v -> {
                Log.d(TAG, "Already on Survey screen");
                showToast("You are already on Survey screen");
            });

            navDrone.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Navigating to FlightControlActivity");
                    Intent intent = new Intent(this, FlightControlActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Drone Control: " + e.getMessage());
                    showToast("Drone Control Coming Soon");
                }
            });

            navSettings.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Navigating to SettingsActivity");
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Settings: " + e.getMessage());
                    showToast("Settings Coming Soon");
                }
            });

            Log.d(TAG, "setupListeners: Listeners set up successfully");
        } catch (Exception e) {
            Log.e(TAG, "setupListeners: Error setting up listeners: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up interactions", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebView(String url) {
        try {
            Log.d("SettingsActivity", "Opening WebView: " + url);
            Intent intent = new Intent(SurveyActivity.this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error opening WebView", e);
            showToast("Failed to open page");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: SurveyActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: SurveyActivity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: SurveyActivity destroyed");
    }
}