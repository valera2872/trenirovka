package com.valera2872.bjjarm;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Version 0.6.4 keeps speech recognition inside the app. Tapping a microphone
 * starts dictation in the current field without opening the Google voice dialog.
 */
public class CombatPerformanceV11Activity extends CombatPerformanceV10Activity {
    private static final int REQUEST_RECORD_AUDIO = 9401;
    private static final String ROUTINE_PREFS = "combat_personal_routine";
    private static final String PROFILE_PREFS = "combat_performance_profile";

    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

    private SharedPreferences routine;
    private SharedPreferences athleteProfile;
    private SpeechRecognizer speechRecognizer;
    private EditText voiceTarget;
    private Button voiceButton;
    private String voicePrefix = "";
    private boolean listening;
    private boolean updating;

    private EditText pendingTarget;
    private Button pendingButton;
    private String pendingPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routine = getSharedPreferences(ROUTINE_PREFS, MODE_PRIVATE);
        athleteProfile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);

        View decor = getWindow().getDecorView();
        decor.post(this::upgradeVoiceInterface);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        upgradeVoiceInterface();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::upgradeVoiceInterface);
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            EditText target = pendingTarget;
            Button button = pendingButton;
            String prompt = pendingPrompt;
            pendingTarget = null;
            pendingButton = null;
            pendingPrompt = null;
            if (target != null && button != null) startInlineVoice(target, button, prompt);
        } else {
            toast("Без разрешения на микрофон голосовой ввод не работает.");
        }
    }

    private void upgradeVoiceInterface() {
        if (updating) return;
        updating = true;
        try {
            View root = getWindow().getDecorView();
            rewireExistingMicrophones(root);
            replaceRoutineCard(root);
        } finally {
            updating = false;
        }
    }

    private void rewireExistingMicrophones(View view) {
        if (view instanceof Button) {
            Button button = (Button) view;
            String text = button.getText() == null ? "" : button.getText().toString();
            if ("🎤".equals(text) && !"inline-speech".equals(button.getTag())) {
                EditText target = siblingEditText(button);
                if (target != null) {
                    button.setTag("inline-speech");
                    button.setContentDescription("Начать голосовой ввод");
                    button.setOnClickListener(v -> startInlineVoice(
                            target,
                            button,
                            target.getHint() == null ? "Голосовой ввод" : target.getHint().toString()
                    ));
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rewireExistingMicrophones(group.getChildAt(i));
            }
        }
    }

    private EditText siblingEditText(Button button) {
        ViewParent parent = button.getParent();
        if (!(parent instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) parent;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof EditText) return (EditText) group.getChildAt(i);
        }
        return null;
    }

    private void replaceRoutineCard(View root) {
        View oldCard = findTaggedView(root, "personal-routine-card");
        if (oldCard == null || "inline-personal-routine".contentEquals(oldCard.getContentDescription())) return;
        ViewParent parent = oldCard.getParent();
        if (!(parent instanceof ViewGroup)) return;

        ViewGroup page = (ViewGroup) parent;
        int index = page.indexOfChild(oldCard);
        ViewGroup.LayoutParams params = oldCard.getLayoutParams();
        page.removeView(oldCard);

        LinearLayout card = buildPersonalRoutineCard();
        card.setTag("personal-routine-card");
        card.setContentDescription("inline-personal-routine");
        if (params != null) page.addView(card, index, params);
        else page.addView(card, index);
    }

    private LinearLayout buildPersonalRoutineCard() {
        LinearLayout card = vertical(9);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(Color.WHITE, 18, 1, BORDER));
        card.setElevation(dp(1));

        card.addView(label("Моя настройка перед выходом", 18, TEXT, true));
        card.addView(label(
                "Сохрани только то, что действительно помогает именно тебе, лучше вместе с тренером или родителем.",
                14, MUTED, false));

        if (isRoutineConfigured()) {
            String first = routine.getString("first_action", "").trim();
            String prepare = routine.getString("prepare", "").trim();
            if (!first.isEmpty()) card.addView(summaryRow("Первое действие", first));
            else if (!prepare.isEmpty()) card.addView(summaryRow("Перед выходом", prepare));

            Button open = primaryButton("Открыть мой план");
            open.setOnClickListener(v -> showPersonalRoutine());
            card.addView(open);

            Button edit = secondaryButton("Изменить настройку");
            edit.setOnClickListener(v -> showRoutineEditor());
            card.addView(edit);
        } else {
            Button setup = primaryButton("Настроить мой план");
            setup.setOnClickListener(v -> showRoutineEditor());
            card.addView(setup);
        }
        return card;
    }

    private boolean isRoutineConfigured() {
        return routine.getBoolean("configured", false)
                && (!routine.getString("prepare", "").trim().isEmpty()
                || !routine.getString("first_action", "").trim().isEmpty()
                || !routine.getString("after_error", "").trim().isEmpty());
    }

    private void showRoutineEditor() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = vertical(8);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        scroll.addView(form);

        form.addView(label(
                "Нажми микрофон и говори. Текст появится в поле без отдельного окна.",
                14, MUTED, false));

        EditText problem = input("Что обычно мешает перед выходом?", routine.getString("problem", ""));
        EditText prepare = input("Что помогает собраться?", routine.getString("prepare", ""));
        EditText firstAction = input("Первое действие в схватке", routine.getString("first_action", ""));
        EditText afterError = input("Что делать сразу после ошибки?", routine.getString("after_error", ""));

        form.addView(voiceRow(problem));
        form.addView(voiceRow(prepare));
        form.addView(voiceRow(firstAction));
        form.addView(voiceRow(afterError));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Личная настройка")
                .setView(scroll)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();

        dialog.setOnDismissListener(ignored -> stopAndResetVoice());
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String prepareText = prepare.getText().toString().trim();
                    String firstText = firstAction.getText().toString().trim();
                    String errorText = afterError.getText().toString().trim();
                    if (prepareText.isEmpty() && firstText.isEmpty() && errorText.isEmpty()) {
                        toast("Запиши хотя бы одно действие, которое действительно помогает.");
                        return;
                    }
                    routine.edit()
                            .putBoolean("configured", true)
                            .putString("problem", problem.getText().toString().trim())
                            .putString("prepare", prepareText)
                            .putString("first_action", firstText)
                            .putString("after_error", errorText)
                            .apply();
                    dialog.dismiss();
                    recreate();
                }));
        dialog.show();
    }

    private LinearLayout voiceRow(EditText field) {
        LinearLayout row = horizontal(8);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button mic = miniButton("🎤");
        mic.setTag("inline-speech");
        mic.setContentDescription("Начать голосовой ввод");
        mic.setOnClickListener(v -> startInlineVoice(
                field,
                mic,
                field.getHint() == null ? "Голосовой ввод" : field.getHint().toString()
        ));
        row.addView(mic, new LinearLayout.LayoutParams(dp(54), dp(52)));
        return row;
    }

    private void startInlineVoice(EditText target, Button button, String prompt) {
        if (listening) {
            if (voiceTarget == target) {
                if (speechRecognizer != null) speechRecognizer.stopListening();
                if (voiceButton != null) voiceButton.setText("…");
                return;
            }
            stopAndResetVoice();
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingTarget = target;
            pendingButton = button;
            pendingPrompt = prompt;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("На телефоне не найден сервис распознавания речи.");
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new InlineRecognitionListener());
        }

        voiceTarget = target;
        voiceButton = button;
        voicePrefix = target.getText().toString().trim();
        listening = true;
        button.setText("●");
        button.setContentDescription("Остановить голосовой ввод");
        target.requestFocus();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        speechRecognizer.startListening(intent);
    }

    private void applyRecognition(Bundle results) {
        if (voiceTarget == null || results == null) return;
        ArrayList<String> values = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (values == null || values.isEmpty()) return;
        String spoken = values.get(0).trim();
        if (spoken.isEmpty()) return;
        String combined = voicePrefix.isEmpty() ? spoken : voicePrefix + " " + spoken;
        voiceTarget.setText(combined);
        voiceTarget.setSelection(combined.length());
    }

    private void stopAndResetVoice() {
        if (speechRecognizer != null && listening) speechRecognizer.cancel();
        listening = false;
        if (voiceButton != null) {
            voiceButton.setText("🎤");
            voiceButton.setContentDescription("Начать голосовой ввод");
        }
        voiceTarget = null;
        voiceButton = null;
        voicePrefix = "";
    }

    private final class InlineRecognitionListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) {
            if (voiceButton != null) voiceButton.setText("●");
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (voiceButton != null) voiceButton.setText("…");
        }
        @Override public void onError(int error) {
            boolean silent = error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    || error == SpeechRecognizer.ERROR_CLIENT;
            stopAndResetVoice();
            if (!silent) toast("Не удалось распознать речь. Нажми микрофон ещё раз.");
        }
        @Override public void onResults(Bundle results) {
            applyRecognition(results);
            listening = false;
            if (voiceButton != null) {
                voiceButton.setText("🎤");
                voiceButton.setContentDescription("Начать голосовой ввод");
            }
            voiceTarget = null;
            voiceButton = null;
            voicePrefix = "";
        }
        @Override public void onPartialResults(Bundle partialResults) {
            applyRecognition(partialResults);
        }
        @Override public void onEvent(int eventType, Bundle params) { }
    }

    private void showPersonalRoutine() {
        if (!isRoutineConfigured()) {
            showRoutineEditor();
            return;
        }
        LinearLayout content = vertical(8);
        content.setPadding(dp(18), dp(6), dp(18), dp(6));
        addPlanItem(content, "Что обычно мешает", routine.getString("problem", ""));
        addPlanItem(content, "Перед выходом", routine.getString("prepare", ""));
        addPlanItem(content, "Первое действие", routine.getString("first_action", ""));
        addPlanItem(content, "После ошибки", routine.getString("after_error", ""));
        addPlanItem(content, "Техника, которую закрепляешь", athleteProfile.getString("mission", ""));

        new AlertDialog.Builder(this)
                .setTitle("Твой план перед выходом")
                .setView(content)
                .setNegativeButton("Изменить", (dialog, which) -> showRoutineEditor())
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void addPlanItem(LinearLayout parent, String title, String value) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) parent.addView(summaryRow(title, clean));
    }

    private LinearLayout summaryRow(String title, String value) {
        LinearLayout row = vertical(2);
        row.setPadding(0, dp(6), 0, dp(6));
        row.addView(label(title, 12, PRIMARY, true));
        row.addView(label(value, 15, TEXT, false));
        return row;
    }

    private EditText input(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setMinLines(2);
        field.setMaxLines(4);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 147, 143));
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
        return field;
    }

    private View findTaggedView(View view, String tag) {
        if (tag.equals(view.getTag())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTaggedView(group.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
    }

    private LinearLayout vertical(int spacing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        if (spacing > 0) layout.setDividerDrawable(new SpacerDrawable(dp(spacing)));
        return layout;
    }

    private LinearLayout horizontal(int spacing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        if (spacing > 0) layout.setDividerDrawable(new SpacerDrawable(dp(spacing)));
        return layout;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.13f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
    }

    private Button miniButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 12, 46);
    }

    private Button styledButton(String value, int textColor, int backgroundColor, int textSize, int height) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setTextSize(textSize);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, textSize, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setMinHeight(dp(height));
        button.setBackground(rounded(backgroundColor, 13, 0, Color.TRANSPARENT));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawable(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
