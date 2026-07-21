package com.valera2872.bjjarm;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

/** Clear terminology and short controls for the weekly plan. */
public class WeeklyPlanV3Activity extends WeeklyPlanV2Activity {
    private boolean rewriting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::rewriteInterface);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rewriteInterface();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::rewriteInterface);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) rewriteInterface();
    }

    private void rewriteInterface() {
        if (rewriting) return;
        rewriting = true;
        try {
            rewriteTree(getWindow().getDecorView());
        } finally {
            rewriting = false;
        }
    }

    private void rewriteTree(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            String value = textView.getText() == null ? "" : textView.getText().toString();
            String changed = rewrite(value);
            if (!changed.equals(value)) textView.setText(changed);
        }

        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAllCaps(false);
            button.setSingleLine(true);
            button.setMaxLines(1);
            button.setEllipsize(null);
            button.setIncludeFontPadding(false);
            button.setAutoSizeTextTypeUniformWithConfiguration(
                    10, 15, 1, TypedValue.COMPLEX_UNIT_SP);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rewriteTree(group.getChildAt(i));
            }
        }
    }

    private String rewrite(String source) {
        if ("Техническая миссия".equals(source)) return "Техника на 30 дней";
        if ("Попыток технической миссии".equals(source)) return "Попыток выбранной техники";
        if ("Тренировка на ковре + техническая миссия".equals(source)) {
            return "Тренировка на ковре + выбранная техника";
        }
        if ("Открыть техническую миссию".equals(source)) return "Отметить технику";
        if ("Начать тренировку рук".equals(source)) return "Руки и хват";
        if ("Начать тренировку ног".equals(source)) return "Ноги и корпус";
        if ("Открыть восстановление".equals(source)) return "План восстановления";
        if ("Открыть план соревнования".equals(source)) return "План соревнования";
        if ("Получилось".equals(source)) return "Удалось";

        String result = source;
        result = result.replace("техническую миссию", "выбранную технику");
        result = result.replace("Техническую миссию", "Выбранную технику");
        result = result.replace("технической миссии", "выбранной техники");
        result = result.replace("Технической миссии", "Выбранной техники");
        result = result.replace("техническая миссия", "выбранная техника");
        result = result.replace("Техническая миссия", "Техника на 30 дней");
        return result;
    }
}
