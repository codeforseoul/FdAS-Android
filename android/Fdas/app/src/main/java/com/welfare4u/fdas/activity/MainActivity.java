package com.welfare4u.fdas.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;


public class MainActivity extends ActionBarActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // webview
        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(Constants.SERVICE_URL);
        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");

        // webview setting
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * javascript bridge
     */
    private class AndroidBridge {
        // properties
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

        /**
         * alarm off
         * @param arg
         */
        public void alarmOff(final String arg) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    sharedPreferencesEditor.putBoolean("alarm", false);
                    sharedPreferencesEditor.commit();
                    webView.loadUrl("javascript:alarmOff()");
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.alarm_off), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * alarm on
         * @param arg
         */
        public void alarmOn(final String arg) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    sharedPreferencesEditor.putBoolean("alarm", true);
                    sharedPreferencesEditor.commit();
                    webView.loadUrl("javascript:alarmOn()");
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.alarm_on), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
