package com.valera2872.bjjarm;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared scheduling logic for the 0.6 weekly-plan experience. */
final class WeekPlanEngine {
    static final String WEEK_PREFS = "combat_week_plan";
    static final String PROFILE_PREFS = "combat_performance_profile";
    static final String ARMS_PREFS = "bjj_arm_tracker";
    static final String BASE_PREFS = "combat_base_strength";

    static final String[] DAYS = {
            "Понедельник", "Вторник", "Среда", "Четверг",
            "Пятница", "Суббота", "Воскресенье"
    };

    static final String[] DAY_TYPES = {
            "Нет тренировки на ковре",
            "Лёгкая техника",
            "Обычная тренировка",
            "Тяжёлые раунды",
            "Соревнование"
    };

    private WeekPlanEngine() {}

    static boolean isConfigured(Context context) {
        return context.getSharedPreferences(WEEK_PREFS, Context.MODE_PRIVATE)
                .getBoolean("configured", false);
    }

    static int todayIndex() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SUNDAY) return 6;
        return day - Calendar.MONDAY;
    }

    static String typeForDay(Context context, int dayIndex) {
        SharedPreferences prefs = context.getSharedPreferences(WEEK_PREFS, Context.MODE_PRIVATE);
        return prefs.getString("day_type_" + dayIndex, DAY_TYPES[0]);
    }

    static int desiredStrengthSessions(Context context) {
        return context.getSharedPreferences(WEEK_PREFS, Context.MODE_PRIVATE)
                .getInt("strength_sessions", 2);
    }

    static int[] chooseStrengthDays(Context context) {
        int wanted = Math.max(1, Math.min(2, desiredStrengthSessions(context)));
        List<ScoredDay> candidates = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            String type = typeForDay(context, day);
            if (isHeavy(type) || isCompetition(type)) continue;

            int score;
            if (DAY_TYPES[0].equals(type)) score = 12;
            else if (DAY_TYPES[1].equals(type)) score = 8;
            else score = 3;

            String previous = typeForDay(context, (day + 6) % 7);
            String next = typeForDay(context, (day + 1) % 7);
            if (isHeavy(previous) || isCompetition(previous)) score -= 3;
            if (isHeavy(next) || isCompetition(next)) score -= 7;
            candidates.add(new ScoredDay(day, score));
        }

        Collections.sort(candidates, new Comparator<ScoredDay>() {
            @Override
            public int compare(ScoredDay a, ScoredDay b) {
                if (a.score != b.score) return Integer.compare(b.score, a.score);
                return Integer.compare(a.day, b.day);
            }
        });

        List<Integer> selected = new ArrayList<>();
        for (ScoredDay candidate : candidates) {
            if (selected.isEmpty()) {
                selected.add(candidate.day);
            } else if (selected.size() < wanted && circularDistance(selected.get(0), candidate.day) >= 2) {
                selected.add(candidate.day);
            }
            if (selected.size() == wanted) break;
        }
        if (selected.size() < wanted) {
            for (ScoredDay candidate : candidates) {
                if (!selected.contains(candidate.day)) selected.add(candidate.day);
                if (selected.size() == wanted) break;
            }
        }
        Collections.sort(selected);
        int[] result = new int[selected.size()];
        for (int i = 0; i < selected.size(); i++) result[i] = selected.get(i);
        return result;
    }

    static Task taskForDay(Context context, int dayIndex) {
        if (!isConfigured(context)) {
            return new Task("setup", "Настрой недельный план",
                    "Укажи дни ковра и тяжёлые занятия. После этого приложение само распределит дополнительную подготовку.",
                    "Настроить неделю");
        }

        String type = typeForDay(context, dayIndex);
        if (isCompetition(type)) {
            return new Task("competition", "Соревнование",
                    "Сегодня без дополнительной силовой. Оставь только привычную разминку, первый технический план и восстановление между схватками.",
                    "Открыть план соревнования");
        }
        if (isHeavy(type)) {
            return new Task("heavy", "Тяжёлые раунды на ковре",
                    "Основная нагрузка уже запланирована. Силовую не добавляем; техническую миссию пробуй только без разрушения основной задачи тренировки.",
                    "Открыть техническую миссию");
        }

        int[] strengthDays = chooseStrengthDays(context);
        for (int order = 0; order < strengthDays.length; order++) {
            if (strengthDays[order] == dayIndex) {
                String module = moduleForOrder(context, order);
                if ("arms".equals(module)) {
                    return new Task("arms", "Сильные руки и контроль",
                            "Сегодня дополнительная силовая для рук и предплечий. Перед началом обязательно пройди проверку восстановления.",
                            "Начать тренировку рук");
                }
                return new Task("base", "Базовая сила ног и корпуса",
                        "Сегодня общая силовая база: ноги, корпус и устойчивость. Нагрузка будет адаптирована после проверки готовности.",
                        "Начать тренировку ног");
            }
        }

        if (DAY_TYPES[1].equals(type) || DAY_TYPES[2].equals(type)) {
            return new Task("mat", "Тренировка на ковре + техническая миссия",
                    "На ковре ищи одну возможность применить выбранную технику. Не нужно менять всю тренировку ради отчёта.",
                    "Открыть техническую миссию");
        }

        return new Task("recovery", "Восстановление и короткий разбор",
                "Сегодня силовая не нужна. Проверь самочувствие, сделай 5–10 минут лёгкой подвижности и вспомни один момент последней тренировки.",
                "Открыть восстановление");
    }

    static String moduleForOrder(Context context, int order) {
        SharedPreferences profile = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        String first = profile.getString("priority_1", "");
        String second = profile.getString("priority_2", "");
        String priorities = first + " " + second;
        boolean arms = priorities.contains("Руки") || priorities.contains("предплеч");
        boolean base = priorities.contains("Ноги") || priorities.contains("Корпус")
                || priorities.contains("Взрыв") || priorities.contains("Подвиж");

        if (order == 0) {
            if (first.contains("Руки") || first.contains("предплеч")) return "arms";
            if (first.contains("Ноги") || first.contains("Корпус")
                    || first.contains("Взрыв") || first.contains("Подвиж")) return "base";
            if (arms && !base) return "arms";
            return "base";
        }
        return "arms".equals(moduleForOrder(context, 0)) ? "base" : "arms";
    }

    static boolean isTaskDone(Context context, int dayIndex, Task task) {
        String date = dateKeyForWeekDay(dayIndex);
        SharedPreferences week = context.getSharedPreferences(WEEK_PREFS, Context.MODE_PRIVATE);
        if (week.getBoolean("done_" + date, false)) return true;

        if ("arms".equals(task.kind)) {
            return date.equals(context.getSharedPreferences(ARMS_PREFS, Context.MODE_PRIVATE)
                    .getString("last_session_date", ""));
        }
        if ("base".equals(task.kind)) {
            return date.equals(context.getSharedPreferences(BASE_PREFS, Context.MODE_PRIVATE)
                    .getString("last_date", ""));
        }
        if ("mat".equals(task.kind) || "heavy".equals(task.kind)) {
            return date.equals(context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
                    .getString("mission_last_day", ""));
        }
        return false;
    }

    static void setManualDone(Context context, int dayIndex, boolean done) {
        context.getSharedPreferences(WEEK_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("done_" + dateKeyForWeekDay(dayIndex), done)
                .apply();
    }

    static String dateKeyForWeekDay(int dayIndex) {
        Calendar calendar = Calendar.getInstance();
        int current = todayIndex();
        calendar.add(Calendar.DAY_OF_YEAR, dayIndex - current);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    static String displayDateForWeekDay(int dayIndex) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, dayIndex - todayIndex());
        return new SimpleDateFormat("d MMM", new Locale("ru")).format(calendar.getTime());
    }

    static boolean isPastDay(int dayIndex) {
        return dayIndex < todayIndex();
    }

    static boolean isHeavy(String type) {
        return DAY_TYPES[3].equals(type);
    }

    static boolean isCompetition(String type) {
        return DAY_TYPES[4].equals(type);
    }

    private static int circularDistance(int a, int b) {
        int raw = Math.abs(a - b);
        return Math.min(raw, 7 - raw);
    }

    static final class Task {
        final String kind;
        final String title;
        final String details;
        final String actionLabel;

        Task(String kind, String title, String details, String actionLabel) {
            this.kind = kind;
            this.title = title;
            this.details = details;
            this.actionLabel = actionLabel;
        }
    }

    private static final class ScoredDay {
        final int day;
        final int score;

        ScoredDay(int day, int score) {
            this.day = day;
            this.score = score;
        }
    }
}