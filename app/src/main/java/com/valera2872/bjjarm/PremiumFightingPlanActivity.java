package com.valera2872.bjjarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/** Clear premium replacement for the abstract “fighting system / chain” wording. */
public class PremiumFightingPlanActivity extends Activity {
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String[] STEP_KEYS = {"step_0", "step_1", "step_2", "step_3", "step_4"};
    private static final int REQUEST_AUDIO = 9201;

    private SharedPreferences system;
    private SharedPreferences profile;
    private String activeSport;
    private String prefix;

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
        system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        PremiumUi.applyWindow(this);
        resolveSport();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String before = activeSport;
        resolveSport();
        if (before != null && !before.equals(activeSport)) showHome();
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

    private void resolveSport() {
        ArrayList<String> sports = profileSports();
        String selected = profile.getString("active_sport", "").trim();
        if (selected.isEmpty() || !sports.contains(selected)) selected = sports.get(0);
        activeSport = selected;
        prefix = SportGuidance.slug(activeSport) + "_";
    }

    private void showHome() {
        if (system.getBoolean(key("configured"), false)) showOverview();
        else showIntro();
    }

    private void showIntro() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Мой план борьбы"));
        page.addView(PremiumUi.title(this, "Как ты хочешь провести схватку"));
        page.addView(PremiumUi.body(this,
                "Это не готовая программа и не база приёмов. Ты сам описываешь свой рабочий маршрут: от первого контакта до запасного решения."));

        LinearLayout explanation = PremiumUi.softCard(this);
        page.addView(explanation);
        explanation.addView(PremiumUi.sectionTitle(this, "Зачем это нужно"));
        explanation.addView(PremiumUi.bodyDark(this,
                "Перед тренировкой приложение напомнит один выбранный этап. После тренировки дневник поможет записать, где план сработал и где остановился."));

        LinearLayout steps = PremiumUi.card(this);
        page.addView(steps);
        steps.addView(PremiumUi.cardTitle(this, "Пять вопросов к своей борьбе"));
        String[] labels = SportGuidance.stepLabels(activeSport);
        String[] hints = SportGuidance.hints(activeSport);
        for (int i = 0; i < labels.length; i++) {
            steps.addView(stepPreview(i + 1, labels[i], hints[i].replace("Например: ", "")));
        }

        LinearLayout sportCard = PremiumUi.card(this);
        sportCard.addView(PremiumUi.cardTitle(this, "Текущая дисциплина"));
        sportCard.addView(PremiumUi.chip(this, activeSport));
        sportCard.addView(PremiumUi.small(this, SportGuidance.description(activeSport)));
        Button changeSport = PremiumUi.secondaryButton(this, "Изменить дисциплину");
        changeSport.setOnClickListener(v -> startActivity(new Intent(this, SportFocusActivity.class)));
        sportCard.addView(changeSport);
        page.addView(sportCard);

        Button create = PremiumUi.primaryButton(this, "Настроить мой план");
        create.setOnClickListener(v -> showEditor());
        page.addView(create);
        setContentView(scroll);
    }

    private void showOverview() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Мой план борьбы"));
        page.addView(PremiumUi.title(this, activeSport));
        page.addView(PremiumUi.body(this,
                "Твой рабочий маршрут по схватке. Он не оценивает тебя и не заменяет тренера — только удерживает выбранную логику действий."));

        LinearLayout plan = PremiumUi.card(this);
        page.addView(plan);
        plan.addView(PremiumUi.cardTitle(this, "План по этапам"));
        String[] labels = SportGuidance.stepLabels(activeSport);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            String value = system.getString(key(STEP_KEYS[i]), "").trim();
            if (!value.isEmpty()) plan.addView(savedStep(i + 1, labels[i], value));
        }

        LinearLayout missionCard = PremiumUi.hero(this);
        page.addView(missionCard);
        missionCard.addView(PremiumUi.heroEyebrow(this, "Фокус на 30 дней"));
        String mission = profile.getString("mission", "").trim();
        missionCard.addView(PremiumUi.heroTitle(this,
                mission.isEmpty() ? "Фокус ещё не выбран" : mission));
        missionCard.addView(PremiumUi.heroBody(this,
                "Можно выбрать любой заполненный этап плана и отслеживать его попытки в дневнике."));
        Button choose = PremiumUi.lightButton(this, "Выбрать этап");
        choose.setOnClickListener(v -> showFocusDialog());
        missionCard.addView(choose);

        Button edit = PremiumUi.primaryButton(this, "Изменить план");
        edit.setOnClickListener(v -> showEditor());
        page.addView(edit);
        Button sport = PremiumUi.secondaryButton(this, "Переключить дисциплину");
        sport.setOnClickListener(v -> startActivity(new Intent(this, SportFocusActivity.class)));
        page.addView(sport);
        setContentView(scroll);
    }

    private void showEditor() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← К плану");
        back.setOnClickListener(v -> showHome());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Настройка плана"));
        page.addView(PremiumUi.title(this, activeSport));
        page.addView(PremiumUi.body(this,
                "Пиши простыми словами, как объяснил бы тренеру. Не нужно знать официальные названия всех действий."));

        LinearLayout form = PremiumUi.card(this);
        page.addView(form);
        String[] labels = SportGuidance.stepLabels(activeSport);
        String[] hints = SportGuidance.hints(activeSport);
        EditText[] fields = new EditText[STEP_KEYS.length];
        for (int i = 0; i < STEP_KEYS.length; i++) {
            form.addView(PremiumUi.accentText(this, (i + 1) + ". " + labels[i]));
            EditText field = PremiumUi.multiline(this, hints[i], valueOrDraft(STEP_KEYS[i]));
            bindDraft(field, "draft_" + STEP_KEYS[i]);
            fields[i] = field;
            form.addView(voiceRow(field));
        }

        LinearLayout help = PremiumUi.softCard(this);
        help.addView(PremiumUi.sectionTitle(this, "Можно заполнить постепенно"));
        help.addView(PremiumUi.bodyDark(this,
                "Для сохранения достаточно первого этапа и основного действия. Остальные части можно добавить после нескольких тренировок."));
        page.addView(help);

        Button save = PremiumUi.primaryButton(this, "Сохранить план");
        save.setOnClickListener(v -> savePlan(save, fields));
        page.addView(save);
        setContentView(scroll);
    }

    private void savePlan(Button saveButton, EditText[] fields) {
        String first = text(fields[0]);
        String main = text(fields[2]);
        if (first.isEmpty()) {
            toast("Заполни, откуда начинается твой план.");
            fields[0].requestFocus();
            return;
        }
        if (main.isEmpty()) {
            toast("Заполни главное действие или атаку.");
            fields[2].requestFocus();
            return;
        }
        saveButton.setEnabled(false);
        saveButton.setText("Сохраняю…");
        stopVoice();

        SharedPreferences.Editor editor = system.edit().putBoolean(key("configured"), true);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            editor.putString(key(STEP_KEYS[i]), text(fields[i]))
                    .remove(key("draft_" + STEP_KEYS[i]));
        }
        boolean saved;
        try { saved = editor.commit(); }
        catch (RuntimeException error) { saved = false; }
        if (!saved) {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить план");
            toast("Не удалось сохранить план. Текст остался на экране.");
            return;
        }
        toast("План сохранён.");
        showOverview();
    }

    private LinearLayout stepPreview(int number, String title, String example) {
        LinearLayout row = PremiumUi.horizontal(this, 11);
        row.setGravity(Gravity.TOP);
        row.addView(PremiumUi.numberBadge(this, String.valueOf(number)),
                new LinearLayout.LayoutParams(PremiumUi.dp(this, 36), PremiumUi.dp(this, 36)));
        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.sectionTitle(this, title));
        copy.addView(PremiumUi.small(this, example));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private LinearLayout savedStep(int number, String title, String value) {
        LinearLayout row = PremiumUi.horizontal(this, 12);
        row.setGravity(Gravity.TOP);
        row.addView(PremiumUi.numberBadge(this, String.valueOf(number)),
                new LinearLayout.LayoutParams(PremiumUi.dp(this, 36), PremiumUi.dp(this, 36)));
        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.accentText(this, title));
        copy.addView(PremiumUi.bodyDark(this, value));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private void showFocusDialog() {
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();
        String[] labels = SportGuidance.stepLabels(activeSport);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            String value = system.getString(key(STEP_KEYS[i]), "").trim();
            if (!value.isEmpty()) {
                indexes.add(i);
                items.add(labels[i] + "\n" + value);
            }
        }
        if (items.isEmpty()) {
            toast("Сначала сохрани хотя бы один этап плана.");
            return;
        }
        final int[] selected = {0};
        new AlertDialog.Builder(this)
                .setTitle("Что отрабатывать 30 дней?")
                .setSingleChoiceItems(items.toArray(new String[0]), 0,
                        (dialog, which) -> selected[0] = which)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Выбрать", (dialog, which) -> {
                    int index = indexes.get(selected[0]);
                    confirmMissionChange(system.getString(key(STEP_KEYS[index]), "").trim());
                })
                .show();
    }

    private void confirmMissionChange(String value) {
        if (value.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Сменить фокус на 30 дней?")
                .setMessage("Новый фокус:\n\n" + value
                        + "\n\nСчётчики попыток, успешных применений и финишей начнутся заново.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сменить", (dialog, which) -> {
                    boolean saved = profile.edit()
                            .putString("mission", value)
                            .putLong("mission_started_at", System.currentTimeMillis())
                            .putInt("mission_active_days", 0)
                            .putInt("mission_attempts", 0)
                            .putInt("mission_successes", 0)
                            .putInt("mission_finishes", 0)
                            .remove("mission_last_day")
                            .commit();
                    if (!saved) toast("Не удалось изменить фокус.");
                    else {
                        toast("Новый фокус сохранён.");
                        showOverview();
                    }
                })
                .show();
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

    private void bindDraft(EditText field, String suffix) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                system.edit().putString(key(suffix), s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String valueOrDraft(String suffix) {
        String saved = system.getString(key(suffix), "").trim();
        return saved.isEmpty() ? system.getString(key("draft_" + suffix), "") : saved;
    }

    private String key(String suffix) {
        return prefix + suffix;
    }

    private ArrayList<String> profileSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String part : saved.split("\\|")) if (!part.trim().isEmpty()) result.add(part.trim());
        }
        if (result.isEmpty()) result.add(profile.getString("sport", "Грэпплинг / No-Gi"));
        return result;
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
