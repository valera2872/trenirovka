package com.valera2872.bjjarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.valera2872.grapplingarm.TRAINING_REMINDER";
    private static final String CHANNEL_ID = "training_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MainActivity.scheduleAllReminders(context);
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о тренировках",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Две дополнительные тренировки рук для грэпплинга в неделю");
            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, GrapplingActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                101,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Пора на тренировку рук")
                .setContentText("20 минут силовой подготовки для грэпплинга. Технично и без отказа.")
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            manager.notify(2026, builder.build());
        } catch (SecurityException ignored) {
            // Notification permission may be disabled by the user.
        }
    }
}
