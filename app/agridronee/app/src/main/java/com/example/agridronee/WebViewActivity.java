package com.example.agridronee;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        // DEBUGGING: Print the URL being loaded
        String url = getIntent().getStringExtra("url");
        Log.d("WebViewActivity", "🔍 Loading URL: " + url);

        if (url != null) {
            webView.loadUrl(url);
        } else {
            Log.e("WebViewActivity", "❌ Error: URL is null");
            webView.loadUrl("file:///android_asset/error_page.html"); // Optional: Load an error page
        }
    }
}
