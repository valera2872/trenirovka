package com.valera2872.bjjarm;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Version 0.5 enhancements layered over the working platform prototype:
 * voice entry for personal techniques and the 30-day mission, plus access to
 * the second physical module for legs and trunk.
 */
public class CombatPerformanceV5Activity extends CombatPerformanceActivity {
    private static final int REQUEST_FAVORITE_VOICE = 8101;
    private static final int REQUEST_MISSION_VOICE = 8102;
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private boolean enhancing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::installEnhancements);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        installEnhancements();
                    }
                }
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) installEnhancements();
    }

    private void installEnhancements() {
        if (enhancing) return;
        enhancing = true;
        try {
            View root = getWindow().getDecorView();
            installVoiceButtons(root);
            installStrengthModuleAccess(root);
            updatePrototypeCopy(root);
        } finally {
            enhancing = false;
        }
    }

    private void installVoiceButtons(View view) {
        if (view instanceof EditText) {
            EditText field = (EditText) view;
            String hint = field.getHint() == null ? "" : field.getHint().toString();
            if ("Любимые позиции и техники".equals(hint)) {
                enableVoice(field, REQUEST_FAVORITE_VOICE,
                        "Назови любимые позиции и техники");
            } else if ("Одна техническая миссия на 30 дней".equals(hint)) {
                enableVoice(field, REQUEST_MISSION_VOICE,
                        "Продиктуй техническую задачу на ближайшие 30 дней");
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                installVoiceButtons(group.getChildAt(i));
            }
        }
    }

    private void enableVoice(EditText field, int requestCode, String prompt) {
        if ("voice-enabled".equals(field.getTag())) return;
        field.setTag("voice-enabled");
        field.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_btn_speak_now, 0);
        field.setCompoundDrawablePadding(dp(10));
        field.setPadding(dp(13), dp(11), dp(10), dp(11));
        field.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            int drawableWidth = field.getCompoundDrawables()[2] == null
                    ? 0 : field.getCompoundDrawables()[2].getBounds().width();
            boolean touchedMic = event.getX() >= field.getWidth()
                    - field.getPaddingRight() - drawableWidth - dp(12);
            if (!touchedMic) return false;
            launchVoiceInput(requestCode, prompt);
            return true;
        });
    }

    private void launchVoiceInput(int requestCode, String prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this,
                    "На телефоне не найден сервис распознавания речи. Поле можно заполнить с клавиатуры.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode != REQUEST_FAVORITE_VOICE && requestCode != REQUEST_MISSION_VOICE) return;

        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String hint = requestCode == REQUEST_FAVORITE_VOICE
                ? "Любимые позиции и техники"
                : "Одна техническая миссия на 30 дней";
        EditText target = findEditText(getWindow().getDecorView(), hint);
        if (target == null) return;

        String spoken = results.get(0).trim();
        String current = target.getText().toString().trim();
        if (current.isEmpty()) {
            target.setText(spoken);
        } else {
            target.setText(current + (requestCode == REQUEST_FAVORITE_VOICE ? ", " : " ") + spoken);
        }
        target.setSelection(target.getText().length());
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

    private void installStrengthModuleAccess(View root) {
        Button armsButton = findButton(root, "Открыть модуль «Сильные руки и контроль»");
        if (armsButton != null && armsButton.getParent() instanceof LinearLayout) {
            LinearLayout parent = (LinearLayout) armsButton.getParent();
            if (findButton(parent, "Открыть модуль «Ноги и корпус»") == null) {
                Button base = secondaryButton("Открыть модуль «Ноги и корпус»");
                base.setOnClickListener(v -> openBaseStrength());
                parent.addView(space(8));
                parent.addView(base);
            }
        }

        Button today = findButton(root, "Открыть силовую тренировку");
        if (today != null && !"strength-choice".equals(today.getTag())) {
            today.setTag("strength-choice");
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
                        openBaseStrength();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void openBaseStrength() {
        startActivity(new Intent(this, BaseStrengthActivity.class));
    }

    private void updatePrototypeCopy(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (value.startsWith("Первый доступный модуль")) {
                text.setText("Доступны два физических модуля: руки и контроль либо базовая сила ног и корпуса. Перед началом приложение проверит восстановление.");
            } else if (value.startsWith("Профиль сохраняется, техническая миссия считается")) {
                text.setText("Профиль сохраняется, техническая миссия считается, голосовой ввод помогает быстро записать личную игру, а два силовых модуля ведут собственную прогрессию нагрузки.");
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updatePrototypeCopy(group.getChildAt(i));
            }
        }
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button) {
            Button button = (Button) view;
            if (text.equals(button.getText() == null ? "" : button.getText().toString())) return button;
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

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(PRIMARY);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setMinHeight(dp(48));
        button.setBackground(rounded(PRIMARY_SOFT, 13));
        return button;
    }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private View space(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
