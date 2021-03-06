package com.welfare4u.fdas.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by koo on 2015-01-26.
 */
public class GCMBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("GcmBroadcastReceiver.java | onReceive", "Received: " + intent.getExtras().toString());

        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());

        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, intent.setComponent(comp));
        setResultCode(Activity.RESULT_OK);
    }
}
