package com.valera2872.bjjarm;

import android.Manifest;
import android.app.Activity;
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
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 0.7.0: one practical fighting chain for the athlete.
 * The module stores only the athlete's own plan and does not invent techniques.
 */
public class FightingSystemActivity extends Activity {
    private static final String SYSTEM_PREFS = "combat_fighting_system";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int REQUEST_AUDIO = 7001;

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

    private static final String[] STEP_LABELS = {
            "Стартовая позиция",
            "Вход в нужную позицию",
            "Основное продолжение",
            "Запасной вариант",
            "Возврат после потери позиции"
    };

    private static final String[] STEP_KEYS = {
            "start_position",
            "entry",
            "main_action",
            "backup_action",
            "recovery"
    };

    private SharedPreferences system;
    private SharedPreferences profile;
    private SpeechRecognizer recognizer;
    private EditText voiceTarget;
    private Button voiceButton;
    private String voicePrefix = "";
    private String voiceLanguage = "ru-RU";
    private boolean listening;
    private EditText pendingTarget;
    private Button pendingButton;
    private String pendingLanguage;
    private boolean normalizing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (system.getBoolean("configured", false)) showOverview();
        else showIntro();
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

    private void showIntro() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label("Собери одну рабочую цепочку", 29, TEXT, true));
        page.addView(label(
                "Не нужно описывать всю свою борьбу. Начни с одной ситуации, в которую ты часто попадаешь или хочешь научиться попадать.",
                15, MUTED, false));

        LinearLayout explanation = card();
        page.addView(explanation);
        explanation.addView(infoRow("1. Откуда начинаешь", "Например: закрытый гард, стойка, контроль сбоку."));
        explanation.addView(infoRow("2. Как входишь", "Как получаешь нужный захват или позицию."));
        explanation.addView(infoRow("3. Что делаешь дальше", "Основное продолжение, которое хочешь сделать своим рабочим."));
        explanation.addView(infoRow("4. Что делаешь при защите", "Запасной вариант, если соперник остановил основной ход."));
        explanation.addView(infoRow("5. Как возвращаешься", "План после потери позиции или собственной ошибки."));

        Button create = primaryButton("Собрать цепочку");
        create.setOnClickListener(v -> showEditor());
        page.addView(create);
        setContentView(scroll);
    }

    private void showOverview() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label(system.getString("sport", "Борьба"), 28, TEXT, true));
        page.addView(label(
                "Одна рабочая цепочка. Её можно уточнять вместе с тренером после тренировок и соревнований.",
                14, MUTED, false));

        LinearLayout chain = card();
        page.addView(chain);
        chain.addView(sectionTitle("Рабочая цепочка"));
        for (int i = 0; i < STEP_KEYS.length; i++) {
            String value = system.getString(STEP_KEYS[i], "").trim();
            if (!value.isEmpty()) chain.addView(stepRow(i + 1, STEP_LABELS[i], value));
        }

        String compact = compactChain();
        if (!compact.isEmpty()) {
            LinearLayout preview = heroCard();
            page.addView(preview);
            preview.addView(label("КРАТКО", 12, Color.rgb(217, 239, 233), true));
            preview.addView(label(compact, 19, Color.WHITE, true));
        }

        LinearLayout current = card();
        page.addView(current);
        current.addView(sectionTitle("Что сейчас отрабатываешь"));
        current.addView(label(profile.getString("mission", "Цель ещё не выбрана"), 17, TEXT, true));
        current.addView(label(
                "Приложение считает попытки, успешные применения и финиши именно для этой текущей цели.",
                13, MUTED, false));
        Button choose = primaryButton("Выбрать этап на 30 дней");
        choose.setOnClickListener(v -> showFocusDialog());
        current.addView(choose);

        Button edit = secondaryButton("Изменить цепочку");
        edit.setOnClickListener(v -> showEditor());
        page.addView(edit);
        setContentView(scroll);
    }

    private void showEditor() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label("Опиши одну рабочую цепочку", 27, TEXT, true));
        page.addView(label(
                "Пиши так, как объяснил бы тренеру. Можно использовать русские и английские названия техник.",
                14, MUTED, false));

        LinearLayout sportCard = card();
        page.addView(sportCard);
        sportCard.addView(fieldLabel("Для какого вида борьбы эта цепочка?"));
        String[] sports = profileSports();
        Spinner sport = spinner(sports, system.getString("sport", sports[0]));
        sportCard.addView(sport);

        LinearLayout form = card();
        page.addView(form);
        form.addView(sectionTitle("Пять частей цепочки"));

        EditText[] fields = new EditText[STEP_KEYS.length];
        String[] hints = {
                "Например: закрытый гард",
                "Например: контроль руки и переход в Williams Guard",
                "Например: треугольник",
                "Например: омоплата или свип",
                "Например: рама, колено внутрь и восстановление гарда"
        };

        for (int i = 0; i < STEP_KEYS.length; i++) {
            form.addView(fieldLabel(STEP_LABELS[i]));
            EditText field = multilineInput(hints[i], valueOrDraft(STEP_KEYS[i]));
            bindDraft(field, "draft_" + STEP_KEYS[i]);
            attachGlossary(field);
            fields[i] = field;
            form.addView(voiceRow(field));
        }

        LinearLayout focusCard = card();
        page.addView(focusCard);
        focusCard.addView(sectionTitle("Текущая задача"));
        focusCard.addView(label(
                "Выбери один этап, на котором сосредоточишься в ближайшие 30 дней.",
                14, MUTED, false));
        Spinner focus = spinner(STEP_LABELS,
                STEP_LABELS[clamp(system.getInt("focus_index", 2), 0, STEP_LABELS.length - 1)]);
        focusCard.addView(focus);
        CheckBox makeCurrent = new CheckBox(this);
        makeCurrent.setText("Сделать выбранный этап текущей целью на 30 дней");
        makeCurrent.setTextSize(14);
        makeCurrent.setTextColor(TEXT);
        makeCurrent.setChecked(!system.getBoolean("configured", false));
        focusCard.addView(makeCurrent);

        Button save = primaryButton("Сохранить систему");
        save.setOnClickListener(v -> saveSystem(save, sport, fields, focus, makeCurrent));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveSystem(Button saveButton,
                            Spinner sport,
                            EditText[] fields,
                            Spinner focus,
                            CheckBox makeCurrent) {
        String start = fields[0].getText().toString().trim();
        String main = fields[2].getText().toString().trim();
        int focusIndex = focus.getSelectedItemPosition();
        String focusValue = fields[focusIndex].getText().toString().trim();

        if (start.isEmpty()) {
            toast("Напиши, с какой позиции начинается цепочка.");
            fields[0].requestFocus();
            return;
        }
        if (main.isEmpty()) {
            toast("Напиши основное продолжение цепочки.");
            fields[2].requestFocus();
            return;
        }
        if (makeCurrent.isChecked() && focusValue.isEmpty()) {
            toast("Выбранный этап пока пуст. Заполни его или выбери другой этап.");
            fields[focusIndex].requestFocus();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Сохраняю…");
        stopVoice();

        SharedPreferences.Editor editor = system.edit()
                .putBoolean("configured", true)
                .putString("sport", sport.getSelectedItem().toString())
                .putInt("focus_index", focusIndex);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            editor.putString(STEP_KEYS[i], fields[i].getText().toString().trim())
                    .remove("draft_" + STEP_KEYS[i]);
        }

        boolean saved;
        try {
            saved = editor.commit();
        } catch (RuntimeException error) {
            saved = false;
        }
        if (!saved) {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить систему");
            toast("Не удалось сохранить цепочку. Данные остались на экране.");
            return;
        }

        if (makeCurrent.isChecked() && !setMission(focusValue)) {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить систему");
            toast("Цепочка сохранена, но текущую цель изменить не удалось.");
            return;
        }

        toast(makeCurrent.isChecked()
                ? "Система сохранена. Текущая цель обновлена."
                : "Система сохранена.");
        showOverview();
    }

    private void showFocusDialog() {
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < STEP_KEYS.length; i++) {
            String value = system.getString(STEP_KEYS[i], "").trim();
            if (!value.isEmpty()) {
                indexes.add(i);
                items.add(STEP_LABELS[i] + "\n" + value);
            }
        }
        if (items.isEmpty()) {
            toast("Сначала заполни рабочую цепочку.");
            return;
        }

        final int[] selected = {0};
        new AlertDialog.Builder(this)
                .setTitle("Что отрабатывать 30 дней?")
                .setSingleChoiceItems(items.toArray(new String[0]), 0,
                        (dialog, which) -> selected[0] = which)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Выбрать", (dialog, which) -> {
                    int stepIndex = indexes.get(selected[0]);
                    String value = system.getString(STEP_KEYS[stepIndex], "").trim();
                    confirmMissionChange(stepIndex, value);
                })
                .show();
    }

    private void confirmMissionChange(int stepIndex, String value) {
        new AlertDialog.Builder(this)
                .setTitle("Сменить текущую цель?")
                .setMessage("Новая цель: " + value
                        + "\n\nСчётчики попыток, успешных применений и финишей начнутся заново.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сменить цель", (dialog, which) -> {
                    if (setMission(value)) {
                        system.edit().putInt("focus_index", stepIndex).apply();
                        toast("Новая цель сохранена.");
                        showOverview();
                    } else {
                        toast("Не удалось изменить текущую цель.");
                    }
                })
                .show();
    }

    private boolean setMission(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        return profile.edit()
                .putString("mission", value.trim())
                .putLong("mission_started_at", System.currentTimeMillis())
                .putInt("mission_active_days", 0)
                .putInt("mission_attempts", 0)
                .putInt("mission_successes", 0)
                .putInt("mission_finishes", 0)
                .remove("mission_last_day")
                .remove("draft_mission")
                .commit();
    }

    private Button backButton() {
        Button back = secondaryButton("← К главному экрану");
        back.setOnClickListener(v -> finish());
        return back;
    }

    private LinearLayout voiceRow(EditText field) {
        LinearLayout row = horizontal(6);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button ru = miniButton("RU");
        ru.setContentDescription("Голосовой ввод на русском");
        ru.setOnClickListener(v -> startVoice(field, ru, "ru-RU"));
        Button en = miniButton("EN");
        en.setContentDescription("Голосовой ввод английских названий");
        en.setOnClickListener(v -> startVoice(field, en, "en-US"));
        row.addView(ru, new LinearLayout.LayoutParams(dp(47), dp(52)));
        row.addView(en, new LinearLayout.LayoutParams(dp(47), dp(52)));
        return row;
    }

    private void startVoice(EditText target, Button button, String language) {
        String safeLanguage = language == null ? "ru-RU" : language;
        if (listening) {
            if (voiceTarget == target && safeLanguage.equals(voiceLanguage)) {
                try {
                    if (recognizer != null) recognizer.stopListening();
                } catch (RuntimeException ignored) { }
                button.setText("…");
                return;
            }
            stopVoice();
        }

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
            voicePrefix = target.getText().toString().trim();
            voiceLanguage = safeLanguage;
            listening = true;
            button.setText("●");
            target.requestFocus();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
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
        combined = normalizeTerms(combined);
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
            stopVoice();
            if (!silent) toast("Не удалось распознать речь. Текст можно поправить вручную.");
        }
        @Override public void onResults(Bundle results) {
            applyRecognition(results);
            stopVoice();
        }
        @Override public void onPartialResults(Bundle partialResults) {
            applyRecognition(partialResults);
        }
        @Override public void onEvent(int eventType, Bundle params) { }
    }

    private void attachGlossary(EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                if (normalizing || editable == null) return;
                String current = editable.toString();
                String normalized = normalizeTerms(current);
                if (current.equals(normalized)) return;
                normalizing = true;
                field.setText(normalized);
                field.setSelection(normalized.length());
                normalizing = false;
            }
        });
    }

    private String normalizeTerms(String value) {
        String result = value == null ? "" : value;
        result = replaceTerm(result, "вильямс\\s*гард|вильямсгард", "Williams Guard");
        result = replaceTerm(result, "багги\\s*чок|баггичок|баги\\s*чок|багичок", "Buggy Choke");
        result = replaceTerm(result, "де\\s*ла\\s*рива", "De La Riva");
        result = replaceTerm(result, "икс\\s*гард", "X-Guard");
        result = replaceTerm(result, "сингл\\s*лег\\s*икс", "Single Leg X");
        result = replaceTerm(result, "баттерфляй\\s*гард", "Butterfly Guard");
        result = replaceTerm(result, "спайдер\\s*гард", "Spider Guard");
        result = replaceTerm(result, "аши\\s*гарами", "Ashi Garami");
        result = replaceTerm(result, "беримболо", "Berimbolo");
        result = replaceTerm(result, "дарс", "D'Arce");
        return result;
    }

    private String replaceTerm(String value, String expression, String replacement) {
        return Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])(?:" + expression + ")(?![\\p{L}\\p{N}])")
                .matcher(value)
                .replaceAll(replacement);
    }

    private void bindDraft(EditText field, String key) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                system.edit().putString(key, s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String valueOrDraft(String key) {
        String saved = system.getString(key, "").trim();
        return saved.isEmpty() ? system.getString("draft_" + key, "") : saved;
    }

    private String[] profileSports() {
        String saved = profile.getString("sports", "").trim();
        ArrayList<String> values = new ArrayList<>();
        if (!saved.isEmpty()) {
            for (String part : saved.split("\\|")) {
                if (!part.trim().isEmpty()) values.add(part.trim());
            }
        }
        if (values.isEmpty()) values.add(profile.getString("sport", "Грэпплинг / No-Gi"));
        return values.toArray(new String[0]);
    }

    private String compactChain() {
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String value = system.getString(STEP_KEYS[i], "").trim();
            if (!value.isEmpty()) parts.add(value);
        }
        return TextUtils.join("  →  ", parts);
    }

    private LinearLayout stepRow(int number, String title, String value) {
        LinearLayout row = vertical(2);
        row.setPadding(0, dp(7), 0, dp(7));
        row.addView(label(number + ". " + title, 12, PRIMARY, true));
        row.addView(label(value, 16, TEXT, false));
        return row;
    }

    private View infoRow(String title, String details) {
        LinearLayout row = vertical(2);
        row.setPadding(0, dp(7), 0, dp(7));
        row.addView(label(title, 16, TEXT, true));
        row.addView(label(details, 14, MUTED, false));
        return row;
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        return scroll;
    }

    private LinearLayout pageContent(ScrollView scroll) {
        LinearLayout page = vertical(14);
        page.setPadding(dp(18), dp(20), dp(18), dp(36));
        scroll.addView(page);
        return page;
    }

    private LinearLayout card() {
        LinearLayout layout = vertical(9);
        layout.setPadding(dp(18), dp(17), dp(18), dp(17));
        layout.setBackground(rounded(CARD, 18, 1, BORDER));
        layout.setElevation(dp(1));
        return layout;
    }

    private LinearLayout heroCard() {
        LinearLayout layout = vertical(8);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackground(rounded(PRIMARY_DARK, 22, 0, Color.TRANSPARENT));
        layout.setElevation(dp(3));
        return layout;
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

    private TextView sectionTitle(String value) {
        return label(value, 18, TEXT, true);
    }

    private TextView fieldLabel(String value) {
        TextView view = label(value, 13, MUTED, true);
        view.setPadding(0, dp(5), 0, 0);
        return view;
    }

    private EditText multilineInput(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 147, 143));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setMinLines(2);
        field.setMaxLines(4);
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
        return field;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(TEXT);
                view.setTextSize(15);
                view.setPadding(dp(12), dp(10), dp(12), dp(10));
                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) spinner.setSelection(i);
        }
        return spinner;
    }

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
    }

    private Button miniButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 11, 46);
    }

    private Button styledButton(String value, int textColor, int backgroundColor,
                                int textSize, int height) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setTextSize(textSize);
        button.setAutoSizeTextTypeUniformWithConfiguration(
                9, textSize, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
