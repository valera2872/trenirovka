package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Setup, daily action and summary for the personal weekly plan. */
public class WeeklyPlanActivity extends Activity {
    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int WARNING_SOFT = Color.rgb(250, 241, 219);

    private static final String[] STRENGTH_COUNTS = {"1 дополнительная силовая", "2 дополнительные силовые"};
    private static final String[] EQUIPMENT = {
            "Только собственный вес",
            "Гантели",
            "Гантели и резинки",
            "Тренажёрный зал"
    };

    private SharedPreferences week;
    private SharedPreferences profile;
    private boolean setupScreen;
    private Spinner[] daySpinners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        week = getSharedPreferences(WeekPlanEngine.WEEK_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(WeekPlanEngine.PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);

        boolean edit = getIntent().getBooleanExtra("edit", false);
        if (!WeekPlanEngine.isConfigured(this) || edit) showSetup();
        else showPlan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!setupScreen && WeekPlanEngine.isConfigured(this)) {
            View decor = getWindow().getDecorView();
            decor.post(this::showPlan);
        }
    }

    @Override
    public void onBackPressed() {
        if (setupScreen && WeekPlanEngine.isConfigured(this)) showPlan();
        else finish();
    }

    private void showSetup() {
        setupScreen = true;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        Button back = smallButton(WeekPlanEngine.isConfigured(this) ? "← К моей неделе" : "← Назад");
        back.setOnClickListener(v -> {
            if (WeekPlanEngine.isConfigured(this)) showPlan();
            else finish();
        });
        page.addView(back);
        page.addView(label("МОЯ НЕДЕЛЯ", 12, PRIMARY, true));
        page.addView(label("Когда ты тренируешься на ковре?", 29, TEXT, true));
        page.addView(label(
                "Отметь обычные и тяжёлые дни. Приложение поставит дополнительную силовую туда, где она меньше мешает восстановлению и борьбе.",
                15, MUTED, false));

        int matSessions = profile.getInt("mat_sessions", 5);
        daySpinners = new Spinner[7];
        LinearLayout schedule = card();
        page.addView(schedule);
        schedule.addView(sectionTitle("Расписание ковра"));
        for (int i = 0; i < 7; i++) {
            schedule.addView(fieldLabel(WeekPlanEngine.DAYS[i]));
            String saved = week.getString("day_type_" + i, defaultDayType(i, matSessions));
            daySpinners[i] = spinner(WeekPlanEngine.DAY_TYPES, saved);
            schedule.addView(daySpinners[i]);
        }

        LinearLayout options = card();
        page.addView(options);
        options.addView(sectionTitle("Дополнительная подготовка"));
        options.addView(fieldLabel("Сколько силовых реально выполнимо?"));
        Spinner count = spinner(STRENGTH_COUNTS,
                week.getInt("strength_sessions", 2) == 1 ? STRENGTH_COUNTS[0] : STRENGTH_COUNTS[1]);
        options.addView(count);
        options.addView(fieldLabel("Доступное оборудование"));
        Spinner equipment = spinner(EQUIPMENT, week.getString("equipment", EQUIPMENT[1]));
        options.addView(equipment);
        options.addView(label(
                "Оборудование сохраняется для последующей замены упражнений. В этой версии оба силовых модуля уже доступны, но ещё не все упражнения автоматически заменяются.",
                13, MUTED, false));

        Button save = primaryButton("Составить мою неделю");
        save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = week.edit().putBoolean("configured", true)
                    .putInt("strength_sessions", count.getSelectedItemPosition() + 1)
                    .putString("equipment", equipment.getSelectedItem().toString());
            for (int i = 0; i < 7; i++) {
                editor.putString("day_type_" + i, daySpinners[i].getSelectedItem().toString());
            }
            editor.apply();
            showPlan();
        });
        page.addView(save);
        setContentView(scroll);
    }

    private void showPlan() {
        if (!WeekPlanEngine.isConfigured(this)) {
            showSetup();
            return;
        }
        setupScreen = false;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        LinearLayout top = horizontal(8);
        LinearLayout heading = vertical(2);
        heading.addView(label("МОЯ НЕДЕЛЯ", 12, PRIMARY, true));
        heading.addView(label(profile.getString("name", "Спортсмен"), 27, TEXT, true));
        top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button edit = smallButton("Изменить");
        edit.setOnClickListener(v -> showSetup());
        top.addView(edit, new LinearLayout.LayoutParams(dp(100), dp(44)));
        page.addView(top);

        int today = WeekPlanEngine.todayIndex();
        WeekPlanEngine.Task todayTask = WeekPlanEngine.taskForDay(this, today);
        boolean todayDone = WeekPlanEngine.isTaskDone(this, today, todayTask);
        LinearLayout hero = heroCard();
        page.addView(hero);
        hero.addView(label("СЕГОДНЯ · " + WeekPlanEngine.DAYS[today].toUpperCase(new Locale("ru")),
                12, Color.rgb(217, 239, 233), true));
        hero.addView(label(todayTask.title, 25, Color.WHITE, true));
        hero.addView(label(todayTask.details, 14, Color.rgb(224, 238, 235), false));
        if (todayDone) {
            hero.addView(label("✓ Сегодняшняя задача отмечена выполненной", 14, Color.WHITE, true));
        } else {
            Button action = lightButton(todayTask.actionLabel);
            action.setOnClickListener(v -> performTodayTask(today, todayTask));
            hero.addView(space(10));
            hero.addView(action);
        }

        LinearLayout weekCard = card();
        page.addView(weekCard);
        weekCard.addView(sectionTitle("План на семь дней"));
        int planned = 0;
        int completed = 0;
        for (int day = 0; day < 7; day++) {
            WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, day);
            boolean done = WeekPlanEngine.isTaskDone(this, day, task);
            if (!"recovery".equals(task.kind)) planned++;
            if (done && !"recovery".equals(task.kind)) completed++;
            weekCard.addView(dayRow(day, task, done, day == today));
            if (day < 6) weekCard.addView(divider());
        }

        LinearLayout summary = tintedCard();
        page.addView(summary);
        summary.addView(label("Итог текущей недели", 18, TEXT, true));
        summary.addView(metric("Выполнено основных задач", completed + " из " + planned));
        summary.addView(metric("Попыток технической миссии",
                String.valueOf(profile.getInt("mission_attempts", 0))));
        summary.addView(metric("Успешных применений",
                String.valueOf(profile.getInt("mission_successes", 0))));
        summary.addView(metric("Оборудование", week.getString("equipment", EQUIPMENT[1])));
        summary.addView(label(
                "Счётчики техники пока показывают весь период текущей 30-дневной миссии. В следующем развитии разделим их по неделям и этапам техники.",
                12, MUTED, false));

        Button back = secondaryButton("← На главный экран");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        setContentView(scroll);
    }

    private void performTodayTask(int day, WeekPlanEngine.Task task) {
        if ("arms".equals(task.kind)) {
            startActivity(new Intent(this, GrapplingV3Activity.class));
        } else if ("base".equals(task.kind)) {
            startActivity(new Intent(this, BaseStrengthV2Activity.class));
        } else if ("mat".equals(task.kind) || "heavy".equals(task.kind)) {
            showMissionDialog(day);
        } else if ("competition".equals(task.kind)) {
            showCompetitionPlan(day);
        } else {
            showRecoveryPlan(day);
        }
    }

    private void showMissionDialog(int day) {
        String mission = profile.getString("mission", "Техническая миссия пока не задана");
        new AlertDialog.Builder(this)
                .setTitle("Техническая миссия")
                .setMessage(mission + "\n\nЧто получилось сегодня?")
                .setNegativeButton("Пока не пробовал", null)
                .setNeutralButton("Была попытка", (d, w) -> recordMission(day, false))
                .setPositiveButton("Получилось", (d, w) -> recordMission(day, true))
                .show();
    }

    private void recordMission(int day, boolean success) {
        String date = WeekPlanEngine.dateKeyForWeekDay(day);
        SharedPreferences.Editor editor = profile.edit();
        if (!date.equals(profile.getString("mission_last_day", ""))) {
            editor.putString("mission_last_day", date)
                    .putInt("mission_active_days", profile.getInt("mission_active_days", 0) + 1);
        }
        editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1);
        if (success) editor.putInt("mission_successes", profile.getInt("mission_successes", 0) + 1);
        editor.apply();
        toast(success ? "Успешное применение записано." : "Попытка записана.");
        showPlan();
    }

    private void showCompetitionPlan(int day) {
        String[] items = {
                "Экипировка и документы подготовлены",
                "Есть одна задача на первую схватку",
                "Выбрана привычная музыка или настрой",
                "После результата сначала восстановление, потом разбор"
        };
        boolean[] checked = new boolean[items.length];
        new AlertDialog.Builder(this)
                .setTitle("План соревнования")
                .setMultiChoiceItems(items, checked, (d, which, value) -> checked[which] = value)
                .setNegativeButton("Закрыть", null)
                .setPositiveButton("Готово", (d, w) -> {
                    WeekPlanEngine.setManualDone(this, day, true);
                    showPlan();
                })
                .show();
    }

    private void showRecoveryPlan(int day) {
        String[] items = {
                "Нет необычной или суставной боли",
                "5–10 минут лёгкой подвижности",
                "Вспомнил один удачный момент тренировки",
                "Сегодня достаточно сна и еды"
        };
        boolean[] checked = new boolean[items.length];
        new AlertDialog.Builder(this)
                .setTitle("Восстановление")
                .setMultiChoiceItems(items, checked, (d, which, value) -> checked[which] = value)
                .setNegativeButton("Позже", null)
                .setPositiveButton("Отметить выполненным", (d, w) -> {
                    WeekPlanEngine.setManualDone(this, day, true);
                    showPlan();
                })
                .show();
    }

    private View dayRow(int day, WeekPlanEngine.Task task, boolean done, boolean today) {
        LinearLayout row = vertical(3);
        row.setPadding(dp(2), dp(9), dp(2), dp(9));
        if (today) row.setBackground(rounded(PRIMARY_SOFT, 12, 0, Color.TRANSPARENT));
        LinearLayout titleRow = horizontal(8);
        TextView name = label(WeekPlanEngine.DAYS[day] + " · " + WeekPlanEngine.displayDateForWeekDay(day),
                14, today ? PRIMARY : TEXT, true);
        TextView status = label(done ? "✓" : WeekPlanEngine.isPastDay(day) ? "—" : "", 18,
                done ? PRIMARY : MUTED, true);
        status.setGravity(Gravity.END);
        titleRow.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(status, new LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(titleRow);
        row.addView(label(WeekPlanEngine.typeForDay(this, day), 12, MUTED, false));
        row.addView(label(task.title, 14, TEXT, true));
        return row;
    }

    private String defaultDayType(int day, int sessions) {
        if (sessions >= 5 && day <= 4) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 4 && (day == 0 || day == 1 || day == 3 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 3 && (day == 0 || day == 2 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 2 && (day == 1 || day == 4)) return WeekPlanEngine.DAY_TYPES[2];
        if (sessions == 1 && day == 2) return WeekPlanEngine.DAY_TYPES[2];
        return WeekPlanEngine.DAY_TYPES[0];
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        return scroll;
    }

    private LinearLayout pageContent(ScrollView scroll) {
        LinearLayout page = vertical(14);
        page.setPadding(dp(18), dp(20), dp(18), dp(34));
        scroll.addView(page);
        return page;
    }

    private LinearLayout heroCard() {
        LinearLayout layout = vertical(7);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackground(rounded(PRIMARY_DARK, 22, 0, Color.TRANSPARENT));
        layout.setElevation(dp(4));
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = vertical(7);
        layout.setPadding(dp(18), dp(17), dp(18), dp(17));
        layout.setBackground(rounded(CARD, 18, 1, Color.rgb(226, 231, 229)));
        layout.setElevation(dp(1));
        return layout;
    }

    private LinearLayout tintedCard() {
        LinearLayout layout = vertical(7);
        layout.setPadding(dp(18), dp(16), dp(18), dp(16));
        layout.setBackground(rounded(PRIMARY_SOFT, 18, 0, Color.TRANSPARENT));
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
        spinner.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, Color.rgb(214, 223, 220)));
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) spinner.setSelection(i);
        }
        return spinner;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView sectionTitle(String value) {
        return label(value, 18, TEXT, true);
    }

    private TextView fieldLabel(String value) {
        TextView view = label(value, 13, MUTED, true);
        view.setPadding(0, dp(4), 0, 0);
        return view;
    }

    private View metric(String left, String right) {
        LinearLayout row = horizontal(8);
        TextView l = label(left, 13, MUTED, false);
        TextView r = label(right, 14, TEXT, true);
        r.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(r, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private Button primaryButton(String value) {
        return baseButton(value, Color.WHITE, PRIMARY, 52);
    }

    private Button secondaryButton(String value) {
        return baseButton(value, PRIMARY, PRIMARY_SOFT, 48);
    }

    private Button lightButton(String value) {
        return baseButton(value, PRIMARY_DARK, Color.WHITE, 50);
    }

    private Button smallButton(String value) {
        Button button = baseButton(value, PRIMARY, PRIMARY_SOFT, 44);
        button.setTextSize(13);
        return button;
    }

    private Button baseButton(String value, int textColor, int background, int minHeight) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setMinHeight(dp(minHeight));
        button.setBackground(rounded(background, 13, 0, Color.TRANSPARENT));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(Color.rgb(229, 233, 231));
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return view;
    }

    private View space(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;

        SpacerDrawable(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }

        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}