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
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

/** Premium, readable diary that keeps the 0.9.0 local data format. */
public class PremiumTrainingDiaryActivity extends Activity {
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String ENTRIES_KEY = "entries_json";
    private static final int MAX_ENTRIES = 100;
    private static final int REQUEST_AUDIO = 9301;

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

    private void showHome() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Дневник тренировок"));
        page.addView(PremiumUi.title(this, "Сохрани главное с ковра"));
        page.addView(PremiumUi.body(this,
                "Не нужно пересказывать всю тренировку. Запиши, что пробовал, где возникла проблема и что проверить в следующий раз."));

        String sport = activeSport();
        String mission = currentMission();
        LinearLayout hero = PremiumUi.hero(this);
        page.addView(hero);
        hero.addView(PremiumUi.heroEyebrow(this, "Следующая тренировка"));
        hero.addView(PremiumUi.heroTitle(this, nextTask(sport, mission)));
        hero.addView(PremiumUi.heroBody(this, sport));
        if (!mission.isEmpty()) {
            hero.addView(PremiumUi.heroBody(this, "Фокус на 30 дней: " + mission));
        }
        Button add = PremiumUi.lightButton(this, "Новая запись");
        add.setOnClickListener(v -> showEntryForm());
        hero.addView(add);

        JSONArray entries = loadEntries();
        LinearLayout history = PremiumUi.card(this);
        page.addView(history);
        history.addView(PremiumUi.cardTitle(this, "История"));
        if (entries.length() == 0) {
            history.addView(PremiumUi.body(this,
                    "Записей пока нет. Первая запись обычно занимает около минуты."));
        } else {
            history.addView(PremiumUi.accentText(this,
                    entries.length() + " " + entryWord(entries.length()) + " сохранено"));
            history.addView(PremiumUi.small(this, "Последняя: " + lastEntryCaption(entries)));
            String recurring = recurringProblem(entries, sport);
            if (!recurring.isEmpty()) {
                LinearLayout note = PremiumUi.softCard(this);
                note.addView(PremiumUi.sectionTitle(this, "Повторяется в записях"));
                note.addView(PremiumUi.bodyDark(this, recurring));
                history.addView(note);
            }
        }
        Button openHistory = PremiumUi.secondaryButton(this, "Открыть историю");
        openHistory.setOnClickListener(v -> showHistory());
        history.addView(openHistory);

        LinearLayout principle = PremiumUi.softCard(this);
        principle.addView(PremiumUi.sectionTitle(this, "На тренировке не обязательно побеждать"));
        principle.addView(PremiumUi.bodyDark(this,
                "Обычный раунд можно оставить без результата. Важнее отметить попытку, позицию и место, где остановилось действие."));
        page.addView(principle);
        setContentView(scroll);
    }

    private void showEntryForm() {
        stopVoice();
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← К дневнику");
        back.setOnClickListener(v -> showHome());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Новая запись"));
        page.addView(PremiumUi.title(this, activeSport()));
        page.addView(PremiumUi.body(this,
                "Заполни только полезное. Все текстовые поля необязательны, но в записи должен остаться хотя бы один факт."));

        LinearLayout basics = PremiumUi.card(this);
        page.addView(basics);
        basics.addView(PremiumUi.cardTitle(this, "Тренировка"));
        basics.addView(fieldLabel("Формат"));
        Spinner type = spinner(SESSION_TYPES, diary.getString("draft_type", SESSION_TYPES[0]));
        bindSpinnerDraft(type, "draft_type");
        basics.addView(type);
        EditText duration = PremiumUi.input(this, "Например: 90",
                diary.getString("draft_duration", ""), InputType.TYPE_CLASS_NUMBER);
        EditText rounds = PremiumUi.input(this, "Например: 6",
                diary.getString("draft_rounds", ""), InputType.TYPE_CLASS_NUMBER);
        bindDraft(duration, "draft_duration");
        bindDraft(rounds, "draft_rounds");
        basics.addView(fieldLabel("Продолжительность, минут"));
        basics.addView(duration);
        basics.addView(fieldLabel("Схваток или раундов"));
        basics.addView(rounds);

        EditText partners = PremiumUi.multiline(this,
                "Имена, пояс или короткое описание партнёров",
                diary.getString("draft_partners", ""));
        bindDraft(partners, "draft_partners");
        basics.addView(fieldLabel("С кем боролся"));
        basics.addView(voiceRow(partners));

        basics.addView(fieldLabel("Результат"));
        Spinner result = spinner(RESULTS, diary.getString("draft_result", RESULTS[0]));
        bindSpinnerDraft(result, "draft_result");
        basics.addView(result);
        basics.addView(PremiumUi.small(this,
                "Победа и поражение нужны главным образом для соревнований. На обычной тренировке результат можно не отмечать."));

        LinearLayout focus = PremiumUi.hero(this);
        page.addView(focus);
        focus.addView(PremiumUi.heroEyebrow(this, "Текущий фокус"));
        String mission = currentMission();
        focus.addView(PremiumUi.heroTitle(this,
                mission.isEmpty() ? "Фокус на 30 дней не выбран" : mission));
        focus.addView(PremiumUi.heroBody(this,
                "Счётчики из этой записи автоматически добавятся к общей статистике техники."));

        LinearLayout counts = PremiumUi.card(this);
        page.addView(counts);
        counts.addView(PremiumUi.cardTitle(this, "Что успел попробовать"));
        EditText attempts = PremiumUi.input(this, "0", diary.getString("draft_attempts", "0"),
                InputType.TYPE_CLASS_NUMBER);
        EditText successes = PremiumUi.input(this, "0", diary.getString("draft_successes", "0"),
                InputType.TYPE_CLASS_NUMBER);
        EditText finishes = PremiumUi.input(this, "0", diary.getString("draft_finishes", "0"),
                InputType.TYPE_CLASS_NUMBER);
        bindDraft(attempts, "draft_attempts");
        bindDraft(successes, "draft_successes");
        bindDraft(finishes, "draft_finishes");
        counts.addView(fieldLabel("Осознанных попыток"));
        counts.addView(attempts);
        counts.addView(fieldLabel("Успешных применений"));
        counts.addView(successes);
        counts.addView(fieldLabel("Финишей или чистых завершений"));
        counts.addView(finishes);

        LinearLayout reflection = PremiumUi.card(this);
        page.addView(reflection);
        reflection.addView(PremiumUi.cardTitle(this, "Главное с ковра"));
        EditText techniques = PremiumUi.multiline(this,
                "Какие техники и позиции применял?",
                diary.getString("draft_techniques", ""));
        EditText breakdown = PremiumUi.multiline(this,
                "Где остановилось действие или потерялась позиция?",
                diary.getString("draft_breakdown", ""));
        EditText coach = PremiumUi.multiline(this,
                "Что сказал тренер?",
                diary.getString("draft_coach", ""));
        EditText next = PremiumUi.multiline(this,
                "Что проверить в следующий раз?",
                diary.getString("draft_next", ""));
        bindDraft(techniques, "draft_techniques");
        bindDraft(breakdown, "draft_breakdown");
        bindDraft(coach, "draft_coach");
        bindDraft(next, "draft_next");
        reflection.addView(fieldLabel("Техники и позиции"));
        reflection.addView(voiceRow(techniques));
        reflection.addView(fieldLabel("Где возникла проблема"));
        reflection.addView(voiceRow(breakdown));
        reflection.addView(fieldLabel("Комментарий тренера"));
        reflection.addView(voiceRow(coach));
        reflection.addView(fieldLabel("Следующий фокус"));
        reflection.addView(voiceRow(next));

        Button save = PremiumUi.primaryButton(this, "Сохранить");
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
        if (successCount > attemptCount && attemptCount > 0) {
            toast("Успешных применений не может быть больше попыток.");
            successes.requestFocus();
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
            restoreSaveButton(save);
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

        boolean saved;
        try {
            saved = diary.edit()
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
        } catch (RuntimeException error) {
            saved = false;
        }

        if (!saved) {
            restoreSaveButton(save);
            toast("Не удалось сохранить запись. Текст остался на экране.");
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
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← К дневнику");
        back.setOnClickListener(v -> showHome());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "История"));
        page.addView(PremiumUi.title(this, "Что происходило на ковре"));

        JSONArray entries = loadEntries();
        if (entries.length() == 0) {
            LinearLayout empty = PremiumUi.card(this);
            empty.addView(PremiumUi.cardTitle(this, "Записей пока нет"));
            empty.addView(PremiumUi.body(this, "После тренировки создай первую короткую запись."));
            page.addView(empty);
        } else {
            int limit = Math.min(entries.length(), 40);
            for (int i = 0; i < limit; i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry == null) continue;
                LinearLayout item = PremiumUi.card(this);
                item.addView(PremiumUi.eyebrow(this, formatDate(entry.optLong("timestamp"))));
                item.addView(PremiumUi.cardTitle(this,
                        entry.optString("sport", "Борьба") + " · " + entry.optString("type", "Тренировка")));
                String summary = entrySummary(entry);
                if (!summary.isEmpty()) item.addView(PremiumUi.body(this, summary));
                String next = entry.optString("next_focus", "").trim();
                if (!next.isEmpty()) {
                    LinearLayout nextCard = PremiumUi.softCard(this);
                    nextCard.addView(PremiumUi.accentText(this, "Следующий фокус"));
                    nextCard.addView(PremiumUi.bodyDark(this, next));
                    item.addView(nextCard);
                }
                Button open = PremiumUi.secondaryButton(this, "Открыть запись");
                open.setOnClickListener(v -> showEntryDetails(entry));
                item.addView(open);
                page.addView(item);
            }
        }
        setContentView(scroll);
    }

    private void showEntryDetails(JSONObject entry) {
        LinearLayout content = PremiumUi.vertical(this, 8);
        content.setPadding(PremiumUi.dp(this, 8), PremiumUi.dp(this, 6),
                PremiumUi.dp(this, 8), PremiumUi.dp(this, 6));
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
        addDetail(content, "Фокус на 30 дней", entry.optString("mission"));
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

    private String buildNextTask(String mission, int attempts, int successes,
                                 String breakdown, String coach, String userNext) {
        if (!userNext.isEmpty()) return userNext;
        if (!coach.isEmpty()) return "Проверить совет тренера: " + coach;
        String target = mission.isEmpty() ? "текущий этап плана" : mission;
        if (attempts == 0) return "Сделать хотя бы одну осознанную попытку: " + target + ".";
        if (successes == 0) {
            if (!breakdown.isEmpty()) return "Оставить тот же фокус и проверить только один момент: " + breakdown + ".";
            return "Повторить " + target + " и заметить точку, где действие останавливается.";
        }
        if (successes >= 2) return "Повторить " + target + " и связать успешное действие со следующим этапом плана.";
        if (!breakdown.isEmpty()) return "Повторить успешный момент и отдельно проверить: " + breakdown + ".";
        return "Ещё раз применить " + target + " и сохранить ключевой контроль после выполнения.";
    }

    private String nextTask(String sport, String mission) {
        String saved = diary.getString("next_task_" + SportGuidance.slug(sport), "").trim();
        if (!saved.isEmpty()) return saved;
        if (!mission.isEmpty()) return "Сделай одну осознанную попытку текущей техники и запомни, где действие получилось или остановилось.";
        return "Выбери один этап своего плана борьбы и попробуй его хотя бы один раз.";
    }

    private void updateMissionCounters(int attempts, int successes, int finishes) {
        if (attempts == 0 && successes == 0 && finishes == 0) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDay = profile.getString("mission_last_day", "");
        int activeDays = profile.getInt("mission_active_days", 0);
        if (!today.equals(lastDay)) activeDays++;
        profile.edit()
                .putInt("mission_attempts", profile.getInt("mission_attempts", 0) + attempts)
                .putInt("mission_successes", profile.getInt("mission_successes", 0) + successes)
                .putInt("mission_finishes", profile.getInt("mission_finishes", 0) + finishes)
                .putInt("mission_active_days", activeDays)
                .putString("mission_last_day", today)
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
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (JSONException error) { return new JSONArray(); }
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
            voicePrefix = text(target);
            voiceLanguage = safeLanguage;
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

    private void bindSpinnerDraft(Spinner spinner, String key) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                diary.edit().putString(key, item == null ? "" : String.valueOf(item)).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private TextView fieldLabel(String value) {
        TextView label = PremiumUi.accentText(this, value);
        label.setPadding(0, PremiumUi.dp(this, 4), 0, 0);
        return label;
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

    private void restoreSaveButton(Button save) {
        save.setEnabled(true);
        save.setText("Сохранить");
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
