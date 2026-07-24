package com.valera2872.bjjarm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;

/** Personal pre-match routine in the shared premium visual system. */
public class PremiumRoutineActivity extends Activity {
    private static final String ROUTINE_PREFS = "combat_personal_routine";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int REQUEST_AUDIO = 9401;

    private SharedPreferences routine;
    private SharedPreferences profile;
    private SpeechRecognizer recognizer;
    private EditText voiceTarget;
    private Button voiceButton;
    private String voiceLanguage = "ru-RU";
    private String voicePrefix = "";
    private boolean listening;
    private EditText pendingTarget;
    private Button pendingButton;
    private String pendingLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routine = getSharedPreferences(ROUTINE_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        PremiumUi.applyWindow(this);
        showHome();
    }

    @Override
    protected void onPause() {
        stopVoice();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) { }
            recognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_AUDIO) return;
        EditText target = pendingTarget;
        Button button = pendingButton;
        String language = pendingLanguage;
        pendingTarget = null;
        pendingButton = null;
        pendingLanguage = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (target != null && button != null) startVoice(target, button, language);
        } else {
            toast("Без разрешения на микрофон голосовой ввод не работает.");
        }
    }

    private boolean configured() {
        return routine.getBoolean("configured", false)
                && (!routine.getString("prepare", "").trim().isEmpty()
                || !routine.getString("first_action", "").trim().isEmpty()
                || !routine.getString("after_error", "").trim().isEmpty());
    }

    private void showHome() {
        if (configured()) showOverview();
        else showEditor();
    }

    private void showOverview() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);
        Button back = PremiumUi.outlineButton(this, "← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Перед выходом"));
        page.addView(PremiumUi.title(this, "Твой короткий план"));
        page.addView(PremiumUi.body(this,
                "Здесь только твои собственные формулировки — лучше согласованные с тренером или родителем."));

        LinearLayout hero = PremiumUi.hero(this);
        page.addView(hero);
        hero.addView(PremiumUi.heroEyebrow(this, "Первое действие"));
        String first = routine.getString("first_action", "").trim();
        String prepare = routine.getString("prepare", "").trim();
        hero.addView(PremiumUi.heroTitle(this,
                !first.isEmpty() ? first : (!prepare.isEmpty() ? prepare : "План ещё не заполнен")));
        String mission = profile.getString("mission", "").trim();
        if (!mission.isEmpty()) hero.addView(PremiumUi.heroBody(this, "Фокус на 30 дней: " + mission));

        LinearLayout details = PremiumUi.card(this);
        addItem(details, "Что обычно мешает", routine.getString("problem", ""));
        addItem(details, "Что помогает собраться", routine.getString("prepare", ""));
        addItem(details, "Первое действие", routine.getString("first_action", ""));
        addItem(details, "После ошибки", routine.getString("after_error", ""));
        page.addView(details);

        Button edit = PremiumUi.primaryButton(this, "Изменить план");
        edit.setOnClickListener(v -> showEditor());
        page.addView(edit);
        setContentView(scroll);
    }

    private void showEditor() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);
        Button back = PremiumUi.outlineButton(this, configured() ? "← К плану" : "← На главный экран");
        back.setOnClickListener(v -> { if (configured()) showOverview(); else finish(); });
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Личная настройка"));
        page.addView(PremiumUi.title(this, "Что помогает именно тебе"));
        page.addView(PremiumUi.body(this,
                "Не ищи красивую фразу. Запиши конкретные слова и действия, которые реально можно выполнить перед схваткой."));

        LinearLayout form = PremiumUi.card(this);
        page.addView(form);
        EditText problem = PremiumUi.multiline(this, "Например: спешу и забываю про стойку",
                routine.getString("problem", ""));
        EditText prepare = PremiumUi.multiline(this, "Например: два спокойных выдоха и взгляд на центр ковра",
                routine.getString("prepare", ""));
        EditText first = PremiumUi.multiline(this, "Например: занять центр и первым взять рабочий захват",
                routine.getString("first_action", ""));
        EditText afterError = PremiumUi.multiline(this, "Например: поставить рамы, вернуть стойку и начать заново",
                routine.getString("after_error", ""));
        form.addView(PremiumUi.accentText(this, "Что обычно мешает"));
        form.addView(voiceRow(problem));
        form.addView(PremiumUi.accentText(this, "Что помогает собраться"));
        form.addView(voiceRow(prepare));
        form.addView(PremiumUi.accentText(this, "Первое действие"));
        form.addView(voiceRow(first));
        form.addView(PremiumUi.accentText(this, "Что делать после ошибки"));
        form.addView(voiceRow(afterError));

        Button save = PremiumUi.primaryButton(this, "Сохранить");
        save.setOnClickListener(v -> {
            String prepareText = text(prepare);
            String firstText = text(first);
            String errorText = text(afterError);
            if (prepareText.isEmpty() && firstText.isEmpty() && errorText.isEmpty()) {
                toast("Запиши хотя бы одно действие, которое действительно помогает.");
                return;
            }
            save.setEnabled(false);
            save.setText("Сохраняю…");
            boolean saved;
            try {
                saved = routine.edit()
                        .putBoolean("configured", true)
                        .putString("problem", text(problem))
                        .putString("prepare", prepareText)
                        .putString("first_action", firstText)
                        .putString("after_error", errorText)
                        .commit();
            } catch (RuntimeException error) {
                saved = false;
            }
            if (!saved) {
                save.setEnabled(true);
                save.setText("Сохранить");
                toast("Не удалось сохранить план.");
                return;
            }
            toast("Личный план сохранён.");
            showOverview();
        });
        page.addView(save);
        setContentView(scroll);
    }

    private void addItem(LinearLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) return;
        parent.addView(PremiumUi.accentText(this, title));
        parent.addView(PremiumUi.bodyDark(this, value.trim()));
    }

    private LinearLayout voiceRow(EditText field) {
        LinearLayout row = PremiumUi.horizontal(this, 7);
        row.setGravity(Gravity.TOP);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout controls = PremiumUi.vertical(this, 7);
        Button ru = PremiumUi.secondaryButton(this, "RU");
        Button en = PremiumUi.outlineButton(this, "EN");
        ru.setOnClickListener(v -> startVoice(field, ru, "ru-RU"));
        en.setOnClickListener(v -> startVoice(field, en, "en-US"));
        controls.addView(ru, new LinearLayout.LayoutParams(PremiumUi.dp(this, 58), PremiumUi.dp(this, 52)));
        controls.addView(en, new LinearLayout.LayoutParams(PremiumUi.dp(this, 58), PremiumUi.dp(this, 52)));
        row.addView(controls);
        return row;
    }

    private void startVoice(EditText target, Button button, String language) {
        String safeLanguage = language == null ? "ru-RU" : language;
        if (listening) stopVoice();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingTarget = target;
            pendingButton = button;
            pendingLanguage = safeLanguage;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("На телефоне не найден сервис распознавания речи.");
            return;
        }
        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this);
                recognizer.setRecognitionListener(new VoiceListener());
            }
            voiceTarget = target;
            voiceButton = button;
            voiceLanguage = safeLanguage;
            voicePrefix = text(target);
            listening = true;
            button.setText("●");
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, safeLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, safeLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            recognizer.startListening(intent);
        } catch (RuntimeException error) {
            stopVoice();
            toast("Голосовой ввод не запустился. Текст можно написать вручную.");
        }
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

    private void stopVoice() {
        if (recognizer != null && listening) {
            try { recognizer.cancel(); } catch (RuntimeException ignored) { }
        }
        listening = false;
        if (voiceButton != null) voiceButton.setText("en-US".equals(voiceLanguage) ? "EN" : "RU");
        voiceTarget = null;
        voiceButton = null;
        voicePrefix = "";
        voiceLanguage = "ru-RU";
    }

    private final class VoiceListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { if (voiceButton != null) voiceButton.setText("●"); }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() { if (voiceButton != null) voiceButton.setText("…"); }
        @Override public void onError(int error) {
            boolean silent = error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    || error == SpeechRecognizer.ERROR_CLIENT;
            stopVoice();
            if (!silent) toast("Не удалось распознать речь. Текст можно поправить вручную.");
        }
        @Override public void onResults(Bundle results) { applyRecognition(results); stopVoice(); }
        @Override public void onPartialResults(Bundle partialResults) { applyRecognition(partialResults); }
        @Override public void onEvent(int eventType, Bundle params) { }
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
