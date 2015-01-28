package com.welfare4u.fdas.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.welfare4u.fdas.Constants;
import com.welfare4u.fdas.R;
import com.welfare4u.fdas.activity.MainActivity;

/**
 * Created by koo on 2015-01-26.
 * GCM에서 알림이 왔을 때 폰에 표시하기 위한 기능
 */
public class GcmIntentService extends IntentService {
    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) { // has effect of unparcelling Bundle
         /*
          * Filter messages based on message type. Since it is likely that GCM
          * will be extended in the future with new message types, just ignore
          * any message types you're not interested in, or that you don't
          * recognize.
          */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                String msg = intent.getStringExtra("message");
                sendNotification(msg);
                Log.i("GcmIntentService.java | onHandleIntent", "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    /*
     * display notification, function add
     */
    private void sendNotification(String msg) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(this);
        notificationCompat.setSmallIcon(R.drawable.ic_launcher);
        notificationCompat.setTicker(getResources().getString(R.string.notification_ticker));
        notificationCompat.setWhen(System.currentTimeMillis());
        notificationCompat.setContentTitle(getResources().getString(R.string.notification_title));
        notificationCompat.setContentText(msg);
        notificationCompat.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        notificationCompat.setContentIntent(pendingIntent);
        notificationCompat.setAutoCancel(true);

        // notification send
        notificationManager.notify(Constants.NOTIFICATION_ID, notificationCompat.build());
    }
}