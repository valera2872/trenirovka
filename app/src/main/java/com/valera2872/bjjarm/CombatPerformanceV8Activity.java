package com.valera2872.bjjarm;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * UX polish for 0.6.1: clear combat-sport wording, one-line buttons,
 * removal of prototype copy, and visible navigation at the top of profile edit.
 */
public class CombatPerformanceV8Activity extends CombatPerformanceV7Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private boolean polishing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::polishInterface);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        polishInterface();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::polishInterface);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) polishInterface();
    }

    private void polishInterface() {
        if (polishing) return;
        polishing = true;
        try {
            View root = getWindow().getDecorView();
            rewriteAndFit(root);
            removePrototypeCard(root);
            moveProfileBackToTop(root);
        } finally {
            polishing = false;
        }
    }

    private void rewriteAndFit(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (value.startsWith("Твоя игровая база:")) {
                text.setText(value.replaceFirst("Твоя игровая база:", "Твоя борцовская база:"));
            }
        }

        if (view instanceof Button) {
            Button button = (Button) view;
            String value = button.getText() == null ? "" : button.getText().toString().trim();

            if ("Получилось".equals(value)) {
                button.setText("Удалось");
                value = "Удалось";
            }

            button.setAllCaps(false);
            button.setSingleLine(true);
            button.setMaxLines(1);
            button.setEllipsize(TextUtils.TruncateAt.END);
            button.setIncludeFontPadding(false);
            button.setAutoSizeTextTypeUniformWithConfiguration(
                    9, 15, 1, TypedValue.COMPLEX_UNIT_SP);

            if ("Профиль".equals(value)) {
                button.setTextSize(12);
                button.setPadding(dp(5), 0, dp(5), 0);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                int desiredWidth = dp(100);
                if (params != null && params.width > 0 && params.width != desiredWidth) {
                    params.width = desiredWidth;
                    button.setLayoutParams(params);
                }
            }

            rewireNavigation(button, value);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rewriteAndFit(group.getChildAt(i));
            }
        }
    }

    private void rewireNavigation(Button button, String value) {
        if ("Открыть модуль «Сильные руки и контроль»".equals(value)
                || "Начать тренировку рук".equals(value)) {
            button.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV4Activity.class)));
        } else if ("Начать тренировку ног".equals(value)
                || "Открыть модуль «Ноги и корпус»".equals(value)) {
            button.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
        } else if ("Открыть силовую тренировку".equals(value)) {
            button.setOnClickListener(v -> showStrengthChoice());
        } else if ("Открыть мою неделю".equals(value)
                || "Настроить мою неделю".equals(value)
                || "Настроить неделю".equals(value)
                || "Открыть техническую миссию".equals(value)
                || "Открыть восстановление".equals(value)
                || "Открыть план соревнования".equals(value)) {
            button.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV2Activity.class)));
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
                        startActivity(new Intent(this, GrapplingV4Activity.class));
                    } else {
                        startActivity(new Intent(this, BaseStrengthV2Activity.class));
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void removePrototypeCard(View root) {
        TextView title = findText(root, "Что уже работает");
        if (title == null) return;
        ViewParent cardParent = title.getParent();
        if (!(cardParent instanceof View)) return;
        View card = (View) cardParent;
        ViewParent pageParent = card.getParent();
        if (pageParent instanceof ViewGroup) {
            ((ViewGroup) pageParent).removeView(card);
        }
    }

    private void moveProfileBackToTop(View root) {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("profile_complete", false)) return;
        EditText mission = findEditText(root, "Одна техническая миссия на 30 дней");
        if (mission == null) return;
        LinearLayout page = findPage(mission);
        if (page == null) return;

        Button back = findButton(root, "← Назад без сохранения");
        if (back == null) back = findButton(root, "← Главный экран");
        if (back == null) {
            back = navigationButton("← Главный экран");
        }

        back.setText("← Главный экран");
        back.setTag("profile-top-back");
        back.setOnClickListener(v -> recreate());

        ViewParent oldParent = back.getParent();
        if (oldParent == page && page.indexOfChild(back) == 0) return;
        if (oldParent instanceof ViewGroup) ((ViewGroup) oldParent).removeView(back);
        page.addView(back, 0);
    }

    private LinearLayout findPage(View view) {
        View current = view;
        while (current != null) {
            ViewParent parent = current.getParent();
            if (parent instanceof ScrollView && current instanceof LinearLayout) {
                return (LinearLayout) current;
            }
            if (!(parent instanceof View)) break;
            current = (View) parent;
        }
        return null;
    }

    private TextView findText(View view, String exact) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (exact.equals(value)) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findText(group.getChildAt(i), exact);
                if (result != null) return result;
            }
        }
        return null;
    }

    private EditText findEditText(View view, String hint) {
        if (view instanceof EditText) {
            EditText field = (EditText) view;
            String value = field.getHint() == null ? "" : field.getHint().toString();
            if (hint.equals(value)) return field;
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

    private Button findButton(View view, String exact) {
        if (view instanceof Button) {
            Button button = (Button) view;
            String value = button.getText() == null ? "" : button.getText().toString();
            if (exact.equals(value)) return button;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findButton(group.getChildAt(i), exact);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Button navigationButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setSingleLine(true);
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
