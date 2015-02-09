package com.welfare4u.fdas.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kakao.APIErrorResult;
import com.kakao.MeResponseCallback;
import com.kakao.Session;
import com.kakao.UserManagement;
import com.kakao.UserProfile;
import com.kakao.helper.Logger;
import com.welfare4u.fdas.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class KakaoHelpActivity extends Activity {

    private String TAG = "KakaoHelpActivity.java | ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestMe();
    }

    private void redirectMainActivity(UserProfile userProfile) {
        JSONObject json = new JSONObject();

        try {
            json.put("id", String.valueOf(userProfile.getId()));
            json.put("nickname", userProfile.getNickname());
            json.put("profileImagePath", userProfile.getProfileImagePath());
            json.put("thumbnailImagePath", userProfile.getThumbnailImagePath());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(KakaoHelpActivity.this, MainActivity.class);
        intent.putExtra("kakaoInfo", json.toString());
        intent.putExtra("kakaoToken", Session.getCurrentSession().getAccessToken());
        startActivity(intent);
        finish();
    }

    private void redirectLoginActivity() {
        Intent intent = new Intent(KakaoHelpActivity.this, KakaoActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 사용자의 상태를 알아 보기 위해 me API 호출을 한다.
     */
    private void requestMe() {
        UserManagement.requestMe(new MeResponseCallback() {

            @Override
            protected void onSuccess(final UserProfile userProfile) {
                Log.d(TAG + "onSessionOpened | onSuccess: ", userProfile.toString());
                userProfile.saveUserToCache();
                redirectMainActivity(userProfile);
            }

            @Override
            protected void onNotSignedUp() {
                Log.d(TAG + "onSessionOpened | onNotSignedUp: ", "");
                redirectLoginActivity();
            }

            @Override
            protected void onSessionClosedFailure(final APIErrorResult errorResult) {
                Log.d(TAG + "onSessionOpened | onSessionClosedFailure: ", errorResult.toString());
                redirectLoginActivity();
            }

            @Override
            protected void onFailure(final APIErrorResult errorResult) {
                Log.d(TAG + "onKakaoSessionOpened | onFailure: ", errorResult.toString());
                redirectLoginActivity();
            }
        });
    }
}
