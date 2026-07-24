package com.valera2872.bjjarm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import java.util.List;
import java.util.Locale;

/** Premium standalone profile editor. Keeps the stable synchronous save semantics from 0.6.6. */
public class PremiumProfileActivity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int REQUEST_AUDIO = 9101;

    private static final String[] SPORTS = {
            "Грэпплинг / No-Gi",
            "Бразильское джиу-джитсу",
            "Дзюдо",
            "Вольная борьба",
            "Греко-римская борьба"
    };

    private static final String[] STYLES = {
            "Работа снизу / гард",
            "Проход и контроль сверху",
            "Стойка и броски",
            "Смешанный стиль",
            "Разный стиль в разных видах",
            "Стиль ещё формируется"
    };

    private static final String[] AREAS = {
            "Спина и тяга",
            "Руки и предплечья",
            "Ноги",
            "Корпус",
            "Шея и плечевой пояс",
            "Взрывная сила",
            "Силовая выносливость",
            "Подвижность"
    };

    private SharedPreferences profile;
    private boolean saving;
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
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        PremiumUi.applyWindow(this);
        showProfile();
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

    private void showProfile() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← Назад");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Профиль спортсмена"));
        page.addView(PremiumUi.title(this, "Настрой подготовку под себя"));
        page.addView(PremiumUi.body(this,
                "Профиль нужен только для личного плана. Все данные остаются на этом телефоне."));

        LinearLayout basics = PremiumUi.card(this);
        page.addView(basics);
        basics.addView(PremiumUi.cardTitle(this, "Основные данные"));
        EditText name = PremiumUi.input(this, "Имя спортсмена",
                valueOrDraft("name", "draft_name"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText age = PremiumUi.input(this, "Возраст",
                String.valueOf(profile.getInt("age", 14)), InputType.TYPE_CLASS_NUMBER);
        EditText sessions = PremiumUi.input(this, "Тренировок на ковре в неделю",
                String.valueOf(profile.getInt("mat_sessions", 5)), InputType.TYPE_CLASS_NUMBER);
        bindDraft(name, "draft_name");
        basics.addView(fieldLabel("Имя"));
        basics.addView(name);
        basics.addView(fieldLabel("Возраст"));
        basics.addView(age);
        basics.addView(fieldLabel("Обычная нагрузка"));
        basics.addView(sessions);

        LinearLayout sportsCard = PremiumUi.card(this);
        page.addView(sportsCard);
        sportsCard.addView(PremiumUi.cardTitle(this, "Виды борьбы"));
        sportsCard.addView(PremiumUi.small(this,
                "Можно выбрать несколько. На главном экране затем указывается текущая дисциплина."));
        List<String> selectedSports = savedSports();
        CheckBox[] sportChecks = new CheckBox[SPORTS.length];
        for (int i = 0; i < SPORTS.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(SPORTS[i]);
            box.setTextSize(15);
            box.setTextColor(PremiumUi.INK);
            box.setChecked(selectedSports.contains(SPORTS[i]));
            box.setPadding(0, PremiumUi.dp(this, 7), 0, PremiumUi.dp(this, 7));
            sportChecks[i] = box;
            sportsCard.addView(box);
        }

        LinearLayout physical = PremiumUi.card(this);
        page.addView(physical);
        physical.addView(PremiumUi.cardTitle(this, "Стиль и физические задачи"));
        physical.addView(fieldLabel("Как ты чаще борешься?"));
        Spinner style = spinner(STYLES, profile.getString("style", STYLES[0]));
        physical.addView(style);
        physical.addView(fieldLabel("Что уже развито лучше всего?"));
        Spinner strong = spinner(AREAS, profile.getString("strong_area", AREAS[0]));
        physical.addView(strong);
        physical.addView(fieldLabel("Главный приоритет"));
        Spinner priority1 = spinner(AREAS, profile.getString("priority_1", AREAS[1]));
        physical.addView(priority1);
        physical.addView(fieldLabel("Второй приоритет"));
        Spinner priority2 = spinner(AREAS, profile.getString("priority_2", AREAS[2]));
        physical.addView(priority2);

        LinearLayout technique = PremiumUi.card(this);
        page.addView(technique);
        technique.addView(PremiumUi.cardTitle(this, "Твой технический фокус"));
        technique.addView(PremiumUi.small(this,
                "Говори по-русски через RU. Английские названия техник лучше произносить через EN."));

        EditText favorite = PremiumUi.multiline(this,
                "Например: закрытый гард, arm drag, учи-мата",
                valueOrDraft("favorite_techniques", "draft_favorite"));
        bindDraft(favorite, "draft_favorite");
        technique.addView(fieldLabel("Любимые позиции и техники"));
        technique.addView(voiceRow(favorite));

        EditText mission = PremiumUi.multiline(this,
                "Одна техника или действие, которое хочешь закрепить",
                valueOrDraft("mission", "draft_mission"));
        bindDraft(mission, "draft_mission");
        technique.addView(fieldLabel("Фокус на 30 дней"));
        technique.addView(voiceRow(mission));
        technique.addView(PremiumUi.small(this,
                "Например: на каждой тренировке хотя бы один раз выйти в Williams Guard и сохранить контроль руки."));

        Button save = PremiumUi.primaryButton(this, "Сохранить профиль");
        save.setOnClickListener(v -> saveProfile(save, name, age, sessions, sportChecks,
                style, strong, priority1, priority2, favorite, mission));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveProfile(Button saveButton,
                             EditText name,
                             EditText age,
                             EditText sessions,
                             CheckBox[] sportChecks,
                             Spinner style,
                             Spinner strong,
                             Spinner priority1,
                             Spinner priority2,
                             EditText favorite,
                             EditText mission) {
        if (saving) return;
        String cleanName = text(name);
        String cleanMission = text(mission);
        ArrayList<String> sports = new ArrayList<>();
        for (int i = 0; i < sportChecks.length; i++) {
            if (sportChecks[i].isChecked()) sports.add(SPORTS[i]);
        }
        if (cleanName.isEmpty()) {
            toast("Укажи имя спортсмена.");
            name.requestFocus();
            return;
        }
        if (sports.isEmpty()) {
            toast("Выбери хотя бы один вид борьбы.");
            return;
        }
        if (cleanMission.isEmpty()) {
            toast("Укажи технику или действие на 30 дней.");
            mission.requestFocus();
            return;
        }

        saving = true;
        saveButton.setEnabled(false);
        saveButton.setText("Сохраняю…");
        stopVoice();

        String oldMission = profile.getString("mission", "").trim();
        boolean missionChanged = !oldMission.equals(cleanMission);
        String oldActive = profile.getString("active_sport", "").trim();
        String active = sports.contains(oldActive) ? oldActive : sports.get(0);

        SharedPreferences.Editor editor = profile.edit()
                .putBoolean("profile_complete", true)
                .putString("name", cleanName)
                .putInt("age", clamp(safeInt(text(age), 14), 8, 80))
                .putInt("mat_sessions", clamp(safeInt(text(sessions), 5), 1, 14))
                .putString("sports", android.text.TextUtils.join("|", sports))
                .putString("sport", sports.get(0))
                .putString("active_sport", active)
                .putString("style", String.valueOf(style.getSelectedItem()))
                .putString("strong_area", String.valueOf(strong.getSelectedItem()))
                .putString("priority_1", String.valueOf(priority1.getSelectedItem()))
                .putString("priority_2", String.valueOf(priority2.getSelectedItem()))
                .putString("favorite_techniques", text(favorite))
                .putString("mission", cleanMission)
                .remove("draft_name")
                .remove("draft_favorite")
                .remove("draft_mission");

        if (missionChanged || !profile.contains("mission_started_at")) {
            editor.putLong("mission_started_at", System.currentTimeMillis())
                    .putInt("mission_active_days", 0)
                    .putInt("mission_attempts", 0)
                    .putInt("mission_successes", 0)
                    .putInt("mission_finishes", 0)
                    .remove("mission_last_day");
        }

        boolean committed;
        try {
            committed = editor.commit();
        } catch (RuntimeException error) {
            committed = false;
        }
        boolean verified = committed
                && cleanName.equals(profile.getString("name", "").trim())
                && profile.getBoolean("profile_complete", false);
        if (!verified) {
            saving = false;
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить профиль");
            toast("Не удалось сохранить профиль. Введённые данные остались на экране.");
            return;
        }
        setResult(RESULT_OK);
        toast("Профиль сохранён.");
        finish();
    }

    private LinearLayout voiceRow(EditText field) {
        LinearLayout row = PremiumUi.horizontal(this, 7);
        row.setGravity(Gravity.TOP);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout controls = PremiumUi.vertical(this, 7);
        Button ru = PremiumUi.secondaryButton(this, "RU");
        Button en = PremiumUi.outlineButton(this, "EN");
        ru.setMinWidth(PremiumUi.dp(this, 54));
        en.setMinWidth(PremiumUi.dp(this, 54));
        ru.setOnClickListener(v -> startVoice(field, ru, "ru-RU"));
        en.setOnClickListener(v -> startVoice(field, en, "en-US"));
        controls.addView(ru, new LinearLayout.LayoutParams(PremiumUi.dp(this, 58), PremiumUi.dp(this, 52)));
        controls.addView(en, new LinearLayout.LayoutParams(PremiumUi.dp(this, 58), PremiumUi.dp(this, 52)));
        row.addView(controls);
        return row;
    }

    private void startVoice(EditText target, Button button, String language) {
        String safeLanguage = language == null ? "ru-RU" : language;
        if (listening) {
            stopVoice();
            if (voiceTarget == target && safeLanguage.equals(voiceLanguage)) return;
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
            voiceLanguage = safeLanguage;
            voicePrefix = text(target);
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
        combined = normalizeTechnique(combined);
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

    private String normalizeTechnique(String value) {
        String result = value == null ? "" : value.trim();
        String lower = result.toLowerCase(Locale.ROOT);
        if (lower.contains("вильямс гард") || lower.contains("уильямс гард")) return replaceIgnoreCase(result, "вильямс гард", "Williams Guard");
        if (lower.contains("багги чок") || lower.contains("баггичок")) {
            result = replaceIgnoreCase(result, "багги чок", "Buggy Choke");
            return replaceIgnoreCase(result, "баггичок", "Buggy Choke");
        }
        if (lower.contains("де ла рива")) return replaceIgnoreCase(result, "де ла рива", "De La Riva");
        if (lower.contains("икс гард")) return replaceIgnoreCase(result, "икс гард", "X-Guard");
        if (lower.contains("аши гарами")) return replaceIgnoreCase(result, "аши гарами", "Ashi Garami");
        if (lower.contains("беримболо")) return replaceIgnoreCase(result, "беримболо", "Berimbolo");
        if (lower.contains("дарс")) return replaceIgnoreCase(result, "дарс", "D'Arce");
        return result;
    }

    private String replaceIgnoreCase(String source, String target, String replacement) {
        return source.replaceAll("(?iu)" + java.util.regex.Pattern.quote(target),
                java.util.regex.Matcher.quoteReplacement(replacement));
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) index = i;
        spinner.setSelection(index);
        spinner.setPadding(PremiumUi.dp(this, 12), PremiumUi.dp(this, 5),
                PremiumUi.dp(this, 12), PremiumUi.dp(this, 5));
        spinner.setBackground(PremiumUi.rounded(Color.WHITE, 14, 1, PremiumUi.BORDER));
        spinner.setMinimumHeight(PremiumUi.dp(this, 52));
        return spinner;
    }

    private TextView fieldLabel(String value) {
        TextView label = PremiumUi.accentText(this, value);
        label.setPadding(0, PremiumUi.dp(this, 4), 0, 0);
        return label;
    }

    private List<String> savedSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String value : saved.split("\\|")) if (!value.trim().isEmpty()) result.add(value.trim());
        }
        if (result.isEmpty()) result.add(profile.getString("sport", SPORTS[0]));
        return result;
    }

    private String valueOrDraft(String valueKey, String draftKey) {
        String value = profile.getString(valueKey, "").trim();
        return value.isEmpty() ? profile.getString(draftKey, "") : value;
    }

    private void bindDraft(EditText field, String key) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                profile.edit().putString(key, s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
    }

    private int safeInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); }
        catch (Exception ignored) { return fallback; }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
