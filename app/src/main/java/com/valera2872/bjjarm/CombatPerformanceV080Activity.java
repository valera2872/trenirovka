package com.valera2872.bjjarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** 0.8.0 dashboard: current sport focus and discipline-specific fighting systems. */
public class CombatPerformanceV080Activity extends CombatPerformanceV070Activity {
    private static final String TAG_SPORT_CARD = "sport-focus-card-080";
    private static final String FIGHTING_CARD_TAG = "fighting-system-card";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String SYSTEM_PREFS = "combat_fighting_system_v2";

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View root = getWindow().getDecorView();
        root.post(this::applySportSpecialization);
    }

    private void applySportSpecialization() {
        View root = getWindow().getDecorView();
        SharedPreferences profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        ArrayList<String> sports = profileSports(profile);
        String active = profile.getString("active_sport", "").trim();
        if (active.isEmpty() || !sports.contains(active)) active = sports.get(0);

        View fightingCard = findTaggedView(root, FIGHTING_CARD_TAG);
        refreshFightingSystemCard(fightingCard, active);
        addSportFocusCard(fightingCard, active);
    }

    private void refreshFightingSystemCard(View fightingCard, String activeSport) {
        if (!(fightingCard instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) fightingCard;
        card.removeAllViews();
        card.addView(label("Моя борцовская система", 18, Color.rgb(25, 36, 34), true));
        card.addView(label(activeSport, 12, Color.rgb(28, 104, 91), true));

        SharedPreferences system = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        String prefix = SportGuidance.slug(activeSport) + "_";
        boolean configured = system.getBoolean(prefix + "configured", false);
        if (configured) {
            ArrayList<String> parts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String value = system.getString(prefix + "step_" + i, "").trim();
                if (!value.isEmpty()) parts.add(value);
            }
            String compact = TextUtils.join("  →  ", parts);
            if (!compact.isEmpty()) {
                card.addView(label(compact, 15, Color.rgb(25, 36, 34), false));
            }
            card.addView(label(
                    "У каждой выбранной дисциплины хранится своя рабочая цепочка.",
                    13, Color.rgb(94, 111, 107), false));
        } else {
            card.addView(label(
                    SportGuidance.description(activeSport),
                    14, Color.rgb(94, 111, 107), false));
        }

        Button open = styledButton(configured ? "Открыть цепочку" : "Собрать цепочку",
                Color.WHITE, Color.rgb(28, 104, 91), 15, 52);
        open.setOnClickListener(v -> startActivity(new Intent(this, FightingSystemV2Activity.class)));
        card.addView(open);
    }

    private void addSportFocusCard(View fightingCard, String active) {
        if (fightingCard == null) return;
        ViewParent parent = fightingCard.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout page = (LinearLayout) parent;

        View existing = findTaggedView(page, TAG_SPORT_CARD);
        if (existing != null) page.removeView(existing);

        LinearLayout card = vertical(9);
        card.setTag(TAG_SPORT_CARD);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(Color.WHITE, 18, 1, Color.rgb(222, 230, 227)));
        card.setElevation(dp(1));
        card.addView(label("Текущий вид борьбы", 18, Color.rgb(25, 36, 34), true));
        card.addView(label(active, 16, Color.rgb(28, 104, 91), true));
        card.addView(label(SportGuidance.description(active), 14, Color.rgb(94, 111, 107), false));

        String[] points = SportGuidance.focusPoints(active);
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < points.length; i++) {
            if (i > 0) summary.append(" · ");
            summary.append(points[i]);
        }
        card.addView(label(summary.toString(), 13, Color.rgb(94, 111, 107), false));

        Button change = styledButton("Изменить вид борьбы", Color.WHITE,
                Color.rgb(28, 104, 91), 14, 50);
        change.setOnClickListener(v -> startActivity(new Intent(this, SportFocusActivity.class)));
        card.addView(change);

        int fightingIndex = page.indexOfChild(fightingCard);
        page.addView(card, Math.max(0, fightingIndex));
    }

    private ArrayList<String> profileSports(SharedPreferences profile) {
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

    private View findTaggedView(View view, String tag) {
        if (tag.equals(view.getTag())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTaggedView(group.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
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
