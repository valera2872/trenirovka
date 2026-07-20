package com.valera2872.bjjarm;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Clear progress wording, one-line controls and explicit return navigation. */
public class GrapplingV4Activity extends GrapplingV3Activity {
    private static final int PRIMARY = Color.rgb(32, 104, 93);
    private static final int PRIMARY_SOFT = Color.rgb(222, 239, 234);
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
            addTopNavigation(root);
        } finally {
            polishing = false;
        }
    }

    private void rewriteAndFit(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if ("Смотрим не только на сантиметры".equals(value)) {
                text.setText("Как меняются сила и нагрузка");
            } else if (value.startsWith("Главные признаки прогресса:")) {
                text.setText("Здесь видны выполненные тренировки, рабочие веса, стабильность подходов, замеры и отметки боли.");
            }
        }
        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAllCaps(false);
            button.setSingleLine(true);
            button.setMaxLines(1);
            button.setEllipsize(TextUtils.TruncateAt.END);
            button.setIncludeFontPadding(false);
            button.setAutoSizeTextTypeUniformWithConfiguration(
                    9, 15, 1, TypedValue.COMPLEX_UNIT_SP);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rewriteAndFit(group.getChildAt(i));
            }
        }
    }

    private void addTopNavigation(View root) {
        ScrollView scroll = findScrollView(root);
        if (scroll == null || scroll.getChildCount() == 0
                || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout page = (LinearLayout) scroll.getChildAt(0);
        boolean workout = isWorkoutScreen(root);
        String label = workout ? "← К обзору модуля" : "← Combat Performance";

        Button existing = findTaggedButton(page, "strength-top-back");
        if (existing != null) {
            existing.setText(label);
            return;
        }

        Button back = navigationButton(label);
        back.setTag("strength-top-back");
        back.setOnClickListener(v -> {
            if (isWorkoutScreen(getWindow().getDecorView())) {
                confirmReturnToOverview();
            } else {
                finish();
            }
        });
        page.addView(back, 0);
    }

    @Override
    public void onBackPressed() {
        if (isWorkoutScreen(getWindow().getDecorView())) {
            confirmReturnToOverview();
        } else {
            finish();
        }
    }

    private void confirmReturnToOverview() {
        new AlertDialog.Builder(this)
                .setTitle("Вернуться к обзору?")
                .setMessage("Отмеченные, но не завершённые подходы не сохранятся.")
                .setNegativeButton("Остаться", null)
                .setPositiveButton("Вернуться", (dialog, which) -> recreate())
                .show();
    }

    private boolean isWorkoutScreen(View root) {
        return findExactText(root, "РАБОЧАЯ ТРЕНИРОВКА") != null
                || findExactText(root, "ОБЛЕГЧЁННАЯ ТРЕНИРОВКА") != null;
    }

    private ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView result = findScrollView(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private TextView findExactText(View view, String exact) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (exact.equals(value)) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findExactText(group.getChildAt(i), exact);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Button findTaggedButton(View view, String tag) {
        if (view instanceof Button && tag.equals(view.getTag())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findTaggedButton(group.getChildAt(i), tag);
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
