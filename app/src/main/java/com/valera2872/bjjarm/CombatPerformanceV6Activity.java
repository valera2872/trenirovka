package com.valera2872.bjjarm;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Navigation layer for the platform dashboard.
 *
 * Dynamic screens drawn inside one Activity do not automatically create an
 * Android back stack. This class restores predictable back behaviour and adds
 * a visible back action while editing an existing athlete profile.
 */
public class CombatPerformanceV6Activity extends CombatPerformanceV5Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
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
            addProfileBackButton(root);
            redirectStrengthButtons(root);
        } finally {
            installingNavigation = false;
        }
    }

    private void addProfileBackButton(View root) {
        if (!isEditingExistingProfile(root)) return;
        Button save = findButton(root, "Сохранить изменения");
        if (save == null || !(save.getParent() instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) save.getParent();
        if (findButton(parent, "← Назад без сохранения") != null) return;

        Button back = navigationButton("← Назад без сохранения");
        back.setOnClickListener(v -> returnToDashboard());
        int index = parent.indexOfChild(save);
        parent.addView(back, Math.max(0, index));
    }

    private void redirectStrengthButtons(View root) {
        Button legs = findButton(root, "Открыть модуль «Ноги и корпус»");
        if (legs != null) {
            legs.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
        }

        Button today = findButton(root, "Открыть силовую тренировку");
        if (today != null) {
            today.setOnClickListener(v -> showStrengthChoice());
        }
    }

    private void showStrengthChoice() {
        String[] modules = {
                "Сильные руки и контроль",
                "Базовая сила ног и корпуса"
        };
        new AlertDialog.Builder(this)
                .setTitle("Что тренируем сегодня?")
                .setItems(modules, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, GrapplingV3Activity.class));
                    } else {
                        startActivity(new Intent(this, BaseStrengthV2Activity.class));
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (isEditingExistingProfile(getWindow().getDecorView())) {
            returnToDashboard();
            return;
        }
        super.onBackPressed();
    }

    private void returnToDashboard() {
        // Recreate reads the last saved profile and opens the dashboard.
        recreate();
    }

    private boolean isEditingExistingProfile(View root) {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        return prefs.getBoolean("profile_complete", false)
                && findEditText(root, "Одна техническая миссия на 30 дней") != null;
    }

    private EditText findEditText(View view, String hint) {
        if (view instanceof EditText) {
            EditText field = (EditText) view;
            String currentHint = field.getHint() == null ? "" : field.getHint().toString();
            if (hint.equals(currentHint)) return field;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText result = findEditText(group.getChildAt(i), hint);
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
