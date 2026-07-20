package com.valera2872.bjjarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.valera2872.grapplingarm.TRAINING_REMINDER";
    public static final String ACTION_DAY_REMINDER = "com.valera2872.grapplingarm.DAY_REMINDER";
    private static final String CHANNEL_ID = "training_reminders";
    private static final String PREFS = "bjj_arm_tracker";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            MainActivity.scheduleAllReminders(context);
            GrapplingV3Activity.schedulePreparationReminders(context);
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о тренировках",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Подготовительное и основное напоминания о тренировках рук");
            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, GrapplingV3Activity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                ACTION_DAY_REMINDER.equals(action) ? 102 : 101,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title;
        String text;
        int notificationId;
        if (ACTION_DAY_REMINDER.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int hour = prefs.getInt("reminder_hour", 18);
            int minute = prefs.getInt("reminder_minute", 30);
            String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            title = "Сегодня тренировка рук";
            text = "Запланирована на " + time + ". Проверь восстановление и освободи около 20 минут.";
            notificationId = 2025;
        } else {
            title = "Пора на тренировку рук";
            text = "20 минут силовой подготовки для грэпплинга. Технично и без отказа.";
            notificationId = 2026;
        }

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Notification permission may be disabled by the user.
        }
    }
}
