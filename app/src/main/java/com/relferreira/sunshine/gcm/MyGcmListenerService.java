package com.relferreira.sunshine.gcm;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;
import com.relferreira.sunshine.MainActivity;
import com.relferreira.sunshine.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by relferreira on 7/23/16.
 */
public class MyGcmListenerService extends GcmListenerService {

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_WEATHER = "weather";
    private static final String EXTRA_LOCATION = "location";

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (!data.isEmpty()) {
            String senderId = getString(R.string.gcm_defaultSenderId);
            if (senderId.length() == 0) {
                Toast.makeText(this, "SenderID string needs to be set", Toast.LENGTH_LONG).show();
            }
            if ((senderId).equals(from)) {
                String weather = data.getString(EXTRA_WEATHER);
                String location = data.getString(EXTRA_LOCATION);
                String message =
                        String.format(getString(R.string.gcm_weather_alert), weather, location);
                sendNotification(message);

            }
        }
    }


    private void sendNotification(String message) {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.art_storm);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notification_alert_title))
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID, notification);

    }
}
