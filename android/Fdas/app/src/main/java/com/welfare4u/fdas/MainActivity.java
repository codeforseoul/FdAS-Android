package com.welfare4u.fdas;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.welfare4u.fdas.gcm.GcmIntentService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogRecord;

import static android.app.PendingIntent.*;


public class MainActivity extends ActionBarActivity {

    private WebView webView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // webview
        webView = (WebView)findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("http://www.welfare4u.com");
        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");

        // webview setting
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);

        // properties
        sharedPreferences = getSharedPreferences("fdas", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // notification
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(this);
        notificationCompat.setSmallIcon(R.drawable.ic_launcher);
        notificationCompat.setTicker(getResources().getString(R.string.notification_ticker));
        notificationCompat.setWhen(System.currentTimeMillis());
        notificationCompat.setContentTitle(getResources().getString(R.string.notification_title));
        notificationCompat.setContentText("message");
        notificationCompat.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        notificationCompat.setContentIntent(pendingIntent);
        notificationCompat.setAutoCancel(true);

        // notification send
        notificationManager.notify(15978943, notificationCompat.build());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    /**
     * javascript bridge
     */
    private class AndroidBridge {
        /**
         * alarm off
         * @param arg
         */
        public void alarmOff(final String arg){
            Handler handler = new Handler();
            handler.post( new Runnable() {
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
        public void alarmOn(final String arg){
            Handler handler = new Handler();
            handler.post( new Runnable() {
                @Override
                public void run() {
                    sharedPreferencesEditor.putBoolean("alarm", true);
                    sharedPreferencesEditor.commit();
                    webView.loadUrl("javascript:alarmOn()");
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.alarm_off), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
