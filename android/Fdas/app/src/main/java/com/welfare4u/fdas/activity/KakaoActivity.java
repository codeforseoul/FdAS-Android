package com.welfare4u.fdas.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.kakao.APIErrorResult;
import com.kakao.AuthType;
import com.kakao.KakaoLink;
import com.kakao.KakaoParameterException;
import com.kakao.KakaoTalkLinkMessageBuilder;
import com.kakao.MeResponseCallback;
import com.kakao.Session;
import com.kakao.SessionCallback;
import com.kakao.SignupResponseCallback;
import com.kakao.UserManagement;
import com.kakao.UserProfile;
import com.kakao.exception.KakaoException;
import com.kakao.helper.SharedPreferencesCache;
import com.kakao.widget.LoginButton;
import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;

import java.util.HashMap;

/**
 * Created by mac on 2015. 2. 5..
 */
public class KakaoActivity extends Activity {

    private String TAG = "KakaoActivity.java | ";

    private SharedPreferences.Editor sharedPreferencesEditor;

    private LoginButton loginButton;
    private final SessionCallback kakaoSessionCallback = new KakaoSessionStatusCallback() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kakao);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        loginButton = (LoginButton)findViewById(R.id.com_kakao_login);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ( Session.initializeSession(KakaoActivity.this, kakaoSessionCallback) ){
            Toast.makeText(KakaoActivity.this, getResources().getString(R.string.wait), Toast.LENGTH_SHORT).show();
            loginButton.setVisibility(View.GONE);
        } else if ( Session.getCurrentSession().isOpened() ){
            onSessionOpened();
        } else {
            loginButton.setVisibility(View.VISIBLE);
        }
    }

    /*
     * kakao Session
     * session state change callback
     */
    private class KakaoSessionStatusCallback implements com.kakao.SessionCallback {

        @Override
        public void onSessionOpened(){
            Log.d(TAG + "KakaoSessionStatusCallback: ", "onSessionOpened");
            KakaoActivity.this.onSessionOpened();
        }

        @Override
        public void onSessionClosed(final KakaoException e) {
            Log.d(TAG + "KakaoSessionStatusCallback: ", "onSessionClosed");
            loginButton.setVisibility(View.VISIBLE);
        }
    }

    private void onSessionOpened(){
        Log.d(TAG, "onSessionOpened: ");
        final Intent intent = new Intent(KakaoActivity.this, KakaoHelpActivity.class);
        startActivity(intent);
        finish();
    }

    /*
     * kakao share dialog
     */
    public static void kakaoShareDialog(MainActivity activity, String name, String caption, String description, String link, String picture){
        try {
            String text = name + " : " + description;

            if ( text.length() > 99 ){
                text = text.substring(0, 100 - 3 );
                text += "...";
            } else {
                text = text.substring(0, text.length() - 1);
            }

            KakaoLink kakaoLink = KakaoLink.getKakaoLink(activity);
            KakaoTalkLinkMessageBuilder kakaoTalkLinkMessageBuilder = kakaoLink.createKakaoTalkLinkMessageBuilder();

            kakaoTalkLinkMessageBuilder.addText(text);
            kakaoTalkLinkMessageBuilder.addImage(picture, 320, 320);
            kakaoTalkLinkMessageBuilder.addWebLink(activity.getResources().getString(R.string.kakako_link), link);
            kakaoLink.sendMessage(kakaoTalkLinkMessageBuilder.build(), activity);
        } catch (Exception e ){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra(Intent.EXTRA_SUBJECT, name);
            intent.putExtra(Intent.EXTRA_TEXT, link);
            intent.setType("text/plain");
            activity.startActivity(Intent.createChooser(intent, "공유"));
        }
    }
}
