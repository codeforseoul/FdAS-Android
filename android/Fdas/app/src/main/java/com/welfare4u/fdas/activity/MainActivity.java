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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Arrays;


public class MainActivity extends Activity {

    private WebView webView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private GoogleCloudMessaging gcm;
    private String registrationId;

    private UiLifecycleHelper uiLifecycleHelper;

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
        // webView.clearCache(true);
        webView.setWebViewClient(new WebViewClientClass());
        webView.setWebChromeClient(new WebChromeClientClass());
        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");
        webView.loadUrl(Constants.SERVICE_URL);

        // properties
        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // facebook help
        uiLifecycleHelper = new UiLifecycleHelper(this, null);
        uiLifecycleHelper.onCreate(savedInstanceState);



        // background service start
        if ( checkPlayServices() ){
            gcm = GoogleCloudMessaging.getInstance(this);
            registrationId = getRegistrationId();

            Log.i("MainActivity.java | has registrationId: ", registrationId);

            if ( TextUtils.isEmpty(registrationId) ){
                registerInBackground();
            }
        }

        // test
        // sharedPreferencesEditor.putBoolean("isAlarm", true);
        // sharedPreferencesEditor.commit();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);

        uiLifecycleHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle bundle) {
                Log.i("Activity", "Success!");
            }

            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception e, Bundle bundle) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiLifecycleHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiLifecycleHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiLifecycleHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiLifecycleHelper.onDestroy();
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

                /*if ( isAlarm ){
                    webView.loadUrl("javascript:fromDeviceCall('alarmSetFromDeviceOn')");
                } else {
                    webView.loadUrl("javascript:fromDeviceCall('alarmSetFromDeviceOff')");
                }*/
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

        /*
         * 링크 페이지 클릭시
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            if ( url.indexOf(Constants.SERVICE_URL) == -1 ){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return false;
            }

            return true;
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

    /*
     * 설정
     */
    @Override
    public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
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
                    Boolean isAlarm = str.equals("1") ? true : false;
                    sharedPreferencesEditor.putBoolean("isAlarm", isAlarm);
                    sharedPreferencesEditor.commit();
                    Toast.makeText(MainActivity.this, getResources().getString( isAlarm ? R.string.alarm_on : R.string.alarm_off ), Toast.LENGTH_SHORT).show();
                    Log.i("AndroidBridge | alarmFromJS: ", Boolean.toString(isAlarm));
                }
            });
        }

        @JavascriptInterface
        public void facebookLogin(){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
//                    Session.openActiveSession(MainActivity.this, true, sessionStatusCallback);
                    Session session = Session.getActiveSession();

                    if (!session.isOpened() && !session.isClosed()) {
                        session.openForRead(new Session.
                            OpenRequest(MainActivity.this).
                            setPermissions(Arrays.asList("public_profile")).
                            setCallback(sessionStatusCallback));
                    } else {
                        Session.openActiveSession(MainActivity.this, true, sessionStatusCallback);
                    }
                }

            });
        }

        @JavascriptInterface
        public void facebookShare(final String name, final String caption, final String description, final String link, final String picture){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i("AndroidBridge | facebookShare: ", name);
                    Log.i("AndroidBridge | facebookShare: ", description);
                    Log.i("AndroidBridge | facebookShare: ", link);
                    Log.i("AndroidBridge | facebookShare: ", picture);
                    facebookShareDialog(name, caption, description, link, picture);
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

                    registrationId = gcm.register(Constants.GCM_SENDER_ID);

                    sharedPreferencesEditor.putBoolean("isAlarm", false);
                    sharedPreferencesEditor.putString("registrationId", registrationId);
                    sharedPreferencesEditor.putInt("appVersion", getAppVersion());
                    sharedPreferencesEditor.commit();

                    registerServer();

                    return "Device registered, id = " + registrationId;
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

    /*
     * registration id to server
     */
    private String registerServer() throws IOException {
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();

        /* server send */
        HttpGet httpGet = new HttpGet(
                            Constants.SERVICE_URL +
                            Constants.PUSH_SERVER_PATH +
                            "?device=android" +
                            "&registrationId=" + registrationId +
                            "&appVersion=" + getAppVersion() );

        /* delay */
        HttpParams httpParams = defaultHttpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpConnectionParams.setSoTimeout(httpParams, 10000);

        /* response from server */
        HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

        String line = "";
        String result = "";

        while ((line = bufferedReader.readLine()) != null){
            result += line;
        }

        return result;
    }

    /*
     * facebook Session
     */
    private Session.StatusCallback sessionStatusCallback = new Session.StatusCallback() {
        // session state change callback
        @Override
        public void call(Session session, SessionState state, Exception e) {
            if (state.isOpened()) {
                Log.i("MainActivity.java | StatusCallback: ", "Logged in...");

                Request.newMeRequest(session, new Request.GraphUserCallback() {
                    // callback after Graph API response with user object
                    @Override
                    public void onCompleted(GraphUser graphUser, Response response) {
                        if (graphUser != null) {
                            Log.i("MainActivity.java | StatusCallback: ", graphUser.toString());
                        }
                    }
                });
            } else if (state.isClosed()) {
                Log.i("MainActivity.java | StatusCallback: ", "Logged out...");
            }
        }
    };

    /*
     * facebook share dialog
     */
    private void facebookShareDialog(String name, String caption, String description, String link, String picture){
        try {
            if (FacebookDialog.canPresentShareDialog(this, FacebookDialog.ShareDialogFeature.SHARE_DIALOG)){
                FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this).
                        setName(name).
                        setCaption(caption).
                        setDescription(description).
                        setLink(link).
                        setPicture(picture).
                        build();
                uiLifecycleHelper.trackPendingDialogCall(shareDialog.present());
            } else {
                Bundle params = new Bundle();
                params.putString("name", name);
                params.putString("caption", caption);
                params.putString("description", description);
                params.putString("link", link);
                params.putString("picture", picture);

                WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(this, Session.getActiveSession(), params)).
                        setOnCompleteListener(new WebDialog.OnCompleteListener(){

                            @Override
                            public void onComplete(Bundle bundle, FacebookException e) {}
                        }).build();
                feedDialog.show();
            }
        } catch (Exception e ){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra(Intent.EXTRA_SUBJECT, name);
            intent.putExtra(Intent.EXTRA_TEXT, link);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "공유"));
        }
    }
}
