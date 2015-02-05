package com.welfare4u.fdas.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;

import java.util.Arrays;

/**
 * Created by mac on 2015. 2. 5..
 */
public class FacebookActivity extends Activity {

    private String TAG = "FacebookActivity.java | ";

    private UiLifecycleHelper uiLifecycleHelper;
    private Session.StatusCallback facebookSessionStatusCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiLifecycleHelper = new UiLifecycleHelper(this, null);
        uiLifecycleHelper.onCreate(savedInstanceState);

        facebookSessionStatusCallback = new FacebookSessionStatusCallback();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        uiLifecycleHelper.onActivityResult(requestCode, resultCode, data);

        // facebook session
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiLifecycleHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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

    /*
     * facebook Session
     * session state change callback
     */
    private class FacebookSessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(final Session session, SessionState state, Exception e) {

            if (state.isOpened()) {
                Log.d(TAG + "StatusCallback: ", "Logged in...");

                // callback after Graph API response with user object
                Request.newMeRequest(session, new Request.GraphUserCallback() {

                    @Override
                    public void onCompleted(GraphUser graphUser, Response response) {
                        if (graphUser != null && session != null) {
                            String str = "{" + graphUser.getInnerJSONObject().toString() + ",accessToken:\"" + session.getAccessToken() + "\"}";
                            Log.d(TAG + "StatusCallback: ", str);

                            // e.g.
                            // {user={"id":"-","first_name":"-","birthday":"-","timezone":-,"location":{"id":"-","name":"-"},"email":"-","verified":true,"name":"-","locale":"-","link":"-","last_name":"-","gender":"-","updated_time":"-"}, accessToken="-"}
                        }
                    }
                }).executeAsync();
            } else if (state.isClosed()) {
                Log.d(TAG + "StatusCallback: ", "Logged out...");
            }
        }
    }

    protected void facebookLoginDialog(){
        Session session = Session.getActiveSession();

        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(new Session.
                    OpenRequest(FacebookActivity.this).
                    setPermissions(Arrays.asList("public_profile")).
                    setCallback(facebookSessionStatusCallback));
        } else {
            Session.openActiveSession(FacebookActivity.this, true, facebookSessionStatusCallback);
        }
    }

    /*
     * facebook share dialog
     */
    protected void facebookShareDialog(String name, String caption, String description, String link, String picture){
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

                WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(FacebookActivity.this, Session.getActiveSession(), params)).
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
