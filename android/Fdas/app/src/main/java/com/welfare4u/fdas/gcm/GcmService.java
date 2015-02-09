package com.welfare4u.fdas.gcm;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.activity.MainActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by mac on 2015. 2. 5..
 */
public class GcmService {

    private String TAG = "GcmService.java | ";

    private MainActivity mainActivity;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    public GcmService(MainActivity mainActivity){
        this.mainActivity = mainActivity;

        sharedPreferences = mainActivity.getSharedPreferences(Constants.SP_NAME, mainActivity.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        if ( checkPlayServices() ){
            String registrationId = getRegistrationId();
            Log.d(TAG + "has registrationId: ", registrationId);

            if ( TextUtils.isEmpty(registrationId) ){
                registerInBackground();
            }
        }
    }

    /*
     * google play service 사용가능한가?
     */
    private boolean checkPlayServices(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mainActivity);

        if ( resultCode != ConnectionResult.SUCCESS ){
            if ( GooglePlayServicesUtil.isUserRecoverableError(resultCode) ){
                GooglePlayServicesUtil.getErrorDialog(resultCode, mainActivity, 9000 ).show();
            } else {
                Log.i("MainActivity.java | canPlayService: ", "This device is not supported");
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
            Log.d(TAG + "getRegistrationId: ", "Registration not found");
            return "";
        }

        int registeredVersion = sharedPreferences.getInt("appVersion", 0);
        int currentVersion = getAppVersion();

        if ( registeredVersion != currentVersion ){
            Log.d(TAG + "getRegistrationId: ", "App version changed");
            return "";
        }

        return registrationId;
    }

    /*
     * get app version
     */
    private int getAppVersion(){
        try {
            PackageInfo packageInfo = mainActivity.getPackageManager().getPackageInfo(mainActivity.getPackageName(), 0);
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
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(mainActivity);

                    if ( gcm == null ){
                        gcm = GoogleCloudMessaging.getInstance(mainActivity);
                    }

                    String registrationId = gcm.register(Constants.GCM_SENDER_ID);

                    sharedPreferencesEditor.putString("registrationId", registrationId);
                    sharedPreferencesEditor.putInt("appVersion", getAppVersion());
                    sharedPreferencesEditor.commit();

                    registerServer(registrationId);

                    return "Device registered, id = " + registrationId;
                } catch (IOException e){
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String msg){
                Log.d(TAG + "onPostExecute: ", msg);
            }
        };

        asyncTask.execute(null, null, null);
    }

    /*
     * registration id to server
     */
    private String registerServer(String registrationId) throws IOException {
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        String url = Constants.SERVICE_URL + Constants.PUSH_SERVER_PATH + "?device=android" + "&registrationId=" + registrationId + "&appVersion=" + getAppVersion();

        /* server send */
        HttpGet httpGet = new HttpGet(url);

        /* delay */
        HttpParams httpParams = defaultHttpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpConnectionParams.setSoTimeout(httpParams, 10000);

        /* response from server */
        HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

        String line;
        String result = "";

        while ((line = bufferedReader.readLine()) != null){
            result += line;
        }

        Log.d(TAG + "registerServer: ", url);

        return result;
    }
}
