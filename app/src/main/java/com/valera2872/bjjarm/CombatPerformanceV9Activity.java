package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
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

/**
 * Clean 0.6.2 entry point. Replaces the layered prototype dashboard with a
 * direct Russian-language flow and supports several combat sports per athlete.
 */
public class CombatPerformanceV9Activity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int SCREEN_WELCOME = 0;
    private static final int SCREEN_DASHBOARD = 1;
    private static final int SCREEN_PROFILE = 2;
    private static final int VOICE_REQUEST = 9201;

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

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
    private int currentScreen = SCREEN_WELCOME;
    private int profileReturnScreen = SCREEN_WELCOME;
    private EditText voiceTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);

        if (profile.getBoolean("profile_complete", false)) showDashboard();
        else showWelcome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentScreen == SCREEN_DASHBOARD && profile.getBoolean("profile_complete", false)) {
            showDashboard();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == SCREEN_PROFILE) {
            if (profileReturnScreen == SCREEN_DASHBOARD && profile.getBoolean("profile_complete", false)) {
                showDashboard();
            } else {
                showWelcome();
            }
        } else if (currentScreen == SCREEN_WELCOME && profile.getBoolean("profile_complete", false)) {
            showDashboard();
        } else {
            super.onBackPressed();
        }
    }

    private void showWelcome() {
        currentScreen = SCREEN_WELCOME;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        page.addView(label("COMBAT PERFORMANCE", 13, PRIMARY, true));
        page.addView(label("Дополнительная подготовка для борца", 30, TEXT, true));
        page.addView(label(
                "Приложение помогает понять, что делать сегодня: силовая тренировка, работа над выбранной техникой или восстановление.",
                16, MUTED, false));

        LinearLayout what = card();
        page.addView(what);
        what.addView(sectionTitle("Что здесь можно делать"));
        what.addView(infoRow("Сила", "Тренировать то, чего не хватает именно тебе."));
        what.addView(infoRow("Техника", "Выбрать одну технику и регулярно пробовать её на ковре."));
        what.addView(infoRow("Неделя", "Распределить дополнительную нагрузку между тренировками."));

        Button setup = primaryButton(profile.getBoolean("profile_complete", false)
                ? "Изменить профиль" : "Настроить профиль");
        setup.setOnClickListener(v -> showProfile(SCREEN_WELCOME));
        page.addView(setup);

        if (profile.getBoolean("profile_complete", false)) {
            Button dashboard = secondaryButton("К сегодняшнему плану");
            dashboard.setOnClickListener(v -> showDashboard());
            page.addView(dashboard);
        }
        setContentView(scroll);
    }

    private void showProfile(int returnScreen) {
        currentScreen = SCREEN_PROFILE;
        profileReturnScreen = returnScreen;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        Button back = secondaryButton("← Назад");
        back.setOnClickListener(v -> onBackPressed());
        page.addView(back);
        page.addView(label("ПРОФИЛЬ СПОРТСМЕНА", 12, PRIMARY, true));
        page.addView(label("Настрой подготовку под себя", 28, TEXT, true));
        page.addView(label(
                "Можно выбрать несколько видов борьбы. План будет учитывать общую нагрузку на ковре и твои физические задачи.",
                15, MUTED, false));

        LinearLayout basics = card();
        page.addView(basics);
        basics.addView(sectionTitle("Основные данные"));
        EditText name = input("Имя спортсмена", profile.getString("name", ""), InputType.TYPE_CLASS_TEXT);
        EditText age = input("Возраст", String.valueOf(profile.getInt("age", 14)), InputType.TYPE_CLASS_NUMBER);
        EditText sessions = input("Тренировок на ковре в неделю",
                String.valueOf(profile.getInt("mat_sessions", 5)), InputType.TYPE_CLASS_NUMBER);
        basics.addView(name);
        basics.addView(age);
        basics.addView(sessions);

        LinearLayout sportsCard = card();
        page.addView(sportsCard);
        sportsCard.addView(sectionTitle("Какими видами борьбы ты занимаешься?"));
        sportsCard.addView(label("Можно отметить несколько вариантов.", 13, MUTED, false));
        List<String> selectedSports = savedSports();
        CheckBox[] sportChecks = new CheckBox[SPORTS.length];
        for (int i = 0; i < SPORTS.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(SPORTS[i]);
            box.setTextSize(15);
            box.setTextColor(TEXT);
            box.setChecked(selectedSports.contains(SPORTS[i]));
            box.setPadding(0, dp(5), 0, dp(5));
            sportChecks[i] = box;
            sportsCard.addView(box);
        }

        LinearLayout physical = card();
        page.addView(physical);
        physical.addView(sectionTitle("Стиль и физические задачи"));
        physical.addView(fieldLabel("Как ты чаще борешься?"));
        Spinner style = spinner(STYLES, profile.getString("style", STYLES[0]));
        physical.addView(style);
        physical.addView(fieldLabel("Что уже развито лучше всего?"));
        Spinner strong = spinner(AREAS, profile.getString("strong_area", AREAS[0]));
        physical.addView(strong);
        physical.addView(fieldLabel("Что нужно усилить в первую очередь?"));
        Spinner priority1 = spinner(AREAS, profile.getString("priority_1", AREAS[1]));
        physical.addView(priority1);
        physical.addView(fieldLabel("Второй приоритет"));
        Spinner priority2 = spinner(AREAS, profile.getString("priority_2", AREAS[2]));
        physical.addView(priority2);

        LinearLayout technique = card();
        page.addView(technique);
        technique.addView(sectionTitle("Техника"));
        EditText favorite = input("Любимые позиции и техники",
                profile.getString("favorite_techniques", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        favorite.setMinLines(2);
        technique.addView(voiceField(favorite, "Назови любимые позиции и техники"));

        EditText focus = input("Какую технику хочешь закрепить за 30 дней?",
                profile.getString("mission", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        focus.setMinLines(2);
        technique.addView(voiceField(focus, "Продиктуй технику, которую хочешь закрепить"));
        technique.addView(label(
                "Например: на каждом занятии хотя бы один раз выйти в Williams Guard и попробовать перейти к треугольнику.",
                13, MUTED, false));

        Button save = primaryButton("Сохранить профиль");
        save.setOnClickListener(v -> {
            String cleanName = name.getText().toString().trim();
            String cleanFocus = focus.getText().toString().trim();
            ArrayList<String> sports = new ArrayList<>();
            for (int i = 0; i < sportChecks.length; i++) {
                if (sportChecks[i].isChecked()) sports.add(SPORTS[i]);
            }
            if (cleanName.isEmpty()) {
                toast("Укажи имя спортсмена.");
                return;
            }
            if (sports.isEmpty()) {
                toast("Выбери хотя бы один вид борьбы.");
                return;
            }
            if (cleanFocus.isEmpty()) {
                toast("Напиши, какую технику хочешь закрепить.");
                return;
            }

            boolean focusChanged = !cleanFocus.equals(profile.getString("mission", ""));
            SharedPreferences.Editor editor = profile.edit()
                    .putBoolean("profile_complete", true)
                    .putString("name", cleanName)
                    .putInt("age", clamp(safeInt(age.getText().toString(), 14), 8, 60))
                    .putInt("mat_sessions", clamp(safeInt(sessions.getText().toString(), 5), 1, 14))
                    .putString("sports", TextUtils.join("|", sports))
                    .putString("sport", sports.get(0))
                    .putString("style", style.getSelectedItem().toString())
                    .putString("strong_area", strong.getSelectedItem().toString())
                    .putString("priority_1", priority1.getSelectedItem().toString())
                    .putString("priority_2", priority2.getSelectedItem().toString())
                    .putString("favorite_techniques", favorite.getText().toString().trim())
                    .putString("mission", cleanFocus);
            if (focusChanged || !profile.contains("mission_started_at")) {
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
        currentScreen = SCREEN_DASHBOARD;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        page.addView(label("COMBAT PERFORMANCE", 12, PRIMARY, true));
        page.addView(label(profile.getString("name", "Спортсмен"), 29, TEXT, true));
        page.addView(label(sportsDisplay() + " · " + profile.getInt("mat_sessions", 5)
                + " тренировок на ковре в неделю", 14, MUTED, false));

        LinearLayout navigation = horizontal(8);
        Button start = navButton("Начало");
        Button edit = navButton("Профиль");
        Button week = navButton("Неделя");
        start.setOnClickListener(v -> showWelcome());
        edit.setOnClickListener(v -> showProfile(SCREEN_DASHBOARD));
        week.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV3Activity.class)));
        navigation.addView(start, new LinearLayout.LayoutParams(0, dp(46), 1));
        navigation.addView(edit, new LinearLayout.LayoutParams(0, dp(46), 1));
        navigation.addView(week, new LinearLayout.LayoutParams(0, dp(46), 1));
        page.addView(navigation);

        DisplayTask todayTask = todayTask();
        LinearLayout hero = heroCard();
        page.addView(hero);
        hero.addView(label("СЕГОДНЯ", 12, Color.rgb(217, 239, 233), true));
        hero.addView(label(todayTask.title, 25, Color.WHITE, true));
        hero.addView(label(todayTask.details, 14, Color.rgb(224, 238, 235), false));
        Button todayAction = lightButton(todayTask.action);
        todayAction.setOnClickListener(v -> runTodayAction(todayTask.kind));
        hero.addView(todayAction);

        if (WeekPlanEngine.isConfigured(this)) {
            Button openWeek = secondaryButton("Открыть план недели");
            openWeek.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV3Activity.class)));
            page.addView(openWeek);
        }

        LinearLayout physical = card();
        page.addView(physical);
        physical.addView(sectionTitle("Физическая подготовка"));
        physical.addView(label(buildPhysicalRecommendation(), 14, MUTED, false));
        Button arms = secondaryButton("Руки и хват");
        Button base = secondaryButton("Ноги и корпус");
        arms.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV4Activity.class)));
        base.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
        physical.addView(arms);
        physical.addView(base);

        LinearLayout technique = card();
        page.addView(technique);
        technique.addView(sectionTitle("Техника на 30 дней"));
        technique.addView(label(profile.getString("mission", ""), 17, TEXT, true));
        String favorite = profile.getString("favorite_techniques", "").trim();
        if (!favorite.isEmpty()) {
            technique.addView(label("Твоя борцовская база: " + favorite, 13, MUTED, false));
        }
        technique.addView(progressRow());
        LinearLayout actions = horizontal(8);
        Button attempt = miniButton("Попытка");
        Button success = miniButton("Удалось");
        Button finish = miniButton("Финиш");
        attempt.setOnClickListener(v -> recordTechnique("attempt"));
        success.setOnClickListener(v -> recordTechnique("success"));
        finish.setOnClickListener(v -> recordTechnique("finish"));
        actions.addView(attempt, new LinearLayout.LayoutParams(0, dp(46), 1));
        actions.addView(success, new LinearLayout.LayoutParams(0, dp(46), 1));
        actions.addView(finish, new LinearLayout.LayoutParams(0, dp(46), 1));
        technique.addView(actions);

        LinearLayout state = card();
        page.addView(state);
        state.addView(sectionTitle("Состояние перед борьбой"));
        state.addView(label("Короткая настройка под конкретную ситуацию.", 14, MUTED, false));
        Button before = secondaryButton("Перед тренировкой");
        Button mistake = secondaryButton("После ошибки");
        Button competition = secondaryButton("Перед соревнованием");
        before.setOnClickListener(v -> showRoutine("before"));
        mistake.setOnClickListener(v -> showRoutine("mistake"));
        competition.setOnClickListener(v -> showRoutine("competition"));
        state.addView(before);
        state.addView(mistake);
        state.addView(competition);

        setContentView(scroll);
    }

    private DisplayTask todayTask() {
        if (!WeekPlanEngine.isConfigured(this)) {
            return new DisplayTask("setup", "Настрой расписание тренировок",
                    "Отметь обычные и тяжёлые дни на ковре. После этого приложение подберёт место для дополнительной силовой.",
                    "Настроить неделю");
        }
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        if ("arms".equals(task.kind)) {
            return new DisplayTask("arms", "Руки и хват",
                    "Сегодня дополнительная силовая для рук и предплечий. Перед началом проверь восстановление.",
                    "Начать тренировку");
        }
        if ("base".equals(task.kind)) {
            return new DisplayTask("base", "Ноги и корпус",
                    "Сегодня общая силовая база: ноги, корпус и устойчивость.",
                    "Начать тренировку");
        }
        if ("mat".equals(task.kind)) {
            return new DisplayTask("week", "Тренировка на ковре",
                    "Сегодня попробуй применить технику, которую закрепляешь в течение 30 дней.",
                    "Отметить технику");
        }
        if ("heavy".equals(task.kind)) {
            return new DisplayTask("week", "Тяжёлые раунды",
                    "Дополнительную силовую сегодня не добавляем. Сохрани силы для основной работы на ковре.",
                    "Открыть план");
        }
        if ("competition".equals(task.kind)) {
            return new DisplayTask("week", "Соревнование",
                    "Сегодня без дополнительной силовой. Проверь экипировку и первую задачу на схватку.",
                    "План соревнования");
        }
        return new DisplayTask("week", "Восстановление",
                "Сегодня достаточно лёгкой подвижности и короткого разбора последней тренировки.",
                "План восстановления");
    }

    private void runTodayAction(String kind) {
        if ("arms".equals(kind)) {
            startActivity(new Intent(this, GrapplingV4Activity.class));
        } else if ("base".equals(kind)) {
            startActivity(new Intent(this, BaseStrengthV2Activity.class));
        } else {
            startActivity(new Intent(this, WeeklyPlanV3Activity.class));
        }
    }

    private String buildPhysicalRecommendation() {
        String style = profile.getString("style", STYLES[0]);
        String strong = profile.getString("strong_area", AREAS[0]);
        String p1 = profile.getString("priority_1", AREAS[1]);
        String p2 = profile.getString("priority_2", AREAS[2]);
        String start;
        if (style.contains("снизу") || style.contains("гард")) {
            start = "При работе из гарда особенно важны ноги, корпус, статическая сила рук и подвижность.";
        } else if (style.contains("сверху")) {
            start = "Для контроля сверху особенно важны тяга, ноги, корпус и плечевой пояс.";
        } else if (style.contains("Стойка")) {
            start = "Для работы в стойке особенно важны ноги, корпус, тяга и взрывная сила.";
        } else {
            start = "Сначала развиваем общую силу, затем добавляем работу под твой стиль борьбы.";
        }
        return start + " Сейчас главный акцент: " + p1 + " и " + p2
                + ". Сильную сторону — " + strong + " — поддерживаем без лишнего объёма.";
    }

    private void recordTechnique(String type) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new java.util.Date());
        SharedPreferences.Editor editor = profile.edit();
        if (!date.equals(profile.getString("mission_last_day", ""))) {
            editor.putString("mission_last_day", date)
                    .putInt("mission_active_days", profile.getInt("mission_active_days", 0) + 1);
        }
        String message;
        if ("finish".equals(type)) {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1)
                    .putInt("mission_successes", profile.getInt("mission_successes", 0) + 1)
                    .putInt("mission_finishes", profile.getInt("mission_finishes", 0) + 1);
            message = "Финиш записан.";
        } else if ("success".equals(type)) {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1)
                    .putInt("mission_successes", profile.getInt("mission_successes", 0) + 1);
            message = "Успешное применение записано.";
        } else {
            editor.putInt("mission_attempts", profile.getInt("mission_attempts", 0) + 1);
            message = "Попытка записана.";
        }
        editor.apply();
        toast(message);
        showDashboard();
    }

    private void showRoutine(String type) {
        String title;
        String message;
        if ("mistake".equals(type)) {
            title = "После ошибки";
            message = "Остановись на один выдох. Назови, что произошло, одним словом. Затем выбери только следующее действие: вернуть позицию, защититься или снова начать атаку.";
        } else if ("competition".equals(type)) {
            title = "Перед соревнованием";
            message = "Не пытайся выиграть турнир заранее. Подготовь экипировку, вспомни первый захват или вход и сосредоточься только на начале первой схватки.";
        } else {
            title = "Перед тренировкой";
            message = "Выбери одну задачу на сегодня: позицию, вход или защиту. Во время раундов ищи возможность попробовать её, не ломая всю борьбу ради отчёта.";
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
    }

    private LinearLayout voiceField(EditText field, String prompt) {
        LinearLayout row = horizontal(8);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button voice = miniButton("🎤");
        voice.setContentDescription("Голосовой ввод");
        voice.setOnClickListener(v -> launchVoice(field, prompt));
        row.addView(voice, new LinearLayout.LayoutParams(dp(54), dp(52)));
        return row;
    }

    private void launchVoice(EditText target, String prompt) {
        voiceTarget = target;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        try {
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (ActivityNotFoundException error) {
            toast("На телефоне не найден сервис голосового ввода.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != VOICE_REQUEST || resultCode != RESULT_OK || data == null || voiceTarget == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String spoken = results.get(0).trim();
        String current = voiceTarget.getText().toString().trim();
        voiceTarget.setText(current.isEmpty() ? spoken : current + " " + spoken);
        voiceTarget.setSelection(voiceTarget.getText().length());
    }

    private List<String> savedSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "");
        if (!saved.trim().isEmpty()) {
            String[] parts = saved.split("\\|");
            for (String part : parts) if (!part.trim().isEmpty()) result.add(part.trim());
        }
        if (result.isEmpty()) result.add(profile.getString("sport", SPORTS[0]));
        return result;
    }

    private String sportsDisplay() {
        return TextUtils.join(" · ", savedSports());
    }

    private LinearLayout progressRow() {
        LinearLayout row = horizontal(6);
        row.addView(progressMetric("Дни", profile.getInt("mission_active_days", 0)), new LinearLayout.LayoutParams(0, dp(68), 1));
        row.addView(progressMetric("Попытки", profile.getInt("mission_attempts", 0)), new LinearLayout.LayoutParams(0, dp(68), 1));
        row.addView(progressMetric("Успехи", profile.getInt("mission_successes", 0)), new LinearLayout.LayoutParams(0, dp(68), 1));
        row.addView(progressMetric("Финиши", profile.getInt("mission_finishes", 0)), new LinearLayout.LayoutParams(0, dp(68), 1));
        return row;
    }

    private LinearLayout progressMetric(String title, int value) {
        LinearLayout box = vertical(1);
        box.setGravity(Gravity.CENTER);
        box.setBackground(rounded(PRIMARY_SOFT, 12, 0, Color.TRANSPARENT));
        box.addView(label(String.valueOf(value), 20, PRIMARY, true));
        box.addView(label(title, 10, MUTED, false));
        return box;
    }

    private View infoRow(String title, String details) {
        LinearLayout row = vertical(2);
        row.setPadding(0, dp(7), 0, dp(7));
        row.addView(label(title, 16, TEXT, true));
        row.addView(label(details, 14, MUTED, false));
        return row;
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

    private LinearLayout heroCard() {
        LinearLayout layout = vertical(8);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackground(rounded(PRIMARY_DARK, 22, 0, Color.TRANSPARENT));
        layout.setElevation(dp(3));
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = vertical(9);
        layout.setPadding(dp(18), dp(17), dp(18), dp(17));
        layout.setBackground(rounded(CARD, 18, 1, BORDER));
        layout.setElevation(dp(1));
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
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.13f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView sectionTitle(String value) {
        return label(value, 18, TEXT, true);
    }

    private TextView fieldLabel(String value) {
        TextView view = label(value, 13, MUTED, true);
        view.setPadding(0, dp(5), 0, 0);
        return view;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 147, 143));
        field.setInputType(inputType);
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
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
        spinner.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) spinner.setSelection(i);
        }
        return spinner;
    }

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
    }

    private Button lightButton(String value) {
        return styledButton(value, PRIMARY_DARK, Color.WHITE, 15, 50);
    }

    private Button navButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 13, 46);
    }

    private Button miniButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 12, 46);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class DisplayTask {
        final String kind;
        final String title;
        final String details;
        final String action;

        DisplayTask(String kind, String title, String details, String action) {
            this.kind = kind;
            this.title = title;
            this.details = details;
            this.action = action;
        }
    }

    private static final class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawable(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
