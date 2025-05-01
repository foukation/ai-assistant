
package com.skythinker.gptassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

public class H5TripPlanActivity extends Activity {
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trip_plan_webview);

        ImageView closeIcon = findViewById(R.id.float_model_view_close);
        closeIcon.setOnClickListener((view) -> {
           finish();
        });

        loadWebViewContent();
    }

    private void handleCustomUrl(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String destination= uri.getQueryParameter("destination");
        if ("page".equals(host)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setData(Uri.parse(destination));
            startActivity(intent);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebViewContent() {
        WebView webView = null;
        webView = findViewById(R.id.h5_web);
        webView.clearCache(true);
        webView.clearHistory();
        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> {
                request.grant(request.getResources());
            });
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
            public boolean
            shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("lingxiapp://")) {
                    handleCustomUrl(url);
                    return true;
                }
                return false;
            }
        });
        String webUrl = getIntent().getStringExtra("webUrl");
        if(webUrl != null) {
            webView.loadUrl(webUrl);
        }
    }
}
