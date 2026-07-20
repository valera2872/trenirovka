package com.valera2872.bjjarm;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Adds a real previous-screen action to the dynamic legs-and-core module. */
public class BaseStrengthV2Activity extends BaseStrengthActivity {
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private boolean installingNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::installNavigation);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        installNavigation();
                    }
                }
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) installNavigation();
    }

    private void installNavigation() {
        if (installingNavigation) return;
        installingNavigation = true;
        try {
            View root = getWindow().getDecorView();
            LinearLayout workoutPage = findWorkoutPage(root);
            if (workoutPage != null && findButton(workoutPage, "← К обзору модуля") == null) {
                Button back = navigationButton("← К обзору модуля");
                back.setOnClickListener(v -> confirmReturnToOverview());
                workoutPage.addView(back, 0);
            }
        } finally {
            installingNavigation = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (findWorkoutPage(getWindow().getDecorView()) != null) {
            confirmReturnToOverview();
        } else {
            finish();
        }
    }

    private void confirmReturnToOverview() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться к обзору?")
                .setMessage("Отмеченные, но не завершённые подходы этой тренировки не сохранятся.")
                .setNegativeButton("Остаться", null)
                .setPositiveButton("Вернуться", (dialog, which) -> recreate())
                .show();
    }

    private LinearLayout findWorkoutPage(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText() == null
                    ? "" : ((TextView) view).getText().toString();
            if (text.startsWith("Ноги и корпус · A") || text.startsWith("Ноги и корпус · B")) {
                View parent = (View) view.getParent();
                if (parent instanceof LinearLayout) return (LinearLayout) parent;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                LinearLayout result = findWorkoutPage(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button) {
            Button button = (Button) view;
            String value = button.getText() == null ? "" : button.getText().toString();
            if (text.equals(value)) return button;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findButton(group.getChildAt(i), text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Button navigationButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(PRIMARY);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setMinHeight(dp(48));
        button.setBackground(rounded(PRIMARY_SOFT, 13));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
