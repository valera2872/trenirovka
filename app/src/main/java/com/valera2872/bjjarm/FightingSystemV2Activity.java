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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/** 0.8.0 fighting system with a separate chain and wording for each sport. */
public class FightingSystemV2Activity extends Activity {
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int REQUEST_AUDIO = 8001;
    private static final String[] STEP_KEYS = {"step_0", "step_1", "step_2", "step_3", "step_4"};

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

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
    private String activeSport;
    private String prefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        resolveSport();
        showCurrentScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String before = activeSport;
        resolveSport();
        if (before != null && !before.equals(activeSport)) showCurrentScreen();
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

    private void showCurrentScreen() {
        if (system.getBoolean(key("configured"), false)) showOverview();
        else showIntro();
    }

    private void showIntro() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label(activeSport, 28, TEXT, true));
        page.addView(label(SportGuidance.description(activeSport), 15, MUTED, false));

        LinearLayout info = card();
        page.addView(info);
        info.addView(sectionTitle("Пять частей рабочей цепочки"));
        String[] labels = SportGuidance.stepLabels(activeSport);
        String[] hints = SportGuidance.hints(activeSport);
        for (int i = 0; i < labels.length; i++) {
            info.addView(infoRow((i + 1) + ". " + labels[i], hints[i].replace("Например: ", "")));
        }

        Button create = primaryButton("Собрать цепочку");
        create.setOnClickListener(v -> showEditor());
        page.addView(create);
        Button sport = secondaryButton("Выбрать другой вид борьбы");
        sport.setOnClickListener(v -> startActivity(new Intent(this, SportFocusActivity.class)));
        page.addView(sport);
        setContentView(scroll);
    }

    private void showOverview() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label(activeSport, 28, TEXT, true));
        page.addView(label(
                "Для каждого вида борьбы сохраняется своя цепочка. Переключение дисциплины не удаляет остальные.",
                14, MUTED, false));

        LinearLayout chain = card();
        page.addView(chain);
        chain.addView(sectionTitle("Рабочая цепочка"));
        String[] labels = SportGuidance.stepLabels(activeSport);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            String value = system.getString(key(STEP_KEYS[i]), "").trim();
            if (!value.isEmpty()) chain.addView(stepRow(i + 1, labels[i], value));
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
                "Попытки, успешные применения и финиши считаются для этой цели независимо от выбранного вида борьбы.",
                13, MUTED, false));
        Button choose = primaryButton("Выбрать этап на 30 дней");
        choose.setOnClickListener(v -> showFocusDialog());
        current.addView(choose);

        Button edit = secondaryButton("Изменить цепочку");
        edit.setOnClickListener(v -> showEditor());
        page.addView(edit);
        Button sport = secondaryButton("Переключить вид борьбы");
        sport.setOnClickListener(v -> startActivity(new Intent(this, SportFocusActivity.class)));
        page.addView(sport);
        setContentView(scroll);
    }

    private void showEditor() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton());
        page.addView(label("МОЯ БОРЦОВСКАЯ СИСТЕМА", 12, PRIMARY, true));
        page.addView(label(activeSport, 28, TEXT, true));
        page.addView(label(
                "Пиши так, как объяснил бы тренеру. Для английских названий используй кнопку EN.",
                14, MUTED, false));

        LinearLayout form = card();
        page.addView(form);
        form.addView(sectionTitle("Рабочая цепочка"));
        String[] labels = SportGuidance.stepLabels(activeSport);
        String[] hints = SportGuidance.hints(activeSport);
        EditText[] fields = new EditText[STEP_KEYS.length];
        for (int i = 0; i < STEP_KEYS.length; i++) {
            form.addView(fieldLabel(labels[i]));
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
                "После сохранения можно выбрать любой заполненный этап как цель на 30 дней.",
                14, MUTED, false));
        CheckBox makeCurrent = new CheckBox(this);
        makeCurrent.setText("Сделать основное продолжение текущей целью на 30 дней");
        makeCurrent.setTextSize(14);
        makeCurrent.setTextColor(TEXT);
        makeCurrent.setChecked(!system.getBoolean(key("configured"), false));
        focusCard.addView(makeCurrent);

        Button save = primaryButton("Сохранить цепочку");
        save.setOnClickListener(v -> saveSystem(save, fields, makeCurrent));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveSystem(Button saveButton, EditText[] fields, CheckBox makeCurrent) {
        String start = fields[0].getText().toString().trim();
        String main = fields[2].getText().toString().trim();
        if (start.isEmpty()) {
            toast("Заполни первый этап цепочки.");
            fields[0].requestFocus();
            return;
        }
        if (main.isEmpty()) {
            toast("Заполни основное продолжение.");
            fields[2].requestFocus();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Сохраняю…");
        stopVoice();

        SharedPreferences.Editor editor = system.edit().putBoolean(key("configured"), true);
        for (int i = 0; i < STEP_KEYS.length; i++) {
            editor.putString(key(STEP_KEYS[i]), fields[i].getText().toString().trim())
                    .remove(key("draft_" + STEP_KEYS[i]));
        }
        boolean saved;
        try {
            saved = editor.commit();
        } catch (RuntimeException error) {
            saved = false;
        }
        if (!saved) {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить цепочку");
            toast("Не удалось сохранить цепочку. Данные остались на экране.");
            return;
        }
        if (makeCurrent.isChecked() && !setMission(main)) {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить цепочку");
            toast("Цепочка сохранена, но текущую цель изменить не удалось.");
            return;
        }
        toast(makeCurrent.isChecked() ? "Цепочка и текущая цель сохранены." : "Цепочка сохранена.");
        showOverview();
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
                    int index = indexes.get(selected[0]);
                    String value = system.getString(key(STEP_KEYS[index]), "").trim();
                    confirmMissionChange(value);
                })
                .show();
    }

    private void confirmMissionChange(String value) {
        new AlertDialog.Builder(this)
                .setTitle("Сменить текущую цель?")
                .setMessage("Новая цель: " + value
                        + "\n\nСчётчики попыток, успешных применений и финишей начнутся заново.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сменить цель", (dialog, which) -> {
                    if (setMission(value)) {
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

    private String key(String name) {
        return prefix + name;
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
        ru.setOnClickListener(v -> startVoice(field, ru, "ru-RU"));
        Button en = miniButton("EN");
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
        result = replaceTerm(result, "аши\\s*гарами", "Ashi Garami");
        result = replaceTerm(result, "беримболо", "Berimbolo");
        result = replaceTerm(result, "дарс", "D'Arce");
        return result;
    }

    private String replaceTerm(String value, String expression, String replacement) {
        return Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])(?:" + expression + ")(?![\\p{L}\\p{N}])")
                .matcher(value).replaceAll(replacement);
    }

    private void bindDraft(EditText field, String name) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                system.edit().putString(key(name), s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String valueOrDraft(String name) {
        String saved = system.getString(key(name), "").trim();
        return saved.isEmpty() ? system.getString(key("draft_" + name), "") : saved;
    }

    private ArrayList<String> profileSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String part : saved.split("\\|")) {
                if (!part.trim().isEmpty()) result.add(part.trim());
            }
        }
        if (result.isEmpty()) result.add(profile.getString("sport", "Грэпплинг / No-Gi"));
        return result;
    }

    private String compactChain() {
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String value = system.getString(key(STEP_KEYS[i]), "").trim();
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

    private TextView sectionTitle(String value) { return label(value, 18, TEXT, true); }

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

    private Button primaryButton(String value) { return styledButton(value, Color.WHITE, PRIMARY, 15, 52); }
    private Button secondaryButton(String value) { return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48); }
    private Button miniButton(String value) { return styledButton(value, PRIMARY, PRIMARY_SOFT, 11, 46); }

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

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawable(int size) { super(Color.TRANSPARENT); this.size = size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
