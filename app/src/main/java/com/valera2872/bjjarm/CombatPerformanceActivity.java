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
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * First working prototype of the wider athlete-development platform.
 *
 * The existing arm-strength tracker remains available as the first physical
 * module. This activity adds an athlete profile, style-specific priorities,
 * a 30-day technical mission and short mental routines.
 */
public class CombatPerformanceActivity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String TRAINING_PREFS = "bjj_arm_tracker";

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int ACCENT = Color.rgb(210, 149, 54);
    private static final int DANGER = Color.rgb(177, 62, 62);

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
    private SharedPreferences training;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        training = getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);

        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);

        requestNotificationPermission();
        MainActivity.scheduleAllReminders(this);
        GrapplingV3Activity.schedulePreparationReminders(this);

        if (profile.getBoolean("profile_complete", false)) {
            showDashboard();
        } else {
            showOnboarding();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 730);
        }
    }

    private void showOnboarding() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = vertical(14);
        page.setPadding(dp(20), dp(24), dp(20), dp(36));
        scroll.addView(page);

        page.addView(label("COMBAT PERFORMANCE", 13, PRIMARY, true));
        page.addView(label("Соберём твою систему борьбы", 30, TEXT, true));
        page.addView(label(
                "Не общая программа для всех, а первый профиль: вид спорта, стиль, физические приоритеты и одна техническая миссия на 30 дней.",
                15, MUTED, false));

        LinearLayout form = card();
        page.addView(form);
        form.addView(sectionTitle("Спортсмен"));

        EditText name = input("Имя", profile.getString("name", ""), InputType.TYPE_CLASS_TEXT);
        EditText age = input("Возраст", String.valueOf(profile.getInt("age", 14)), InputType.TYPE_CLASS_NUMBER);
        EditText sessions = input("Тренировок на ковре в неделю", String.valueOf(profile.getInt("mat_sessions", 5)), InputType.TYPE_CLASS_NUMBER);
        form.addView(name);
        form.addView(age);
        form.addView(sessions);

        form.addView(fieldLabel("Вид борьбы"));
        Spinner sport = spinner(SPORTS, profile.getString("sport", SPORTS[0]));
        form.addView(sport);

        form.addView(fieldLabel("Основной стиль"));
        Spinner style = spinner(STYLES, profile.getString("style", STYLES[0]));
        form.addView(style);

        form.addView(fieldLabel("Самая сильная сторона"));
        Spinner strong = spinner(AREAS, profile.getString("strong_area", AREAS[0]));
        form.addView(strong);

        form.addView(fieldLabel("Главный физический приоритет"));
        Spinner priority1 = spinner(AREAS, profile.getString("priority_1", AREAS[1]));
        form.addView(priority1);

        form.addView(fieldLabel("Второй физический приоритет"));
        Spinner priority2 = spinner(AREAS, profile.getString("priority_2", AREAS[2]));
        form.addView(priority2);

        LinearLayout technique = card();
        page.addView(technique);
        technique.addView(sectionTitle("Техническая система"));
        EditText favorite = input(
                "Любимые позиции и техники",
                profile.getString("favorite_techniques", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        favorite.setMinLines(2);
        technique.addView(favorite);

        EditText mission = input(
                "Одна техническая миссия на 30 дней",
                profile.getString("mission", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mission.setMinLines(2);
        technique.addView(mission);
        technique.addView(label(
                "Пример: в каждом живом раунде хотя бы один раз войти в Williams Guard и попытаться перейти к треугольнику.",
                13, MUTED, false));

        Button save = primaryButton(profile.getBoolean("profile_complete", false) ? "Сохранить изменения" : "Создать мой профиль");
        save.setOnClickListener(v -> {
            String cleanName = name.getText().toString().trim();
            String cleanMission = mission.getText().toString().trim();
            if (cleanName.isEmpty()) {
                toast("Укажи имя спортсмена.");
                return;
            }
            if (cleanMission.isEmpty()) {
                toast("Сформулируй одну техническую миссию на 30 дней.");
                return;
            }

            boolean missionChanged = !cleanMission.equals(profile.getString("mission", ""));
            SharedPreferences.Editor editor = profile.edit()
                    .putBoolean("profile_complete", true)
                    .putString("name", cleanName)
                    .putInt("age", clamp(safeInt(age.getText().toString(), 14), 10, 60))
                    .putInt("mat_sessions", clamp(safeInt(sessions.getText().toString(), 5), 1, 14))
                    .putString("sport", sport.getSelectedItem().toString())
                    .putString("style", style.getSelectedItem().toString())
                    .putString("strong_area", strong.getSelectedItem().toString())
                    .putString("priority_1", priority1.getSelectedItem().toString())
                    .putString("priority_2", priority2.getSelectedItem().toString())
                    .putString("favorite_techniques", favorite.getText().toString().trim())
                    .putString("mission", cleanMission);

            if (missionChanged || !profile.contains("mission_started_at")) {
                editor.putLong("mission_started_at", System.currentTimeMillis())
                        .putInt("mission_active_days", 0)
                        .putInt("mission_attempts", 0)
                        .putInt("mission_successes", 0)
                        .putInt("mission_finishes", 0)
                        .remove("mission_last_day");
            }
            editor.apply();
            showDashboard();
        });
        page.addView(save);
        setContentView(scroll);
    }

    private void showDashboard() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = vertical(14);
        page.setPadding(dp(18), dp(20), dp(18), dp(34));
        scroll.addView(page);

        LinearLayout top = horizontal(10);
        LinearLayout heading = vertical(2);
        heading.addView(label("COMBAT PERFORMANCE", 12, PRIMARY, true));
        heading.addView(label(profile.getString("name", "Спортсмен"), 28, TEXT, true));
        top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button edit = compactButton("Профиль");
        edit.setOnClickListener(v -> showOnboarding());
        top.addView(edit, new LinearLayout.LayoutParams(dp(92), dp(42)));
        page.addView(top);

        page.addView(label(
                profile.getString("sport", SPORTS[0]) + " · "
                        + profile.getInt("mat_sessions", 5) + " тренировок на ковре в неделю",
                14, MUTED, false));

        boolean strengthDay = isStrengthDayToday();
        LinearLayout today = heroCard();
        page.addView(today);
        today.addView(label("СЕГОДНЯ", 12, Color.rgb(217, 239, 233), true));
        today.addView(label(
                strengthDay ? "Силовая подготовка" : "Техническая миссия на ковре",
                25, Color.WHITE, true));
        today.addView(label(
                strengthDay
                        ? "Первый доступный модуль — «Сильные руки и контроль». Перед началом приложение проверит восстановление."
                        : "Главная задача — не тренировать всё подряд, а найти возможность применить выбранную технику в живой борьбе.",
                14, Color.rgb(224, 238, 235), false));
        today.addView(space(12));

        Button todayAction = lightButton(strengthDay ? "Открыть силовую тренировку" : "Отметить работу над техникой");
        todayAction.setOnClickListener(v -> {
            if (strengthDay) openStrengthModule();
            else showMissionCheckIn();
        });
        today.addView(todayAction);

        LinearLayout profileCard = card();
        page.addView(profileCard);
        profileCard.addView(sectionTitle("Твой физический профиль"));
        profileCard.addView(metric("Сильная сторона", profile.getString("strong_area", AREAS[0])));
        profileCard.addView(metric("Приоритет №1", profile.getString("priority_1", AREAS[1])));
        profileCard.addView(metric("Приоритет №2", profile.getString("priority_2", AREAS[2])));
        profileCard.addView(space(7));
        profileCard.addView(label(buildPhysicalRecommendation(), 14, MUTED, false));
        Button bodyModule = secondaryButton("Открыть модуль «Сильные руки и контроль»");
        bodyModule.setOnClickListener(v -> openStrengthModule());
        profileCard.addView(space(10));
        profileCard.addView(bodyModule);

        LinearLayout missionCard = card();
        page.addView(missionCard);
        missionCard.addView(sectionTitle("Техническая миссия · 30 дней"));
        missionCard.addView(label(profile.getString("mission", ""), 17, TEXT, true));
        String favorite = profile.getString("favorite_techniques", "");
        if (!favorite.isEmpty()) {
            missionCard.addView(label("Твоя игровая база: " + favorite, 13, MUTED, false));
        }
        missionCard.addView(space(10));
        missionCard.addView(missionProgressRow());
        missionCard.addView(space(10));

        LinearLayout checkButtons = horizontal(8);
        Button attempt = miniAction("Попытка");
        Button success = miniAction("Получилось");
        Button finish = miniAction("Финиш");
        attempt.setOnClickListener(v -> recordMission("attempt"));
        success.setOnClickListener(v -> recordMission("success"));
        finish.setOnClickListener(v -> recordMission("finish"));
        checkButtons.addView(attempt, new LinearLayout.LayoutParams(0, dp(46), 1));
        checkButtons.addView(success, new LinearLayout.LayoutParams(0, dp(46), 1));
        checkButtons.addView(finish, new LinearLayout.LayoutParams(0, dp(46), 1));
        missionCard.addView(checkButtons);

        LinearLayout mind = card();
        page.addView(mind);
        mind.addView(sectionTitle("Состояние"));
        mind.addView(label(
                "Не мотивационные лозунги, а короткая рутина под конкретный момент.",
                14, MUTED, false));
        Button before = secondaryButton("Перед тренировкой");
        Button mistake = secondaryButton("После ошибки");
        Button competition = secondaryButton("Перед соревнованием");
        before.setOnClickListener(v -> showMentalRoutine("before"));
        mistake.setOnClickListener(v -> showMentalRoutine("mistake"));
        competition.setOnClickListener(v -> showMentalRoutine("competition"));
        mind.addView(space(8));
        mind.addView(before);
        mind.addView(space(7));
        mind.addView(mistake);
        mind.addView(space(7));
        mind.addView(competition);

        LinearLayout next = tintedCard();
        page.addView(next);
        next.addView(label("Что уже работает", 17, TEXT, true));
        next.addView(label(
                "Профиль сохраняется, техническая миссия считается, напоминания приходят, а силовой модуль продолжает адаптировать вес и повторения. Следующий физический модуль — общая сила ног и корпуса.",
                14, MUTED, false));

        setContentView(scroll);
    }

    private View missionProgressRow() {
        LinearLayout row = horizontal(8);
        row.addView(progressMetric("Дней", String.valueOf(profile.getInt("mission_active_days", 0))), new LinearLayout.LayoutParams(0, dp(70), 1));
        row.addView(progressMetric("Попыток", String.valueOf(profile.getInt("mission_attempts", 0))), new LinearLayout.LayoutParams(0, dp(70), 1));
        row.addView(progressMetric("Успехов", String.valueOf(profile.getInt("mission_successes", 0))), new LinearLayout.LayoutParams(0, dp(70), 1));
        row.addView(progressMetric("Финишей", String.valueOf(profile.getInt("mission_finishes", 0))), new LinearLayout.LayoutParams(0, dp(70), 1));
        return row;
    }

    private LinearLayout progressMetric(String title, String value) {
        LinearLayout box = vertical(1);
        box.setGravity(Gravity.CENTER);
        box.setBackground(rounded(PRIMARY_SOFT, 12, 0, Color.TRANSPARENT));
        box.addView(label(value, 21, PRIMARY, true));
        box.addView(label(title, 11, MUTED, false));
        return box;
    }

    private void recordMission(String type) {
        String today = dateKey();
        SharedPreferences.Editor editor = profile.edit();
        if (!today.equals(profile.getString("mission_last_day", ""))) {
            editor.putString("mission_last_day", today)
                    .putInt("mission_active_days", profile.getInt("mission_active_days", 0) + 1);
        }

        String message;
        if ("finish".equals(type)) {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1)
                    .putInt("mission_successes", profile.getInt("mission_successes", 0) + 1)
                    .putInt("mission_finishes", profile.getInt("mission_finishes", 0) + 1);
            message = "Финиш записан. Важно понять, какой вход к нему сработал.";
        } else if ("success".equals(type)) {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1)
                    .putInt("mission_successes", profile.getInt("mission_successes", 0) + 1);
            message = "Успешное применение записано.";
        } else {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1);
            message = "Попытка записана. Даже неудачный вход обучает распознавать момент.";
        }
        editor.apply();
        toast(message);
        showDashboard();
    }

    private void showMissionCheckIn() {
        new AlertDialog.Builder(this)
                .setTitle("Техническая миссия")
                .setMessage(profile.getString("mission", "")
                        + "\n\nЧто произошло сегодня?")
                .setNegativeButton("Пока не пробовал", null)
                .setNeutralButton("Была попытка", (d, w) -> recordMission("attempt"))
                .setPositiveButton("Получилось", (d, w) -> recordMission("success"))
                .show();
    }

    private void showMentalRoutine(String kind) {
        String title;
        String message;
        if ("mistake".equals(kind)) {
            title = "После ошибки";
            message = "1. Один длинный выдох.\n\n"
                    + "2. Назови факт без оценки: «Я потерял позицию».\n\n"
                    + "3. Выбери только следующее действие: вернуть рамку, закрыть локоть, повернуться к сопернику.\n\n"
                    + "4. Верни внимание в контакт, а не в прошлую ошибку.";
        } else if ("competition".equals(kind)) {
            title = "Перед соревнованием";
            message = "Не пытайся выиграть турнир заранее.\n\n"
                    + "1. Вспомни свой первый безопасный контакт.\n"
                    + "2. Назови одну техническую задачу.\n"
                    + "3. Выбери музыку или личный аудиотрек, который приводит тебя в рабочее состояние.\n"
                    + "4. Следующая цель — первое действие, а не вся схватка.";
        } else {
            title = "Перед тренировкой";
            message = "Сегодня не нужно доказать, что ты сильнее всех.\n\n"
                    + "1. Выбери одну техническую задачу.\n"
                    + "2. Реши, в каких раундах попробуешь её применить.\n"
                    + "3. После неудачной попытки вернись к ней ещё один раз.\n\n"
                    + "Твоя задача сегодня: " + profile.getString("mission", "");
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Готов", null)
                .show();
    }

    private String buildPhysicalRecommendation() {
        String sport = profile.getString("sport", SPORTS[0]);
        String style = profile.getString("style", STYLES[0]);
        String p1 = profile.getString("priority_1", AREAS[1]);
        String p2 = profile.getString("priority_2", AREAS[2]);

        String base;
        if (sport.contains("Дзюдо")) {
            base = "Для дзюдо основа — ноги, тяга, вращение корпуса, устойчивость на одной ноге и выносливость захвата дзюдоги.";
        } else if (sport.contains("Вольная") || sport.contains("Греко")) {
            base = "Для спортивной борьбы основа — ноги, корпус, шея, повторная взрывная работа и способность сохранять позицию под давлением.";
        } else if (style.contains("снизу")) {
            base = "Для гардиста важны общая сила ног, статическая сила рук, контроль таза и подвижность — но без отказа от базовой подготовки всего тела.";
        } else if (style.contains("сверху")) {
            base = "Для контроля сверху важны тяга, ноги, корпус, плечевой пояс и силовая выносливость при постоянном давлении.";
        } else if (style.contains("Стойка")) {
            base = "Для работы в стойке нужны ноги, корпус, тяга, вращательная мощность и устойчивость при смене направления.";
        } else {
            base = "Сначала строим общую силовую базу, затем добавляем около трети работы под личный стиль и любимые техники.";
        }
        return base + " Текущие персональные приоритеты: " + p1 + " и " + p2 + ".";
    }

    private boolean isStrengthDayToday() {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int day1 = training.getInt("reminder_day_1", Calendar.TUESDAY);
        int day2 = training.getInt("reminder_day_2", Calendar.FRIDAY);
        return today == day1 || today == day2;
    }

    private void openStrengthModule() {
        startActivity(new Intent(this, GrapplingV3Activity.class));
    }

    private LinearLayout heroCard() {
        LinearLayout layout = vertical(6);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackground(rounded(PRIMARY_DARK, 22, 0, Color.TRANSPARENT));
        layout.setElevation(dp(4));
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = vertical(8);
        layout.setPadding(dp(18), dp(17), dp(18), dp(17));
        layout.setBackground(rounded(CARD, 18, 1, Color.rgb(226, 231, 229)));
        layout.setElevation(dp(1));
        return layout;
    }

    private LinearLayout tintedCard() {
        LinearLayout layout = vertical(6);
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

    private TextView sectionTitle(String value) {
        TextView view = label(value, 18, TEXT, true);
        view.setPadding(0, 0, 0, dp(3));
        return view;
    }

    private TextView fieldLabel(String value) {
        TextView view = label(value, 13, MUTED, true);
        view.setPadding(0, dp(4), 0, 0);
        return view;
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

    private EditText input(String hint, String value, int type) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 147, 143));
        field.setInputType(type);
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, Color.rgb(214, 223, 220)));
        return field;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
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
            if (values[i].equals(selected)) {
                spinner.setSelection(i);
                break;
            }
        }
        return spinner;
    }

    private Button primaryButton(String value) {
        Button button = baseButton(value, Color.WHITE, PRIMARY);
        button.setMinHeight(dp(52));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = baseButton(value, PRIMARY, PRIMARY_SOFT);
        button.setMinHeight(dp(48));
        return button;
    }

    private Button lightButton(String value) {
        Button button = baseButton(value, PRIMARY_DARK, Color.WHITE);
        button.setMinHeight(dp(50));
        return button;
    }

    private Button compactButton(String value) {
        Button button = baseButton(value, PRIMARY, PRIMARY_SOFT);
        button.setTextSize(13);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private Button miniAction(String value) {
        Button button = baseButton(value, PRIMARY, PRIMARY_SOFT);
        button.setTextSize(12);
        button.setPadding(dp(4), 0, dp(4), 0);
        return button;
    }

    private Button baseButton(String value, int textColor, int backgroundColor) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackground(rounded(backgroundColor, 13, 0, Color.TRANSPARENT));
        return button;
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

    private GradientDrawable rounded(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private View space(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int safeInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String dateKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** Lightweight divider used to provide reliable spacing in dynamic layouts. */
    private static class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;

        SpacerDrawable(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }
    }
}
