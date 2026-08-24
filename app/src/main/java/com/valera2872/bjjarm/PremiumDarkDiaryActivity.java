package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Premium quick diary. Keeps entries_json format used since 0.9.0. */
public class PremiumDarkDiaryActivity extends Activity {
    private static final String DIARY_PREFS = "combat_training_diary";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int REQ_VOICE = 10021;
    private SharedPreferences diary;
    private SharedPreferences profile;
    private EditText voiceTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        DarkUi.apply(this);
        showHome();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_VOICE || resultCode != RESULT_OK || data == null || voiceTarget == null) return;
        ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (r == null || r.isEmpty()) return;
        String spoken = r.get(0) == null ? "" : r.get(0).trim();
        if (spoken.isEmpty()) return;
        String old = voiceTarget.getText().toString().trim();
        String value = old.isEmpty() ? spoken : old + " " + spoken;
        voiceTarget.setText(value);
        voiceTarget.setSelection(value.length());
        voiceTarget = null;
    }

    private void showHome() {
        LinearLayout root = baseRoot();
        ScrollView scroll = (ScrollView) root.getChildAt(0);
        LinearLayout page = (LinearLayout) scroll.getChildAt(0);
        page.addView(topBack("Дневник"));
        page.addView(DarkUi.title(this, "Запиши только полезное"));
        page.addView(DarkUi.body(this, "Быстрая запись занимает около минуты. Всё лишнее можно добавить позже."));

        LinearLayout hero = DarkUi.hero(this);
        hero.addView(DarkUi.gold(this, "СЛЕДУЮЩАЯ ЗАДАЧА"));
        hero.addView(DarkUi.h1(this, nextTask()));
        hero.addView(DarkUi.small(this, activeSport() + " · фокус: " + mission()));
        Button quick = DarkUi.primary(this, "Быстрая запись · около минуты");
        quick.setOnClickListener(v -> showQuickEntry());
        hero.addView(quick);
        page.addView(hero);

        JSONArray entries = entries();
        LinearLayout stats = DarkUi.h(this, 8);
        stats.addView(DarkUi.stat(this, String.valueOf(entries.length()), "Записей", DarkUi.GOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_attempts", 0)), "Попыток", DarkUi.ORANGE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(DarkUi.stat(this, String.valueOf(profile.getInt("mission_successes", 0)), "Успехов", DarkUi.GREEN), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        page.addView(stats);

        LinearLayout history = DarkUi.card(this);
        history.addView(DarkUi.h1(this, "Последние тренировки"));
        if (entries.length() == 0) {
            history.addView(DarkUi.body(this, "Первая запись появится здесь после тренировки."));
        } else {
            for (int i = 0; i < Math.min(3, entries.length()); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e != null) history.addView(historyRow(e));
            }
        }
        Button openHistory = DarkUi.secondary(this, "Открыть историю");
        openHistory.setOnClickListener(v -> showHistory());
        history.addView(openHistory);
        page.addView(history);

        Button manage = DarkUi.outline(this, "Подробная запись и редактирование");
        manage.setOnClickListener(v -> startActivity(new Intent(this, PremiumTrainingDiaryV2Activity.class)));
        page.addView(manage);
        setContentView(root);
    }

    private void showQuickEntry() {
        LinearLayout root = baseRoot();
        ScrollView scroll = (ScrollView) root.getChildAt(0);
        LinearLayout page = (LinearLayout) scroll.getChildAt(0);
        page.addView(topBack("Быстрая запись"));
        page.addView(DarkUi.title(this, activeSport()));
        page.addView(DarkUi.body(this, "Главное — попытки, один вывод и следующая задача."));

        Counter rounds = new Counter("Раунды", 0, 0, 30);
        Counter attempts = new Counter("Попытки", 0, 0, 99);
        Counter successes = new Counter("Успешные применения", 0, 0, 99);
        page.addView(rounds.view);

        LinearLayout technique = DarkUi.goldCard(this);
        technique.addView(DarkUi.gold(this, "ТЕКУЩАЯ ТЕХНИКА"));
        technique.addView(DarkUi.h1(this, mission()));
        technique.addView(attempts.view);
        technique.addView(successes.view);
        page.addView(technique);

        EditText partners = darkInput("Партнёры", false);
        EditText worked = darkInput("Что отрабатывал?", true);
        EditText problem = darkInput("Главная проблема", true);
        EditText next = darkInput("Следующий фокус", true);
        page.addView(fieldCard("С кем боролся", partners, true));
        page.addView(fieldCard("Что отрабатывал", worked, true));
        page.addView(fieldCard("Главная проблема", problem, true));
        page.addView(fieldCard("Следующий фокус", next, true));

        Button save = DarkUi.primary(this, "Сохранить запись");
        save.setOnClickListener(v -> saveQuick(save, rounds.value, attempts.value, successes.value,
                text(partners), text(worked), text(problem), text(next)));
        page.addView(save);
        setContentView(root);
    }

    private void saveQuick(Button button, int rounds, int attempts, int successes, String partners,
                           String techniques, String problem, String userNext) {
        if (successes > attempts && attempts > 0) {
            toast("Успешных применений не может быть больше попыток.");
            return;
        }
        if (rounds == 0 && attempts == 0 && partners.isEmpty() && techniques.isEmpty() && problem.isEmpty() && userNext.isEmpty()) {
            toast("Запиши хотя бы один полезный факт.");
            return;
        }
        button.setEnabled(false);
        button.setText("Сохраняю…");
        long now = System.currentTimeMillis();
        String generated = buildNext(attempts, successes, problem, userNext);
        JSONObject e = new JSONObject();
        try {
            e.put("id", now); e.put("timestamp", now); e.put("sport", activeSport()); e.put("type", "Тренировка");
            e.put("duration", 0); e.put("rounds", rounds); e.put("partners", partners); e.put("result", "Не отмечать результат");
            e.put("mission", mission()); e.put("attempts", attempts); e.put("successes", successes); e.put("finishes", 0);
            e.put("techniques", techniques); e.put("breakdown", problem); e.put("coach", ""); e.put("next_focus", generated); e.put("entry_mode", "quick-dark");
        } catch (JSONException ex) {
            button.setEnabled(true); button.setText("Сохранить запись"); toast("Не удалось подготовить запись."); return;
        }
        JSONArray old = entries();
        JSONArray out = new JSONArray(); out.put(e);
        for (int i=0; i<old.length() && i<499; i++) { JSONObject item=old.optJSONObject(i); if(item!=null) out.put(item); }
        boolean saved = diary.edit().putString("entries_json", out.toString())
                .putString("next_task_" + SportGuidance.slug(activeSport()), generated)
                .putLong("last_timestamp", now).commit();
        if (!saved) { button.setEnabled(true); button.setText("Сохранить запись"); toast("Не удалось сохранить запись."); return; }
        updateCounters(attempts, successes);
        new AlertDialog.Builder(this).setTitle("Запись сохранена")
                .setMessage("Следующая задача:\n\n" + generated)
                .setPositiveButton("Готово", (d,w) -> showHome()).setCancelable(false).show();
    }

    private void showHistory() {
        LinearLayout root = baseRoot();
        ScrollView scroll = (ScrollView) root.getChildAt(0);
        LinearLayout page = (LinearLayout) scroll.getChildAt(0);
        page.addView(topBack("История"));
        page.addView(DarkUi.title(this, "Тренировки и выводы"));
        JSONArray a = entries();
        if (a.length()==0) page.addView(DarkUi.body(this, "Записей пока нет."));
        for (int i=0; i<Math.min(60,a.length()); i++) {
            JSONObject e=a.optJSONObject(i); if(e==null) continue;
            LinearLayout card=DarkUi.card(this);
            card.addView(DarkUi.gold(this, date(e.optLong("timestamp"))));
            card.addView(DarkUi.h2(this, e.optString("sport","Борьба") + " · " + e.optString("type","Тренировка")));
            String summary=summary(e); if(!summary.isEmpty()) card.addView(DarkUi.bodyWhite(this, summary));
            String next=e.optString("next_focus","").trim(); if(!next.isEmpty()) { card.addView(DarkUi.gold(this,"СЛЕДУЮЩИЙ ФОКУС")); card.addView(DarkUi.body(this,next)); }
            page.addView(card);
        }
        Button manage = DarkUi.secondary(this, "Редактировать или удалить записи");
        manage.setOnClickListener(v -> startActivity(new Intent(this, PremiumTrainingDiaryV2Activity.class)));
        page.addView(manage);
        setContentView(root);
    }

    private LinearLayout baseRoot() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(DarkUi.BG);
        ScrollView scroll=DarkUi.scroll(this); LinearLayout page=DarkUi.page(this); page.setPadding(DarkUi.dp(this,16),DarkUi.dp(this,18),DarkUi.dp(this,16),DarkUi.dp(this,28)); scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout nav=DarkUi.h(this,0); nav.setBackgroundColor(DarkUi.BG_2); nav.setPadding(DarkUi.dp(this,8),0,DarkUi.dp(this,8),0);
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_today,"Сегодня",false,v->{startActivity(new Intent(this,CombatPerformanceV100Activity.class));finish();}),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_week,"Неделя",false,v->startActivity(new Intent(this,WeeklyPlanV4Activity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_diary,"Дневник",true,v->showHome()),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        nav.addView(DarkUi.navItem(this,R.drawable.ic_nav_profile,"Профиль",false,v->startActivity(new Intent(this,PremiumProfileActivity.class))),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));
        root.addView(nav,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DarkUi.dp(this,68)));
        return root;
    }

    private View topBack(String title) {
        LinearLayout row=DarkUi.h(this,10); TextView back=DarkUi.text(this,"‹",32,DarkUi.TEXT,android.graphics.Typeface.DEFAULT); back.setGravity(Gravity.CENTER); back.setOnClickListener(v->showHome()); row.addView(back,new LinearLayout.LayoutParams(DarkUi.dp(this,34),DarkUi.dp(this,40))); row.addView(DarkUi.h2(this,title),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); return row;
    }

    private View fieldCard(String label, EditText field, boolean voice) {
        LinearLayout card=DarkUi.card(this); card.addView(DarkUi.small(this,label.toUpperCase(Locale.ROOT))); card.addView(field);
        if(voice){ Button mic=DarkUi.outline(this,"🎙 Голосом по-русски"); mic.setOnClickListener(v->voice(field)); card.addView(mic); }
        return card;
    }

    private EditText darkInput(String hint, boolean multi) {
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(DarkUi.MUTED); e.setTextColor(DarkUi.TEXT); e.setTextSize(15); e.setPadding(DarkUi.dp(this,12),DarkUi.dp(this,11),DarkUi.dp(this,12),DarkUi.dp(this,11)); e.setBackground(DarkUi.round(DarkUi.BG_2,12,1,DarkUi.BORDER));
        e.setInputType(InputType.TYPE_CLASS_TEXT | (multi?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0)); if(multi){e.setMinLines(2);e.setMaxLines(5);e.setGravity(Gravity.TOP|Gravity.START);} return e;
    }

    private void voice(EditText target){ voiceTarget=target; try{ Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU"); i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1); startActivityForResult(i,REQ_VOICE);}catch(ActivityNotFoundException ex){toast("На телефоне не найден сервис распознавания речи.");}}

    private View historyRow(JSONObject e){ LinearLayout row=DarkUi.v(this,4); row.setPadding(0,DarkUi.dp(this,7),0,DarkUi.dp(this,7)); row.addView(DarkUi.gold(this,date(e.optLong("timestamp")))); row.addView(DarkUi.bodyWhite(this,summary(e))); return row; }

    private String summary(JSONObject e){ ArrayList<String> p=new ArrayList<>(); int r=e.optInt("rounds"); if(r>0)p.add(r+" раундов"); int a=e.optInt("attempts"),s=e.optInt("successes"); if(a>0||s>0)p.add(a+" попыток · "+s+" успехов"); String t=e.optString("techniques","").trim(); if(!t.isEmpty())p.add(t); return android.text.TextUtils.join(" · ",p); }

    private void updateCounters(int attempts,int successes){ if(attempts==0&&successes==0)return; String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); String last=profile.getString("mission_last_day",""); int days=profile.getInt("mission_active_days",0); if(!today.equals(last))days++; profile.edit().putInt("mission_attempts",profile.getInt("mission_attempts",0)+attempts).putInt("mission_successes",profile.getInt("mission_successes",0)+successes).putInt("mission_active_days",days).putString("mission_last_day",today).apply(); }

    private String buildNext(int attempts,int successes,String problem,String user){ if(!user.isEmpty())return user; String m=mission(); if(attempts==0)return "Сделать хотя бы одну осознанную попытку: "+m+"."; if(successes==0&&!problem.isEmpty())return "Оставить тот же фокус и проверить: "+problem+"."; if(successes==0)return "Повторить "+m+" и заметить точку, где действие останавливается."; if(successes>=2)return "Повторить "+m+" и связать успешное действие со следующим этапом плана."; return "Ещё раз применить "+m+" и сохранить контроль после выполнения."; }
    private String nextTask(){ String n=diary.getString("next_task_"+SportGuidance.slug(activeSport()),"").trim(); return n.isEmpty()?"Сделай одну осознанную попытку текущей техники и запомни, где действие получилось или остановилось.":n; }
    private JSONArray entries(){ try{return new JSONArray(diary.getString("entries_json","[]"));}catch(JSONException e){return new JSONArray();} }
    private String activeSport(){String s=profile.getString("active_sport","").trim();return s.isEmpty()?profile.getString("sport","Грэпплинг / No-Gi"):s;}
    private String mission(){String m=profile.getString("mission","").trim();return m.isEmpty()?"Текущая техника":m;}
    private String date(long t){return t<=0?"—":new SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(new Date(t));}
    private String text(EditText e){return e.getText().toString().trim();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private final class Counter {
        final LinearLayout view; int value; final int min,max; final TextView number;
        Counter(String label,int initial,int min,int max){this.value=initial;this.min=min;this.max=max; view=DarkUi.card(PremiumDarkDiaryActivity.this); LinearLayout row=DarkUi.h(PremiumDarkDiaryActivity.this,10); row.addView(DarkUi.h2(PremiumDarkDiaryActivity.this,label),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); Button minus=DarkUi.outline(PremiumDarkDiaryActivity.this,"−"); minus.setMinWidth(DarkUi.dp(PremiumDarkDiaryActivity.this,46)); number=DarkUi.number(PremiumDarkDiaryActivity.this,String.valueOf(value)); number.setGravity(Gravity.CENTER); Button plus=DarkUi.outline(PremiumDarkDiaryActivity.this,"+"); plus.setMinWidth(DarkUi.dp(PremiumDarkDiaryActivity.this,46)); minus.setOnClickListener(v->{if(value>min){value--;number.setText(String.valueOf(value));}}); plus.setOnClickListener(v->{if(value<max){value++;number.setText(String.valueOf(value));}}); row.addView(minus,new LinearLayout.LayoutParams(DarkUi.dp(PremiumDarkDiaryActivity.this,52),DarkUi.dp(PremiumDarkDiaryActivity.this,48))); row.addView(number,new LinearLayout.LayoutParams(DarkUi.dp(PremiumDarkDiaryActivity.this,58),ViewGroup.LayoutParams.WRAP_CONTENT)); row.addView(plus,new LinearLayout.LayoutParams(DarkUi.dp(PremiumDarkDiaryActivity.this,52),DarkUi.dp(PremiumDarkDiaryActivity.this,48))); view.addView(row);}
    }
}
