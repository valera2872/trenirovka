package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/** Selects which combat sport is the current focus when the athlete practices several disciplines. */
public class SportFocusActivity extends Activity {
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final int BG = Color.rgb(245, 247, 246);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_DARK = Color.rgb(18, 73, 65);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

    private SharedPreferences profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        showScreen();
    }

    private void showScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = vertical(14);
        page.setPadding(dp(18), dp(20), dp(18), dp(36));
        scroll.addView(page);

        Button back = secondaryButton("← К главному экрану");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        page.addView(label("ТЕКУЩИЙ ВИД БОРЬБЫ", 12, PRIMARY, true));
        page.addView(label("На чём сосредоточиться сейчас?", 28, TEXT, true));
        page.addView(label(
                "Если ты занимаешься несколькими видами борьбы, приложение будет показывать цепочку и подсказки для выбранной дисциплины. Другие цепочки сохранятся отдельно.",
                15, MUTED, false));

        ArrayList<String> sports = profileSports();
        String active = profile.getString("active_sport", "").trim();
        if (active.isEmpty() || !sports.contains(active)) active = sports.get(0);

        LinearLayout chooseCard = card();
        page.addView(chooseCard);
        chooseCard.addView(sectionTitle("Выбери текущую дисциплину"));
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        chooseCard.addView(group);

        int checkedId = -1;
        for (int i = 0; i < sports.size(); i++) {
            RadioButton radio = new RadioButton(this);
            radio.setId(View.generateViewId());
            radio.setText(sports.get(i));
            radio.setTextSize(16);
            radio.setTextColor(TEXT);
            radio.setPadding(0, dp(7), 0, dp(7));
            radio.setTag(sports.get(i));
            group.addView(radio);
            if (sports.get(i).equals(active)) checkedId = radio.getId();
        }
        if (checkedId != -1) group.check(checkedId);

        LinearLayout preview = card();
        page.addView(preview);
        preview.addView(sectionTitle("Что изменится"));
        TextView description = label(SportGuidance.description(active), 15, TEXT, false);
        preview.addView(description);
        LinearLayout points = vertical(4);
        preview.addView(points);
        fillPoints(points, active);

        group.setOnCheckedChangeListener((radioGroup, id) -> {
            RadioButton selected = findViewById(id);
            if (selected == null) return;
            String sport = String.valueOf(selected.getTag());
            description.setText(SportGuidance.description(sport));
            fillPoints(points, sport);
        });

        Button save = primaryButton("Сохранить выбор");
        save.setOnClickListener(v -> {
            int id = group.getCheckedRadioButtonId();
            RadioButton selected = findViewById(id);
            if (selected == null) {
                toast("Выбери вид борьбы.");
                return;
            }
            String sport = String.valueOf(selected.getTag());
            boolean saved = profile.edit().putString("active_sport", sport).commit();
            if (!saved) {
                toast("Не удалось сохранить выбор.");
                return;
            }
            toast("Текущий вид борьбы: " + sport);
            finish();
        });
        page.addView(save);

        setContentView(scroll);
    }

    private void fillPoints(LinearLayout parent, String sport) {
        parent.removeAllViews();
        String[] values = SportGuidance.focusPoints(sport);
        for (String value : values) {
            parent.addView(label("• " + value, 14, MUTED, false));
        }
    }

    private ArrayList<String> profileSports() {
        ArrayList<String> result = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String part : saved.split("\\|")) {
                if (!part.trim().isEmpty()) result.add(part.trim());
            }
        }
        if (result.isEmpty()) result.add(profile.getString("sport", "Грэпплинг / No-Gi"));
        return result;
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

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
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

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
