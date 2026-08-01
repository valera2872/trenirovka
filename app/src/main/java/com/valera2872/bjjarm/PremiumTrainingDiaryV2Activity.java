package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.text.TextUtils;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 0.9.7 diary: fast entry, detailed entry, editing and deletion.
 * Keeps the entries_json format used since 0.9.0.
 */
public class PremiumTrainingDiaryV2Activity extends Activity {
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String ENTRIES_KEY = "entries_json";
    private static final int MAX_ENTRIES = 500;
    private static final int REQUEST_VOICE = 9701;

    private static final String[] SESSION_TYPES = {
            "Тренировка", "Открытая тренировка", "Индивидуальная работа", "Соревнование"
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
    private EditText pendingVoiceTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        PremiumUi.applyWindow(this);
        ensureCounterBaseline(loadEntries());
        showHome();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VOICE) return;
        EditText target = pendingVoiceTarget;
        pendingVoiceTarget = null;
        if (target == null || resultCode != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String spoken = results.get(0) == null ? "" : results.get(0).trim();
        if (spoken.isEmpty()) return;
        String old = target.getText().toString().trim();
        String value = old.isEmpty() ? spoken : old + " " + spoken;
        target.setText(value);
        target.setSelection(value.length());
    }

    private void showHome() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Дневник тренировок"));
        page.addView(PremiumUi.title(this, "Запиши только полезное"));
        page.addView(PremiumUi.body(this,
                "Быстрая запись занимает около минуты. Подробный режим оставлен для соревнований, сложных тренировок и комментариев тренера."));

        String sport = activeSport();
        String mission = currentMission();
        LinearLayout hero = PremiumUi.hero(this);
        page.addView(hero);
        hero.addView(PremiumUi.heroEyebrow(this, "Следующая задача"));
        hero.addView(PremiumUi.heroTitle(this, nextTask(sport, mission)));
        hero.addView(PremiumUi.heroBody(this, sport));
        if (!mission.isEmpty()) hero.addView(PremiumUi.heroBody(this, "Фокус на 30 дней: " + mission));

        Button quick = PremiumUi.lightButton(this, "Быстрая запись · около минуты");
        quick.setOnClickListener(v -> showEditor(null, false));
        hero.addView(quick);

        Button detailed = PremiumUi.secondaryButton(this, "Подробная запись");
        detailed.setOnClickListener(v -> showEditor(null, true));
        page.addView(detailed);

        JSONArray entries = loadEntries();
        LinearLayout history = PremiumUi.card(this);
        page.addView(history);
        history.addView(PremiumUi.cardTitle(this, "История"));
        if (entries.length() == 0) {
            history.addView(PremiumUi.body(this, "Записей пока нет."));
        } else {
            history.addView(PremiumUi.accentText(this,
                    entries.length() + " " + entryWord(entries.length()) + " сохранено"));
            JSONObject last = entries.optJSONObject(0);
            if (last != null) history.addView(PremiumUi.small(this,
                    "Последняя: " + formatDate(last.optLong("timestamp"))));
            String recurring = recurringProblem(entries, sport);
            if (!recurring.isEmpty()) {
                LinearLayout repeat = PremiumUi.softCard(this);
                repeat.addView(PremiumUi.sectionTitle(this, "Повторяющаяся проблема"));
                repeat.addView(PremiumUi.bodyDark(this, recurring));
                history.addView(repeat);
            }
        }
        Button open = PremiumUi.primaryButton(this, "Открыть историю");
        open.setOnClickListener(v -> showHistory());
        history.addView(open);
        setContentView(scroll);
    }

    private void showEditor(JSONObject source, boolean detailed) {
        boolean editing = source != null;
        JSONObject entry = source == null ? new JSONObject() : source;
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, editing ? "← К записи" : "← К дневнику");
        back.setOnClickListener(v -> { if (editing) showHistory(); else showHome(); });
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this,
                editing ? "Редактирование" : detailed ? "Подробная запись" : "Быстрая запись"));

        String sport = editing ? entry.optString("sport", activeSport()) : activeSport();
        String mission = editing ? entry.optString("mission", currentMission()) : currentMission();
        page.addView(PremiumUi.title(this, sport));
        page.addView(PremiumUi.body(this,
                detailed
                        ? "Заполни только те поля, которые помогут следующей тренировке."
                        : "Шесть коротких пунктов. Остальные детали можно добавить позднее через редактирование."));

        LinearLayout focus = PremiumUi.softCard(this);
        focus.addView(PremiumUi.accentText(this, "Фокус этой записи"));
        focus.addView(PremiumUi.bodyDark(this,
                mission.isEmpty() ? "Фокус на 30 дней не выбран" : mission));
        page.addView(focus);

        Form form = new Form();
        form.type = spinner(SESSION_TYPES, entry.optString("type", SESSION_TYPES[0]));
        form.result = spinner(RESULTS, entry.optString("result", RESULTS[0]));
        form.duration = numberInput("Минут", entry.optInt("duration", 0));
        form.rounds = numberInput("Количество", entry.optInt("rounds", 0));
        form.attempts = numberInput("0", entry.optInt("attempts", 0));
        form.successes = numberInput("0", entry.optInt("successes", 0));
        form.finishes = numberInput("0", entry.optInt("finishes", 0));
        form.partners = PremiumUi.multiline(this, "Имена или короткое описание партнёров",
                entry.optString("partners", ""));
        form.techniques = PremiumUi.multiline(this, "Что пробовал или отрабатывал?",
                entry.optString("techniques", ""));
        form.breakdown = PremiumUi.multiline(this, "Где остановилось действие?",
                entry.optString("breakdown", ""));
        form.coach = PremiumUi.multiline(this, "Что сказал тренер?",
                entry.optString("coach", ""));
        form.next = PremiumUi.multiline(this, "Что проверить в следующий раз?",
                entry.optString("next_focus", ""));

        LinearLayout base = PremiumUi.card(this);
        page.addView(base);
        base.addView(PremiumUi.cardTitle(this, "Тренировка"));
        if (detailed) {
            addField(base, "Формат", form.type);
            addField(base, "Продолжительность, минут", form.duration);
        }
        addField(base, "Схваток или раундов", form.rounds);
        addVoiceField(base, "С кем боролся", form.partners);
        if (detailed) {
            addField(base, "Результат", form.result);
            base.addView(PremiumUi.small(this,
                    "На обычной тренировке победу или поражение отмечать необязательно."));
        }

        LinearLayout attemptsCard = PremiumUi.card(this);
        page.addView(attemptsCard);
        attemptsCard.addView(PremiumUi.cardTitle(this, "Текущая техника"));
        addField(attemptsCard, "Осознанных попыток", form.attempts);
        addField(attemptsCard, "Успешных применений", form.successes);
        if (detailed) addField(attemptsCard, "Финишей или чистых завершений", form.finishes);

        LinearLayout reflection = PremiumUi.card(this);
        page.addView(reflection);
        reflection.addView(PremiumUi.cardTitle(this, "Главное с тренировки"));
        addVoiceField(reflection, "Что пробовал", form.techniques);
        addVoiceField(reflection, "Главная проблема", form.breakdown);
        if (detailed) addVoiceField(reflection, "Комментарий тренера", form.coach);
        addVoiceField(reflection, "Следующий фокус", form.next);

        Button save = PremiumUi.primaryButton(this, editing ? "Сохранить изменения" : "Сохранить запись");
        long editingId = editing ? entry.optLong("id", entry.optLong("timestamp")) : 0L;
        save.setOnClickListener(v -> saveEntry(save, form, sport, mission, editingId,
                editing ? entry.optLong("timestamp", System.currentTimeMillis()) : System.currentTimeMillis(),
                detailed));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveEntry(Button save, Form form, String sport, String mission,
                           long editingId, long timestamp, boolean detailed) {
        int attempts = intValue(form.attempts);
        int successes = intValue(form.successes);
        int finishes = detailed ? intValue(form.finishes) : 0;
        String partners = text(form.partners);
        String techniques = text(form.techniques);
        String breakdown = text(form.breakdown);
        String coach = detailed ? text(form.coach) : "";
        String userNext = text(form.next);

        if (successes > attempts && attempts > 0) {
            toast("Успешных применений не может быть больше попыток.");
            return;
        }
        if (partners.isEmpty() && techniques.isEmpty() && breakdown.isEmpty()
                && coach.isEmpty() && userNext.isEmpty() && attempts == 0) {
            toast("Запиши хотя бы один полезный факт.");
            return;
        }

        save.setEnabled(false);
        save.setText("Сохраняю…");
        long id = editingId > 0 ? editingId : System.currentTimeMillis();
        String generatedNext = buildNextTask(mission, attempts, successes, breakdown, coach, userNext);
        JSONObject updated = new JSONObject();
        try {
            updated.put("id", id);
            updated.put("timestamp", timestamp);
            updated.put("sport", sport);
            updated.put("type", detailed ? selected(form.type) : SESSION_TYPES[0]);
            updated.put("duration", detailed ? intValue(form.duration) : 0);
            updated.put("rounds", intValue(form.rounds));
            updated.put("partners", partners);
            updated.put("result", detailed ? selected(form.result) : RESULTS[0]);
            updated.put("mission", mission);
            updated.put("attempts", attempts);
            updated.put("successes", successes);
            updated.put("finishes", finishes);
            updated.put("techniques", techniques);
            updated.put("breakdown", breakdown);
            updated.put("coach", coach);
            updated.put("next_focus", generatedNext);
            updated.put("entry_mode", detailed ? "detailed" : "quick");
        } catch (JSONException error) {
            restore(save, editingId > 0);
            toast("Не удалось подготовить запись.");
            return;
        }

        JSONArray old = loadEntries();
        JSONArray next = new JSONArray();
        boolean replaced = false;
        if (editingId <= 0) next.put(updated);
        for (int i = 0; i < old.length() && next.length() < MAX_ENTRIES; i++) {
            JSONObject item = old.optJSONObject(i);
            if (item == null) continue;
            long itemId = item.optLong("id", item.optLong("timestamp"));
            if (editingId > 0 && itemId == editingId) {
                next.put(updated);
                replaced = true;
            } else {
                next.put(item);
            }
        }
        if (editingId > 0 && !replaced) next.put(updated);

        if (!saveEntries(next)) {
            restore(save, editingId > 0);
            toast("Не удалось сохранить запись.");
            return;
        }
        rebuildMissionCounters(next);
        rebuildNextTasks(next);
        new AlertDialog.Builder(this)
                .setTitle(editingId > 0 ? "Изменения сохранены" : "Запись сохранена")
                .setMessage("Следующая задача:\n\n" + generatedNext)
                .setCancelable(false)
                .setPositiveButton("Готово", (d, w) -> showHome())
                .show();
    }

    private void showHistory() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);
        Button back = PremiumUi.outlineButton(this, "← К дневнику");
        back.setOnClickListener(v -> showHome());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "История"));
        page.addView(PremiumUi.title(this, "Тренировки и выводы"));

        JSONArray entries = loadEntries();
        if (entries.length() == 0) {
            LinearLayout empty = PremiumUi.card(this);
            empty.addView(PremiumUi.cardTitle(this, "Записей пока нет"));
            empty.addView(PremiumUi.body(this, "Создай первую быструю запись после тренировки."));
            page.addView(empty);
        } else {
            int limit = Math.min(entries.length(), 80);
            for (int i = 0; i < limit; i++) {
                JSONObject item = entries.optJSONObject(i);
                if (item == null) continue;
                page.addView(historyCard(item));
            }
        }
        setContentView(scroll);
    }

    private LinearLayout historyCard(JSONObject entry) {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, formatDate(entry.optLong("timestamp"))));
        card.addView(PremiumUi.cardTitle(this,
                entry.optString("sport", "Борьба") + " · " + entry.optString("type", "Тренировка")));
        String summary = summary(entry);
        if (!summary.isEmpty()) card.addView(PremiumUi.body(this, summary));
        String problem = entry.optString("breakdown", "").trim();
        if (!problem.isEmpty()) {
            LinearLayout box = PremiumUi.softCard(this);
            box.addView(PremiumUi.accentText(this, "Где возникла проблема"));
            box.addView(PremiumUi.bodyDark(this, problem));
            card.addView(box);
        }
        String next = entry.optString("next_focus", "").trim();
        if (!next.isEmpty()) card.addView(PremiumUi.small(this, "Дальше: " + next));

        Button open = PremiumUi.secondaryButton(this, "Открыть запись");
        open.setOnClickListener(v -> showDetails(entry));
        card.addView(open);
        Button edit = PremiumUi.outlineButton(this, "Редактировать");
        edit.setOnClickListener(v -> showEditor(copy(entry), true));
        card.addView(edit);
        Button delete = PremiumUi.dangerButton(this, "Удалить запись");
        delete.setOnClickListener(v -> confirmDelete(entry));
        card.addView(delete);
        return card;
    }

    private void showDetails(JSONObject entry) {
        LinearLayout content = PremiumUi.vertical(this, 8);
        content.setPadding(PremiumUi.dp(this, 8), PremiumUi.dp(this, 6),
                PremiumUi.dp(this, 8), PremiumUi.dp(this, 6));
        addDetail(content, "Дата", formatDate(entry.optLong("timestamp")));
        addDetail(content, "Вид борьбы", entry.optString("sport"));
        addDetail(content, "Формат", entry.optString("type"));
        if (entry.optInt("duration") > 0) addDetail(content, "Продолжительность", entry.optInt("duration") + " мин");
        if (entry.optInt("rounds") > 0) addDetail(content, "Схваток или раундов", String.valueOf(entry.optInt("rounds")));
        addDetail(content, "Партнёры", entry.optString("partners"));
        String result = entry.optString("result", RESULTS[0]);
        if (!RESULTS[0].equals(result)) addDetail(content, "Результат", result);
        addDetail(content, "Фокус на 30 дней", entry.optString("mission"));
        addDetail(content, "Попытки / удалось / финиши",
                entry.optInt("attempts") + " / " + entry.optInt("successes") + " / " + entry.optInt("finishes"));
        addDetail(content, "Техники и позиции", entry.optString("techniques"));
        addDetail(content, "Где возникла проблема", entry.optString("breakdown"));
        addDetail(content, "Комментарий тренера", entry.optString("coach"));
        addDetail(content, "Следующая задача", entry.optString("next_focus"));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this)
                .setTitle("Запись тренировки")
                .setView(scroll)
                .setNegativeButton("Закрыть", null)
                .setPositiveButton("Редактировать", (d, w) -> showEditor(copy(entry), true))
                .show();
    }

    private void confirmDelete(JSONObject entry) {
        String date = formatDate(entry.optLong("timestamp"));
        new AlertDialog.Builder(this)
                .setTitle("Удалить запись?")
                .setMessage("Запись от " + date + " будет удалена. Счётчики техники пересчитаются автоматически.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> deleteEntry(entry))
                .show();
    }

    private void deleteEntry(JSONObject entry) {
        long id = entry.optLong("id", entry.optLong("timestamp"));
        JSONArray old = loadEntries();
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item == null) continue;
            long itemId = item.optLong("id", item.optLong("timestamp"));
            if (itemId != id) next.put(item);
        }
        if (!saveEntries(next)) {
            toast("Не удалось удалить запись.");
            return;
        }
        rebuildMissionCounters(next);
        rebuildNextTasks(next);
        toast("Запись удалена.");
        showHistory();
    }

    private boolean saveEntries(JSONArray entries) {
        try {
            return diary.edit().putString(ENTRIES_KEY, entries.toString()).commit();
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void ensureCounterBaseline(JSONArray entries) {
        String key = baselineKey();
        if (diary.getBoolean(key + "_ready", false)) return;
        Totals totals = totalsForCurrentMission(entries);
        int baseAttempts = Math.max(0, profile.getInt("mission_attempts", 0) - totals.attempts);
        int baseSuccesses = Math.max(0, profile.getInt("mission_successes", 0) - totals.successes);
        int baseFinishes = Math.max(0, profile.getInt("mission_finishes", 0) - totals.finishes);
        int baseDays = Math.max(0, profile.getInt("mission_active_days", 0) - totals.days.size());
        diary.edit()
                .putBoolean(key + "_ready", true)
                .putInt(key + "_attempts", baseAttempts)
                .putInt(key + "_successes", baseSuccesses)
                .putInt(key + "_finishes", baseFinishes)
                .putInt(key + "_days", baseDays)
                .apply();
    }

    private void rebuildMissionCounters(JSONArray entries) {
        ensureCounterBaseline(entries);
        String key = baselineKey();
        Totals totals = totalsForCurrentMission(entries);
        int attempts = diary.getInt(key + "_attempts", 0) + totals.attempts;
        int successes = diary.getInt(key + "_successes", 0) + totals.successes;
        int finishes = diary.getInt(key + "_finishes", 0) + totals.finishes;
        int activeDays = diary.getInt(key + "_days", 0) + totals.days.size();
        SharedPreferences.Editor edit = profile.edit()
                .putInt("mission_attempts", Math.max(0, attempts))
                .putInt("mission_successes", Math.max(0, successes))
                .putInt("mission_finishes", Math.max(0, finishes))
                .putInt("mission_active_days", Math.max(0, activeDays));
        if (totals.latestDay != null) edit.putString("mission_last_day", totals.latestDay);
        edit.apply();
    }

    private Totals totalsForCurrentMission(JSONArray entries) {
        Totals totals = new Totals();
        String mission = currentMission();
        long started = profile.getLong("mission_started_at", 0L);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null) continue;
            long timestamp = item.optLong("timestamp", 0L);
            if (started > 0 && timestamp < started) continue;
            if (!mission.equals(item.optString("mission", ""))) continue;
            totals.attempts += Math.max(0, item.optInt("attempts"));
            totals.successes += Math.max(0, item.optInt("successes"));
            totals.finishes += Math.max(0, item.optInt("finishes"));
            String day = dayKey(timestamp);
            totals.days.add(day);
            if (totals.latestDay == null || day.compareTo(totals.latestDay) > 0) totals.latestDay = day;
        }
        return totals;
    }

    private void rebuildNextTasks(JSONArray entries) {
        Set<String> cleared = new HashSet<>();
        String[] sports = {"grappling", "bjj", "judo", "freestyle", "greco"};
        SharedPreferences.Editor edit = diary.edit();
        for (String slug : sports) {
            edit.remove("next_task_" + slug);
            cleared.add(slug);
        }
        Set<String> filled = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null) continue;
            String slug = SportGuidance.slug(item.optString("sport", ""));
            if (filled.contains(slug)) continue;
            String next = item.optString("next_focus", "").trim();
            if (!next.isEmpty()) {
                edit.putString("next_task_" + slug, next);
                filled.add(slug);
            }
        }
        edit.apply();
    }

    private String baselineKey() {
        return "v097_base_" + profile.getLong("mission_started_at", 0L);
    }

    private String buildNextTask(String mission, int attempts, int successes,
                                 String breakdown, String coach, String userNext) {
        if (!userNext.isEmpty()) return userNext;
        if (!coach.isEmpty()) return "Проверить совет тренера: " + coach;
        String target = mission.isEmpty() ? "текущий этап плана" : mission;
        if (attempts == 0) return "Сделать хотя бы одну осознанную попытку: " + target + ".";
        if (successes == 0) {
            if (!breakdown.isEmpty()) return "Оставить тот же фокус и проверить один момент: " + breakdown + ".";
            return "Повторить " + target + " и заметить место, где действие останавливается.";
        }
        if (successes >= 2) return "Повторить " + target + " и связать его со следующим этапом плана.";
        if (!breakdown.isEmpty()) return "Повторить успешный момент и отдельно проверить: " + breakdown + ".";
        return "Ещё раз применить " + target + " и сохранить контроль после выполнения.";
    }

    private String nextTask(String sport, String mission) {
        String saved = diary.getString("next_task_" + SportGuidance.slug(sport), "").trim();
        if (!saved.isEmpty()) return saved;
        if (!mission.isEmpty()) return "Сделай одну осознанную попытку текущей техники и запомни, где она остановилась.";
        return "Выбери один этап плана борьбы и попробуй его хотя бы один раз.";
    }

    private LinearLayout voiceRow(EditText field) {
        LinearLayout box = PremiumUi.vertical(this, 7);
        box.addView(field, PremiumUi.fullWidthWrap());
        Button voice = PremiumUi.secondaryButton(this, "🎙 Надиктовать по-русски");
        voice.setOnClickListener(v -> launchVoice(field));
        box.addView(voice);
        return box;
    }

    private void launchVoice(EditText target) {
        pendingVoiceTarget = target;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Расскажите коротко о тренировке");
        try {
            startActivityForResult(intent, REQUEST_VOICE);
        } catch (ActivityNotFoundException error) {
            pendingVoiceTarget = null;
            toast("На телефоне не найден сервис распознавания речи.");
        } catch (RuntimeException error) {
            pendingVoiceTarget = null;
            toast("Не удалось открыть голосовой ввод.");
        }
    }

    private void addField(LinearLayout parent, String label, View field) {
        parent.addView(PremiumUi.accentText(this, label));
        parent.addView(field);
    }

    private void addVoiceField(LinearLayout parent, String label, EditText field) {
        parent.addView(PremiumUi.accentText(this, label));
        parent.addView(voiceRow(field));
    }

    private void addDetail(LinearLayout parent, String title, String value) {
        if (value == null || value.trim().isEmpty()) return;
        parent.addView(PremiumUi.accentText(this, title));
        parent.addView(PremiumUi.bodyDark(this, value.trim()));
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

    private EditText numberInput(String hint, int value) {
        return PremiumUi.input(this, hint, value == 0 ? "" : String.valueOf(value), InputType.TYPE_CLASS_NUMBER);
    }

    private String selected(Spinner spinner) {
        return spinner == null || spinner.getSelectedItem() == null ? "" : spinner.getSelectedItem().toString();
    }

    private int intValue(EditText field) {
        try { return Math.max(0, Integer.parseInt(text(field))); }
        catch (Exception error) { return 0; }
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
    }

    private void restore(Button button, boolean editing) {
        button.setEnabled(true);
        button.setText(editing ? "Сохранить изменения" : "Сохранить запись");
    }

    private JSONArray loadEntries() {
        try { return new JSONArray(diary.getString(ENTRIES_KEY, "[]")); }
        catch (JSONException error) { return new JSONArray(); }
    }

    private JSONObject copy(JSONObject item) {
        try { return new JSONObject(item.toString()); }
        catch (JSONException error) { return item; }
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

    private String summary(JSONObject entry) {
        ArrayList<String> parts = new ArrayList<>();
        if (entry.optInt("rounds") > 0) parts.add(entry.optInt("rounds") + " раундов");
        if (entry.optInt("attempts") > 0 || entry.optInt("successes") > 0) {
            parts.add("попытки " + entry.optInt("attempts") + ", удалось " + entry.optInt("successes"));
        }
        String partners = entry.optString("partners", "").trim();
        if (!partners.isEmpty()) parts.add("с кем: " + partners);
        String techniques = entry.optString("techniques", "").trim();
        if (!techniques.isEmpty()) parts.add(techniques);
        return TextUtils.join(" · ", parts);
    }

    private String recurringProblem(JSONArray entries, String sport) {
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < entries.length() && i < 10; i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null || !sport.equals(item.optString("sport"))) continue;
            String value = item.optString("breakdown", "").trim();
            if (!value.isEmpty()) values.add(value);
        }
        if (values.size() < 2) return "";
        String first = values.get(0).toLowerCase(Locale.ROOT);
        for (int i = 1; i < values.size(); i++) {
            String current = values.get(i).toLowerCase(Locale.ROOT);
            if (first.equals(current) || sharedWord(first, current)) return values.get(0);
        }
        return "";
    }

    private boolean sharedWord(String a, String b) {
        for (String word : a.split("\\s+")) if (word.length() >= 5 && b.contains(word)) return true;
        return false;
    }

    private String dayKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(timestamp));
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Дата не указана";
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private String entryWord(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod10 == 1 && mod100 != 11) return "запись";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "записи";
        return "записей";
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private static final class Form {
        Spinner type;
        Spinner result;
        EditText duration;
        EditText rounds;
        EditText partners;
        EditText attempts;
        EditText successes;
        EditText finishes;
        EditText techniques;
        EditText breakdown;
        EditText coach;
        EditText next;
    }

    private static final class Totals {
        int attempts;
        int successes;
        int finishes;
        final Set<String> days = new HashSet<>();
        String latestDay;
    }
}
