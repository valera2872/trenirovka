package com.valera2872.bjjarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import java.util.Calendar;

/**
 * Version 0.3 presentation and reminder improvements.
 *
 * Keeps the existing training/progression logic intact while improving the
 * plus/minus controls and adding a preparation reminder on workout days.
 */
public class GrapplingV3Activity extends GrapplingActivity {
    private static final String PREFS = "bjj_arm_tracker";
    private static final int REQUEST_MORNING_1 = 601;
    private static final int REQUEST_MORNING_2 = 602;
    private boolean stylingControls = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        schedulePreparationReminders(this);

        View decor = getWindow().getDecorView();
        decor.post(this::styleAdjustmentControls);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        styleAdjustmentControls();
                    }
                }
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Reschedule after the user closes the day/time settings dialog.
            schedulePreparationReminders(this);
            styleAdjustmentControls();
        }
    }

    private void styleAdjustmentControls() {
        if (stylingControls) return;
        stylingControls = true;
        try {
            styleTree(getWindow().getDecorView());
        } finally {
            stylingControls = false;
        }
    }

    private void styleTree(View view) {
        if (view instanceof Button) {
            Button button = (Button) view;
            String text = button.getText() == null ? "" : button.getText().toString().trim();
            if ("+".equals(text) || "−".equals(text) || "-".equals(text)) {
                boolean minus = !"+".equals(text);
                button.setText(minus ? "−" : "+");
                button.setAllCaps(false);
                button.setGravity(Gravity.CENTER);
                button.setIncludeFontPadding(false);
                button.setTextSize(23);
                button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                button.setTextColor(Color.rgb(32, 104, 93));
                button.setPadding(0, 0, 0, dp(2));
                button.setMinWidth(0);
                button.setMinHeight(0);

                GradientDrawable background = new GradientDrawable();
                background.setColor(Color.rgb(232, 242, 238));
                background.setCornerRadius(dp(12));
                background.setStroke(dp(1), Color.rgb(161, 202, 190));
                button.setBackground(background);
                button.setElevation(dp(1));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleTree(group.getChildAt(i));
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static void schedulePreparationReminders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int day1 = prefs.getInt("reminder_day_1", Calendar.TUESDAY);
        int day2 = prefs.getInt("reminder_day_2", Calendar.FRIDAY);
        scheduleWeeklyPreparation(context, day1, REQUEST_MORNING_1);
        scheduleWeeklyPreparation(context, day2, REQUEST_MORNING_2);
    }

    private static void scheduleWeeklyPreparation(Context context, int dayOfWeek, int requestCode) {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        target.set(Calendar.HOUR_OF_DAY, 9);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= System.currentTimeMillis()) {
            target.add(Calendar.DAY_OF_YEAR, 7);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_DAY_REMINDER);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    target.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7,
                    pending
            );
        }
    }
}
