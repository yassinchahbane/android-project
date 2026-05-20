package com.chahbane.localllmchat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Track the cloud token for push targeting
        android.util.Log.d("FCM_TOKEN", "New Refresh Token: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        android.util.Log.d("FCM_SERVICE", "From: " + remoteMessage.getFrom());
        
        // Handle payload messages
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            android.util.Log.d("FCM_SERVICE", "Notification Payload Received - Title: " + title + ", Body: " + body);
            sendNotification(title, body);
        } else if (remoteMessage.getData().size() > 0) {
            // Handle raw data payloads if provided
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            android.util.Log.d("FCM_SERVICE", "Data Payload Received - Title: " + title + ", Body: " + body);
            if (title != null && body != null) {
                sendNotification(title, body);
            }
        }
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "fcm_default_channel_v2";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title != null ? title : "Chahbane Message")
                        .setContentText(messageBody != null ? messageBody : "")
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Required on Android Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Cloud Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Firebase Cloud Messaging Notifications");
            channel.enableLights(true);
            channel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }
}

