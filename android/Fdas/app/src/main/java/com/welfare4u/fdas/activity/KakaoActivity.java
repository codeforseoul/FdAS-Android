package com.welfare4u.fdas.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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
import com.welfare4u.fdas.R;

import java.util.HashMap;

/**
 * Created by mac on 2015. 2. 5..
 */
public class KakaoActivity extends FacebookActivity {

    private String TAG = "KakaoActivity.java | ";


    private SessionCallback kakaoSessionCallback;

    private KakaoLink kakaoLink;
    private KakaoTalkLinkMessageBuilder kakaoTalkLinkMessageBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kakaoSessionCallback = new KakaoSessionStatusCallback();

        try {
            kakaoLink = KakaoLink.getKakaoLink(this);
            kakaoTalkLinkMessageBuilder = kakaoLink.createKakaoTalkLinkMessageBuilder();
        } catch (KakaoParameterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + String.valueOf(Session.initializeSession(this, kakaoSessionCallback)));
        Log.d(TAG, "onResume: " + String.valueOf(Session.getCurrentSession()));
        Log.d(TAG, "onResume: " + String.valueOf(Session.getCurrentSession().isOpened()));
    }

    /*
     * kakao Session
     * session state change callback
     */
    private class KakaoSessionStatusCallback implements com.kakao.SessionCallback {

        @Override
        public void onSessionOpened(){
            Log.d(TAG, "KakaoSessionStatusCallback: onSessionOpened");
        }

        @Override
        public void onSessionClosed(final KakaoException e) {
            Log.d(TAG, "KakaoSessionStatusCallback: onSessionClosed");
        }
    }

    /*
     * kakao login dialog
     */
    protected void kakaoLoginDialog(){
        Log.d(TAG, "kakaoLoginDialog: " + String.valueOf(Session.initializeSession(this, kakaoSessionCallback,AuthType.KAKAO_ACCOUNT)));
        Log.d(TAG, "kakaoLoginDialog: " + String.valueOf(Session.getCurrentSession()));
        Log.d(TAG, "kakaoLoginDialog: " + String.valueOf(Session.getCurrentSession().isOpened()));

        if ( Session.initializeSession(KakaoActivity.this, kakaoSessionCallback) ){
            Log.d(TAG, "com.kakao.Session.initializeSession");
        } else if ( Session.getCurrentSession().isOpened() ){
            onKakaoSessionOpened();
        }

        // Log.d(TAG, String.valueOf( Session.getCurrentSession() ));
        // Log.d(TAG, String.valueOf( Session.getCurrentSession().isOpened() ));
    }

    private void onKakaoSessionOpened(){
        UserManagement.requestSignup(new SignupResponseCallback(){
            @Override
            protected void onSuccess(long l) {
                Log.d(TAG + "onKakaoSessionOpened | onSuccess: ", String.valueOf(l));
            }

            @Override
            protected void onSessionClosedFailure(APIErrorResult apiErrorResult) {
                Log.d(TAG + "onKakaoSessionOpened | onSessionClosedFailure: ", apiErrorResult.toString());
            }

            @Override
            protected void onFailure(APIErrorResult apiErrorResult) {
                Log.d(TAG + "onKakaoSessionOpened | onFailure: ", apiErrorResult.toString());
            }
        }, new HashMap());
        /*
        UserManagement.requestMe(new MeResponseCallback() {

            @Override
            protected void onSuccess(final UserProfile userProfile) {
//                Logger.getInstance().d("UserProfile : " + userProfile);
//                userProfile.saveUserToCache();
                Log.d(TAG + "onKakaoSessionOpened | onSuccess: ", userProfile.toString());
            }

            @Override
            protected void onNotSignedUp() {
                Log.d(TAG + "onKakaoSessionOpened | onNotSignedUp: ", "");
            }

            @Override
            protected void onSessionClosedFailure(final APIErrorResult errorResult) {
                Log.d(TAG + "onKakaoSessionOpened | onSessionClosedFailure: ", errorResult.toString());
            }

            @Override
            protected void onFailure(final APIErrorResult errorResult) {
                Log.d(TAG + "onKakaoSessionOpened | onFailure: ", errorResult.toString());
            }
        });
        */
    }

    /*
     * kakao share dialog
     */
    protected void kakaoShareDialog(String name, String caption, String description, String link, String picture){
        try {
            String text = name + " : " + description;

            if ( text.length() > 99 ){
                text = text.substring(0, 100 - 3 );
                text += "...";
            } else {
                text = text.substring(0, text.length() - 1);
            }

            kakaoTalkLinkMessageBuilder.addText(text);
            kakaoTalkLinkMessageBuilder.addImage(picture, 320, 320);
            kakaoTalkLinkMessageBuilder.addWebLink(getResources().getString(R.string.kakako_link), link);
            kakaoLink.sendMessage(kakaoTalkLinkMessageBuilder.build(), this);
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
