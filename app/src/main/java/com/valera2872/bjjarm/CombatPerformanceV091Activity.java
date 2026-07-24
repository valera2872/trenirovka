package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/** 0.9.1 standalone premium home. No Activity inheritance or post-layout patching. */
public class CombatPerformanceV091Activity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String ROUTINE_PREFS = "combat_personal_routine";
    private static final int REQUEST_PROFILE = 951;

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
        PremiumUi.applyWindow(this);
        showCurrentScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profile != null && profile.getBoolean("profile_complete", false)) showDashboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROFILE) showCurrentScreen();
    }

    private void showCurrentScreen() {
        if (profile.getBoolean("profile_complete", false)) showDashboard();
        else showWelcome();
    }

    private void showWelcome() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        page.addView(PremiumUi.eyebrow(this, "Подготовка борца"));
        page.addView(PremiumUi.title(this, "На каждый ковёр — с понятной задачей"));
        page.addView(PremiumUi.body(this,
                "Приложение соединяет недельную нагрузку, личный план борьбы и короткий дневник тренировок."));

        LinearLayout hero = PremiumUi.hero(this);
        page.addView(hero);
        hero.addView(PremiumUi.heroEyebrow(this, "Как это работает"));
        hero.addView(PremiumUi.heroTitle(this, "Подготовься. Попробуй. Разбери."));
        hero.addView(PremiumUi.heroBody(this,
                "Перед тренировкой — одна задача. После тренировки — несколько полезных фактов. На следующий ковёр — новый конкретный фокус."));

        LinearLayout benefits = PremiumUi.card(this);
        page.addView(benefits);
        benefits.addView(benefitRow("01", "План борьбы",
                "Опиши свой рабочий маршрут по схватке, а не список случайных приёмов."));
        benefits.addView(benefitRow("02", "Задача на сегодня",
                "Учитывай ковёр, силовую нагрузку, восстановление и соревнования."));
        benefits.addView(benefitRow("03", "Дневник тренировок",
                "Сохраняй, что получилось, где возникла проблема и что сказал тренер."));

        Button setup = PremiumUi.primaryButton(this, "Настроить профиль");
        setup.setOnClickListener(v -> openProfile());
        page.addView(setup);
        setContentView(scroll);
    }

    private void showDashboard() {
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        page.addView(profileHeader());
        page.addView(bottomNavigation(true));

        TodayTask today = todayTask();
        LinearLayout hero = PremiumUi.hero(this);
        page.addView(hero);
        hero.addView(PremiumUi.heroEyebrow(this, "Сегодня"));
        hero.addView(PremiumUi.heroTitle(this, today.title));
        hero.addView(PremiumUi.heroBody(this, today.details));
        Button action = PremiumUi.lightButton(this, today.action);
        action.setOnClickListener(v -> runTodayAction(today.kind));
        hero.addView(action);

        page.addView(fightingPlanCard());
        page.addView(diaryCard());
        page.addView(focusCard());
        page.addView(physicalCard());
        page.addView(routineCard());
        setContentView(scroll);
    }

    private LinearLayout profileHeader() {
        LinearLayout row = PremiumUi.horizontal(this, 12);
        TextView avatar = PremiumUi.numberBadge(this, firstLetter(profile.getString("name", "С")));
        avatar.setTextSize(17);
        row.addView(avatar, new LinearLayout.LayoutParams(PremiumUi.dp(this, 46), PremiumUi.dp(this, 46)));

        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.eyebrow(this, "Личный план"));
        copy.addView(PremiumUi.cardTitle(this, profile.getString("name", "Спортсмен")));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(PremiumUi.chip(this, activeSport()));
        return row;
    }

    private LinearLayout bottomNavigation(boolean todaySelected) {
        LinearLayout nav = PremiumUi.horizontal(this, 6);
        Button today = PremiumUi.navButton(this, "Сегодня", todaySelected);
        Button week = PremiumUi.navButton(this, "Неделя", false);
        Button diaryButton = PremiumUi.navButton(this, "Дневник", false);
        Button profileButton = PremiumUi.navButton(this, "Профиль", false);
        today.setOnClickListener(v -> showDashboard());
        week.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV3Activity.class)));
        diaryButton.setOnClickListener(v -> startActivity(new Intent(this, PremiumTrainingDiaryActivity.class)));
        profileButton.setOnClickListener(v -> openProfile());
        nav.addView(today, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 50), 1));
        nav.addView(week, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 50), 1));
        nav.addView(diaryButton, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 50), 1));
        nav.addView(profileButton, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 50), 1));
        return nav;
    }

    private LinearLayout fightingPlanCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Техника и решения"));
        card.addView(PremiumUi.cardTitle(this, "Мой план борьбы"));
        String sport = activeSport();
        String prefix = SportGuidance.slug(sport) + "_";
        boolean configured = system.getBoolean(prefix + "configured", false);
        if (configured) {
            ArrayList<String> parts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String value = system.getString(prefix + "step_" + i, "").trim();
                if (!value.isEmpty()) parts.add(value);
            }
            String summary = TextUtils.join("  →  ", parts);
            if (!summary.isEmpty()) card.addView(PremiumUi.bodyDark(this, summary));
            card.addView(PremiumUi.small(this,
                    "Твой маршрут: от исходной позиции к главному действию и запасному решению."));
        } else {
            card.addView(PremiumUi.body(this,
                    "Опиши, откуда начинаешь, как выходишь в нужную позицию, чем продолжаешь и что делаешь, если первая попытка не сработала."));
        }
        Button open = PremiumUi.primaryButton(this, configured ? "Открыть план" : "Настроить план");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumFightingPlanActivity.class)));
        card.addView(open);
        return card;
    }

    private LinearLayout diaryCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "После ковра"));
        card.addView(PremiumUi.cardTitle(this, "Дневник тренировок"));
        String sport = activeSport();
        String next = diary.getString("next_task_" + SportGuidance.slug(sport), "").trim();
        if (next.isEmpty()) {
            next = "После тренировки сохрани один успешный момент и одно место, где действие остановилось.";
        }
        LinearLayout task = PremiumUi.softCard(this);
        task.addView(PremiumUi.accentText(this, "Следующая задача"));
        task.addView(PremiumUi.bodyDark(this, next));
        card.addView(task);
        int count = diaryEntryCount();
        card.addView(PremiumUi.small(this,
                count == 0 ? "Первая запись займёт около минуты."
                        : count + " " + entryWord(count) + " уже сохранено."));
        Button open = PremiumUi.primaryButton(this, "Новая запись");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumTrainingDiaryActivity.class)));
        card.addView(open);
        return card;
    }

    private LinearLayout focusCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Фокус на 30 дней"));
        String mission = profile.getString("mission", "").trim();
        card.addView(PremiumUi.cardTitle(this, mission.isEmpty() ? "Фокус не выбран" : mission));
        LinearLayout stats = PremiumUi.horizontal(this, 8);
        stats.addView(statBox("Попытки", profile.getInt("mission_attempts", 0)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statBox("Удалось", profile.getInt("mission_successes", 0)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statBox("Финиши", profile.getInt("mission_finishes", 0)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(stats);
        card.addView(PremiumUi.small(this,
                "Счётчики обновляются из дневника, поэтому не нужно отмечать одну тренировку дважды."));
        return card;
    }

    private LinearLayout statBox(String title, int value) {
        LinearLayout box = PremiumUi.vertical(this, 4);
        box.setGravity(Gravity.CENTER);
        box.setPadding(PremiumUi.dp(this, 8), PremiumUi.dp(this, 11),
                PremiumUi.dp(this, 8), PremiumUi.dp(this, 11));
        box.setBackground(PremiumUi.rounded(PremiumUi.PAPER, 15, 0, Color.TRANSPARENT));
        TextView number = PremiumUi.cardTitle(this, String.valueOf(value));
        number.setGravity(Gravity.CENTER);
        TextView label = PremiumUi.small(this, title);
        label.setGravity(Gravity.CENTER);
        box.addView(number);
        box.addView(label);
        return box;
    }

    private LinearLayout physicalCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Дополнительная работа"));
        card.addView(PremiumUi.cardTitle(this, "Физическая подготовка"));
        card.addView(PremiumUi.body(this,
                "Силовые модули дополняют ковёр и не ставятся в тяжёлые или соревновательные дни."));
        LinearLayout actions = PremiumUi.horizontal(this, 8);
        Button arms = PremiumUi.secondaryButton(this, "Руки и хват");
        Button base = PremiumUi.secondaryButton(this, "Ноги и корпус");
        arms.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV4Activity.class)));
        base.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
        actions.addView(arms, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 58), 1));
        actions.addView(base, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 58), 1));
        card.addView(actions);
        return card;
    }

    private LinearLayout routineCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Перед выходом"));
        card.addView(PremiumUi.cardTitle(this, "Личная настройка"));
        boolean configured = routine.getBoolean("configured", false);
        String first = routine.getString("first_action", "").trim();
        String prepare = routine.getString("prepare", "").trim();
        if (configured && (!first.isEmpty() || !prepare.isEmpty())) {
            card.addView(PremiumUi.bodyDark(this, !first.isEmpty() ? first : prepare));
            card.addView(PremiumUi.small(this,
                    "Твои собственные слова и действия, а не универсальная мотивация."));
        } else {
            card.addView(PremiumUi.body(this,
                    "Сохрани, что помогает собраться, какое действие сделать первым и как вернуться после ошибки."));
        }
        Button open = PremiumUi.secondaryButton(this, configured ? "Открыть настройку" : "Настроить");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumRoutineActivity.class)));
        card.addView(open);
        return card;
    }

    private LinearLayout benefitRow(String number, String title, String details) {
        LinearLayout row = PremiumUi.horizontal(this, 12);
        row.setGravity(Gravity.TOP);
        row.addView(PremiumUi.numberBadge(this, number),
                new LinearLayout.LayoutParams(PremiumUi.dp(this, 38), PremiumUi.dp(this, 38)));
        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.sectionTitle(this, title));
        copy.addView(PremiumUi.small(this, details));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private TodayTask todayTask() {
        if (!WeekPlanEngine.isConfigured(this)) {
            return new TodayTask("week", "Настрой неделю",
                    "Отметь обычные, тяжёлые и соревновательные дни. После этого приложение распределит дополнительную нагрузку.",
                    "Настроить неделю");
        }
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        if ("arms".equals(task.kind)) {
            return new TodayTask("arms", "Руки и хват",
                    "Короткая дополнительная работа для рук и предплечий. Сначала проверь восстановление.",
                    "Открыть тренировку");
        }
        if ("base".equals(task.kind)) {
            return new TodayTask("base", "Ноги и корпус",
                    "Сегодня работа над общей силовой базой, устойчивостью и контролем корпуса.",
                    "Открыть тренировку");
        }
        if ("mat".equals(task.kind)) {
            return new TodayTask("diary", "Тренировка на ковре",
                    "Перед началом посмотри текущий фокус. После ковра сохрани одну короткую запись.",
                    "Открыть задачу");
        }
        if ("heavy".equals(task.kind)) {
            return new TodayTask("week", "Тяжёлые раунды",
                    "Дополнительную силовую сегодня не добавляем. Основная задача — качественная работа на ковре.",
                    "Открыть неделю");
        }
        if ("competition".equals(task.kind)) {
            return new TodayTask("routine", "Соревнование",
                    "Без дополнительной силовой. Открой личную настройку и удерживай первое действие.",
                    "Перед выходом");
        }
        return new TodayTask("week", "Восстановление",
                "Лёгкая подвижность и короткий разбор последней тренировки — достаточная работа на сегодня.",
                "Открыть неделю");
    }

    private void runTodayAction(String kind) {
        if ("arms".equals(kind)) startActivity(new Intent(this, GrapplingV4Activity.class));
        else if ("base".equals(kind)) startActivity(new Intent(this, BaseStrengthV2Activity.class));
        else if ("diary".equals(kind)) startActivity(new Intent(this, PremiumTrainingDiaryActivity.class));
        else if ("routine".equals(kind)) startActivity(new Intent(this, PremiumRoutineActivity.class));
        else startActivity(new Intent(this, WeeklyPlanV3Activity.class));
    }

    private void openProfile() {
        startActivityForResult(new Intent(this, PremiumProfileActivity.class), REQUEST_PROFILE);
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

    private int diaryEntryCount() {
        try {
            return new JSONArray(diary.getString("entries_json", "[]")).length();
        } catch (JSONException error) {
            return 0;
        }
    }

    private String entryWord(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod10 == 1 && mod100 != 11) return "запись";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "записи";
        return "записей";
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
