package com.valera2872.bjjarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small local catalog for exact combat-sport terminology.
 * It is intentionally offline and never pretends to understand a technique through AI.
 */
public final class TechniqueCatalog {
    private TechniqueCatalog() { }

    private static final Entry[] ENTRIES = {
            new Entry("Closed Guard", "закрытый гард", "клоз гард", "closed guard"),
            new Entry("Half Guard", "халф гард", "полугард", "half guard"),
            new Entry("Butterfly Guard", "баттерфляй гард", "бабочка", "butterfly guard"),
            new Entry("Williams Guard", "вильямс гард", "уильямс гард", "williams"),
            new Entry("De La Riva Guard", "де ла рива", "деларива", "de la riva"),
            new Entry("Reverse De La Riva", "реверс де ла рива", "обратная де ла рива", "rdlr"),
            new Entry("X-Guard", "икс гард", "x guard", "х гард"),
            new Entry("Single-Leg X", "сингл лег икс", "одноногий икс", "slx"),
            new Entry("Ashi Garami", "аши гарами", "аши", "ashi"),
            new Entry("Inside Sankaku", "инсайд санкаку", "седло", "saddle"),
            new Entry("Arm Drag", "арм драг", "срыв руки", "armdrag"),
            new Entry("Body Lock", "боди лок", "захват корпуса", "bodylock"),
            new Entry("Double-Leg Takedown", "дабл лег", "проход в две ноги", "double leg"),
            new Entry("Single-Leg Takedown", "сингл лег", "проход в одну ногу", "single leg"),
            new Entry("High Crotch", "хай кротч", "высокий проход", "high crotch"),
            new Entry("Duck Under", "дак андер", "нырок под руку", "duckunder"),
            new Entry("Snapdown", "снэпдаун", "сбивание головы", "snap down"),
            new Entry("Sprawl", "спрол", "защита от прохода", "sprawl"),
            new Entry("Knee Cut Pass", "ни кат", "проход коленом", "knee slice"),
            new Entry("Toreando Pass", "тореандо", "тореадор", "bullfighter pass"),
            new Entry("Over-Under Pass", "овер андер", "проход овер андер", "over under"),
            new Entry("Hip Bump Sweep", "хип бамп", "свип тазом", "hip bump"),
            new Entry("Scissor Sweep", "ножницы", "свип ножницами", "scissor sweep"),
            new Entry("Technical Stand-Up", "технический подъём", "technical standup"),
            new Entry("Back Take", "выход на спину", "забрать спину", "backtake"),
            new Entry("Mount", "маунт", "позиция сверху", "mount position"),
            new Entry("Side Control", "сайд контроль", "боковой контроль", "sidecontrol"),
            new Entry("North-South", "север юг", "норт саут", "north south"),
            new Entry("Rear Naked Choke", "удушение сзади", "рнс", "rear naked"),
            new Entry("Guillotine Choke", "гильотина", "guillotine"),
            new Entry("Triangle Choke", "треугольник", "трайангл", "triangle"),
            new Entry("Armbar", "рычаг локтя", "армбар", "juji gatame"),
            new Entry("Kimura", "кимура", "обратный узел плеча"),
            new Entry("Americana", "американа", "узел плеча"),
            new Entry("D'Arce Choke", "дарс", "дарс чок", "d'arce"),
            new Entry("Anaconda Choke", "анаконда", "anaconda"),
            new Entry("Buggy Choke", "багги чок", "баггичок", "buggy"),
            new Entry("Straight Ankle Lock", "прямой ахилл", "ущемление ахилла", "ankle lock"),
            new Entry("Heel Hook", "хил хук", "скручивание пятки", "heelhook"),
            new Entry("Kneebar", "ки бар", "рычаг колена", "knee bar"),
            new Entry("Osoto Gari", "осото гари", "отхват", "osotogari"),
            new Entry("Ouchi Gari", "оучи гари", "зацеп изнутри", "ouchigari"),
            new Entry("Kouchi Gari", "коучи гари", "подсечка изнутри", "kouchigari"),
            new Entry("Uchi Mata", "учи мата", "подхват изнутри", "uchimata"),
            new Entry("Seoi Nage", "сэой нагэ", "бросок через спину", "seoinage"),
            new Entry("Tai Otoshi", "тай отоши", "передняя подножка", "taiotoshi"),
            new Entry("Harai Goshi", "хараи гоши", "подхват под две ноги", "haraigoshi"),
            new Entry("Sasae Tsurikomi Ashi", "сасаэ", "передняя подсечка", "sasae"),
            new Entry("De Ashi Barai", "де аши барай", "боковая подсечка", "deashi"),
            new Entry("Tomoe Nage", "томоэ нагэ", "бросок через голову", "tomoenage"),
            new Entry("Kesa Gatame", "кеса гатамэ", "удержание сбоку", "kesagatame"),
            new Entry("Fireman's Carry", "мельница", "фаерман керри", "fireman carry"),
            new Entry("Arm Spin", "вертушка", "бросок через руку", "arm spin"),
            new Entry("Gut Wrench", "накат", "гат ренч", "gutwrench"),
            new Entry("Granby Roll", "гранби", "кувырок гранби", "granby"),
            new Entry("Stand-Up Escape", "выход вставанием", "подъём из партера", "stand up escape")
    };

    public static void show(Activity activity, EditText target, boolean replaceExisting) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = PremiumUi.dp(activity, 16);
        box.setPadding(pad, PremiumUi.dp(activity, 8), pad, 0);

        TextView help = PremiumUi.body(activity,
                "Ищи по-русски или по-английски: «вильямс», «де ла рива», «дарс», «учи мата». Техника выбирается из локального списка и не отправляется в интернет.");
        box.addView(help);

        EditText search = PremiumUi.input(activity, "Название или произношение", "",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = PremiumUi.dp(activity, 12);
        box.addView(search, searchParams);

        ListView list = new ListView(activity);
        list.setDividerHeight(1);
        list.setBackgroundColor(Color.WHITE);
        box.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, PremiumUi.dp(activity, 310)));

        ArrayList<Entry> visible = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);

        AlertDialog[] dialog = new AlertDialog[1];
        Runnable refresh = () -> {
            visible.clear();
            labels.clear();
            visible.addAll(find(search.getText().toString()));
            for (Entry entry : visible) labels.add(entry.display());
            adapter.notifyDataSetChanged();
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= visible.size()) return;
            apply(target, visible.get(position).name, replaceExisting);
            if (dialog[0] != null) dialog[0].dismiss();
        });

        dialog[0] = new AlertDialog.Builder(activity)
                .setTitle("Найти технику")
                .setView(box)
                .setNegativeButton("Закрыть", null)
                .setPositiveButton("Использовать свой текст", (d, which) -> {
                    String custom = search.getText().toString().trim();
                    if (custom.isEmpty()) {
                        Toast.makeText(activity, "Сначала введи название.", Toast.LENGTH_SHORT).show();
                    } else {
                        apply(target, custom, replaceExisting);
                    }
                })
                .create();
        refresh.run();
        dialog[0].show();
        search.requestFocus();
    }

    private static List<Entry> find(String query) {
        String clean = normalize(query);
        ArrayList<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (clean.isEmpty() || entry.matches(clean)) result.add(entry);
            if (result.size() >= 35) break;
        }
        return result;
    }

    private static void apply(EditText target, String value, boolean replaceExisting) {
        String existing = target.getText().toString().trim();
        String next;
        if (replaceExisting || existing.isEmpty()) next = value;
        else if (normalize(existing).contains(normalize(value))) next = existing;
        else next = existing + ", " + value;
        target.setText(next);
        target.setSelection(next.length());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace("-", " ")
                .replace("'", "")
                .replace("’", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final class Entry {
        final String name;
        final String[] aliases;

        Entry(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
        }

        String display() {
            return aliases.length == 0 ? name : name + "\n" + aliases[0];
        }

        boolean matches(String query) {
            String all = normalize(name);
            for (String alias : aliases) all += " " + normalize(alias);
            if (all.contains(query)) return true;
            for (String token : query.split(" ")) {
                if (token.length() > 1 && !all.contains(token)) return false;
            }
            return !query.isEmpty();
        }
    }
}
