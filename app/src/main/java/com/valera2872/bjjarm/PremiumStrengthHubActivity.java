package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Premium strength entry screen. Existing workout engines remain unchanged. */
public class PremiumStrengthHubActivity extends Activity {
    private SharedPreferences profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences("combat_performance_profile", MODE_PRIVATE);
        DarkUi.apply(this);
        render();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DarkUi.BG);
        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this,16),DarkUi.dp(this,18),DarkUi.dp(this,16),DarkUi.dp(this,28));
        scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        LinearLayout header=DarkUi.h(this,10);
        TextView back=DarkUi.text(this,"‹",32,DarkUi.TEXT,android.graphics.Typeface.DEFAULT); back.setGravity(Gravity.CENTER); back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(DarkUi.dp(this,34),DarkUi.dp(this,40)));
        header.addView(DarkUi.h2(this,"Силовая подготовка"),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        page.addView(header);
        page.addView(DarkUi.title(this,"Сила для твоей борьбы"));
        page.addView(DarkUi.body(this,"Дополнительная работа распределяется вокруг ковра, а не конкурирует с основной тренировкой."));

        WeekPlanEngine.Task task=WeekPlanEngine.taskForDay(this,WeekPlanEngine.todayIndex());
        LinearLayout today=DarkUi.goldCard(this);
        today.addView(DarkUi.gold(this,"СЕГОДНЯ ПО ПЛАНУ"));
        today.addView(DarkUi.h1(this,todayTitle(task.kind)));
        today.addView(DarkUi.bodyWhite(this,todayDescription(task)));
        page.addView(today);

        page.addView(strengthCard("Руки и хват","Хват, предплечья и тяговые движения","Персональный приоритет: "+profile.getString("priority_1","не выбран"),DarkUi.GOLD,
                v->startActivity(new Intent(this,GrapplingV5Activity.class))));
        page.addView(strengthCard("Ноги и корпус","Сила ног, устойчивость и контроль корпуса","Персональный приоритет: "+profile.getString("priority_2","не выбран"),DarkUi.GREEN,
                v->startActivity(new Intent(this,BaseStrengthV3Activity.class))));

        LinearLayout rule=DarkUi.card(this);
        rule.addView(DarkUi.gold(this,"ПРИНЦИП НАГРУЗКИ"));
        rule.addView(DarkUi.h2(this,"Ковёр имеет приоритет"));
        rule.addView(DarkUi.body(this,"В тяжёлый день и в день соревнования дополнительная силовая не назначается. Перед тяжёлым днём нагрузка сокращается."));
        page.addView(rule);

        android.widget.Button week=DarkUi.secondary(this,"Открыть недельный план");
        week.setOnClickListener(v->startActivity(new Intent(this,WeeklyPlanV4Activity.class)));
        page.addView(week);

        LinearLayout nav=DarkUi.h(this,0); nav.setBackgroundColor(DarkUi.BG_2); nav.setPadding(DarkUi.dp(this,8),0,DarkUi.dp(this,8),0);
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_today,"Сегодня",false,v->{startActivity(new Intent(this,CombatPerformanceV100Activity.class));finish();}),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_week,"Неделя",false,v->startActivity(new Intent(this,WeeklyPlanV4Activity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_diary,"Дневник",false,v->startActivity(new Intent(this,PremiumDarkDiaryActivity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_profile,"Профиль",false,v->startActivity(new Intent(this,PremiumProfileActivity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        root.addView(nav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DarkUi.dp(this,68)));
        setContentView(root);
    }

    private View strengthCard(String title,String subtitle,String priority,int accent,View.OnClickListener click){
        LinearLayout card=DarkUi.hero(this); card.setOnClickListener(click); card.setClickable(true);
        LinearLayout top=DarkUi.h(this,10);
        TextView mark=DarkUi.text(this,"●",18,accent,android.graphics.Typeface.DEFAULT_BOLD); top.addView(mark,new LinearLayout.LayoutParams(DarkUi.dp(this,28),ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout copy=DarkUi.v(this,4); copy.addView(DarkUi.h1(this,title)); copy.addView(DarkUi.body(this,subtitle)); top.addView(copy,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView arrow=DarkUi.text(this,"›",30,DarkUi.GOLD,android.graphics.Typeface.DEFAULT); arrow.setGravity(Gravity.CENTER); top.addView(arrow,new LinearLayout.LayoutParams(DarkUi.dp(this,34),DarkUi.dp(this,42))); card.addView(top);
        card.addView(DarkUi.chip(this,priority,accent));
        return card;
    }

    private String todayTitle(String kind){
        if("arms".equals(kind))return "Руки и хват";
        if("base".equals(kind))return "Ноги и корпус";
        if("mat".equals(kind))return "Тренировка на ковре";
        if("heavy".equals(kind))return "Тяжёлые раунды";
        if("competition".equals(kind))return "Соревнование";
        if("setup".equals(kind))return "Сначала настрой неделю";
        return "Восстановление";
    }

    private String todayDescription(WeekPlanEngine.Task task){
        if("mat".equals(task.kind)||"heavy".equals(task.kind)||"competition".equals(task.kind))return "Сегодня силовая не является основной задачей. Сохрани ресурсы для ковра.";
        return task.details==null?"Следуй недельному плану нагрузки.":task.details;
    }
}
