package com.valera2872.bjjarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String PREFS = "bjj_arm_tracker";
    private static final int NAV_TODAY = 0;
    private static final int NAV_PLAN = 1;
    private static final int NAV_PROGRESS = 2;
    private static final int NAV_PROFILE = 3;

    private static final int BG = Color.rgb(246, 247, 242);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(31, 40, 42);
    private static final int MUTED = Color.rgb(101, 113, 114);
    private static final int PRIMARY = Color.rgb(32, 104, 93);
    private static final int PRIMARY_SOFT = Color.rgb(222, 239, 234);
    private static final int WARNING = Color.rgb(184, 112, 30);
    private static final int DANGER = Color.rgb(176, 64, 64);
    private static final int BORDER = Color.rgb(222, 227, 222);

    private SharedPreferences prefs;
    private FrameLayout contentHost;
    private LinearLayout navBar;
    private int currentNav = NAV_TODAY;
    private boolean lightMode = false;

    private static final String[] DAYS = {
            "Воскресенье", "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
    };

    private final Exercise[] workoutA = new Exercise[] {
            new Exercise("hammer_curl", "Молотковые сгибания", "Гантели. Локти рядом с корпусом, кисть нейтрально. Без раскачки.", 10, 14, 3.0, 0.5, false, true),
            new Exercise("triceps_pressdown", "Разгибание рук на блоке", "Локти не уходят вперёд. Полное, но мягкое разгибание без удара в сустав.", 10, 14, 5.0, 1.0, false, true),
            new Exercise("reverse_curl", "Обратные сгибания", "Очень лёгкий вес, ладони вниз. Движение медленное, запястье прямое.", 12, 15, 2.0, 0.5, false, false),
            new Exercise("wrist_extension", "Разгибание кистей", "Предплечья лежат на опоре, ладони вниз. Работает только кисть.", 15, 20, 1.5, 0.5, false, false),
            new Exercise("farmer_hold", "Удержание гантелей", "Стой ровно, плечи опущены, не зажимай шею. Держи хват без боли в пальцах.", 25, 40, 8.0, 1.0, true, false)
    };

    private final Exercise[] workoutB = new Exercise[] {
            new Exercise("supinated_curl", "Сгибания с разворотом ладони", "Поднимай гантели поочерёдно. Вверху ладонь смотрит к плечу.", 10, 14, 3.0, 0.5, false, true),
            new Exercise("overhead_triceps", "Разгибание гантели из-за головы", "Одна лёгкая гантель двумя руками. Не прогибай поясницу, локти направлены вперёд.", 10, 14, 4.0, 0.5, false, true),
            new Exercise("zottman", "Сгибания Зоттмана", "Вверх ладонями к себе, вниз ладонями от себя. Вес меньше обычного.", 10, 12, 2.0, 0.5, false, false),
            new Exercise("pronation", "Повороты предплечья с лёгкой гантелью", "Держи гантель за один край. Медленно поворачивай кисть внутрь и наружу.", 12, 15, 1.0, 0.25, false, false),
            new Exercise("static_curl", "Статическое удержание под 90°", "Локти прижаты. Держи гантели без раскачки и без подъёма плеч.", 20, 30, 3.0, 0.5, true, false)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        requestNotificationPermissionIfNeeded();
        scheduleAllReminders(this);

        if (!prefs.getBoolean("onboarded", false)) {
            showOnboarding();
        } else {
            buildShell();
            showToday();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
        }
    }

    private void showOnboarding() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout box = vertical(20);
        box.setPadding(dp(24), dp(34), dp(24), dp(34));
        scroll.addView(box);

        TextView eyebrow = label("ДОПОЛНЕНИЕ К ТРЕНИРОВКАМ BJJ", 12, PRIMARY, true);
        box.addView(eyebrow);
        TextView title = label("Сильнее руки —\nлучше контроль", 32, TEXT, true);
        title.setPadding(0, dp(10), 0, dp(8));
        box.addView(title);
        TextView intro = label("Две короткие тренировки в неделю для бицепса, трицепса, предплечий и хвата. Без отказа и без тяжёлых максимальных весов.", 17, MUTED, false);
        intro.setLineSpacing(0, 1.15f);
        box.addView(intro);

        box.addView(space(22));
        LinearLayout card = card();
        box.addView(card);
        card.addView(sectionTitle("Профиль спортсмена"));

        EditText name = input("Имя", prefs.getString("name", ""), InputType.TYPE_CLASS_TEXT);
        EditText age = input("Возраст", "14", InputType.TYPE_CLASS_NUMBER);
        EditText weight = input("Масса тела, кг (необязательно)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText bjj = input("Тренировок BJJ в неделю", "3", InputType.TYPE_CLASS_NUMBER);
        card.addView(name);
        card.addView(age);
        card.addView(weight);
        card.addView(bjj);

        TextView rules = label("Важно: упражнения выполняются под присмотром взрослого, с чистой техникой и запасом 2–3 повторения. Боль в суставе — причина остановиться, а не преодолевать её.", 14, MUTED, false);
        rules.setPadding(0, dp(14), 0, dp(14));
        card.addView(rules);

        Button start = primaryButton("Начать программу");
        start.setOnClickListener(v -> {
            int parsedAge = safeInt(age.getText().toString(), 14);
            if (parsedAge < 10 || parsedAge > 17) {
                toast("Эта версия рассчитана на возраст 10–17 лет.");
                return;
            }
            prefs.edit()
                    .putBoolean("onboarded", true)
                    .putString("name", name.getText().toString().trim().isEmpty() ? "Спортсмен" : name.getText().toString().trim())
                    .putInt("age", parsedAge)
                    .putFloat("body_weight", (float) safeDouble(weight.getText().toString(), 0))
                    .putInt("bjj_days", Math.max(1, Math.min(7, safeInt(bjj.getText().toString(), 3))))
                    .apply();
            buildShell();
            showToday();
        });
        box.addView(space(16));
        box.addView(start);
        setContentView(scroll);
    }

    private void buildShell() {
        LinearLayout shell = vertical(0);
        shell.setBackgroundColor(BG);
        contentHost = new FrameLayout(this);
        shell.addView(contentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        navBar = horizontal(0);
        navBar.setPadding(dp(8), dp(7), dp(8), dp(9));
        navBar.setBackgroundColor(Color.WHITE);
        shell.addView(navBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
        renderNav();
        setContentView(shell);
    }

    private void renderNav() {
        navBar.removeAllViews();
        addNavButton("Сегодня", NAV_TODAY);
        addNavButton("План", NAV_PLAN);
        addNavButton("Прогресс", NAV_PROGRESS);
        addNavButton("Профиль", NAV_PROFILE);
    }

    private void addNavButton(String text, int index) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(currentNav == index ? PRIMARY : MUTED);
        b.setTypeface(Typeface.DEFAULT, currentNav == index ? Typeface.BOLD : Typeface.NORMAL);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setOnClickListener(v -> {
            currentNav = index;
            renderNav();
            if (index == NAV_TODAY) showToday();
            else if (index == NAV_PLAN) showPlan();
            else if (index == NAV_PROGRESS) showProgress();
            else showProfile();
        });
        navBar.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void showToday() {
        currentNav = NAV_TODAY;
        if (navBar != null) renderNav();
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        String name = prefs.getString("name", "Спортсмен");
        page.addView(label("Сегодня", 13, PRIMARY, true));
        page.addView(label("Готов укрепить руки, " + name + "?", 27, TEXT, true));
        page.addView(label("Сначала проверим восстановление, затем приложение предложит нужный объём.", 15, MUTED, false));
        page.addView(space(18));

        int next = prefs.getInt("next_workout", 0);
        Exercise[] workout = next == 0 ? workoutA : workoutB;
        LinearLayout hero = card();
        page.addView(hero);
        TextView nextLabel = label("СЛЕДУЮЩАЯ ТРЕНИРОВКА", 12, PRIMARY, true);
        hero.addView(nextLabel);
        hero.addView(label(next == 0 ? "Тренировка A · сгибания и хват" : "Тренировка B · руки и контроль", 22, TEXT, true));
        String when = nextAllowedText();
        TextView whenView = label(when, 14, when.startsWith("Можно") ? PRIMARY : WARNING, true);
        whenView.setPadding(0, dp(5), 0, dp(12));
        hero.addView(whenView);

        int totalSets = 0;
        for (Exercise ex : workout) totalSets += lightMode ? 1 : getSets(ex);
        hero.addView(label(workout.length + " упражнений · около 25 минут · " + totalSets + " подходов", 14, MUTED, false));
        hero.addView(space(13));
        Button start = primaryButton("Проверить готовность и начать");
        start.setOnClickListener(v -> runReadinessCheck(next));
        hero.addView(start);

        page.addView(space(14));
        LinearLayout summary = card();
        page.addView(summary);
        summary.addView(sectionTitle("Текущий ритм"));
        int week = sessionsLastDays(7);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(2);
        progress.setProgress(Math.min(2, week));
        summary.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));
        summary.addView(label(week + " из 2 дополнительных тренировок за последние 7 дней", 14, MUTED, false));
        String last = prefs.getString("last_session_date", "");
        summary.addView(label(last.isEmpty() ? "Первая тренировка ещё впереди" : "Последняя тренировка: " + formatDateLong(last), 14, TEXT, true));

        page.addView(space(14));
        LinearLayout note = tintedCard(PRIMARY_SOFT);
        page.addView(note);
        note.addView(label("Почему не каждый день?", 17, TEXT, true));
        note.addView(label("Руки уже получают нагрузку на BJJ. Дополнительные занятия нужны как точная дозировка, а не как ещё одна тяжёлая тренировка.", 14, MUTED, false));

        setPage(scroll);
    }

    private void runReadinessCheck(int workoutIndex) {
        String[] items = {
                "Есть боль в локте, плече, кисти или шее",
                "Сегодня была тяжёлая тренировка BJJ",
                "До соревнования или тяжёлых спаррингов меньше 24 часов",
                "Спал меньше 7 часов или чувствую сильную усталость"
        };
        boolean[] checked = new boolean[items.length];
        new AlertDialog.Builder(this)
                .setTitle("Как восстановился организм?")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Продолжить", (dialog, which) -> {
                    if (checked[0]) {
                        new AlertDialog.Builder(this)
                                .setTitle("Сегодня без силовой тренировки")
                                .setMessage("Боль в суставе или шее нельзя использовать как сигнал для увеличения нагрузки. Отдохни. Если боль повторяется или усиливается, нужна очная оценка специалиста.")
                                .setPositiveButton("Понятно", null)
                                .show();
                        return;
                    }
                    if (checked[2]) {
                        new AlertDialog.Builder(this)
                                .setTitle("Лучше перенести")
                                .setMessage("Перед соревнованием или тяжёлыми спаррингами дополнительная усталость рук может помешать хвату и технике. Вернись к тренировке после восстановления.")
                                .setPositiveButton("Перенести", null)
                                .show();
                        return;
                    }
                    lightMode = checked[1] || checked[3];
                    if (lightMode) {
                        new AlertDialog.Builder(this)
                                .setTitle("Облегчённый режим")
                                .setMessage("Сегодня приложение оставит по одному рабочему подходу и не будет повышать нагрузку.")
                                .setNegativeButton("Перенести", null)
                                .setPositiveButton("Начать легко", (d, w) -> showWorkoutSession(workoutIndex))
                                .show();
                    } else {
                        showWorkoutSession(workoutIndex);
                    }
                })
                .show();
    }

    private void showWorkoutSession(int workoutIndex) {
        Exercise[] workout = workoutIndex == 0 ? workoutA : workoutB;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(label(lightMode ? "ОБЛЕГЧЁННАЯ ТРЕНИРОВКА" : "РАБОЧАЯ ТРЕНИРОВКА", 12, lightMode ? WARNING : PRIMARY, true));
        page.addView(label(workoutIndex == 0 ? "Тренировка A" : "Тренировка B", 28, TEXT, true));
        page.addView(label("Темп: 2 секунды вверх, короткая пауза, 2–3 секунды вниз. Ни одного повторения через потерю техники.", 14, MUTED, false));
        page.addView(space(14));

        Map<String, List<CheckBox>> checks = new HashMap<>();
        Map<String, TextView> weightViews = new HashMap<>();

        for (int i = 0; i < workout.length; i++) {
            Exercise ex = workout[i];
            LinearLayout exCard = card();
            page.addView(exCard);
            TextView index = label((i + 1) + "", 12, PRIMARY, true);
            exCard.addView(index);
            exCard.addView(label(ex.name, 19, TEXT, true));
            exCard.addView(label(ex.cue, 13, MUTED, false));

            int targetSets = lightMode ? 1 : getSets(ex);
            int targetReps = getReps(ex);
            String unit = ex.timed ? "сек" : "повт.";
            TextView target = label(targetSets + " × " + targetReps + " " + unit, 16, PRIMARY, true);
            target.setPadding(0, dp(10), 0, dp(7));
            exCard.addView(target);

            LinearLayout weightRow = horizontal(8);
            Button minus = smallButton("−");
            TextView weight = label(weightText(getWeight(ex)), 16, TEXT, true);
            weight.setGravity(Gravity.CENTER);
            Button plus = smallButton("+");
            weightRow.addView(minus, new LinearLayout.LayoutParams(dp(52), dp(44)));
            weightRow.addView(weight, new LinearLayout.LayoutParams(0, dp(44), 1));
            weightRow.addView(plus, new LinearLayout.LayoutParams(dp(52), dp(44)));
            exCard.addView(weightRow);
            weightViews.put(ex.id, weight);

            minus.setOnClickListener(v -> {
                double nextWeight = Math.max(ex.increment, getWeight(ex) - ex.increment);
                setWeight(ex, nextWeight);
                weight.setText(weightText(nextWeight));
            });
            plus.setOnClickListener(v -> {
                double nextWeight = getWeight(ex) + ex.increment;
                setWeight(ex, nextWeight);
                weight.setText(weightText(nextWeight));
            });

            List<CheckBox> setChecks = new ArrayList<>();
            LinearLayout setRow = horizontal(6);
            for (int s = 1; s <= targetSets; s++) {
                CheckBox cb = new CheckBox(this);
                cb.setText("Подход " + s);
                cb.setTextSize(13);
                cb.setTextColor(TEXT);
                setRow.addView(cb, new LinearLayout.LayoutParams(0, dp(48), 1));
                setChecks.add(cb);
            }
            exCard.addView(setRow);
            checks.put(ex.id, setChecks);
            if (i < workout.length - 1) page.addView(space(12));
        }

        page.addView(space(16));
        LinearLayout finish = card();
        page.addView(finish);
        finish.addView(sectionTitle("После последнего подхода"));

        CheckBox technique = new CheckBox(this);
        technique.setText("Техника оставалась чистой");
        technique.setChecked(true);
        technique.setTextColor(TEXT);
        finish.addView(technique);

        CheckBox pain = new CheckBox(this);
        pain.setText("Появилась боль в суставе или резкая боль");
        pain.setTextColor(DANGER);
        finish.addView(pain);

        finish.addView(label("Насколько тяжело было?", 14, TEXT, true));
        RadioGroup difficulty = new RadioGroup(this);
        difficulty.setOrientation(RadioGroup.HORIZONTAL);
        String[] levels = {"Легко", "В самый раз", "Тяжело"};
        for (int i = 0; i < levels.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setText(levels[i]);
            radio.setId(1000 + i);
            radio.setTextSize(13);
            difficulty.addView(radio, new RadioGroup.LayoutParams(0, dp(50), 1));
        }
        difficulty.check(1001);
        finish.addView(difficulty);

        EditText note = input("Короткая заметка (необязательно)", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        finish.addView(note);
        Button complete = primaryButton("Завершить тренировку");
        finish.addView(complete);
        complete.setOnClickListener(v -> {
            int completedExercises = 0;
            for (Exercise ex : workout) {
                boolean all = true;
                for (CheckBox cb : checks.get(ex.id)) all &= cb.isChecked();
                if (all) completedExercises++;
            }
            if (completedExercises < workout.length) {
                new AlertDialog.Builder(this)
                        .setTitle("Не все подходы отмечены")
                        .setMessage("Можно сохранить неполную тренировку. Нагрузка после неё повышаться не будет.")
                        .setNegativeButton("Вернуться", null)
                        .setPositiveButton("Сохранить", (d, w) -> finishSession(workoutIndex, workout, checks, technique.isChecked(), pain.isChecked(), difficulty.getCheckedRadioButtonId() - 1000, note.getText().toString()))
                        .show();
            } else {
                finishSession(workoutIndex, workout, checks, technique.isChecked(), pain.isChecked(), difficulty.getCheckedRadioButtonId() - 1000, note.getText().toString());
            }
        });

        setPage(scroll);
    }

    private void finishSession(int workoutIndex, Exercise[] workout, Map<String, List<CheckBox>> checks,
                               boolean goodTechnique, boolean pain, int difficulty, String note) {
        int completedSets = 0;
        double volume = 0;
        boolean complete = true;
        for (Exercise ex : workout) {
            int exCompleted = 0;
            for (CheckBox cb : checks.get(ex.id)) if (cb.isChecked()) exCompleted++;
            completedSets += exCompleted;
            if (exCompleted < checks.get(ex.id).size()) complete = false;
            if (!ex.timed) volume += exCompleted * getReps(ex) * getWeight(ex);
        }

        if (complete && !lightMode) {
            adaptProgram(workout, goodTechnique, pain, difficulty);
        }

        JSONObject session = new JSONObject();
        try {
            session.put("date", todayKey());
            session.put("timestamp", System.currentTimeMillis());
            session.put("workout", workoutIndex == 0 ? "A" : "B");
            session.put("sets", completedSets);
            session.put("volume", Math.round(volume));
            session.put("difficulty", difficulty);
            session.put("technique", goodTechnique);
            session.put("pain", pain);
            session.put("light", lightMode);
            session.put("note", note == null ? "" : note.trim());
            JSONArray history = getArray("sessions");
            history.put(session);
            prefs.edit()
                    .putString("sessions", history.toString())
                    .putString("last_session_date", todayKey())
                    .putInt("next_workout", complete ? 1 - workoutIndex : workoutIndex)
                    .apply();
        } catch (JSONException ignored) {
        }

        String result;
        if (pain) result = "Нагрузка снижена. До исчезновения боли силовую тренировку не повторять.";
        else if (!goodTechnique || difficulty == 2) result = "Следующая тренировка будет немного легче, чтобы вернуть чистую технику.";
        else if (lightMode || !complete) result = "Облегчённая или неполная тренировка сохранена без повышения нагрузки.";
        else result = "Тренировка сохранена. Приложение обновило цели следующего занятия.";

        lightMode = false;
        new AlertDialog.Builder(this)
                .setTitle("Готово")
                .setMessage(result)
                .setPositiveButton("На главный экран", (d, w) -> showToday())
                .setCancelable(false)
                .show();
    }

    private void adaptProgram(Exercise[] workout, boolean goodTechnique, boolean pain, int difficulty) {
        for (Exercise ex : workout) {
            int reps = getReps(ex);
            int sets = getSets(ex);
            int mastery = prefs.getInt("mastery_" + ex.id, 0);
            int top = prefs.getInt("top_" + ex.id, 0);
            int hard = prefs.getInt("hard_" + ex.id, 0);

            if (pain) {
                setWeight(ex, Math.max(ex.increment, roundToIncrement(getWeight(ex) * 0.9, ex.increment)));
                prefs.edit().putInt("hard_" + ex.id, 0).putInt("top_" + ex.id, 0).apply();
                continue;
            }

            if (!goodTechnique || difficulty == 2) {
                hard++;
                reps = Math.max(ex.minReps, reps - 1);
                if (hard >= 2) {
                    setWeight(ex, Math.max(ex.increment, roundToIncrement(getWeight(ex) - ex.increment, ex.increment)));
                    hard = 0;
                }
                prefs.edit().putInt("reps_" + ex.id, reps).putInt("hard_" + ex.id, hard).putInt("top_" + ex.id, 0).apply();
                continue;
            }

            hard = 0;
            mastery++;
            if (reps < ex.maxReps) {
                reps = Math.min(ex.maxReps, reps + (difficulty == 0 ? 2 : 1));
                top = 0;
            } else {
                top++;
                if (difficulty == 0 || top >= 2) {
                    setWeight(ex, getWeight(ex) + ex.increment);
                    reps = ex.minReps;
                    top = 0;
                }
            }

            if (ex.canReachThreeSets && mastery >= 5 && sets < 3) {
                sets = 3;
            }
            prefs.edit()
                    .putInt("reps_" + ex.id, reps)
                    .putInt("sets_" + ex.id, sets)
                    .putInt("mastery_" + ex.id, mastery)
                    .putInt("top_" + ex.id, top)
                    .putInt("hard_" + ex.id, hard)
                    .apply();
        }
    }

    private void showPlan() {
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(label("Программа", 13, PRIMARY, true));
        page.addView(label("Две тренировки, которые дополняют BJJ", 27, TEXT, true));
        page.addView(label("Между занятиями — не меньше 48 часов. Не ставь их перед соревнованием и не выполняй после изматывающих спаррингов.", 15, MUTED, false));
        page.addView(space(16));
        addWorkoutPlanCard(page, "A", "Сгибания, трицепс и хват", workoutA);
        page.addView(space(14));
        addWorkoutPlanCard(page, "B", "Контроль предплечья и устойчивость рук", workoutB);
        page.addView(space(14));
        LinearLayout rules = tintedCard(PRIMARY_SOFT);
        page.addView(rules);
        rules.addView(sectionTitle("Правило прогрессии"));
        rules.addView(label("1. Сначала добавляются повторения.\n2. После двух чистых выполнений верхней границы добавляется минимальный вес.\n3. Третий подход появляется только после нескольких успешных тренировок.\n4. При боли, плохой технике или повторной тяжести нагрузка снижается.", 14, TEXT, false));
        setPage(scroll);
    }

    private void addWorkoutPlanCard(LinearLayout page, String letter, String subtitle, Exercise[] workout) {
        LinearLayout block = card();
        page.addView(block);
        block.addView(label("ТРЕНИРОВКА " + letter, 12, PRIMARY, true));
        block.addView(label(subtitle, 20, TEXT, true));
        for (Exercise ex : workout) {
            LinearLayout row = vertical(3);
            row.setPadding(0, dp(12), 0, dp(9));
            TextView title = label(ex.name, 15, TEXT, true);
            TextView target = label(getSets(ex) + " × " + getReps(ex) + (ex.timed ? " сек · " : " повторов · ") + weightText(getWeight(ex)), 13, PRIMARY, true);
            row.addView(title);
            row.addView(target);
            block.addView(row);
        }
    }

    private void showProgress() {
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(label("Прогресс", 13, PRIMARY, true));
        page.addView(label("Смотрим не только на сантиметры", 27, TEXT, true));
        page.addView(label("Главные признаки прогресса: чистая техника, больший рабочий вес, стабильные подходы и отсутствие боли.", 15, MUTED, false));
        page.addView(space(16));

        LinearLayout stats = card();
        page.addView(stats);
        stats.addView(sectionTitle("Итоги"));
        int all = getArray("sessions").length();
        int month = sessionsLastDays(30);
        int streak = calculateStreak();
        stats.addView(metricRow("Всего тренировок", String.valueOf(all)));
        stats.addView(metricRow("За 30 дней", String.valueOf(month)));
        stats.addView(metricRow("Серия недель", streak + ""));

        page.addView(space(14));
        LinearLayout measures = card();
        page.addView(measures);
        measures.addView(sectionTitle("Замеры"));
        Measurement latest = latestMeasurement();
        if (latest == null) {
            measures.addView(label("Пока нет замеров. Первый замер станет исходной точкой.", 14, MUTED, false));
        } else {
            measures.addView(metricRow("Плечо в напряжении", decimal(latest.flexed) + " см"));
            measures.addView(metricRow("Предплечье", decimal(latest.forearm) + " см"));
            if (latest.weight > 0) measures.addView(metricRow("Масса тела", decimal(latest.weight) + " кг"));
        }
        Button add = secondaryButton("Добавить замер");
        add.setOnClickListener(v -> showMeasurementDialog());
        measures.addView(space(10));
        measures.addView(add);

        List<Measurement> list = measurementList();
        if (list.size() >= 2) {
            page.addView(space(14));
            LinearLayout chartCard = card();
            page.addView(chartCard);
            chartCard.addView(sectionTitle("Плечо в напряжении"));
            MeasureChart chart = new MeasureChart(this, list);
            chartCard.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210)));
        }

        page.addView(space(14));
        LinearLayout historyCard = card();
        page.addView(historyCard);
        historyCard.addView(sectionTitle("Последние тренировки"));
        JSONArray sessions = getArray("sessions");
        if (sessions.length() == 0) {
            historyCard.addView(label("История появится после первой тренировки.", 14, MUTED, false));
        } else {
            int from = Math.max(0, sessions.length() - 6);
            for (int i = sessions.length() - 1; i >= from; i--) {
                try {
                    JSONObject s = sessions.getJSONObject(i);
                    String extra = s.optBoolean("light", false) ? " · облегчённая" : "";
                    String pain = s.optBoolean("pain", false) ? " · отмечена боль" : "";
                    historyCard.addView(metricRow(formatDateLong(s.optString("date")) + " · тренировка " + s.optString("workout"), s.optInt("sets") + " подходов" + extra + pain));
                } catch (JSONException ignored) {
                }
            }
        }
        setPage(scroll);
    }

    private void showMeasurementDialog() {
        LinearLayout form = vertical(10);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText relaxed = input("Плечо расслаблено, см", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText flexed = input("Плечо в напряжении, см", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText forearm = input("Предплечье, см", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText weight = input("Масса тела, кг", prefs.getFloat("body_weight", 0) > 0 ? decimal(prefs.getFloat("body_weight", 0)) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(relaxed);
        form.addView(flexed);
        form.addView(forearm);
        form.addView(weight);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Новый замер")
                .setMessage("Измеряй в одинаковое время суток, до тренировки, одной и той же лентой и без перетягивания кожи.")
                .setView(form)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            double f = safeDouble(flexed.getText().toString(), 0);
            double a = safeDouble(relaxed.getText().toString(), 0);
            double fore = safeDouble(forearm.getText().toString(), 0);
            double w = safeDouble(weight.getText().toString(), 0);
            if (f <= 0 || fore <= 0) {
                toast("Нужны как минимум замеры плеча в напряжении и предплечья.");
                return;
            }
            JSONObject m = new JSONObject();
            try {
                m.put("date", todayKey());
                m.put("timestamp", System.currentTimeMillis());
                m.put("relaxed", a);
                m.put("flexed", f);
                m.put("forearm", fore);
                m.put("weight", w);
                JSONArray array = getArray("measurements");
                array.put(m);
                prefs.edit().putString("measurements", array.toString()).putFloat("body_weight", (float) w).apply();
            } catch (JSONException ignored) {
            }
            dialog.dismiss();
            showProgress();
        }));
        dialog.show();
    }

    private void showProfile() {
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(label("Профиль", 13, PRIMARY, true));
        page.addView(label("Настройки спортсмена", 27, TEXT, true));
        page.addView(space(16));

        LinearLayout info = card();
        page.addView(info);
        info.addView(sectionTitle("Основные данные"));
        EditText name = input("Имя", prefs.getString("name", "Спортсмен"), InputType.TYPE_CLASS_TEXT);
        EditText age = input("Возраст", String.valueOf(prefs.getInt("age", 14)), InputType.TYPE_CLASS_NUMBER);
        EditText bjj = input("Тренировок BJJ в неделю", String.valueOf(prefs.getInt("bjj_days", 3)), InputType.TYPE_CLASS_NUMBER);
        info.addView(name);
        info.addView(age);
        info.addView(bjj);
        Button save = secondaryButton("Сохранить профиль");
        save.setOnClickListener(v -> {
            prefs.edit()
                    .putString("name", name.getText().toString().trim().isEmpty() ? "Спортсмен" : name.getText().toString().trim())
                    .putInt("age", Math.max(10, Math.min(17, safeInt(age.getText().toString(), 14))))
                    .putInt("bjj_days", Math.max(1, Math.min(7, safeInt(bjj.getText().toString(), 3))))
                    .apply();
            toast("Профиль сохранён");
        });
        info.addView(space(8));
        info.addView(save);

        page.addView(space(14));
        LinearLayout reminders = card();
        page.addView(reminders);
        reminders.addView(sectionTitle("Напоминания"));
        int day1 = prefs.getInt("reminder_day_1", Calendar.TUESDAY);
        int day2 = prefs.getInt("reminder_day_2", Calendar.FRIDAY);
        int hour = prefs.getInt("reminder_hour", 18);
        int minute = prefs.getInt("reminder_minute", 30);
        TextView schedule = label(scheduleText(day1, day2, hour, minute), 15, TEXT, true);
        reminders.addView(schedule);
        reminders.addView(label("Телефон может немного сдвинуть время из-за режима энергосбережения.", 13, MUTED, false));
        Button configure = secondaryButton("Изменить дни и время");
        configure.setOnClickListener(v -> showReminderDialog(schedule));
        reminders.addView(space(10));
        reminders.addView(configure);

        page.addView(space(14));
        LinearLayout safety = tintedCard(Color.rgb(250, 237, 233));
        page.addView(safety);
        safety.addView(sectionTitle("Когда остановиться"));
        safety.addView(label("Боль в суставе, шее или позвоночнике; онемение; заметная асимметрия силы; отёк; резкая боль; ухудшение, которое не проходит после обычного восстановления.", 14, TEXT, false));

        page.addView(space(14));
        Button reset = secondaryButton("Сбросить тренировочные цели");
        reset.setTextColor(DANGER);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Сбросить нагрузки?")
                .setMessage("История и замеры останутся, но веса, подходы и повторения вернутся к исходным значениям.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сбросить", (d, w) -> resetProgram())
                .show());
        page.addView(reset);
        setPage(scroll);
    }

    private void showReminderDialog(TextView scheduleView) {
        LinearLayout form = vertical(10);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        Spinner first = new Spinner(this);
        Spinner second = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DAYS);
        first.setAdapter(adapter);
        second.setAdapter(adapter);
        first.setSelection(prefs.getInt("reminder_day_1", Calendar.TUESDAY) - 1);
        second.setSelection(prefs.getInt("reminder_day_2", Calendar.FRIDAY) - 1);
        TextView time = label(String.format(Locale.getDefault(), "%02d:%02d", prefs.getInt("reminder_hour", 18), prefs.getInt("reminder_minute", 30)), 20, PRIMARY, true);
        Button chooseTime = secondaryButton("Выбрать время");
        final int[] chosen = {prefs.getInt("reminder_hour", 18), prefs.getInt("reminder_minute", 30)};
        chooseTime.setOnClickListener(v -> new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            chosen[0] = hourOfDay;
            chosen[1] = minute;
            time.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, chosen[0], chosen[1], true).show());
        form.addView(label("Первый день", 13, MUTED, true));
        form.addView(first);
        form.addView(label("Второй день", 13, MUTED, true));
        form.addView(second);
        form.addView(time);
        form.addView(chooseTime);

        new AlertDialog.Builder(this)
                .setTitle("Расписание")
                .setView(form)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", (d, w) -> {
                    int d1 = first.getSelectedItemPosition() + 1;
                    int d2 = second.getSelectedItemPosition() + 1;
                    if (d1 == d2) {
                        toast("Выбери два разных дня.");
                        return;
                    }
                    prefs.edit()
                            .putInt("reminder_day_1", d1)
                            .putInt("reminder_day_2", d2)
                            .putInt("reminder_hour", chosen[0])
                            .putInt("reminder_minute", chosen[1])
                            .apply();
                    scheduleAllReminders(this);
                    scheduleView.setText(scheduleText(d1, d2, chosen[0], chosen[1]));
                })
                .show();
    }

    private void resetProgram() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Exercise ex : allExercises()) {
            editor.remove("weight_" + ex.id)
                    .remove("reps_" + ex.id)
                    .remove("sets_" + ex.id)
                    .remove("mastery_" + ex.id)
                    .remove("top_" + ex.id)
                    .remove("hard_" + ex.id);
        }
        editor.putInt("next_workout", 0).apply();
        toast("Тренировочные цели сброшены");
        showProfile();
    }

    public static void scheduleAllReminders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int day1 = prefs.getInt("reminder_day_1", Calendar.TUESDAY);
        int day2 = prefs.getInt("reminder_day_2", Calendar.FRIDAY);
        int hour = prefs.getInt("reminder_hour", 18);
        int minute = prefs.getInt("reminder_minute", 30);
        scheduleWeekly(context, day1, hour, minute, 501);
        scheduleWeekly(context, day2, hour, minute, 502);
    }

    private static void scheduleWeekly(Context context, int dayOfWeek, int hour, int minute, int requestCode) {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= System.currentTimeMillis()) target.add(Calendar.DAY_OF_YEAR, 7);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMINDER);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    target.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7,
                    pending
            );
        }
    }

    private String nextAllowedText() {
        String last = prefs.getString("last_session_date", "");
        if (last.isEmpty()) return "Можно начинать сегодня";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(last);
            if (d == null) return "Можно начинать сегодня";
            long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - d.getTime());
            if (days >= 2) return "Можно начинать сегодня";
            return "После прошлой тренировки прошло меньше 48 часов";
        } catch (Exception e) {
            return "Можно начинать сегодня";
        }
    }

    private int sessionsLastDays(int days) {
        JSONArray array = getArray("sessions");
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        int count = 0;
        for (int i = 0; i < array.length(); i++) {
            try {
                if (array.getJSONObject(i).optLong("timestamp", 0) >= cutoff) count++;
            } catch (JSONException ignored) {
            }
        }
        return count;
    }

    private int calculateStreak() {
        JSONArray array = getArray("sessions");
        if (array.length() == 0) return 0;
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try { times.add(array.getJSONObject(i).optLong("timestamp")); } catch (JSONException ignored) {}
        }
        Collections.sort(times, Collections.reverseOrder());
        long now = System.currentTimeMillis();
        int streak = 0;
        for (int week = 0; week < 20; week++) {
            long start = now - TimeUnit.DAYS.toMillis((week + 1L) * 7);
            long end = now - TimeUnit.DAYS.toMillis(week * 7L);
            int inWeek = 0;
            for (long t : times) if (t >= start && t < end) inWeek++;
            if (inWeek >= 1) streak++;
            else break;
        }
        return streak;
    }

    private Measurement latestMeasurement() {
        List<Measurement> list = measurementList();
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    private List<Measurement> measurementList() {
        JSONArray array = getArray("measurements");
        List<Measurement> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject m = array.getJSONObject(i);
                list.add(new Measurement(
                        m.optLong("timestamp", 0),
                        m.optDouble("relaxed", 0),
                        m.optDouble("flexed", 0),
                        m.optDouble("forearm", 0),
                        m.optDouble("weight", 0)
                ));
            } catch (JSONException ignored) {
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(a.timestamp, b.timestamp));
        return list;
    }

    private JSONArray getArray(String key) {
        try {
            return new JSONArray(prefs.getString(key, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private int getReps(Exercise ex) {
        return prefs.getInt("reps_" + ex.id, ex.minReps);
    }

    private int getSets(Exercise ex) {
        return prefs.getInt("sets_" + ex.id, 2);
    }

    private double getWeight(Exercise ex) {
        return prefs.getFloat("weight_" + ex.id, (float) ex.defaultWeight);
    }

    private void setWeight(Exercise ex, double value) {
        prefs.edit().putFloat("weight_" + ex.id, (float) value).apply();
    }

    private List<Exercise> allExercises() {
        List<Exercise> all = new ArrayList<>();
        Collections.addAll(all, workoutA);
        Collections.addAll(all, workoutB);
        return all;
    }

    private double roundToIncrement(double value, double increment) {
        return Math.round(value / increment) * increment;
    }

    private String weightText(double weight) {
        return decimal(weight) + " кг";
    }

    private String decimal(double value) {
        DecimalFormat format = new DecimalFormat("0.#");
        return format.format(value);
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String formatDateLong(String key) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key);
            if (date == null) return key;
            return new SimpleDateFormat("d MMMM", new Locale("ru")).format(date);
        } catch (Exception e) {
            return key;
        }
    }

    private String scheduleText(int day1, int day2, int hour, int minute) {
        return DAYS[day1 - 1] + " и " + DAYS[day2 - 1] + " · " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private int safeInt(String raw, int fallback) {
        try { return Integer.parseInt(raw.trim()); } catch (Exception e) { return fallback; }
    }

    private double safeDouble(String raw, double fallback) {
        try { return Double.parseDouble(raw.trim().replace(',', '.')); } catch (Exception e) { return fallback; }
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        return scroll;
    }

    private LinearLayout pageContent(ScrollView scroll) {
        LinearLayout page = vertical(5);
        page.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(page);
        return page;
    }

    private void setPage(View page) {
        contentHost.removeAllViews();
        contentHost.addView(page);
    }

    private LinearLayout card() {
        LinearLayout card = vertical(6);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(CARD, BORDER, 18));
        card.setElevation(dp(1));
        return card;
    }

    private LinearLayout tintedCard(int color) {
        LinearLayout card = vertical(6);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(color, Color.TRANSPARENT, 18));
        return card;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView title = label(text, 17, TEXT, true);
        title.setPadding(0, 0, 0, dp(9));
        return title;
    }

    private EditText input(String hint, String value, int type) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setInputType(type);
        edit.setTextSize(15);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(MUTED);
        edit.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        edit.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.setMargins(0, dp(5), 0, dp(5));
        edit.setLayoutParams(lp);
        return edit;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(PRIMARY, Color.TRANSPARENT, 14));
        button.setMinHeight(dp(52));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(Color.WHITE, PRIMARY, 13));
        button.setMinHeight(dp(48));
        return button;
    }

    private Button smallButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(21);
        button.setBackground(rounded(PRIMARY_SOFT, Color.TRANSPARENT, 12));
        return button;
    }

    private LinearLayout metricRow(String left, String right) {
        LinearLayout row = horizontal(8);
        row.setPadding(0, dp(9), 0, dp(9));
        TextView l = label(left, 14, TEXT, false);
        TextView r = label(right, 14, PRIMARY, true);
        r.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View space(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return view;
    }

    private LinearLayout vertical(int spacing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) layout.setDividerPadding(dp(spacing));
        return layout;
    }

    private LinearLayout horizontal(int spacing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        if (spacing > 0) layout.setPadding(0, dp(spacing / 2), 0, dp(spacing / 2));
        return layout;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private static final class Exercise {
        final String id;
        final String name;
        final String cue;
        final int minReps;
        final int maxReps;
        final double defaultWeight;
        final double increment;
        final boolean timed;
        final boolean canReachThreeSets;

        Exercise(String id, String name, String cue, int minReps, int maxReps,
                 double defaultWeight, double increment, boolean timed, boolean canReachThreeSets) {
            this.id = id;
            this.name = name;
            this.cue = cue;
            this.minReps = minReps;
            this.maxReps = maxReps;
            this.defaultWeight = defaultWeight;
            this.increment = increment;
            this.timed = timed;
            this.canReachThreeSets = canReachThreeSets;
        }
    }

    private static final class Measurement {
        final long timestamp;
        final double relaxed;
        final double flexed;
        final double forearm;
        final double weight;

        Measurement(long timestamp, double relaxed, double flexed, double forearm, double weight) {
            this.timestamp = timestamp;
            this.relaxed = relaxed;
            this.flexed = flexed;
            this.forearm = forearm;
            this.weight = weight;
        }
    }

    private static final class MeasureChart extends View {
        private final List<Measurement> values;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

        MeasureChart(Context context, List<Measurement> values) {
            super(context);
            this.values = values;
            line.setColor(PRIMARY);
            line.setStrokeWidth(6f);
            line.setStyle(Paint.Style.STROKE);
            grid.setColor(BORDER);
            grid.setStrokeWidth(2f);
            text.setColor(MUTED);
            text.setTextSize(28f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (values.size() < 2) return;
            float left = 48f;
            float top = 24f;
            float right = getWidth() - 22f;
            float bottom = getHeight() - 46f;
            for (int i = 0; i <= 4; i++) {
                float y = top + (bottom - top) * i / 4f;
                canvas.drawLine(left, y, right, y, grid);
            }
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (Measurement m : values) {
                min = Math.min(min, m.flexed);
                max = Math.max(max, m.flexed);
            }
            if (Math.abs(max - min) < 0.2) { max += 0.5; min -= 0.5; }
            min -= 0.3;
            max += 0.3;
            Path path = new Path();
            for (int i = 0; i < values.size(); i++) {
                float x = left + (right - left) * i / (values.size() - 1f);
                float y = bottom - (float) ((values.get(i).flexed - min) / (max - min)) * (bottom - top);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                canvas.drawCircle(x, y, 7f, line);
            }
            canvas.drawPath(path, line);
            canvas.drawText(new DecimalFormat("0.#").format(max) + " см", left, top + 18f, text);
            canvas.drawText(new DecimalFormat("0.#").format(min) + " см", left, bottom + 34f, text);
        }
    }
}
