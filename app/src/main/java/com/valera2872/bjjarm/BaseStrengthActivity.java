package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Second physical module: a compact, adaptive base-strength programme for
 * legs and trunk. It is intentionally conservative for athletes who already
 * train on the mat several times per week.
 */
public class BaseStrengthActivity extends Activity {
    private static final String PREFS = "combat_base_strength";

    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int DANGER = Color.rgb(177, 62, 62);

    private SharedPreferences prefs;
    private boolean lightMode;

    private final Exercise[] workoutA = {
            new Exercise("goblet_squat", "Приседание с гантелью перед грудью", "Колени следуют за носками, спина сохраняет устойчивое положение.", 8, 12, 2, 6.0, 1.0, false),
            new Exercise("one_arm_row", "Тяга гантели одной рукой с опорой", "Тяни локоть к тазу, не поворачивай корпус вслед за гантелью.", 8, 12, 2, 5.0, 1.0, false),
            new Exercise("reverse_lunge", "Выпад назад", "Шагни назад и сохрани устойчивость передней ноги.", 8, 10, 2, 3.0, 1.0, false),
            new Exercise("dead_bug", "Мёртвый жук", "Поясница остаётся под контролем, движение медленное.", 6, 10, 2, 0.0, 0.0, false),
            new Exercise("suitcase_hold", "Удержание гантели в одной руке", "Не наклоняйся к весу и не поднимай плечо.", 20, 40, 2, 8.0, 1.0, true)
    };

    private final Exercise[] workoutB = {
            new Exercise("romanian_deadlift", "Румынская тяга с гантелями", "Отводи таз назад, держи вес близко к ногам.", 8, 12, 2, 6.0, 1.0, false),
            new Exercise("step_up", "Подъём на устойчивую ступень", "Поднимайся за счёт рабочей ноги, не отталкивайся второй.", 8, 10, 2, 3.0, 1.0, false),
            new Exercise("floor_press", "Жим гантелей лёжа на полу", "Лопатки устойчивы, локти не проваливаются слишком низко.", 8, 12, 2, 3.0, 1.0, false),
            new Exercise("pallof_press", "Удержание корпуса с резинкой", "Не позволяй резинке разворачивать корпус.", 8, 12, 2, 0.0, 0.0, false),
            new Exercise("farmer_hold", "Фермерское удержание", "Стой ровно, удерживай лопатки и спокойное дыхание.", 20, 40, 2, 8.0, 1.0, true)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        showOverview();
    }

    private void showOverview() {
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);

        Button back = smallButton("← Combat Performance");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(label("БАЗОВАЯ СИЛА", 12, PRIMARY, true));
        page.addView(label("Ноги и корпус", 29, TEXT, true));
        page.addView(label(
                "Второй физический модуль. Две короткие тренировки развивают общую силовую основу, которой часто не хватает при большом объёме работы на ковре.",
                15, MUTED, false));

        LinearLayout next = heroCard();
        page.addView(next);
        int workoutIndex = prefs.getInt("next_workout", 0);
        next.addView(label("СЛЕДУЮЩЕЕ ЗАНЯТИЕ", 12, Color.rgb(217, 239, 233), true));
        next.addView(label(workoutIndex == 0 ? "Тренировка A" : "Тренировка B", 26, Color.WHITE, true));
        next.addView(label(
                workoutIndex == 0
                        ? "Приседание, тяга, выпады, контроль корпуса и одностороннее удержание."
                        : "Задняя цепь, подъёмы на ступень, жим, сопротивление вращению и удержание веса.",
                14, Color.rgb(224, 238, 235), false));
        Button start = lightButton("Проверить готовность и начать");
        start.setOnClickListener(v -> runReadinessCheck(workoutIndex));
        next.addView(space(10));
        next.addView(start);

        LinearLayout progress = card();
        page.addView(progress);
        progress.addView(sectionTitle("Прогресс модуля"));
        progress.addView(metric("Завершено тренировок", String.valueOf(prefs.getInt("sessions", 0))));
        String last = prefs.getString("last_date", "");
        progress.addView(metric("Последняя тренировка", last.isEmpty() ? "ещё не было" : formatDate(last)));
        progress.addView(metric("Следующая", workoutIndex == 0 ? "A" : "B"));

        LinearLayout rules = tintedCard();
        page.addView(rules);
        rules.addView(label("Правила нагрузки", 17, TEXT, true));
        rules.addView(label(
                "Не выполняй подходы до отказа. Оставляй 2–3 чистых повторения в запасе. Между дополнительными силовыми тренировками — не менее 48 часов.",
                14, MUTED, false));

        setContentView(scroll);
    }

    private void runReadinessCheck(int workoutIndex) {
        String[] items = {
                "Есть боль в колене, тазобедренном суставе, спине или шее",
                "Сегодня была тяжёлая тренировка или много жёстких раундов",
                "До соревнования или тяжёлых спаррингов меньше 24 часов",
                "Плохо спал или чувствую выраженную усталость"
        };
        boolean[] checked = new boolean[items.length];
        new AlertDialog.Builder(this)
                .setTitle("Готов ли организм?")
                .setMultiChoiceItems(items, checked, (dialog, which, value) -> checked[which] = value)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Продолжить", (dialog, which) -> {
                    if (checked[0]) {
                        showStopDialog("Сегодня без силовой тренировки", "Боль в суставе, спине или шее нельзя использовать как сигнал для тренировки. Отдохни и сообщи взрослому. Если боль повторяется или усиливается, нужна очная оценка специалиста.");
                        return;
                    }
                    if (checked[2]) {
                        showStopDialog("Лучше перенести", "Перед соревнованием или тяжёлыми спаррингами дополнительная усталость ног и корпуса может ухудшить движение на ковре.");
                        return;
                    }
                    lightMode = checked[1] || checked[3];
                    if (lightMode) {
                        new AlertDialog.Builder(this)
                                .setTitle("Облегчённая тренировка")
                                .setMessage("Сегодня останется по одному рабочему подходу. Нагрузка повышаться не будет.")
                                .setNegativeButton("Перенести", null)
                                .setPositiveButton("Начать легко", (d, w) -> showWorkout(workoutIndex))
                                .show();
                    } else {
                        showWorkout(workoutIndex);
                    }
                })
                .show();
    }

    private void showStopDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void showWorkout(int workoutIndex) {
        Exercise[] workout = workoutIndex == 0 ? workoutA : workoutB;
        ScrollView scroll = pageScroll();
        LinearLayout page = pageContent(scroll);
        page.addView(label(lightMode ? "ОБЛЕГЧЁННЫЙ РЕЖИМ" : "РАБОЧАЯ ТРЕНИРОВКА", 12, PRIMARY, true));
        page.addView(label(workoutIndex == 0 ? "Ноги и корпус · A" : "Ноги и корпус · B", 28, TEXT, true));
        page.addView(label("Сначала сделай 5 минут разминки: лёгкое движение, приседания без веса, наклоны таза и пробный подход первого упражнения.", 14, MUTED, false));

        Map<String, List<CheckBox>> checks = new LinkedHashMap<>();
        Map<String, TextView> valueViews = new LinkedHashMap<>();

        for (int i = 0; i < workout.length; i++) {
            Exercise ex = workout[i];
            LinearLayout box = card();
            page.addView(box);
            box.addView(label((i + 1) + ". " + ex.name, 18, TEXT, true));
            box.addView(label(ex.cue, 13, MUTED, false));

            int sets = lightMode ? 1 : prefs.getInt("sets_" + ex.id, ex.defaultSets);
            int reps = prefs.getInt("reps_" + ex.id, ex.minReps);
            String unit = ex.timed ? "секунд" : "повторений";
            box.addView(label(sets + " × " + reps + " " + unit, 16, PRIMARY, true));

            if (ex.defaultWeight > 0) {
                LinearLayout adjust = horizontal(8);
                Button minus = adjustButton("−");
                TextView value = label(weightText(getWeight(ex)), 17, TEXT, true);
                value.setGravity(Gravity.CENTER);
                Button plus = adjustButton("+");
                adjust.addView(minus, new LinearLayout.LayoutParams(dp(56), dp(46)));
                adjust.addView(value, new LinearLayout.LayoutParams(0, dp(46), 1));
                adjust.addView(plus, new LinearLayout.LayoutParams(dp(56), dp(46)));
                box.addView(adjust);
                valueViews.put(ex.id, value);
                minus.setOnClickListener(v -> {
                    double current = getWeight(ex);
                    double next = Math.max(ex.increment, current - ex.increment);
                    setWeight(ex, next);
                    value.setText(weightText(next));
                });
                plus.setOnClickListener(v -> {
                    double next = getWeight(ex) + ex.increment;
                    setWeight(ex, next);
                    value.setText(weightText(next));
                });
            } else {
                box.addView(label("Без дополнительного веса", 13, MUTED, false));
            }

            List<CheckBox> setChecks = new ArrayList<>();
            LinearLayout setRow = horizontal(6);
            for (int s = 1; s <= sets; s++) {
                CheckBox check = new CheckBox(this);
                check.setText("Подход " + s);
                check.setTextSize(13);
                check.setTextColor(TEXT);
                setRow.addView(check, new LinearLayout.LayoutParams(0, dp(48), 1));
                setChecks.add(check);
            }
            box.addView(setRow);
            checks.put(ex.id, setChecks);
        }

        LinearLayout finish = card();
        page.addView(finish);
        finish.addView(sectionTitle("После тренировки"));
        CheckBox technique = new CheckBox(this);
        technique.setText("Техника оставалась чистой");
        technique.setChecked(true);
        technique.setTextColor(TEXT);
        finish.addView(technique);
        CheckBox pain = new CheckBox(this);
        pain.setText("Появилась резкая или суставная боль");
        pain.setTextColor(DANGER);
        finish.addView(pain);

        RadioGroup difficulty = new RadioGroup(this);
        difficulty.setOrientation(RadioGroup.HORIZONTAL);
        String[] levels = {"Легко", "В самый раз", "Тяжело"};
        for (int i = 0; i < levels.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setText(levels[i]);
            radio.setId(2000 + i);
            radio.setTextSize(13);
            difficulty.addView(radio, new RadioGroup.LayoutParams(0, dp(50), 1));
        }
        difficulty.check(2001);
        finish.addView(difficulty);

        Button complete = primaryButton("Завершить тренировку");
        complete.setOnClickListener(v -> finishWorkout(workoutIndex, workout, checks, technique.isChecked(), pain.isChecked(), difficulty.getCheckedRadioButtonId() - 2000));
        finish.addView(complete);
        setContentView(scroll);
    }

    private void finishWorkout(int workoutIndex, Exercise[] workout, Map<String, List<CheckBox>> checks,
                               boolean goodTechnique, boolean pain, int difficulty) {
        boolean complete = true;
        for (Exercise ex : workout) {
            List<CheckBox> list = checks.get(ex.id);
            for (CheckBox cb : list) {
                if (!cb.isChecked()) complete = false;
            }
        }

        if (complete && !lightMode) adapt(workout, goodTechnique, pain, difficulty);

        SharedPreferences.Editor editor = prefs.edit()
                .putInt("sessions", prefs.getInt("sessions", 0) + 1)
                .putString("last_date", dateKey())
                .putInt("next_workout", complete ? 1 - workoutIndex : workoutIndex);
        editor.apply();

        String message;
        if (pain) message = "Тренировка сохранена. Нагрузка снижена; до исчезновения боли силовую не повторять.";
        else if (!complete) message = "Неполная тренировка сохранена без повышения нагрузки.";
        else if (lightMode) message = "Облегчённая тренировка сохранена без повышения нагрузки.";
        else if (!goodTechnique || difficulty == 2) message = "Тренировка сохранена. Следующее занятие не станет тяжелее.";
        else message = "Тренировка сохранена. Цели следующего занятия обновлены.";

        lightMode = false;
        new AlertDialog.Builder(this)
                .setTitle("Готово")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("К модулю", (d, w) -> showOverview())
                .show();
    }

    private void adapt(Exercise[] workout, boolean goodTechnique, boolean pain, int difficulty) {
        for (Exercise ex : workout) {
            int reps = prefs.getInt("reps_" + ex.id, ex.minReps);
            if (pain) {
                if (ex.defaultWeight > 0) setWeight(ex, Math.max(ex.increment, getWeight(ex) - ex.increment));
                continue;
            }
            if (!goodTechnique || difficulty == 2) continue;
            if (difficulty == 0 || difficulty == 1) {
                if (reps < ex.maxReps) {
                    prefs.edit().putInt("reps_" + ex.id, reps + 1).apply();
                } else if (ex.defaultWeight > 0) {
                    setWeight(ex, getWeight(ex) + ex.increment);
                    prefs.edit().putInt("reps_" + ex.id, ex.minReps).apply();
                } else if (ex.timed) {
                    prefs.edit().putInt("reps_" + ex.id, Math.min(ex.maxReps, reps + 5)).apply();
                }
            }
        }
    }

    private double getWeight(Exercise ex) {
        long raw = prefs.getLong("weight_" + ex.id, Double.doubleToRawLongBits(ex.defaultWeight));
        return Double.longBitsToDouble(raw);
    }

    private void setWeight(Exercise ex, double value) {
        prefs.edit().putLong("weight_" + ex.id, Double.doubleToRawLongBits(value)).apply();
    }

    private String weightText(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) return ((int) Math.rint(value)) + " кг";
        return String.format(Locale.getDefault(), "%.1f кг", value);
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

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView sectionTitle(String text) {
        return label(text, 18, TEXT, true);
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

    private Button primaryButton(String text) {
        Button button = button(text, Color.WHITE, PRIMARY);
        button.setMinHeight(dp(52));
        return button;
    }

    private Button lightButton(String text) {
        Button button = button(text, PRIMARY_DARK, Color.WHITE);
        button.setMinHeight(dp(50));
        return button;
    }

    private Button smallButton(String text) {
        Button button = button(text, PRIMARY, PRIMARY_SOFT);
        button.setTextSize(13);
        return button;
    }

    private Button adjustButton(String text) {
        Button button = button(text, PRIMARY, PRIMARY_SOFT);
        button.setTextSize(23);
        button.setIncludeFontPadding(false);
        button.setPadding(0, 0, 0, dp(2));
        button.setBackground(rounded(PRIMARY_SOFT, 12, 1, Color.rgb(161, 202, 190)));
        return button;
    }

    private Button button(String text, int textColor, int fill) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackground(rounded(fill, 13, 0, Color.TRANSPARENT));
        return button;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke > 0) drawable.setStroke(dp(stroke), strokeColor);
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

    private String dateKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String formatDate(String raw) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            if (date == null) return raw;
            return new SimpleDateFormat("d MMMM", new Locale("ru")).format(date);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private static class Exercise {
        final String id;
        final String name;
        final String cue;
        final int minReps;
        final int maxReps;
        final int defaultSets;
        final double defaultWeight;
        final double increment;
        final boolean timed;

        Exercise(String id, String name, String cue, int minReps, int maxReps, int defaultSets,
                 double defaultWeight, double increment, boolean timed) {
            this.id = id;
            this.name = name;
            this.cue = cue;
            this.minReps = minReps;
            this.maxReps = maxReps;
            this.defaultSets = defaultSets;
            this.defaultWeight = defaultWeight;
            this.increment = increment;
            this.timed = timed;
        }
    }

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
