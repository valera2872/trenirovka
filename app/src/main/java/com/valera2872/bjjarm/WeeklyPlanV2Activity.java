package com.valera2872.bjjarm;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

/** One-line buttons and corrected links for the weekly-plan screen. */
public class WeeklyPlanV2Activity extends WeeklyPlanActivity {
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
            polishTree(getWindow().getDecorView());
        } finally {
            polishing = false;
        }
    }

    private void polishTree(View view) {
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

            if ("Начать тренировку рук".equals(value)) {
                button.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV4Activity.class)));
            } else if ("Начать тренировку ног".equals(value)) {
                button.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                polishTree(group.getChildAt(i));
            }
        }
    }
}
