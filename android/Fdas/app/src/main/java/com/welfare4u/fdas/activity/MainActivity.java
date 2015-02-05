package com.welfare4u.fdas.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;
import com.welfare4u.fdas.gcm.GcmService;


public class MainActivity extends KakaoActivity {

    private String TAG = "MainActivity.java | ";

    private SharedPreferences.Editor sharedPreferencesEditor;

    KakaoActivity kakaoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // webview
        WebView webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClientClass());
        webView.setWebChromeClient(new WebChromeClientClass());
        webView.addJavascriptInterface(new AndroidBridge(), "androidBridge");
        webView.loadUrl(Constants.SERVICE_URL);

        // properties
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // GcmService
        new GcmService(this);

        // SNS
//        kakaoService = new KakaoService(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
    }


    private class WebViewClientClass extends WebViewClient{
        /*
         * 페이지 로드시
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){
            super.onPageStarted(view, url, favicon);
            Log.d(TAG + "is started: ", url);
        }

        /*
         * 리소스 로드시
         */
        @Override
        public void onLoadResource(WebView view, String url){
            super.onLoadResource(view, url);
            // Log.d(TAG + "is resource done: ", url);
        }

        /*
         * 페이지 로드이 완료
         */
        @Override
        public void onPageFinished(WebView view, String url){
            super.onPageFinished(view, url);
            Log.d(TAG + "is load done: ", url);

            if ( url.lastIndexOf("/app/setting") > -1 ){
                Log.i("page is /app/setting : ", url);
                /*Boolean isAlarm = sharedPreferences.getBoolean("isAlarm", true);

                if ( isAlarm ){
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
            Log.d(TAG + "error: ", String.format("%s : %s", Integer.toString(errorCode), description));
        }

        /*
         * 링크 페이지 클릭시
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            if (!url.startsWith(Constants.SERVICE_URL)){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return false;
            }

            return true;
        }
    }

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
        public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage){
            Log.e("WebView Log: ", consoleMessage.message() + " -- From line "
                    + consoleMessage.lineNumber() + " of "
                    + consoleMessage.sourceId());
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private class AndroidBridge {

        // js -> android
        @JavascriptInterface
        public void alarmSet(final String str) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Boolean isAlarm = str.equals("1");
                    sharedPreferencesEditor.putBoolean("isAlarm", isAlarm);
                    sharedPreferencesEditor.commit();
                    Toast.makeText(MainActivity.this, getResources().getString( isAlarm ? R.string.alarm_on : R.string.alarm_off ), Toast.LENGTH_SHORT).show();
                    Log.d(TAG + "AndroidBridge | alarmSet: ", Boolean.toString(isAlarm));
                }
            });
        }

        @JavascriptInterface
        public void facebookLogin(){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                 facebookLoginDialog();
                }

            });
        }

        @JavascriptInterface
        public void facebookShare(
                final String name,
                final String caption,
                final String description,
                final String link,
                final String picture){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG + "AndroidBridge | facebookShare: ", name);
                    Log.d(TAG + "AndroidBridge | facebookShare: ", description);
                    Log.d(TAG + "AndroidBridge | facebookShare: ", link);
                    Log.d(TAG + "AndroidBridge | facebookShare: ", picture);
                    facebookShareDialog(name, caption, description, link, picture);
                }
            });
        }


        @JavascriptInterface
        public void kakaoLogin(){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    kakaoLoginDialog();
                }
            });
        }

        @JavascriptInterface
        public void kakaoShare(
                final String name,
                final String caption,
                final String description,
                final String link,
                final String picture){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG + "AndroidBridge | kakaoShare: ", name);
                    Log.d(TAG + "AndroidBridge | kakaoShare: ", description);
                    Log.d(TAG + "AndroidBridge | kakaoShare: ", link);
                    Log.d(TAG + "AndroidBridge | kakaoShare: ", picture);
                    kakaoShareDialog(name, caption, description, link, picture);
                }
            });
        }
    }
}
