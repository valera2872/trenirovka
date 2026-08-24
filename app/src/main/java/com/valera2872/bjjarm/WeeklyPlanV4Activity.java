package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.AlertDialog;
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

/** Premium dark weekly-plan experience for the 0.10 line. */
public class WeeklyPlanV4Activity extends Activity {
    private static final String[] EQUIPMENT = {
            "Только собственный вес",
            "Гантели",
            "Гантели и резинки",
            "Тренажёрный зал"
    };

    private SharedPreferences week;
    private SharedPreferences profile;
    private final String[] selectedTypes = new String[7];
    private int selectedStrengthSessions = 2;
    private String selectedEquipment = EQUIPMENT[1];
    private boolean showingSetup;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        week = getSharedPreferences(WeekPlanEngine.WEEK_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(WeekPlanEngine.PROFILE_PREFS, MODE_PRIVATE);
        DarkUi.apply(this);
        boolean edit = getIntent().getBooleanExtra("edit", false);
        if (!WeekPlanEngine.isConfigured(this) || edit) showSetup();
        else showPlan();
    }

    @Override protected void onResume() {
        super.onResume();
        if (week != null && !showingSetup && WeekPlanEngine.isConfigured(this)) showPlan();
    }

    @Override public void onBackPressed() {
        if (showingSetup && WeekPlanEngine.isConfigured(this)) showPlan();
        else super.onBackPressed();
    }

    private void showSetup() {
        showingSetup = true;
        for (int i = 0; i < 7; i++) {
            selectedTypes[i] = week.getString("day_type_" + i, defaultDayType(i));
        }
        selectedStrengthSessions = Math.max(1, Math.min(2, week.getInt("strength_sessions", 2)));
        selectedEquipment = week.getString("equipment", EQUIPMENT[1]);

        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this, 16), DarkUi.dp(this, 18), DarkUi.dp(this, 16), DarkUi.dp(this, 36));
        scroll.addView(page);

        Button back = DarkUi.outline(this, WeekPlanEngine.isConfigured(this) ? "← К моей неделе" : "← Назад");
        back.setOnClickListener(v -> {
            if (WeekPlanEngine.isConfigured(this)) showPlan();
            else finish();
        });
        page.addView(back);

        LinearLayout hero = DarkUi.hero(this);
        hero.addView(DarkUi.gold(this, "МОЯ НЕДЕЛЯ"));
        hero.addView(DarkUi.title(this, "Когда ты тренируешься на ковре?"));
        hero.addView(DarkUi.bodyWhite(this,
                "Отметь обычные и тяжёлые дни. Дополнительная силовая встанет туда, где она меньше мешает восстановлению и борьбе."));
        page.addView(hero);

        LinearLayout schedule = DarkUi.card(this);
        schedule.addView(DarkUi.gold(this, "РАСПИСАНИЕ КОВРА"));
        schedule.addView(DarkUi.h1(this, "Семь дней без перегруза"));
        schedule.addView(DarkUi.body(this, "Нажми на день и выбери реальную нагрузку."));
        for (int i = 0; i < 7; i++) schedule.addView(daySelector(i));
        page.addView(schedule);

        LinearLayout extra = DarkUi.goldCard(this);
        extra.addView(DarkUi.gold(this, "ДОПОЛНИТЕЛЬНАЯ ПОДГОТОВКА"));
        extra.addView(DarkUi.h1(this, "Сколько силовых реально выполнить?"));
        LinearLayout countRow = DarkUi.h(this, 8);
        Button one = choiceButton("1 силовая", selectedStrengthSessions == 1);
        Button two = choiceButton("2 силовые", selectedStrengthSessions == 2);
        one.setOnClickListener(v -> {
            selectedStrengthSessions = 1;
            showSetup();
        });
        two.setOnClickListener(v -> {
            selectedStrengthSessions = 2;
            showSetup();
        });
        countRow.addView(one, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        countRow.addView(two, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        extra.addView(countRow);
        extra.addView(DarkUi.small(this, "Оборудование"));
        Button equipment = DarkUi.secondary(this, selectedEquipment + "  ›");
        equipment.setOnClickListener(v -> chooseEquipment());
        extra.addView(equipment);
        page.addView(extra);

        LinearLayout note = DarkUi.card(this);
        note.addView(DarkUi.h2(this, "Главный принцип"));
        note.addView(DarkUi.body(this,
                "Тяжёлые раунды и соревнования имеют приоритет. Приложение не будет ставить рядом лишнюю силовую только ради выполнения плана."));
        page.addView(note);

        Button save = DarkUi.primary(this, "Составить мою неделю");
        save.setOnClickListener(v -> saveWeek());
        page.addView(save);
        setContentView(scroll);
    }

    private View daySelector(int index) {
        LinearLayout row = DarkUi.h(this, 10);
        row.setPadding(DarkUi.dp(this, 12), DarkUi.dp(this, 11), DarkUi.dp(this, 12), DarkUi.dp(this, 11));
        row.setBackground(DarkUi.round(DarkUi.CARD_2, 15, 1, DarkUi.BORDER));

        LinearLayout copy = DarkUi.v(this, 3);
        copy.addView(DarkUi.h2(this, WeekPlanEngine.DAYS[index]));
        copy.addView(DarkUi.small(this, shortType(selectedTypes[index])));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView change = DarkUi.chip(this, "Изменить", DarkUi.GOLD);
        change.setOnClickListener(v -> chooseDayType(index));
        row.setOnClickListener(v -> chooseDayType(index));
        row.setClickable(true);
        row.addView(change);
        return row;
    }

    private void chooseDayType(int index) {
        new AlertDialog.Builder(this)
                .setTitle(WeekPlanEngine.DAYS[index])
                .setSingleChoiceItems(WeekPlanEngine.DAY_TYPES, indexOf(WeekPlanEngine.DAY_TYPES, selectedTypes[index]),
                        (dialog, which) -> {
                            selectedTypes[index] = WeekPlanEngine.DAY_TYPES[which];
                            dialog.dismiss();
                            showSetup();
                        })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void chooseEquipment() {
        new AlertDialog.Builder(this)
                .setTitle("Оборудование")
                .setSingleChoiceItems(EQUIPMENT, indexOf(EQUIPMENT, selectedEquipment),
                        (dialog, which) -> {
                            selectedEquipment = EQUIPMENT[which];
                            dialog.dismiss();
                            showSetup();
                        })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private Button choiceButton(String text, boolean selected) {
        Button button = selected ? DarkUi.primary(this, text) : DarkUi.secondary(this, text);
        button.setMinHeight(DarkUi.dp(this, 48));
        return button;
    }

    private void saveWeek() {
        SharedPreferences.Editor editor = week.edit()
                .putBoolean("configured", true)
                .putInt("strength_sessions", selectedStrengthSessions)
                .putString("equipment", selectedEquipment);
        for (int i = 0; i < 7; i++) editor.putString("day_type_" + i, selectedTypes[i]);
        editor.apply();
        showPlan();
    }

    private void showPlan() {
        if (!WeekPlanEngine.isConfigured(this)) {
            showSetup();
            return;
        }
        showingSetup = false;
        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this, 16), DarkUi.dp(this, 18), DarkUi.dp(this, 16), DarkUi.dp(this, 36));
        scroll.addView(page);

        Button back = DarkUi.outline(this, "← Назад");
        back.setOnClickListener(v -> finish());
        page.addView(back);

        LinearLayout heading = DarkUi.v(this, 4);
        heading.addView(DarkUi.gold(this, "МОЯ НЕДЕЛЯ"));
        heading.addView(DarkUi.title(this, profile.getString("name", "Спортсмен")));
        heading.addView(DarkUi.body(this, "Ковёр, силовая и восстановление в одном ритме."));
        page.addView(heading);

        int today = WeekPlanEngine.todayIndex();
        WeekPlanEngine.Task todayTask = WeekPlanEngine.taskForDay(this, today);
        LinearLayout hero = DarkUi.hero(this);
        hero.addView(DarkUi.gold(this, "СЕГОДНЯ · " + WeekPlanEngine.DAYS[today].toUpperCase()));
        hero.addView(DarkUi.h1(this, prettyTitle(todayTask)));
        hero.addView(DarkUi.bodyWhite(this, prettyDetails(todayTask.details)));
        if (WeekPlanEngine.isTaskDone(this, today, todayTask)) {
            TextView done = DarkUi.chip(this, "✓ Выполнено", DarkUi.GREEN);
            hero.addView(done);
        } else {
            Button action = DarkUi.primary(this, prettyAction(todayTask));
            action.setOnClickListener(v -> performTask(today, todayTask));
            hero.addView(action);
        }
        page.addView(hero);

        LinearLayout weekCard = DarkUi.card(this);
        weekCard.addView(DarkUi.gold(this, "ПЛАН НА 7 ДНЕЙ"));
        weekCard.addView(DarkUi.h1(this, "Вся нагрузка перед глазами"));
        for (int day = 0; day < 7; day++) weekCard.addView(dayPlanRow(day, day == today));
        page.addView(weekCard);

        LinearLayout summary = DarkUi.goldCard(this);
        summary.addView(DarkUi.gold(this, "НАСТРОЙКИ НЕДЕЛИ"));
        summary.addView(DarkUi.h2(this,
                WeekPlanEngine.desiredStrengthSessions(this) == 1 ? "1 дополнительная силовая" : "2 дополнительные силовые"));
        summary.addView(DarkUi.bodyWhite(this, week.getString("equipment", EQUIPMENT[1])));
        Button edit = DarkUi.secondary(this, "Изменить расписание");
        edit.setOnClickListener(v -> showSetup());
        summary.addView(edit);
        page.addView(summary);

        setContentView(scroll);
    }

    private View dayPlanRow(int day, boolean today) {
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, day);
        boolean done = WeekPlanEngine.isTaskDone(this, day, task);
        LinearLayout row = DarkUi.h(this, 10);
        row.setPadding(DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12), DarkUi.dp(this, 12));
        row.setBackground(DarkUi.round(today ? Color.rgb(31, 42, 51) : DarkUi.CARD_2,
                15, 1, today ? DarkUi.GOLD_DARK : DarkUi.BORDER));

        LinearLayout copy = DarkUi.v(this, 3);
        TextView dayTitle = DarkUi.h2(this, WeekPlanEngine.DAYS[day] + " · " + WeekPlanEngine.displayDateForWeekDay(day));
        copy.addView(dayTitle);
        copy.addView(DarkUi.small(this, shortType(WeekPlanEngine.typeForDay(this, day))));
        copy.addView(DarkUi.bodyWhite(this, prettyTitle(task)));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        String marker = done ? "✓" : today ? "СЕГОДНЯ" : WeekPlanEngine.isPastDay(day) ? "—" : "";
        if (!marker.isEmpty()) row.addView(DarkUi.chip(this, marker, done ? DarkUi.GREEN : DarkUi.GOLD));
        return row;
    }

    private void performTask(int day, WeekPlanEngine.Task task) {
        if ("arms".equals(task.kind)) {
            startActivity(new Intent(this, GrapplingV5Activity.class));
        } else if ("base".equals(task.kind)) {
            startActivity(new Intent(this, BaseStrengthV3Activity.class));
        } else if ("mat".equals(task.kind) || "heavy".equals(task.kind)) {
            startActivity(new Intent(this, PremiumDarkDiaryActivity.class));
        } else if ("competition".equals(task.kind)) {
            startActivity(new Intent(this, PremiumRoutineActivity.class));
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Восстановление")
                    .setMessage("Сегодня без дополнительной силовой. Лёгкая подвижность, сон, питание и короткий разбор последней тренировки — достаточно.")
                    .setNegativeButton("Закрыть", null)
                    .setPositiveButton("Отметить выполненным", (d, w) -> {
                        WeekPlanEngine.setManualDone(this, day, true);
                        showPlan();
                    })
                    .show();
        }
    }

    private String prettyTitle(WeekPlanEngine.Task task) {
        if ("arms".equals(task.kind)) return "Руки и хват";
        if ("base".equals(task.kind)) return "Ноги и корпус";
        if ("mat".equals(task.kind)) return "Технический фокус на ковре";
        if ("heavy".equals(task.kind)) return "Тяжёлые раунды";
        if ("competition".equals(task.kind)) return "Соревнование";
        if ("recovery".equals(task.kind)) return "Восстановление";
        return task.title;
    }

    private String prettyAction(WeekPlanEngine.Task task) {
        if ("arms".equals(task.kind) || "base".equals(task.kind)) return "Начать";
        if ("mat".equals(task.kind) || "heavy".equals(task.kind)) return "Открыть дневник";
        if ("competition".equals(task.kind)) return "Перед выходом";
        return "Открыть";
    }

    private String prettyDetails(String source) {
        if (source == null) return "";
        return source
                .replace("техническую миссию", "текущий технический фокус")
                .replace("Техническую миссию", "Текущий технический фокус")
                .replace("технической миссии", "технического фокуса");
    }

    private String shortType(String type) {
        if (WeekPlanEngine.DAY_TYPES[0].equals(type)) return "Без ковра";
        if (WeekPlanEngine.DAY_TYPES[1].equals(type)) return "Лёгкая техника";
        if (WeekPlanEngine.DAY_TYPES[2].equals(type)) return "Обычная тренировка";
        if (WeekPlanEngine.DAY_TYPES[3].equals(type)) return "Тяжёлые раунды";
        if (WeekPlanEngine.DAY_TYPES[4].equals(type)) return "Соревнование";
        return type;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return 0;
    }

    private String defaultDayType(int day) {
        int sessions = profile.getInt("mat_sessions", 5);
        if (sessions >= 5 && day <= 4) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 4 && (day == 0 || day == 1 || day == 3 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 3 && (day == 0 || day == 2 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 2 && (day == 1 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 1 && day == 2) return WeekPlanEngine.DAY_TYPES[2];
        return WeekPlanEngine.DAY_TYPES[0];
    }
}
