package com.example.agridronee;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class FlightControlActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flightcontrol);

        // Initialize buttons
        Button btnUp = findViewById(R.id.btnUp);
        Button btnDown = findViewById(R.id.btnDown);
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);
        Button btnFly = findViewById(R.id.btnFly);
        Button btnSpray = findViewById(R.id.btnSpray);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Set up WebView for IP camera
        webView = findViewById(R.id.webView);
        setupWebView();

        // Button Click Events
        btnUp.setOnClickListener(v -> showToast("Moving Up"));
        btnDown.setOnClickListener(v -> showToast("Moving Down"));
        btnLeft.setOnClickListener(v -> showToast("Moving Left"));
        btnRight.setOnClickListener(v -> showToast("Moving Right"));
        btnFly.setOnClickListener(v -> showToast("Drone Taking Off"));
        btnSpray.setOnClickListener(v -> showToast("Spraying Activated"));
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupWebView() {
        try {
            // Configure WebView settings
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

            // Add WebViewClient to handle errors
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    showToast("WebView Error: " + description);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // Don't allow navigation away from the camera feed
                    view.loadUrl(url);
                    return true;
                }
            });

            // Load the IP camera URL
            String cameraUrl = "http://192.168.246.166:4747/video";
            webView.loadUrl(cameraUrl);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume WebView when activity comes to foreground
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause WebView when activity is in background
        webView.onPause();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}