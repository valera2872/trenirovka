package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.text.TextUtils;
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

/** Premium dark profile used by the 0.10 experience on clean install and later edits. */
public class PremiumDarkProfileActivity extends Activity {
    private static final String PREFS = "combat_performance_profile";
    private static final int REQUEST_VOICE = 10101;

    private static final String[] SPORTS = {
            "Грэпплинг / No-Gi", "Бразильское джиу-джитсу", "Дзюдо",
            "Вольная борьба", "Греко-римская борьба"
    };
    private static final String[] STYLES = {
            "Работа снизу / гард", "Проход и контроль сверху", "Стойка и броски",
            "Смешанный стиль", "Разный стиль в разных видах", "Стиль ещё формируется"
    };
    private static final String[] AREAS = {
            "Спина и тяга", "Руки и предплечья", "Ноги", "Корпус",
            "Шея и плечевой пояс", "Взрывная сила", "Силовая выносливость", "Подвижность"
    };

    private SharedPreferences profile;
    private EditText voiceTarget;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PREFS, MODE_PRIVATE);
        DarkUi.apply(this);
        render();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VOICE || voiceTarget == null) return;
        EditText target = voiceTarget;
        voiceTarget = null;
        if (resultCode != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String spoken = results.get(0).trim();
        if (spoken.isEmpty()) return;
        String old = target.getText().toString().trim();
        String next = old.isEmpty() ? spoken : old + " " + spoken;
        target.setText(next);
        target.setSelection(next.length());
    }

    private void render() {
        ScrollView scroll = DarkUi.scroll(this);
        LinearLayout page = DarkUi.page(this);
        page.setPadding(DarkUi.dp(this,16), DarkUi.dp(this,18), DarkUi.dp(this,16), DarkUi.dp(this,34));
        scroll.addView(page);

        if (profile.getBoolean("profile_complete", false)) {
            Button back = DarkUi.outline(this, "← Назад");
            back.setOnClickListener(v -> finish());
            page.addView(back);
        }

        LinearLayout hero = DarkUi.hero(this);
        hero.addView(DarkUi.gold(this, "COMBAT PERFORMANCE · PROFILE"));
        hero.addView(DarkUi.title(this, profile.getBoolean("profile_complete", false)
                ? "Твоя подготовка" : "Соберём личный план"));
        hero.addView(DarkUi.bodyWhite(this,
                "Техника, недельная нагрузка и персональная силовая подготовка будут опираться на этот профиль."));
        TextView mark = DarkUi.text(this, "CP", 36, DarkUi.GOLD, Typeface.DEFAULT_BOLD);
        mark.setGravity(Gravity.END);
        hero.addView(mark);
        page.addView(hero);

        LinearLayout base = DarkUi.card(this);
        base.addView(DarkUi.gold(this, "ОСНОВА"));
        base.addView(DarkUi.h1(this, "Спортсмен"));
        EditText name = input("Имя", profile.getString("name", ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText age = input("Возраст", String.valueOf(profile.getInt("age",14)), InputType.TYPE_CLASS_NUMBER);
        EditText sessions = input("Тренировок в неделю", String.valueOf(profile.getInt("mat_sessions",5)), InputType.TYPE_CLASS_NUMBER);
        base.addView(label("Имя")); base.addView(name);
        base.addView(label("Возраст")); base.addView(age);
        base.addView(label("Тренировок на ковре в неделю")); base.addView(sessions);
        page.addView(base);

        LinearLayout sportsCard = DarkUi.card(this);
        sportsCard.addView(DarkUi.gold(this, "ДИСЦИПЛИНЫ"));
        sportsCard.addView(DarkUi.h1(this, "Где ты борешься"));
        sportsCard.addView(DarkUi.body(this, "Можно выбрать несколько видов борьбы."));
        List<String> selected = savedSports();
        CheckBox[] checks = new CheckBox[SPORTS.length];
        for (int i=0;i<SPORTS.length;i++) {
            CheckBox c = new CheckBox(this);
            c.setText(SPORTS[i]); c.setTextColor(DarkUi.TEXT); c.setTextSize(15);
            c.setButtonTintList(android.content.res.ColorStateList.valueOf(DarkUi.GOLD));
            c.setChecked(selected.contains(SPORTS[i]));
            c.setPadding(0, DarkUi.dp(this,7), 0, DarkUi.dp(this,7));
            checks[i]=c; sportsCard.addView(c);
        }
        page.addView(sportsCard);

        LinearLayout strength = DarkUi.card(this);
        strength.addView(DarkUi.gold(this, "ПЕРСОНАЛЬНАЯ СИЛОВАЯ"));
        strength.addView(DarkUi.h1(this, "Что развивать"));
        Spinner style = spinner(STYLES, profile.getString("style", STYLES[0]));
        Spinner strong = spinner(AREAS, profile.getString("strong_area", AREAS[0]));
        Spinner p1 = spinner(AREAS, profile.getString("priority_1", AREAS[1]));
        Spinner p2 = spinner(AREAS, profile.getString("priority_2", AREAS[2]));
        strength.addView(label("Твой стиль борьбы")); strength.addView(style);
        strength.addView(label("Что уже развито")); strength.addView(strong);
        strength.addView(label("Главный физический приоритет")); strength.addView(p1);
        strength.addView(label("Второй приоритет")); strength.addView(p2);
        page.addView(strength);

        LinearLayout tech = DarkUi.goldCard(this);
        tech.addView(DarkUi.gold(this, "ТЕХНИЧЕСКИЙ ФОКУС"));
        tech.addView(DarkUi.h1(this, "Что переносим на ковёр"));
        tech.addView(DarkUi.body(this,
                "Для точных английских названий используй локальный поиск. Голос оставляем для свободного русского текста."));

        EditText favorite = multiline("Любимые позиции и техники", profile.getString("favorite_techniques", ""));
        tech.addView(label("Любимые позиции и техники"));
        tech.addView(favorite);
        tech.addView(toolRow(favorite, false));

        EditText mission = multiline("Одна техника или действие на 30 дней", profile.getString("mission", ""));
        tech.addView(label("Фокус на 30 дней"));
        tech.addView(mission);
        tech.addView(toolRow(mission, true));
        page.addView(tech);

        Button save = DarkUi.primary(this, "Сохранить и открыть мой план");
        save.setOnClickListener(v -> save(save, name, age, sessions, checks, style, strong, p1, p2, favorite, mission));
        page.addView(save);
        setContentView(scroll);
    }

    private View toolRow(EditText target, boolean replace) {
        LinearLayout row = DarkUi.h(this, 8);
        Button voice = DarkUi.secondary(this, "🎙 Голосом по-русски");
        Button find = DarkUi.outline(this, "⌕ Найти технику");
        voice.setOnClickListener(v -> voice(target));
        find.setOnClickListener(v -> TechniqueCatalog.show(this, target, replace));
        row.addView(voice, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(find, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private void voice(EditText target) {
        voiceTarget = target;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { startActivityForResult(intent, REQUEST_VOICE); }
        catch (ActivityNotFoundException e) { voiceTarget=null; toast("На телефоне не найден сервис распознавания речи."); }
    }

    private void save(Button button, EditText name, EditText age, EditText sessions, CheckBox[] checks,
                      Spinner style, Spinner strong, Spinner p1, Spinner p2, EditText favorite, EditText mission) {
        String n = text(name); String m = text(mission);
        ArrayList<String> sports = new ArrayList<>();
        for (int i=0;i<checks.length;i++) if (checks[i].isChecked()) sports.add(SPORTS[i]);
        if (n.isEmpty()) { toast("Укажи имя спортсмена."); name.requestFocus(); return; }
        if (sports.isEmpty()) { toast("Выбери хотя бы один вид борьбы."); return; }
        if (m.isEmpty()) { toast("Выбери технику или действие на 30 дней."); mission.requestFocus(); return; }

        button.setEnabled(false); button.setText("Сохраняю…");
        boolean changed = !m.equals(profile.getString("mission", "").trim());
        String oldActive = profile.getString("active_sport", "").trim();
        String active = sports.contains(oldActive) ? oldActive : sports.get(0);
        SharedPreferences.Editor e = profile.edit()
                .putBoolean("profile_complete", true)
                .putString("name", n)
                .putInt("age", clamp(number(age,14),8,80))
                .putInt("mat_sessions", clamp(number(sessions,5),1,14))
                .putString("sports", TextUtils.join("|", sports))
                .putString("sport", sports.get(0))
                .putString("active_sport", active)
                .putString("style", String.valueOf(style.getSelectedItem()))
                .putString("strong_area", String.valueOf(strong.getSelectedItem()))
                .putString("priority_1", String.valueOf(p1.getSelectedItem()))
                .putString("priority_2", String.valueOf(p2.getSelectedItem()))
                .putString("favorite_techniques", text(favorite))
                .putString("mission", m);
        if (changed || !profile.contains("mission_started_at")) {
            e.putLong("mission_started_at", System.currentTimeMillis())
                    .putInt("mission_active_days",0).putInt("mission_attempts",0)
                    .putInt("mission_successes",0).putInt("mission_finishes",0)
                    .remove("mission_last_day");
        }
        boolean ok;
        try { ok = e.commit(); } catch (RuntimeException ex) { ok=false; }
        if (!ok || !profile.getBoolean("profile_complete", false)) {
            button.setEnabled(true); button.setText("Сохранить и открыть мой план");
            toast("Не удалось сохранить профиль."); return;
        }
        Intent i = new Intent(this, CombatPerformanceV100Activity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i); finish();
    }

    private EditText input(String hint, String value, int type) {
        EditText f = new EditText(this); f.setHint(hint); f.setText(value); f.setInputType(type);
        f.setTextColor(DarkUi.TEXT); f.setHintTextColor(DarkUi.MUTED); f.setTextSize(15);
        f.setPadding(DarkUi.dp(this,13),DarkUi.dp(this,11),DarkUi.dp(this,13),DarkUi.dp(this,11));
        f.setBackground(DarkUi.round(DarkUi.BG_2,14,1,DarkUi.BORDER)); f.setMinHeight(DarkUi.dp(this,52));
        return f;
    }

    private EditText multiline(String hint, String value) {
        EditText f = input(hint, value, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        f.setMinLines(2); f.setMaxLines(7); f.setGravity(Gravity.TOP | Gravity.START); return f;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent); v.setTextColor(DarkUi.TEXT); v.setTextSize(14); return v;
            }
        };
        s.setAdapter(a); int idx=0; for (int i=0;i<values.length;i++) if (values[i].equals(selected)) idx=i; s.setSelection(idx);
        s.setPadding(DarkUi.dp(this,10),DarkUi.dp(this,5),DarkUi.dp(this,10),DarkUi.dp(this,5));
        s.setBackground(DarkUi.round(DarkUi.BG_2,14,1,DarkUi.BORDER)); s.setMinimumHeight(DarkUi.dp(this,52));
        return s;
    }

    private TextView label(String s) { TextView t=DarkUi.gold(this,s); t.setTextSize(13); return t; }
    private String text(EditText e) { return e.getText()==null?"":e.getText().toString().trim(); }
    private int number(EditText e,int fallback) { try{return Integer.parseInt(text(e));}catch(Exception x){return fallback;} }
    private int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private List<String> savedSports() {
        ArrayList<String> out=new ArrayList<>(); String raw=profile.getString("sports","").trim();
        if(!raw.isEmpty()) for(String s:raw.split("\\|")) if(!s.trim().isEmpty()) out.add(s.trim());
        if(out.isEmpty()) out.add(profile.getString("sport",SPORTS[0])); return out;
    }
}
