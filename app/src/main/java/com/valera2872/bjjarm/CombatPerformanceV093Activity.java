package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.9.3 stability build.
 * Welcome, profile and dashboard live in one Activity. Voice input uses a fresh
 * system recognition Activity for every RU/EN press, so no recognizer instance
 * survives between attempts or profile saving.
 */
public class CombatPerformanceV093Activity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String ROUTINE_PREFS = "combat_personal_routine";

    private static final int SCREEN_WELCOME = 0;
    private static final int SCREEN_DASHBOARD = 1;
    private static final int SCREEN_PROFILE = 2;
    private static final int REQUEST_VOICE = 9531;

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
    private SharedPreferences system;
    private SharedPreferences diary;
    private SharedPreferences routine;

    private int currentScreen = SCREEN_WELCOME;
    private int profileReturnScreen = SCREEN_WELCOME;
    private boolean savingProfile;
    private EditText pendingVoiceTarget;
    private String pendingVoiceLanguage = "ru-RU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        routine = getSharedPreferences(ROUTINE_PREFS, MODE_PRIVATE);
        PremiumUi.applyWindow(this);
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
            return;
        }
        if (currentScreen == SCREEN_WELCOME && profile.getBoolean("profile_complete", false)) {
            showDashboard();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VOICE) return;
        EditText target = pendingVoiceTarget;
        String language = pendingVoiceLanguage;
        pendingVoiceTarget = null;
        pendingVoiceLanguage = "ru-RU";
        if (target == null) return;

        if (resultCode != RESULT_OK || data == null) {
            toast("Речь не распознана. Попробуй ещё раз или введи текст вручную.");
            return;
        }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty() || results.get(0).trim().isEmpty()) {
            toast("Речь не распознана. Для EN может потребоваться английский языковой пакет в системном сервисе.");
            return;
        }
        String spoken = results.get(0).trim();
        String existing = target.getText().toString().trim();
        String combined = existing.isEmpty() ? spoken : existing + " " + spoken;
        if ("ru-RU".equals(language)) combined = normalizeTechnique(combined);
        target.setText(combined);
        target.setSelection(combined.length());
    }

    private void showWelcome() {
        currentScreen = SCREEN_WELCOME;
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        LinearLayout brand = brandHero();
        page.addView(brand);
        brand.addView(PremiumUi.heroEyebrow(this, "Combat performance"));
        brand.addView(PremiumUi.heroTitle(this, "Тренируйся с понятной системой"));
        brand.addView(PremiumUi.heroBody(this,
                "Личный план подготовки, персональные силовые тренировки и дневник помогают понимать, что делать сегодня и над чем работать дальше."));
        TextView mark = PremiumUi.text(this, "CP", 42, Color.rgb(215, 176, 104),
                Typeface.create("sans-serif-black", Typeface.NORMAL));
        mark.setGravity(Gravity.END);
        brand.addView(mark);

        LinearLayout flow = PremiumUi.card(this);
        page.addView(flow);
        flow.addView(PremiumUi.eyebrow(this, "Единый цикл подготовки"));
        flow.addView(processRow("01", "Перед тренировкой",
                "Получить одну конкретную техническую или силовую задачу."));
        flow.addView(processRow("02", "На тренировке",
                "Осознанно выполнить выбранное действие и заметить, где оно остановилось."));
        flow.addView(processRow("03", "После тренировки",
                "Сохранить главное и определить следующий фокус."));

        LinearLayout strength = PremiumUi.softCard(this);
        strength.addView(PremiumUi.sectionTitle(this, "Персональная силовая подготовка"));
        strength.addView(PremiumUi.bodyDark(this,
                "Дополнительная работа для рук, хвата, ног и корпуса распределяется с учётом обычных тренировок, тяжёлых раундов и соревнований."));
        page.addView(strength);

        LinearLayout privacy = PremiumUi.card(this);
        privacy.addView(PremiumUi.sectionTitle(this, "Без регистрации и псевдо-AI"));
        privacy.addView(PremiumUi.body(this,
                "Профиль, план и дневник сохраняются на телефоне. Приложение не заменяет тренера и не выдумывает техники."));
        page.addView(privacy);

        Button setup = PremiumUi.primaryButton(this,
                profile.getBoolean("profile_complete", false) ? "Изменить профиль" : "Настроить профиль");
        setup.setOnClickListener(v -> showProfile(SCREEN_WELCOME));
        page.addView(setup);
        if (profile.getBoolean("profile_complete", false)) {
            Button dashboard = PremiumUi.outlineButton(this, "Перейти к сегодняшней задаче");
            dashboard.setOnClickListener(v -> showDashboard());
            page.addView(dashboard);
        }
        setContentView(scroll);
    }

    private void showProfile(int returnScreen) {
        currentScreen = SCREEN_PROFILE;
        profileReturnScreen = returnScreen;
        savingProfile = false;
        pendingVoiceTarget = null;

        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        Button back = PremiumUi.outlineButton(this, "← Назад");
        back.setOnClickListener(v -> onBackPressed());
        page.addView(back);
        page.addView(PremiumUi.eyebrow(this, "Профиль спортсмена"));
        page.addView(PremiumUi.title(this, "Настрой подготовку под себя"));
        page.addView(PremiumUi.body(this,
                "Эти данные нужны только для личного плана и сохраняются на телефоне."));

        LinearLayout basics = PremiumUi.card(this);
        page.addView(basics);
        basics.addView(PremiumUi.cardTitle(this, "Основные данные"));
        EditText name = PremiumUi.input(this, "Имя спортсмена",
                valueOrDraft("name", "draft_name"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText age = PremiumUi.input(this, "Возраст",
                String.valueOf(profile.getInt("age", 14)), InputType.TYPE_CLASS_NUMBER);
        EditText sessions = PremiumUi.input(this, "Тренировок в неделю",
                String.valueOf(profile.getInt("mat_sessions", 5)), InputType.TYPE_CLASS_NUMBER);
        bindDraft(name, "draft_name");
        basics.addView(fieldLabel("Имя"));
        basics.addView(name);
        basics.addView(fieldLabel("Возраст"));
        basics.addView(age);
        basics.addView(fieldLabel("Обычная нагрузка"));
        basics.addView(sessions);

        LinearLayout sportsCard = PremiumUi.card(this);
        page.addView(sportsCard);
        sportsCard.addView(PremiumUi.cardTitle(this, "Виды борьбы"));
        sportsCard.addView(PremiumUi.small(this, "Можно отметить несколько дисциплин."));
        List<String> selectedSports = savedSports();
        CheckBox[] sportChecks = new CheckBox[SPORTS.length];
        for (int i = 0; i < SPORTS.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(SPORTS[i]);
            box.setTextSize(15);
            box.setTextColor(PremiumUi.INK);
            box.setChecked(selectedSports.contains(SPORTS[i]));
            box.setPadding(0, PremiumUi.dp(this, 7), 0, PremiumUi.dp(this, 7));
            sportChecks[i] = box;
            sportsCard.addView(box);
        }

        LinearLayout physical = PremiumUi.card(this);
        page.addView(physical);
        physical.addView(PremiumUi.cardTitle(this, "Стиль и силовая подготовка"));
        physical.addView(fieldLabel("Как ты чаще борешься?"));
        Spinner style = spinner(STYLES, profile.getString("style", STYLES[0]));
        physical.addView(style);
        physical.addView(fieldLabel("Что уже развито лучше всего?"));
        Spinner strong = spinner(AREAS, profile.getString("strong_area", AREAS[0]));
        physical.addView(strong);
        physical.addView(fieldLabel("Главный физический приоритет"));
        Spinner priority1 = spinner(AREAS, profile.getString("priority_1", AREAS[1]));
        physical.addView(priority1);
        physical.addView(fieldLabel("Второй физический приоритет"));
        Spinner priority2 = spinner(AREAS, profile.getString("priority_2", AREAS[2]));
        physical.addView(priority2);

        LinearLayout technique = PremiumUi.card(this);
        page.addView(technique);
        technique.addView(PremiumUi.cardTitle(this, "Технический фокус"));
        technique.addView(PremiumUi.small(this,
                "Каждое нажатие запускает отдельное системное распознавание. Для EN на телефоне может потребоваться установленный английский языковой пакет."));

        EditText favorite = PremiumUi.multiline(this,
                "Например: закрытый гард, arm drag, учи-мата",
                valueOrDraft("favorite_techniques", "draft_favorite"));
        bindDraft(favorite, "draft_favorite");
        technique.addView(fieldLabel("Любимые позиции и техники"));
        technique.addView(voiceRow(favorite));

        EditText mission = PremiumUi.multiline(this,
                "Одна техника или действие на ближайшие 30 дней",
                valueOrDraft("mission", "draft_mission"));
        bindDraft(mission, "draft_mission");
        technique.addView(fieldLabel("Фокус на 30 дней"));
        technique.addView(voiceRow(mission));
        technique.addView(PremiumUi.small(this,
                "Например: на каждой тренировке хотя бы один раз выйти в Williams Guard и сохранить контроль руки."));

        Button save = PremiumUi.primaryButton(this, "Сохранить профиль");
        save.setOnClickListener(v -> saveProfile(page, save, name, age, sessions, sportChecks,
                style, strong, priority1, priority2, favorite, mission));
        page.addView(save);
        setContentView(scroll);
    }

    private void saveProfile(View page,
                             Button saveButton,
                             EditText name,
                             EditText age,
                             EditText sessions,
                             CheckBox[] sportChecks,
                             Spinner style,
                             Spinner strong,
                             Spinner priority1,
                             Spinner priority2,
                             EditText favorite,
                             EditText mission) {
        if (savingProfile) return;
        String cleanName = text(name);
        String cleanMission = text(mission);
        ArrayList<String> sports = new ArrayList<>();
        for (int i = 0; i < sportChecks.length; i++) {
            if (sportChecks[i].isChecked()) sports.add(SPORTS[i]);
        }
        if (cleanName.isEmpty()) {
            toast("Укажи имя спортсмена.");
            name.requestFocus();
            return;
        }
        if (sports.isEmpty()) {
            toast("Выбери хотя бы один вид борьбы.");
            return;
        }
        if (cleanMission.isEmpty()) {
            toast("Укажи технику или действие на 30 дней.");
            mission.requestFocus();
            return;
        }

        savingProfile = true;
        saveButton.setEnabled(false);
        saveButton.setText("Сохраняю…");
        pendingVoiceTarget = null;

        boolean missionChanged = !cleanMission.equals(profile.getString("mission", "").trim());
        String oldActive = profile.getString("active_sport", "").trim();
        String active = sports.contains(oldActive) ? oldActive : sports.get(0);

        SharedPreferences.Editor editor = profile.edit()
                .putBoolean("profile_complete", true)
                .putString("name", cleanName)
                .putInt("age", clamp(safeInt(text(age), 14), 8, 80))
                .putInt("mat_sessions", clamp(safeInt(text(sessions), 5), 1, 14))
                .putString("sports", TextUtils.join("|", sports))
                .putString("sport", sports.get(0))
                .putString("active_sport", active)
                .putString("style", String.valueOf(style.getSelectedItem()))
                .putString("strong_area", String.valueOf(strong.getSelectedItem()))
                .putString("priority_1", String.valueOf(priority1.getSelectedItem()))
                .putString("priority_2", String.valueOf(priority2.getSelectedItem()))
                .putString("favorite_techniques", text(favorite))
                .putString("mission", cleanMission)
                .remove("draft_name")
                .remove("draft_favorite")
                .remove("draft_mission");

        if (missionChanged || !profile.contains("mission_started_at")) {
            editor.putLong("mission_started_at", System.currentTimeMillis())
                    .putInt("mission_active_days", 0)
                    .putInt("mission_attempts", 0)
                    .putInt("mission_successes", 0)
                    .putInt("mission_finishes", 0)
                    .remove("mission_last_day");
        }

        boolean committed;
        try {
            committed = editor.commit();
        } catch (RuntimeException error) {
            committed = false;
        }

        boolean verified = committed
                && cleanName.equals(profile.getString("name", "").trim())
                && cleanMission.equals(profile.getString("mission", "").trim())
                && profile.getBoolean("profile_complete", false);

        if (!verified) {
            savingProfile = false;
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить профиль");
            toast("Не удалось сохранить профиль. Данные остались на экране.");
            return;
        }

        saveButton.setText("Профиль сохранён");
        toast("Профиль сохранён.");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            savingProfile = false;
            try {
                showDashboard();
            } catch (RuntimeException error) {
                showRecoveryScreen(error);
            }
        }, 180);
    }

    private void showRecoveryScreen(RuntimeException error) {
        currentScreen = SCREEN_PROFILE;
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);
        page.addView(PremiumUi.title(this, "Профиль сохранён"));
        page.addView(PremiumUi.body(this,
                "Данные записаны, но главный экран не удалось открыть. Перезапусти приложение — профиль не потерян."));
        page.addView(PremiumUi.small(this, "Код ошибки: " + error.getClass().getSimpleName()));
        Button restart = PremiumUi.primaryButton(this, "Открыть главный экран");
        restart.setOnClickListener(v -> showDashboard());
        page.addView(restart);
        setContentView(scroll);
    }

    private void showDashboard() {
        currentScreen = SCREEN_DASHBOARD;
        pendingVoiceTarget = null;
        ScrollView scroll = PremiumUi.scroll(this);
        LinearLayout page = PremiumUi.page(this);
        scroll.addView(page);

        LinearLayout identity = brandHero();
        page.addView(identity);
        LinearLayout head = PremiumUi.horizontal(this, 12);
        TextView avatar = PremiumUi.text(this, firstLetter(profile.getString("name", "С")),
                22, PremiumUi.ACCENT_DARK,
                Typeface.create("sans-serif-black", Typeface.NORMAL));
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(PremiumUi.rounded(Color.rgb(239, 219, 179), 999, 0, Color.TRANSPARENT));
        head.addView(avatar, new LinearLayout.LayoutParams(PremiumUi.dp(this, 52), PremiumUi.dp(this, 52)));
        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.heroEyebrow(this, "Личный план подготовки"));
        copy.addView(PremiumUi.heroTitle(this, profile.getString("name", "Спортсмен")));
        copy.addView(PremiumUi.heroBody(this, activeSport()));
        head.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        identity.addView(head);
        Button overview = PremiumUi.lightButton(this, "Как работает приложение");
        overview.setOnClickListener(v -> showWelcome());
        identity.addView(overview);

        page.addView(navigation());

        TodayTask today = todayTask();
        LinearLayout todayCard = accentCard();
        page.addView(todayCard);
        todayCard.addView(PremiumUi.eyebrow(this, "Сегодня"));
        todayCard.addView(PremiumUi.title(this, today.title));
        todayCard.addView(PremiumUi.bodyDark(this, today.details));
        Button todayAction = PremiumUi.primaryButton(this, today.action);
        todayAction.setOnClickListener(v -> runTodayAction(today.kind));
        todayCard.addView(todayAction);

        page.addView(fightingPlanCard());
        page.addView(diaryCard());
        page.addView(focusCard());
        page.addView(physicalCard());
        page.addView(routineCard());
        setContentView(scroll);
    }

    private LinearLayout navigation() {
        LinearLayout nav = PremiumUi.horizontal(this, 6);
        Button today = PremiumUi.navButton(this, "Сегодня", true);
        Button week = PremiumUi.navButton(this, "Неделя", false);
        Button diaryButton = PremiumUi.navButton(this, "Дневник", false);
        Button profileButton = PremiumUi.navButton(this, "Профиль", false);
        today.setOnClickListener(v -> showDashboard());
        week.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanV3Activity.class)));
        diaryButton.setOnClickListener(v -> startActivity(new Intent(this, PremiumTrainingDiaryActivity.class)));
        profileButton.setOnClickListener(v -> showProfile(SCREEN_DASHBOARD));
        nav.addView(today, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 52), 1));
        nav.addView(week, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 52), 1));
        nav.addView(diaryButton, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 52), 1));
        nav.addView(profileButton, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 52), 1));
        return nav;
    }

    private LinearLayout fightingPlanCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Техника и решения"));
        card.addView(PremiumUi.cardTitle(this, "Мой план борьбы"));
        String prefix = SportGuidance.slug(activeSport()) + "_";
        boolean configured = system.getBoolean(prefix + "configured", false);
        if (configured) {
            ArrayList<String> parts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String value = system.getString(prefix + "step_" + i, "").trim();
                if (!value.isEmpty()) parts.add(value);
            }
            if (!parts.isEmpty()) card.addView(PremiumUi.bodyDark(this, TextUtils.join("  →  ", parts)));
            card.addView(PremiumUi.small(this,
                    "Твой рабочий маршрут: от первого контакта к главному действию и запасному решению."));
        } else {
            card.addView(PremiumUi.body(this,
                    "Опиши, откуда начинаешь, как входишь в нужную позицию, чем продолжаешь и что делаешь при защите соперника."));
        }
        Button open = PremiumUi.primaryButton(this, configured ? "Открыть план" : "Настроить план");
        open.setOnClickListener(v -> startActivity(new Intent(this, PremiumFightingPlanActivity.class)));
        card.addView(open);
        return card;
    }

    private LinearLayout diaryCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "После тренировки"));
        card.addView(PremiumUi.cardTitle(this, "Дневник тренировок"));
        String next = diary.getString("next_task_" + SportGuidance.slug(activeSport()), "").trim();
        if (next.isEmpty()) {
            next = "Сохрани один успешный момент и одно место, где действие остановилось.";
        }
        LinearLayout task = PremiumUi.softCard(this);
        task.addView(PremiumUi.accentText(this, "Следующая задача"));
        task.addView(PremiumUi.bodyDark(this, next));
        card.addView(task);
        int count = diaryEntryCount();
        card.addView(PremiumUi.small(this,
                count == 0 ? "Первая запись обычно занимает около минуты."
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
                "Статистика обновляется из дневника, поэтому тренировку не нужно отмечать дважды."));
        return card;
    }

    private LinearLayout physicalCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Персональная силовая подготовка"));
        card.addView(PremiumUi.cardTitle(this, "Сила для твоей борьбы"));
        card.addView(PremiumUi.body(this,
                "Дополнительные тренировки учитывают выбранные физические приоритеты и недельную нагрузку."));
        LinearLayout actions = PremiumUi.horizontal(this, 8);
        Button arms = PremiumUi.secondaryButton(this, "Руки и хват");
        Button base = PremiumUi.secondaryButton(this, "Ноги и корпус");
        arms.setOnClickListener(v -> startActivity(new Intent(this, GrapplingV4Activity.class)));
        base.setOnClickListener(v -> startActivity(new Intent(this, BaseStrengthV2Activity.class)));
        actions.addView(arms, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 60), 1));
        actions.addView(base, new LinearLayout.LayoutParams(0, PremiumUi.dp(this, 60), 1));
        card.addView(actions);
        return card;
    }

    private LinearLayout routineCard() {
        LinearLayout card = PremiumUi.card(this);
        card.addView(PremiumUi.eyebrow(this, "Перед соревнованием"));
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

    private LinearLayout voiceRow(EditText field) {
        LinearLayout row = PremiumUi.horizontal(this, 7);
        row.setGravity(Gravity.TOP);
        row.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout controls = PremiumUi.vertical(this, 7);
        Button ru = voiceButton("RU");
        Button en = voiceButton("EN");
        ru.setOnClickListener(v -> launchVoice(field, "ru-RU"));
        en.setOnClickListener(v -> launchVoice(field, "en-US"));
        controls.addView(ru, new LinearLayout.LayoutParams(PremiumUi.dp(this, 68), PremiumUi.dp(this, 54)));
        controls.addView(en, new LinearLayout.LayoutParams(PremiumUi.dp(this, 68), PremiumUi.dp(this, 54)));
        row.addView(controls);
        return row;
    }

    private Button voiceButton(String languageLabel) {
        Button button = "RU".equals(languageLabel)
                ? PremiumUi.secondaryButton(this, languageLabel)
                : PremiumUi.outlineButton(this, languageLabel);
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_premium, 0, 0, 0);
        button.setCompoundDrawablePadding(PremiumUi.dp(this, 4));
        button.setContentDescription("Голосовой ввод " + languageLabel);
        return button;
    }

    private void launchVoice(EditText target, String language) {
        pendingVoiceTarget = target;
        pendingVoiceLanguage = language;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "en-US".equals(language) ? "Say the technique name in English" : "Назовите технику или позицию");
        try {
            startActivityForResult(intent, REQUEST_VOICE);
        } catch (ActivityNotFoundException error) {
            pendingVoiceTarget = null;
            toast("На телефоне не найден системный сервис распознавания речи.");
        } catch (RuntimeException error) {
            pendingVoiceTarget = null;
            toast("Не удалось открыть голосовой ввод. Текст можно ввести вручную.");
        }
    }

    private LinearLayout brandHero() {
        LinearLayout hero = PremiumUi.vertical(this, 12);
        hero.setPadding(PremiumUi.dp(this, 22), PremiumUi.dp(this, 24),
                PremiumUi.dp(this, 22), PremiumUi.dp(this, 22));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(12, 34, 32), Color.rgb(20, 82, 72)});
        background.setCornerRadius(PremiumUi.dp(this, 28));
        background.setStroke(PremiumUi.dp(this, 1), Color.rgb(73, 115, 104));
        hero.setBackground(background);
        hero.setElevation(PremiumUi.dp(this, 6));
        return hero;
    }

    private LinearLayout accentCard() {
        LinearLayout card = PremiumUi.vertical(this, 10);
        card.setPadding(PremiumUi.dp(this, 20), PremiumUi.dp(this, 20),
                PremiumUi.dp(this, 20), PremiumUi.dp(this, 20));
        card.setBackground(PremiumUi.rounded(Color.rgb(255, 251, 242), 24, 2,
                Color.rgb(193, 153, 86)));
        card.setElevation(PremiumUi.dp(this, 3));
        return card;
    }

    private LinearLayout processRow(String number, String title, String details) {
        LinearLayout row = PremiumUi.horizontal(this, 12);
        row.setGravity(Gravity.TOP);
        row.addView(PremiumUi.numberBadge(this, number),
                new LinearLayout.LayoutParams(PremiumUi.dp(this, 40), PremiumUi.dp(this, 40)));
        LinearLayout copy = PremiumUi.vertical(this, 4);
        copy.addView(PremiumUi.sectionTitle(this, title));
        copy.addView(PremiumUi.small(this, details));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private LinearLayout statBox(String title, int value) {
        LinearLayout box = PremiumUi.vertical(this, 4);
        box.setGravity(Gravity.CENTER);
        box.setPadding(PremiumUi.dp(this, 8), PremiumUi.dp(this, 12),
                PremiumUi.dp(this, 8), PremiumUi.dp(this, 12));
        box.setBackground(PremiumUi.rounded(PremiumUi.PAPER, 15, 0, Color.TRANSPARENT));
        TextView number = PremiumUi.cardTitle(this, String.valueOf(value));
        number.setGravity(Gravity.CENTER);
        TextView label = PremiumUi.small(this, title);
        label.setGravity(Gravity.CENTER);
        box.addView(number);
        box.addView(label);
        return box;
    }

    private TodayTask todayTask() {
        if (!WeekPlanEngine.isConfigured(this)) {
            return new TodayTask("week", "Настрой неделю",
                    "Отметь обычные, тяжёлые и соревновательные дни. После этого приложение распределит персональную силовую нагрузку.",
                    "Настроить неделю");
        }
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        if ("arms".equals(task.kind)) {
            return new TodayTask("arms", "Руки и хват",
                    "Короткая персональная силовая тренировка для рук и предплечий. Сначала проверь восстановление.",
                    "Открыть тренировку");
        }
        if ("base".equals(task.kind)) {
            return new TodayTask("base", "Ноги и корпус",
                    "Персональная работа над силовой базой, устойчивостью и контролем корпуса.",
                    "Открыть тренировку");
        }
        if ("mat".equals(task.kind)) {
            return new TodayTask("diary", "Тренировка по борьбе",
                    "Перед началом посмотри текущий технический фокус. После тренировки сохрани короткий разбор.",
                    "Открыть задачу");
        }
        if ("heavy".equals(task.kind)) {
            return new TodayTask("week", "Тяжёлые раунды",
                    "Дополнительную силовую сегодня не добавляем. Главная нагрузка — основная тренировка.",
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

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) index = i;
        spinner.setSelection(index);
        spinner.setPadding(PremiumUi.dp(this, 12), PremiumUi.dp(this, 5),
                PremiumUi.dp(this, 12), PremiumUi.dp(this, 5));
        spinner.setBackground(PremiumUi.rounded(Color.WHITE, 14, 1, PremiumUi.BORDER));
        spinner.setMinimumHeight(PremiumUi.dp(this, 52));
        return spinner;
    }

    private TextView fieldLabel(String value) {
        TextView label = PremiumUi.accentText(this, value);
        label.setPadding(0, PremiumUi.dp(this, 4), 0, 0);
        return label;
    }

    private void bindDraft(EditText field, String key) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                profile.edit().putString(key, s == null ? "" : s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private List<String> savedSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String value : saved.split("\\|")) if (!value.trim().isEmpty()) result.add(value.trim());
        }
        if (result.isEmpty()) result.add(profile.getString("sport", SPORTS[0]));
        return result;
    }

    private String valueOrDraft(String valueKey, String draftKey) {
        String value = profile.getString(valueKey, "").trim();
        return value.isEmpty() ? profile.getString(draftKey, "") : value;
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

    private String normalizeTechnique(String value) {
        String result = value == null ? "" : value.trim();
        String lower = result.toLowerCase(Locale.ROOT);
        if (lower.contains("вильямс гард") || lower.contains("уильямс гард")) {
            result = replaceIgnoreCase(result, "вильямс гард", "Williams Guard");
            return replaceIgnoreCase(result, "уильямс гард", "Williams Guard");
        }
        if (lower.contains("багги чок") || lower.contains("баггичок")) {
            result = replaceIgnoreCase(result, "багги чок", "Buggy Choke");
            return replaceIgnoreCase(result, "баггичок", "Buggy Choke");
        }
        if (lower.contains("де ла рива")) return replaceIgnoreCase(result, "де ла рива", "De La Riva");
        if (lower.contains("икс гард")) return replaceIgnoreCase(result, "икс гард", "X-Guard");
        if (lower.contains("аши гарами")) return replaceIgnoreCase(result, "аши гарами", "Ashi Garami");
        if (lower.contains("беримболо")) return replaceIgnoreCase(result, "беримболо", "Berimbolo");
        if (lower.contains("дарс")) return replaceIgnoreCase(result, "дарс", "D'Arce");
        return result;
    }

    private String replaceIgnoreCase(String source, String target, String replacement) {
        return source.replaceAll("(?iu)" + java.util.regex.Pattern.quote(target),
                java.util.regex.Matcher.quoteReplacement(replacement));
    }

    private String firstLetter(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? "С" : clean.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private String text(EditText field) {
        return field == null ? "" : field.getText().toString().trim();
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
