package com.welfare4u.fdas.activity;

import android.app.Activity;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;


public class MainActivity extends Activity {

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

        public void alarmFromJS(final String str) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Boolean isAlarm = Boolean.valueOf(str);
                    sharedPreferencesEditor.putBoolean("isAlarm", isAlarm);
                    sharedPreferencesEditor.commit();
                    Toast.makeText(MainActivity.this, getResources().getString( isAlarm ? R.string.alarm_on : R.string.alarm_off ), Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void alarmToJS(final String arg) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Boolean isAlarm = sharedPreferences.getBoolean("isAlarm", true);

                    if ( isAlarm ){
                        webView.loadUrl("javascript:alarmOff()");
                    } else {
                        webView.loadUrl("javascript:alarmOn()");
                    }
                }
            });
        }
    }
}
