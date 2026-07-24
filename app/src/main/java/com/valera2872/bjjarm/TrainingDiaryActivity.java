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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * 0.9.0 closed loop: task before training -> structured diary -> next task.
 * Entries stay offline in SharedPreferences and are linked to the active sport and 30-day mission.
 */
public class TrainingDiaryActivity extends Activity {
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String ENTRIES_KEY = "entries_json";
    private static final int REQUEST_AUDIO = 9001;
    private static final int MAX_ENTRIES = 100;

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

    private static final String[] SESSION_TYPES = {
            "Тренировка", "Открытый ковёр", "Индивидуальная работа", "Соревнование"
    };

    private static final String[] RESULTS = {
            "Не отмечать результат",
            "Рабочий раунд",
            "Задачу выполнил",
            "Задачу не успел выполнить",
            "Победа",
            "Поражение",
            "Ничья"
    };

    private SharedPreferences diary;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
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

    private void showHome() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton("← К главному экрану", this::finish));
        page.addView(label("БОРЦОВСКИЙ ДНЕВНИК", 12, PRIMARY, true));
        page.addView(label("Запомнить тренировку и улучшить следующую", 28, TEXT, true));
        page.addView(label(
                "Дневник связывает твою текущую задачу, реальные схватки и следующий шаг. Это не оценка того, кто сильнее на тренировке.",
                15, MUTED, false));

        String sport = activeSport();
        String mission = currentMission();
        LinearLayout task = heroCard();
        page.addView(task);
        task.addView(label("ПЕРЕД СЛЕДУЮЩЕЙ ТРЕНИРОВКОЙ", 12, Color.rgb(217, 239, 233), true));
        task.addView(label(sport, 14, Color.rgb(217, 239, 233), true));
        task.addView(label(nextTask(sport, mission), 20, Color.WHITE, true));
        if (!mission.isEmpty()) {
            task.addView(label("Текущая цель на 30 дней: " + mission,
                    13, Color.rgb(224, 238, 235), false));
        }

        Button add = primaryButton("Записать тренировку или схватку");
        add.setOnClickListener(v -> showEntryForm());
        page.addView(add);

        JSONArray entries = loadEntries();
        LinearLayout history = card();
        page.addView(history);
        history.addView(sectionTitle("История"));
        if (entries.length() == 0) {
            history.addView(label(
                    "Записей пока нет. Первая запись займёт около двух минут; почти все поля можно заполнить голосом.",
                    14, MUTED, false));
        } else {
            history.addView(label(entries.length() + " " + entryWord(entries.length())
                            + ". Последняя: " + lastEntryCaption(entries),
                    14, MUTED, false));
            String pattern = recurringProblem(entries, sport);
            if (!pattern.isEmpty()) {
                history.addView(label("Повторяющееся внимание: " + pattern, 14, TEXT, true));
            }
        }
        Button openHistory = secondaryButton("Открыть дневник");
        openHistory.setOnClickListener(v -> showHistory());
        history.addView(openHistory);

        setContentView(scroll);
    }

    private void showEntryForm() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton("← К дневнику", this::showHome));
        page.addView(label("НОВАЯ ЗАПИСЬ", 12, PRIMARY, true));
        page.addView(label(activeSport(), 28, TEXT, true));
        page.addView(label(
                "Запиши только то, что поможет на следующем ковре. Не нужно подробно пересказывать всё занятие.",
                14, MUTED, false));

        LinearLayout basics = card();
        page.addView(basics);
        basics.addView(sectionTitle("Что это было"));
        basics.addView(fieldLabel("Тип занятия"));
        Spinner type = spinner(SESSION_TYPES, diary.getString("draft_type", SESSION_TYPES[0]));
        basics.addView(type);
        EditText duration = input("Продолжительность, минут", diary.getString("draft_duration", ""),
                InputType.TYPE_CLASS_NUMBER);
        EditText rounds = input("Количество схваток или раундов", diary.getString("draft_rounds", ""),
                InputType.TYPE_CLASS_NUMBER);
        bindDraft(duration, "draft_duration");
        bindDraft(rounds, "draft_rounds");
        basics.addView(duration);
        basics.addView(rounds);

        EditText partners = multilineInput("С кем боролся? Имена или краткое описание партнёров",
                diary.getString("draft_partners", ""));
        bindDraft(partners, "draft_partners");
        basics.addView(fieldLabel("Партнёры или соперники"));
        basics.addView(voiceRow(partners));

        basics.addView(fieldLabel("Результат"));
        Spinner result = spinner(RESULTS, diary.getString("draft_result", RESULTS[0]));
        basics.addView(result);
        basics.addView(label(
                "На обычной тренировке победу или поражение отмечать не обязательно. Эти варианты нужны прежде всего для соревнований.",
                12, MUTED, false));

        LinearLayout missionCard = card();
        page.addView(missionCard);
        missionCard.addView(sectionTitle("Текущая задача"));
        String mission = currentMission();
        missionCard.addView(label(mission.isEmpty() ? "Цель на 30 дней пока не выбрана." : mission,
                16, TEXT, true));
        EditText attempts = input("Осознанных попыток", diary.getString("draft_attempts", "0"),
                InputType.TYPE_CLASS_NUMBER);
        EditText successes = input("Успешных применений", diary.getString("draft_successes", "0"),
                InputType.TYPE_CLASS_NUMBER);
        EditText finishes = input("Финишей или чистых завершений", diary.getString("draft_finishes", "0"),
                InputType.TYPE_CLASS_NUMBER);
        bindDraft(attempts, "draft_attempts");
        bindDraft(successes, "draft_successes");
        bindDraft(finishes, "draft_finishes");
        missionCard.addView(attempts);
        missionCard.addView(successes);
        missionCard.addView(finishes);

        LinearLayout reflection = card();
        page.addView(reflection);
        reflection.addView(sectionTitle("Что произошло на ковре"));
        EditText techniques = multilineInput("Какие техники и позиции применял?",
                diary.getString("draft_techniques", ""));
        EditText breakdown = multilineInput("Где чаще всего ломалась цепочка или возникала проблема?",
                diary.getString("draft_breakdown", ""));
        EditText coach = multilineInput("Что сказал тренер?",
                diary.getString("draft_coach", ""));
        EditText next = multilineInput("На что хочешь обратить внимание в следующий раз?",
                diary.getString("draft_next", ""));
        bindDraft(techniques, "draft_techniques");
        bindDraft(breakdown, "draft_breakdown");
        bindDraft(coach, "draft_coach");
        bindDraft(next, "draft_next");
        reflection.addView(voiceRow(techniques));
        reflection.addView(voiceRow(breakdown));
        reflection.addView(voiceRow(coach));
        reflection.addView(voiceRow(next));

        Button save = primaryButton("Сохранить запись");
        save.setOnClickListener(v -> saveEntry(save, type, duration, rounds, partners, result,
                attempts, successes, finishes, techniques, breakdown, coach, next));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveEntry(Button save,
                           Spinner type,
                           EditText duration,
                           EditText rounds,
                           EditText partners,
                           Spinner result,
                           EditText attempts,
                           EditText successes,
                           EditText finishes,
                           EditText techniques,
                           EditText breakdown,
                           EditText coach,
                           EditText next) {
        String cleanPartners = text(partners);
        String cleanTechniques = text(techniques);
        String cleanBreakdown = text(breakdown);
        String cleanCoach = text(coach);
        String cleanNext = text(next);
        int attemptCount = nonNegativeInt(text(attempts));
        int successCount = nonNegativeInt(text(successes));
        int finishCount = nonNegativeInt(text(finishes));

        if (cleanPartners.isEmpty() && cleanTechniques.isEmpty() && cleanBreakdown.isEmpty()
                && cleanCoach.isEmpty() && cleanNext.isEmpty() && attemptCount == 0) {
            toast("Запиши хотя бы один полезный факт о тренировке.");
            techniques.requestFocus();
            return;
        }

        save.setEnabled(false);
        save.setText("Сохраняю…");
        stopVoice();

        String sport = activeSport();
        String mission = currentMission();
        String generatedNext = buildNextTask(mission, attemptCount, successCount,
                cleanBreakdown, cleanCoach, cleanNext);

        JSONObject entry = new JSONObject();
        try {
            long now = System.currentTimeMillis();
            entry.put("id", now);
            entry.put("timestamp", now);
            entry.put("sport", sport);
            entry.put("type", String.valueOf(type.getSelectedItem()));
            entry.put("duration", nonNegativeInt(text(duration)));
            entry.put("rounds", nonNegativeInt(text(rounds)));
            entry.put("partners", cleanPartners);
            entry.put("result", String.valueOf(result.getSelectedItem()));
            entry.put("mission", mission);
            entry.put("attempts", attemptCount);
            entry.put("successes", successCount);
            entry.put("finishes", finishCount);
            entry.put("techniques", cleanTechniques);
            entry.put("breakdown", cleanBreakdown);
            entry.put("coach", cleanCoach);
            entry.put("next_focus", generatedNext);
        } catch (JSONException error) {
            save.setEnabled(true);
            save.setText("Сохранить запись");
            toast("Не удалось подготовить запись.");
            return;
        }

        JSONArray oldEntries = loadEntries();
        JSONArray newEntries = new JSONArray();
        newEntries.put(entry);
        for (int i = 0; i < oldEntries.length() && i < MAX_ENTRIES - 1; i++) {
            JSONObject old = oldEntries.optJSONObject(i);
            if (old != null) newEntries.put(old);
        }

        boolean saved = diary.edit()
                .putString(ENTRIES_KEY, newEntries.toString())
                .putString("next_task_" + SportGuidance.slug(sport), generatedNext)
                .putString("last_sport", sport)
                .putLong("last_timestamp", System.currentTimeMillis())
                .remove("draft_duration")
                .remove("draft_rounds")
                .remove("draft_partners")
                .remove("draft_attempts")
                .remove("draft_successes")
                .remove("draft_finishes")
                .remove("draft_techniques")
                .remove("draft_breakdown")
                .remove("draft_coach")
                .remove("draft_next")
                .commit();

        if (!saved) {
            save.setEnabled(true);
            save.setText("Сохранить запись");
            toast("Не удалось сохранить дневник. Данные остались на экране.");
            return;
        }

        updateMissionCounters(attemptCount, successCount, finishCount);
        diary.edit()
                .putString("draft_type", String.valueOf(type.getSelectedItem()))
                .putString("draft_result", RESULTS[0])
                .apply();

        new AlertDialog.Builder(this)
                .setTitle("Запись сохранена")
                .setMessage("Следующая задача:\n\n" + generatedNext)
                .setPositiveButton("Готово", (dialog, which) -> showHome())
                .setCancelable(false)
                .show();
    }

    private void showHistory() {
        stopVoice();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(backButton("← К дневнику", this::showHome));
        page.addView(label("ИСТОРИЯ ДНЕВНИКА", 12, PRIMARY, true));
        page.addView(label("Что происходило на ковре", 28, TEXT, true));

        JSONArray entries = loadEntries();
        if (entries.length() == 0) {
            LinearLayout empty = card();
            empty.addView(label("Записей пока нет.", 16, TEXT, true));
            empty.addView(label("После тренировки создай первую короткую запись.", 14, MUTED, false));
            page.addView(empty);
        } else {
            int limit = Math.min(entries.length(), 40);
            for (int i = 0; i < limit; i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry == null) continue;
                LinearLayout item = card();
                item.addView(label(formatDate(entry.optLong("timestamp")), 12, PRIMARY, true));
                item.addView(label(entry.optString("sport", "Борьба") + " · "
                        + entry.optString("type", "Тренировка"), 17, TEXT, true));
                String summary = entrySummary(entry);
                if (!summary.isEmpty()) item.addView(label(summary, 14, MUTED, false));
                String next = entry.optString("next_focus", "").trim();
                if (!next.isEmpty()) item.addView(label("Следующий фокус: " + next, 13, TEXT, true));
                Button open = secondaryButton("Открыть запись");
                open.setOnClickListener(v -> showEntryDetails(entry));
                item.addView(open);
                page.addView(item);
            }
        }
        setContentView(scroll);
    }

    private void showEntryDetails(JSONObject entry) {
        LinearLayout content = vertical(7);
        content.setPadding(dp(8), dp(4), dp(8), dp(4));
        addDetail(content, "Дата", formatDate(entry.optLong("timestamp")));
        addDetail(content, "Вид борьбы", entry.optString("sport"));
        addDetail(content, "Формат", entry.optString("type"));
        int duration = entry.optInt("duration", 0);
        int rounds = entry.optInt("rounds", 0);
        if (duration > 0) addDetail(content, "Продолжительность", duration + " мин");
        if (rounds > 0) addDetail(content, "Схваток или раундов", String.valueOf(rounds));
        addDetail(content, "Партнёры", entry.optString("partners"));
        String result = entry.optString("result");
        if (!RESULTS[0].equals(result)) addDetail(content, "Результат", result);
        addDetail(content, "Цель", entry.optString("mission"));
        addDetail(content, "Попытки / удалось / финиши",
                entry.optInt("attempts") + " / " + entry.optInt("successes") + " / " + entry.optInt("finishes"));
        addDetail(content, "Техники", entry.optString("techniques"));
        addDetail(content, "Где возникла проблема", entry.optString("breakdown"));
        addDetail(content, "Комментарий тренера", entry.optString("coach"));
        addDetail(content, "Следующая задача", entry.optString("next_focus"));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this)
                .setTitle("Запись тренировки")
                .setView(scroll)
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private String buildNextTask(String mission,
                                 int attempts,
                                 int successes,
                                 String breakdown,
                                 String coach,
                                 String userNext) {
        if (!userNext.isEmpty()) return userNext;
        if (!coach.isEmpty()) return "Проверить совет тренера: " + coach;
        String target = mission.isEmpty() ? "текущую рабочую цепочку" : mission;
        if (attempts == 0) {
            return "Сделать хотя бы одну осознанную попытку: " + target + ".";
        }
        if (successes == 0) {
            if (!breakdown.isEmpty()) {
                return "Оставить ту же цель, но сосредоточиться только на проблеме: " + breakdown + ".";
            }
            return "Повторить " + target + " и разобрать один момент, где действие остановилось.";
        }
        if (successes >= 2) {
            return "Повторить " + target + " и связать успешное действие со следующим этапом своей цепочки.";
        }
        if (!breakdown.isEmpty()) {
            return "Повторить успешный момент и отдельно проверить: " + breakdown + ".";
        }
        return "Ещё раз применить " + target + " и сохранить ключевой контроль после выполнения.";
    }

    private String nextTask(String sport, String mission) {
        String saved = diary.getString("next_task_" + SportGuidance.slug(sport), "").trim();
        if (!saved.isEmpty()) return saved;
        if (!mission.isEmpty()) {
            return "Сделай минимум одну осознанную попытку текущей техники. После тренировки запиши, где действие получилось или остановилось.";
        }
        return "Выбери один этап своей рабочей цепочки и попробуй его хотя бы один раз. После тренировки запиши результат.";
    }

    private void updateMissionCounters(int attempts, int successes, int finishes) {
        if (attempts == 0 && successes == 0 && finishes == 0) return;
        profile.edit()
                .putInt("mission_attempts", profile.getInt("mission_attempts", 0) + attempts)
                .putInt("mission_successes", profile.getInt("mission_successes", 0) + successes)
                .putInt("mission_finishes", profile.getInt("mission_finishes", 0) + finishes)
                .putInt("mission_active_days", profile.getInt("mission_active_days", 0) + 1)
                .putString("mission_last_day", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()))
                .apply();
    }

    private String recurringProblem(JSONArray entries, String sport) {
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < entries.length() && i < 8; i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null || !sport.equals(item.optString("sport"))) continue;
            String value = item.optString("breakdown", "").trim();
            if (!value.isEmpty()) values.add(value);
        }
        if (values.size() < 2) return "";
        String first = values.get(0).toLowerCase(Locale.ROOT);
        for (int i = 1; i < values.size(); i++) {
            String current = values.get(i).toLowerCase(Locale.ROOT);
            if (first.equals(current) || containsSharedWord(first, current)) return values.get(0);
        }
        return "";
    }

    private boolean containsSharedWord(String a, String b) {
        for (String word : a.split("\\s+")) {
            if (word.length() >= 5 && b.contains(word)) return true;
        }
        return false;
    }

    private JSONArray loadEntries() {
        String raw = diary.getString(ENTRIES_KEY, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (JSONException error) {
            return new JSONArray();
        }
    }

    private String lastEntryCaption(JSONArray entries) {
        JSONObject first = entries.optJSONObject(0);
        if (first == null) return "—";
        return formatDate(first.optLong("timestamp")) + ", " + first.optString("sport", "борьба");
    }

    private String entrySummary(JSONObject entry) {
        ArrayList<String> parts = new ArrayList<>();
        int rounds = entry.optInt("rounds", 0);
        if (rounds > 0) parts.add(rounds + " раундов");
        int attempts = entry.optInt("attempts", 0);
        int successes = entry.optInt("successes", 0);
        if (attempts > 0 || successes > 0) parts.add("попытки " + attempts + ", удалось " + successes);
        String partners = entry.optString("partners", "").trim();
        if (!partners.isEmpty()) parts.add("с кем: " + partners);
        String techniques = entry.optString("techniques", "").trim();
        if (!techniques.isEmpty()) parts.add(techniques);
        return TextUtils.join(" · ", parts);
    }

    private String activeSport() {
        String active = profile.getString("active_sport", "").trim();
        if (!active.isEmpty()) return active;
        String sports = profile.getString("sports", "").trim();
        if (!sports.isEmpty()) {
            String[] values = sports.split("\\|");
            if (values.length > 0 && !values[0].trim().isEmpty()) return values[0].trim();
        }
        return profile.getString("sport", "Грэпплинг / No-Gi");
    }

    private String currentMission() {
        return profile.getString("mission", "").trim();
    }

    private String entryWord(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod10 == 1 && mod100 != 11) return "запись";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "записи";
        return "записей";
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Дата не указана";
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private void addDetail(LinearLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) return;
        parent.addView(label(title, 12, PRIMARY, true));
        parent.addView(label(value.trim(), 15, TEXT, false));
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

    private void bindDraft(EditText field, String key) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                diary.edit().putString(key, s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
    }

    private int nonNegativeInt(String value) {
        try { return Math.max(0, Integer.parseInt(value.trim())); }
        catch (Exception ignored) { return 0; }
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) index = i;
        spinner.setSelection(index);
        spinner.setBackground(rounded(Color.WHITE, 11, 1, BORDER));
        spinner.setPadding(dp(10), dp(5), dp(10), dp(5));
        return spinner;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value == null ? "" : value);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 145, 142));
        field.setInputType(inputType);
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.WHITE, 11, 1, BORDER));
        return field;
    }

    private EditText multilineInput(String hint, String value) {
        EditText field = input(hint, value,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setMinLines(2);
        field.setMaxLines(6);
        field.setGravity(Gravity.TOP | Gravity.START);
        return field;
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
        LinearLayout layout = vertical(9);
        layout.setPadding(dp(19), dp(19), dp(19), dp(19));
        layout.setBackground(rounded(PRIMARY_DARK, 20, 0, Color.TRANSPARENT));
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
        view.setText(value == null ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.13f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView sectionTitle(String value) { return label(value, 18, TEXT, true); }
    private TextView fieldLabel(String value) { return label(value, 13, MUTED, true); }

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
    }

    private Button miniButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 11, 46);
    }

    private Button backButton(String value, Runnable action) {
        Button button = secondaryButton(value);
        button.setOnClickListener(v -> action.run());
        return button;
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
        SpacerDrawable(int size) { super(Color.TRANSPARENT); this.size = size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
