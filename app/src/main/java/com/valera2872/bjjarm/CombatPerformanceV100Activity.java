package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

/** Standalone premium 0.10.0 dashboard. Reads existing data without migration. */
public class CombatPerformanceV100Activity extends Activity {
    private SharedPreferences profile;
    private SharedPreferences diary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences("combat_performance_profile", MODE_PRIVATE);
        diary = getSharedPreferences("combat_training_diary", MODE_PRIVATE);
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
        page.addView(strengthCard());
        page.addView(diaryCard());

        root.addView(bottomNav(), new LinearLayout.LayoutParams(
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
        copy.addView(DarkUi.gold(this, "Фокус · " + mission()));
        copy.addView(DarkUi.small(this, activeSport() + " · "
                + profile.getInt("mission_active_days", 0) + " активных дней"));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(DarkUi.chip(this, "0.10", DarkUi.GOLD));
        return row;
    }

    private View todayCard() {
        TodayTask task = todayTask();
        LinearLayout card = DarkUi.hero(this);
        card.addView(DarkUi.small(this, "СЕГОДНЯШНЯЯ ЗАДАЧА"));
        TextView title = DarkUi.title(this, task.title);
        title.setTextSize(23);
        card.addView(title);
        card.addView(DarkUi.bodyWhite(this, task.details));
        Button action = DarkUi.primary(this, task.action);
        action.setOnClickListener(v -> openTask(task.kind));
        card.addView(action);
        return card;
    }

    private View nextTrainingCard() {
        LinearLayout card = DarkUi.card(this);
        LinearLayout row = DarkUi.h(this, 8);
        LinearLayout text = DarkUi.v(this, 4);
        text.addView(DarkUi.small(this, "БЛИЖАЙШАЯ ТРЕНИРОВКА"));
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        text.addView(DarkUi.h2(this, taskTitle(task.kind)));
        text.addView(DarkUi.small(this, task.details == null ? "Открой недельный план" : task.details));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView open = DarkUi.chip(this, "Неделя ›", DarkUi.GOLD);
        open.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class)));
        row.addView(open);
        card.addView(row);
        return card;
    }

    private View quickAccess() {
        LinearLayout block = DarkUi.v(this, 9);
        block.addView(DarkUi.small(this, "БЫСТРЫЙ ДОСТУП"));
        LinearLayout first = DarkUi.h(this, 9);
        first.addView(tile("▣", "Дневник", "Записать тренировку", DarkUi.GOLD,
                v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        first.addView(tile("⌁", "Мой план", "Техника и решения", DarkUi.ORANGE,
                v -> startActivity(new Intent(this, PremiumFightingPlanActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        block.addView(first);

        LinearLayout second = DarkUi.h(this, 9);
        second.addView(tile("▦", "Неделя", "Нагрузка и тренировки", Color.rgb(91, 153, 255),
                v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        second.addView(tile("H", "Силовая", "Персональная работа", DarkUi.GREEN,
                v -> startActivity(new Intent(this, PremiumStrengthHubActivity.class))),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        block.addView(second);
        return block;
    }

    private View tile(String symbol, String title, String subtitle, int accent, View.OnClickListener click) {
        LinearLayout card = DarkUi.card(this);
        card.setMinimumHeight(DarkUi.dp(this, 106));
        card.setClickable(true);
        card.setOnClickListener(click);
        card.addView(DarkUi.text(this, symbol, 18, accent, android.graphics.Typeface.DEFAULT_BOLD));
        card.addView(DarkUi.h2(this, title));
        card.addView(DarkUi.small(this, subtitle));
        return card;
    }

    private View focusCard() {
        LinearLayout card = DarkUi.goldCard(this);
        card.addView(DarkUi.gold(this, "ФОКУС НА 30 ДНЕЙ"));
        card.addView(DarkUi.h1(this, mission()));
        LinearLayout stats = DarkUi.h(this, 8);
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_attempts", 0)),
                "Попыток", DarkUi.ORANGE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_successes", 0)),
                "Успехов", DarkUi.GREEN), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, successPercent() + "%",
                "Успешность", DarkUi.GOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(stats);
        return card;
    }

    private View strengthCard() {
        LinearLayout card = DarkUi.card(this);
        card.addView(DarkUi.small(this, "ПЕРСОНАЛЬНАЯ СИЛОВАЯ ПОДГОТОВКА"));
        card.addView(DarkUi.h1(this, "Сила для твоей борьбы"));
        card.addView(DarkUi.body(this,
                "Руки, хват, ноги и корпус — с учётом тренировок, тяжёлых раундов и соревнований."));

        LinearLayout arms = miniStrength("Руки и хват", profile.getString("priority_1", "Хват и тяга"), DarkUi.GOLD);
        arms.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV5Activity.class)));
        arms.setClickable(true);
        card.addView(arms);
        LinearLayout base = miniStrength("Ноги и корпус", profile.getString("priority_2", "Сила и устойчивость"), DarkUi.GREEN);
        base.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV3Activity.class)));
        base.setClickable(true);
        card.addView(base);

        Button open = DarkUi.secondary(this, "Открыть силовую подготовку");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumStrengthHubActivity.class)));
        card.addView(open);
        return card;
    }

    private LinearLayout miniStrength(String title, String subtitle, int accent) {
        LinearLayout row = DarkUi.h(this, 10);
        row.setPadding(DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12));
        row.setBackground(DarkUi.round(DarkUi.CARD_2, 15, 1, DarkUi.BORDER));
        row.addView(DarkUi.text(this, "●", 12, accent, android.graphics.Typeface.DEFAULT_BOLD));
        LinearLayout copy = DarkUi.v(this, 3);
        copy.addView(DarkUi.h2(this, title));
        copy.addView(DarkUi.small(this, subtitle));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(DarkUi.text(this, "›", 26, DarkUi.GOLD, android.graphics.Typeface.DEFAULT));
        return row;
    }

    private View diaryCard() {
        LinearLayout card = DarkUi.card(this);
        card.addView(DarkUi.small(this, "ПОСЛЕ ТРЕНИРОВКИ"));
        card.addView(DarkUi.h1(this, "Дневник"));
        String next = diary.getString("next_task_" + SportGuidance.slug(activeSport()), "").trim();
        if (next.isEmpty()) next = "После тренировки сохрани один успешный момент и одну проблему.";
        card.addView(DarkUi.bodyWhite(this, next));
        card.addView(DarkUi.small(this, diaryCount() + " записей в истории"));
        Button add = DarkUi.primary(this, "Быстрая запись");
        add.setOnClickListener(v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class)));
        card.addView(add);
        return card;
    }

    private View bottomNav() {
        LinearLayout nav = DarkUi.h(this, 0);
        nav.setBackgroundColor(DarkUi.BG_2);
        nav.setPadding(DarkUi.dp(this, 8), 0, DarkUi.dp(this, 8), 0);
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_today, "Сегодня", true,
                v -> render()), navParams());
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_week, "Неделя", false,
                v -> startActivity(new Intent(this, WeeklyPlanV4Activity.class))), navParams());
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_diary, "Дневник", false,
                v -> startActivity(new Intent(this, PremiumDarkDiaryActivity.class))), navParams());
        nav.addView(DarkUi.navItem(this, R.drawable.ic_nav_profile, "Профиль", false,
                v -> startActivity(new Intent(this, PremiumProfileActivity.class))), navParams());
        return nav;
    }

    private LinearLayout.LayoutParams navParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
    }

    private TodayTask todayTask() {
        if (!WeekPlanEngine.isConfigured(this)) {
            return new TodayTask("week", "Настрой неделю",
                    "Отметь обычные, тяжёлые и соревновательные дни, чтобы распределить дополнительную нагрузку.",
                    "Настроить неделю");
        }
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        if ("arms".equals(task.kind)) return new TodayTask("arms", "Руки и хват",
                "Сегодня короткая дополнительная работа. Сначала оцени восстановление после основной тренировки.", "Начать тренировку");
        if ("base".equals(task.kind)) return new TodayTask("base", "Ноги и корпус",
                "Силовая база, устойчивость и контроль корпуса без лишней нагрузки перед тренировкой.", "Начать тренировку");
        if ("mat".equals(task.kind)) return new TodayTask("diary", "Технический фокус",
                missionTask(), "Открыть задачу");
        if ("heavy".equals(task.kind)) return new TodayTask("diary", "Тяжёлые раунды",
                "Без дополнительной силовой. Проверь текущий технический фокус и сохрани вывод после тренировки.", "Открыть дневник");
        if ("competition".equals(task.kind)) return new TodayTask("routine", "Соревнование",
                "Без силовой нагрузки. Держи первое действие и личную настройку перед выходом.", "Перед выходом");
        return new TodayTask("week", "Восстановление",
                "Сегодня достаточно лёгкой подвижности и короткого разбора последней тренировки.", "Открыть неделю");
    }

    private String missionTask() {
        String next = diary.getString("next_task_" + SportGuidance.slug(activeSport()), "").trim();
        if (!next.isEmpty()) return next;
        return "Сделай минимум три осознанные попытки: " + mission() + ".";
    }

    private void openTask(String kind) {
        if ("arms".equals(kind)) startActivity(new Intent(this, GrapplingV5Activity.class));
        else if ("base".equals(kind)) startActivity(new Intent(this, BaseStrengthV3Activity.class));
        else if ("diary".equals(kind)) startActivity(new Intent(this, PremiumDarkDiaryActivity.class));
        else if ("routine".equals(kind)) startActivity(new Intent(this, PremiumRoutineActivity.class));
        else startActivity(new Intent(this, WeeklyPlanV4Activity.class));
    }

    private String mission() {
        String value = profile.getString("mission", "").trim();
        return value.isEmpty() ? "Техника не выбрана" : value;
    }

    private String activeSport() {
        String active = profile.getString("active_sport", "").trim();
        if (!active.isEmpty()) return active;
        String sport = profile.getString("sport", "").trim();
        return sport.isEmpty() ? "Грэпплинг / No-Gi" : sport;
    }

    private String taskTitle(String kind) {
        if ("arms".equals(kind)) return "Руки и хват";
        if ("base".equals(kind)) return "Ноги и корпус";
        if ("mat".equals(kind)) return activeSport();
        if ("heavy".equals(kind)) return "Тяжёлые раунды";
        if ("competition".equals(kind)) return "Соревнование";
        if ("setup".equals(kind)) return "Настрой неделю";
        return "Восстановление";
    }

    private int successPercent() {
        int attempts = profile.getInt("mission_attempts", 0);
        int successes = profile.getInt("mission_successes", 0);
        return attempts <= 0 ? 0 : Math.min(100, Math.round(successes * 100f / attempts));
    }

    private int diaryCount() {
        try { return new JSONArray(diary.getString("entries_json", "[]")).length(); }
        catch (JSONException error) { return 0; }
    }

    private String firstLetter(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? "С" : clean.substring(0, 1).toUpperCase();
    }

    private static final class TodayTask {
        final String kind;
        final String title;
        final String details;
        final String action;
        TodayTask(String kind, String title, String details, String action) {
            this.kind = kind;
            this.title = title;
            this.details = details;
            this.action = action;
        }
    }
}
