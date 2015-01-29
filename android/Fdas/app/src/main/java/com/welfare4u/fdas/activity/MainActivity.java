package com.welfare4u.fdas.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;

import java.io.IOException;


public class MainActivity extends Activity {

    private WebView webView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private GoogleCloudMessaging gcm;
    private String regId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // webview
        webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClientClass());
        webView.setWebChromeClient(new WebChromeClientClass());
        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");
        webView.loadUrl(Constants.SERVICE_URL);

        // properties
        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // background service start
        if ( checkPlayServices() ){
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId();

            if ( TextUtils.isEmpty(regId) ){
                registerInBackground();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Log.i("MainActivity.java | onNewIntent: ", intent.getStringExtra("msg") );
    }

    /**
     * webview client setting
     */
    private class WebViewClientClass extends WebViewClient{
        /*
         * 페이지 로드시
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){
            super.onPageStarted(view, url, favicon);
            Log.i( "is started : ", url );
        }

        /*
         * 리소스 로드시
         */
        @Override
        public void onLoadResource(WebView view, String url){
            super.onLoadResource(view, url);
            // Log.i("is resource done : ", url);
        }

        /*
         * 페이지 로드이 완료
         */
        @Override
        public void onPageFinished(WebView view, String url){
            super.onPageFinished(view, url);
            Log.i("is load done : ", url);

            if ( url.lastIndexOf("/app/setting") > -1 ){
                Log.i("page is /app/setting : ", url);
                Boolean isAlarm = sharedPreferences.getBoolean("isAlarm", true);

                if ( isAlarm ){
                    webView.loadUrl("javascript:alarmOff()");
                } else {
                    webView.loadUrl("javascript:alarmOn()");
                }
            }
        }

        /*
         * 페이지 에러 발생
         */
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.i("error : ", String.format("%s : %s", Integer.toString(errorCode), description) );
        }
    }

    /**
     * webview chrome setting
     */
    private class WebChromeClientClass extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message msg){
            return super.onCreateWindow(view, dialog, userGesture, msg);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String msg, final android.webkit.JsResult result){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

            alertDialog.
                    setTitle(R.string.alert_title).
                    setMessage(msg).
                    setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            result.confirm();
                        }
                    }).
                    setCancelable(false).
                    create().
                    show();

            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage){
            Log.e("WebView Log: ", consoleMessage.message() + " -- From line "
                                 + consoleMessage.lineNumber() + " of "
                                 + consoleMessage.sourceId());
            return super.onConsoleMessage(consoleMessage);
        }
    }

    /**
     * javascript bridge
     */
    private class AndroidBridge {

        // js -> android
        @JavascriptInterface
        public void alarmSet(final String str) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i("AndroidBridge | alarmFromJS: ", str);
                    Boolean isAlarm = Boolean.valueOf(str);
                    sharedPreferencesEditor.putBoolean("isAlarm", isAlarm);
                    sharedPreferencesEditor.commit();
                    Toast.makeText(MainActivity.this, getResources().getString( isAlarm ? R.string.alarm_on : R.string.alarm_off ), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /*
     * google play service 사용가능한가?
     */
    private boolean checkPlayServices(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if ( resultCode != ConnectionResult.SUCCESS ){
            if ( GooglePlayServicesUtil.isUserRecoverableError(resultCode) ){
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000 ).show();
            } else {
                Log.i("MainActivity.java | canPlayService: ", "This device is not supported");
                finish();
            }

            return false;
        }

        return true;
    }

    /*
     * get registration id
     */
    private String getRegistrationId(){
        String registrationId = sharedPreferences.getString("registrationId", "");

        if (TextUtils.isEmpty(registrationId)){
            Log.i("MainActivity.java | getRegistrationId: ", "Registration not found");
            return "";
        }

        int registeredVersion = sharedPreferences.getInt("appVersion", 0);
        int currentVersion = getAppVersion();

        if ( registeredVersion != currentVersion ){
            Log.i("MainActivity.java | getRegistrationId: ", "App version changed");
            return "";
        }

        return registrationId;
    }

    /*
     * get app version
     */
    private int getAppVersion(){
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch(PackageManager.NameNotFoundException e){
            throw new RuntimeException( e );
        }
    }

    /*
     * get registration id from gcm server
     */
    private void registerInBackground(){
        AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>(){
            @Override
            protected String doInBackground(Void... params){
                try {
                    if ( gcm == null ){
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }

                    regId = gcm.register(Constants.GCM_SENDER_ID);

                    sharedPreferencesEditor.putString("registrationId", regId);
                    sharedPreferencesEditor.putInt("appVersion", getAppVersion());
                    sharedPreferencesEditor.commit();

                    return "Device registered, id = " + regId;
                } catch (IOException e){
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String msg){
                Log.i("MainActivity.java | onPostExecute: ", msg);
            }
        };

        asyncTask.execute(null, null, null);
    }


}
