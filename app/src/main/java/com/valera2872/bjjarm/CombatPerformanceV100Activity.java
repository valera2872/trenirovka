package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/** Standalone 0.10.0 premium dashboard. Reads existing 0.9.x data without migration. */
public class CombatPerformanceV100Activity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String ROUTINE_PREFS = "combat_personal_routine";

    private SharedPreferences profile;
    private SharedPreferences system;
    private SharedPreferences diary;
    private SharedPreferences routine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        routine = getSharedPreferences(ROUTINE_PREFS, MODE_PRIVATE);
        DarkUi.apply(this);
        if (!profile.getBoolean("profile_complete", false)) {
            startActivity(new Intent(this, CombatPerformanceV093Activity.class));
            finish();
            return;
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profile != null && profile.getBoolean("profile_complete", false)) render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DarkUi.BG);

        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this, 16), DarkUi.dp(this, 18), DarkUi.dp(this, 16), DarkUi.dp(this, 24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        page.addView(identity());
        page.addView(todayCard());
        page.addView(nextTrainingCard());
        page.addView(quickAccess());
        page.addView(focusCard());
        page.addView(strengthPreview());
        page.addView(lastDiaryCard());

        root.addView(bottomNav("Сегодня"), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, DarkUi.dp(this, 68)));
        setContentView(root);
    }

    private View identity() {
        LinearLayout row = DarkUi.h(this, 12);
        TextView avatar = DarkUi.text(this, firstLetter(profile.getString("name", "С")), 19,
                DarkUi.BG, android.graphics.Typeface.DEFAULT_BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(DarkUi.round(DarkUi.GOLD, 999, 0, Color.TRANSPARENT));
        row.addView(avatar, new LinearLayout.LayoutParams(DarkUi.dp(this, 48), DarkUi.dp(this, 48)));

        LinearLayout copy = DarkUi.v(this, 3);
        copy.addView(DarkUi.h1(this, "Привет, " + profile.getString("name", "спортсмен") + "!"));
        copy.addView(DarkUi.gold(this, "Фокус · " + shortMission()));
        copy.addView(DarkUi.small(this, activeSport() + " · " + profile.getInt("mission_active_days", 0) + " активных дней"));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView version = DarkUi.chip(this, "0.10", DarkUi.GOLD);
        row.addView(version);
        return row;
    }

    private View todayCard() {
        TodayTask t = todayTask();
        LinearLayout card = DarkUi.hero(this);
        card.addView(DarkUi.small(this, "СЕГОДНЯШНЯЯ ЗАДАЧА"));
        TextView title = DarkUi.title(this, t.title);
        title.setTextSize(23);
        card.addView(title);
        card.addView(DarkUi.bodyWhite(this, t.details));
        android.widget.Button action = DarkUi.primary(this, t.action);
        action.setOnClickListener(v -> runToday(t.kind));
        card.addView(action);
        return card;
    }

    private View nextTrainingCard() {
        LinearLayout card = DarkUi.card(this);
        LinearLayout top = DarkUi.h(this, 8);
        LinearLayout copy = DarkUi.v(this, 4);
        copy.addView(DarkUi.small(this, "БЛИЖАЙШАЯ ТРЕНИРОВКА"));
        WeekPlanEngine.Task t = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        String label = "mat".equals(t.kind) || "heavy".equals(t.kind) ? activeSport() : cleanTaskTitle(t.kind);
        copy.addView(DarkUi.h2(this, label));
        copy.addView(DarkUi.small(this, t.details == null ? "Открой недельный план" : t.details));
        top.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView calendar = DarkUi.chip(this, "Неделя ›", DarkUi.GOLD);
        calendar.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class)));
        top.addView(calendar);
        card.addView(top);
        return card;
    }

    private View quickAccess() {
        LinearLayout wrap = DarkUi.v(this, 9);
        wrap.addView(DarkUi.small(this, "БЫСТРЫЙ ДОСТУП"));
        LinearLayout r1 = DarkUi.h(this, 9);
        r1.addView(actionTile("▣", "Дневник", "Записать тренировку", DarkUi.GOLD,
                v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r1.addView(actionTile("⌁", "Мой план", "Техника и решения", DarkUi.ORANGE,
                v -> startActivity(new Intent(this, PremiumFightingPlanActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        wrap.addView(r1);
        LinearLayout r2 = DarkUi.h(this, 9);
        r2.addView(actionTile("▦", "Неделя", "Нагрузка и ковёр", Color.rgb(91, 153, 255),
                v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r2.addView(actionTile("H", "Силовая", "Персональная работа", DarkUi.GREEN,
                v -> startActivity(new Intent(this, PremiumStrengthHubActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        wrap.addView(r2);
        return wrap;
    }

    private View actionTile(String symbol, String title, String subtitle, int accent, View.OnClickListener click) {
        LinearLayout card = DarkUi.card(this);
        card.setMinHeight(DarkUi.dp(this, 106));
        card.setOnClickListener(click);
        card.setClickable(true);
        TextView icon = DarkUi.text(this, symbol, 18, accent, android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(icon);
        card.addView(DarkUi.h2(this, title));
        card.addView(DarkUi.small(this, subtitle));
        return card;
    }

    private View focusCard() {
        LinearLayout card = DarkUi.goldCard(this);
        card.addView(DarkUi.gold(this, "ФОКУС НА 30 ДНЕЙ"));
        card.addView(DarkUi.h1(this, profile.getString("mission", "Фокус не выбран")));
        LinearLayout stats = DarkUi.h(this, 8);
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_attempts", 0)), "Попыток", DarkUi.ORANGE),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_successes", 0)), "Успехов", DarkUi.GREEN),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, successPercent() + "%", "Успешность", DarkUi.GOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(stats);
        return card;
    }

    private View strengthPreview() {
        LinearLayout card = DarkUi.card(this);
        card.addView(DarkUi.small(this, "ПЕРСОНАЛЬНАЯ СИЛОВАЯ ПОДГОТОВКА"));
        card.addView(DarkUi.h1(this, "Сила для твоей борьбы"));
        card.addView(DarkUi.body(this,
                "Руки, хват, ноги и корпус — с учётом ковра, тяжёлых раундов и соревнований."));
        LinearLayout row = DarkUi.h(this, 8);
        row.addView(miniStrength("Руки и хват", profile.getString("priority_1", "Хват и тяга"), DarkUi.GOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(miniStrength("Ноги и корпус", profile.getString("priority_2", "Сила и устойчивость"), DarkUi.GREEN),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        android.widget.Button open = DarkUi.secondary(this, "Открыть силовую подготовку");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumStrengthHubActivity.class)));
        card.addView(open);
        return card;
    }

    private View miniStrength(String title, String subtitle, int accent) {
        LinearLayout l = DarkUi.v(this, 5);
        l.setPadding(DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12));
        l.setBackground(DarkUi.round(DarkUi.CARD_2, 15, 1, DarkUi.BORDER));
        l.addView(DarkUi.text(this, "●", 12, accent, android.graphics.Typeface.DEFAULT_BOLD));
        l.addView(DarkUi.h2(this, title));
        l.addView(DarkUi.small(this, subtitle));
        return l;
    }

    private View lastDiaryCard() {
        LinearLayout card = DarkUi.card(this);
        card.addView(DarkUi.small(this, "ПОСЛЕ ТРЕНИРОВКИ"));
        card.addView(DarkUi.h1(this, "Дневник"));
        String next = diary.getString("next_task_" + SportGuidance.slug(activeSport()), "").trim();
        if (next.isEmpty()) next = "После тренировки сохрани один успешный момент и одну проблему.";
        card.addView(DarkUi.bodyWhite(this, next));
        card.addView(DarkUi.small(this, diaryEntryCount() + " записей в истории"));
        android.widget.Button add = DarkUi.primary(this, "Быстрая запись");
        add.setOnClickListener(v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class)));
        card.addView(add);
        return card;
    }

    private View bottomNav(String selected) {
        LinearLayout nav = DarkUi.h(this, 0);
        nav.setPadding(DarkUi.dp(this, 8), 0, DarkUi.dp(this, 8), 0);
        nav.setBackgroundColor(DarkUi.BG_2);
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_today, "Сегодня", selected.equals("Сегодня"), v -> render()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_week, "Неделя", selected.equals("Неделя"), v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_diary, "Дневник", selected.equals("Дневник"), v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_profile, "Профиль", selected.equals("Профиль"), v -> startActivity(new Intent(this, PremiumProfileActivity.class))), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return nav;
    }

    private TodayTask todayTask() {
        if (!WeekPlanEngine.isConfigured(this)) return new TodayTask("week", "Настрой неделю", "Отметь обычные, тяжёлые и соревновательные дни, чтобы приложение распределяло дополнительную нагрузку.", "Настроить неделю");
        WeekPlanEngine.Task t = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        if ("arms".equals(t.kind)) return new TodayTask("arms", "Руки и хват", "Сегодня короткая дополнительная работа. Сначала оцени восстановление после ковра.", "Начать тренировку");
        if ("base".equals(t.kind)) return new TodayTask("base", "Ноги и корпус", "Силовая база, устойчивость и контроль корпуса без лишней нагрузки перед ковром.", "Начать тренировку");
        if ("mat".equals(t.kind)) return new TodayTask("diary", "Удержать контроль в своей цепочке", missionTask(), "Открыть задачу");
        if ("heavy".equals(t.kind)) return new TodayTask("diary", "Тяжёлые раунды", "Без дополнительной силовой. На ковре проверь текущий технический фокус и сохрани результат после тренировки.", "Открыть дневник");
        if ("competition".equals(t.kind)) return new TodayTask("routine", "Соревнование", "Без силовой нагрузки. Держи первое действие и личную настройку перед выходом.", "Перед выходом");
        return new TodayTask("week", "Восстановление", "Сегодня достаточно лёгкой подвижности и короткого разбора последней тренировки.", "Открыть неделю");
    }

    private String missionTask() {
        String next = diary.getString("next_task_" + SportGuidance.slug(activeSport()), "").trim();
        if (!next.isEmpty()) return next;
        String mission = profile.getString("mission", "").trim();
        return mission.isEmpty() ? "Выбери один этап своего плана и сделай хотя бы одну осознанную попытку." : "Сделай минимум 3 осознанные попытки: " + mission + ".";
    }

    private void runToday(String kind) {
        if ("arms".equals(kind)) startActivity(new Intent(this, GrapplingV5Activity.class));
        else if ("base".equals(kind)) startActivity(new Intent(this, BaseStrengthV3Activity.class));
        else if ("diary".equals(kind)) startActivity(new Intent(this, PremiumDarkDiaryActivity.class));
        else if ("routine".equals(kind)) startActivity(new Intent(this, PremiumRoutineActivity.class));
        else startActivity(new Intent(this, WeeklyPlanV4Activity.class));
    }

    private String activeSport() {
        String s = profile.getString("active_sport", "").trim();
        if (!s.isEmpty()) return s;
        String all = profile.getString("sports", "").trim();
        if (!all.isEmpty()) return all.split("\\|")[0].trim();
        return profile.getString("sport", "Грэпплинг / No-Gi");
    }

    private String shortMission() {
        String m = profile.getString("mission", "Техника не выбрана").trim();
        return m.length() > 34 ? m.substring(0, 34) + "…" : m;
    }

    private int successPercent() {
        int a = profile.getInt("mission_attempts", 0);
        int s = profile.getInt("mission_successes", 0);
        return a <= 0 ? 0 : Math.min(100, Math.round((s * 100f) / a));
    }

    private int diaryEntryCount() {
        try { return new JSONArray(diary.getString("entries_json", "[]")).length(); }
        catch (JSONException e) { return 0; }
    }

    private String cleanTaskTitle(String kind) {
        if ("arms".equals(kind)) return "Руки и хват";
        if ("base".equals(kind)) return "Ноги и корпус";
        if ("competition".equals(kind)) return "Соревнование";
        if ("rest".equals(kind)) return "Восстановление";
        return "План подготовки";
    }

    private String firstLetter(String value) {
        String s = value == null ? "" : value.trim();
        return s.isEmpty() ? "С" : s.substring(0, 1).toUpperCase();
    }

    private static final class TodayTask {
        final String kind, title, details, action;
        TodayTask(String kind, String title, String details, String action) {
            this.kind=kind; this.title=title; this.details=details; this.action=action;
        }
    }
}
