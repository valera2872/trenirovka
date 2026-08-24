package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** 0.10.1 entry point: clean install never falls back to the old light 0.9.x UI. */
public class CombatPerformanceV101Activity extends Activity {
    private SharedPreferences profile;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences("combat_performance_profile", MODE_PRIVATE);
        DarkUi.apply(this);
        route();
    }

    @Override protected void onResume() {
        super.onResume();
        if (profile != null && profile.getBoolean("profile_complete", false)) openDashboard();
    }

    private void route() {
        if (profile.getBoolean("profile_complete", false)) openDashboard();
        else showWelcome();
    }

    private void showWelcome() {
        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this,18), DarkUi.dp(this,24), DarkUi.dp(this,18), DarkUi.dp(this,34));
        scroll.addView(page);

        LinearLayout hero = DarkUi.hero(this);
        hero.addView(DarkUi.gold(this, "COMBAT PERFORMANCE"));
        hero.addView(DarkUi.title(this, "Тренируйся с понятной системой"));
        hero.addView(DarkUi.bodyWhite(this,
                "Личный план подготовки, персональные силовые тренировки и дневник помогают понимать, что делать сегодня и над чем работать дальше."));
        page.addView(hero);

        LinearLayout cycle = DarkUi.card(this);
        cycle.addView(DarkUi.gold(this, "ЕДИНЫЙ ЦИКЛ"));
        cycle.addView(DarkUi.h1(this, "Одна тренировка продолжает предыдущую"));
        cycle.addView(step("01", "Перед тренировкой", "Получить одну конкретную техническую или силовую задачу."));
        cycle.addView(step("02", "На тренировке", "Осознанно проверить выбранное действие."));
        cycle.addView(step("03", "После тренировки", "Сохранить главное и определить следующий фокус."));
        page.addView(cycle);

        LinearLayout strength = DarkUi.goldCard(this);
        strength.addView(DarkUi.gold(this, "ПЕРСОНАЛЬНАЯ СИЛОВАЯ"));
        strength.addView(DarkUi.h1(this, "Сила под твою борьбу"));
        strength.addView(DarkUi.bodyWhite(this,
                "Руки, хват, ноги и корпус распределяются с учётом ковра, тяжёлых раундов и соревнований."));
        page.addView(strength);

        LinearLayout privacy = DarkUi.card(this);
        privacy.addView(DarkUi.h2(this, "Без регистрации"));
        privacy.addView(DarkUi.body(this,
                "Профиль, план и дневник хранятся на телефоне. Приложение не заменяет тренера и не выдумывает техники."));
        page.addView(privacy);

        Button start = DarkUi.primary(this, "Собрать мой план подготовки");
        start.setOnClickListener(v -> startActivity(new Intent(this, PremiumDarkProfileActivity.class)));
        page.addView(start);
        setContentView(scroll);
    }

    private LinearLayout step(String number, String title, String body) {
        LinearLayout row = DarkUi.h(this, 12);
        LinearLayout badge = DarkUi.v(this,0);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackground(DarkUi.round(DarkUi.GOLD,999,0,0));
        badge.addView(DarkUi.text(this, number, 12, DarkUi.BG, android.graphics.Typeface.DEFAULT_BOLD));
        row.addView(badge, new LinearLayout.LayoutParams(DarkUi.dp(this,38), DarkUi.dp(this,38)));
        LinearLayout copy = DarkUi.v(this,3);
        copy.addView(DarkUi.h2(this,title));
        copy.addView(DarkUi.small(this,body));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1));
        return row;
    }

    private void openDashboard() {
        Intent i = new Intent(this, CombatPerformanceV100Activity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
