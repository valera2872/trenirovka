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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/** 0.9.0 dashboard layer: pre-training task and closed-loop combat diary. */
public class CombatPerformanceV090Activity extends CombatPerformanceV080Activity {
    private static final String TAG_DIARY_CARD = "training-diary-card-090";
    private static final String FIGHTING_CARD_TAG = "fighting-system-card";
    private static final String PROFILE_PREFS = "combat_performance_profile";
    private static final String DIARY_PREFS = "combat_training_diary";

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View root = getWindow().getDecorView();
        root.post(this::addDiaryCard);
    }

    private void addDiaryCard() {
        View root = getWindow().getDecorView();
        View fightingCard = findTaggedView(root, FIGHTING_CARD_TAG);
        if (fightingCard == null) return;
        ViewParent parent = fightingCard.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout page = (LinearLayout) parent;

        View existing = findTaggedView(page, TAG_DIARY_CARD);
        if (existing != null) page.removeView(existing);

        SharedPreferences profile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        SharedPreferences diary = getSharedPreferences(DIARY_PREFS, MODE_PRIVATE);
        String sport = activeSport(profile);
        String mission = profile.getString("mission", "").trim();
        int count = entryCount(diary);
        String next = diary.getString("next_task_" + SportGuidance.slug(sport), "").trim();
        if (next.isEmpty()) {
            next = mission.isEmpty()
                    ? "Выбери один этап цепочки, попробуй его на ковре и после тренировки запиши результат."
                    : "Сделай хотя бы одну осознанную попытку текущей техники и запиши, где действие получилось или остановилось.";
        }

        LinearLayout card = vertical(9);
        card.setTag(TAG_DIARY_CARD);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(Color.WHITE, 18, 1, Color.rgb(222, 230, 227)));
        card.setElevation(dp(1));
        card.addView(label("Борцовский дневник", 18, Color.rgb(25, 36, 34), true));
        card.addView(label("Задача перед тренировкой", 12, Color.rgb(28, 104, 91), true));
        card.addView(label(next, 15, Color.rgb(25, 36, 34), false));
        if (count == 0) {
            card.addView(label(
                    "После ковра запиши партнёров, техники, проблему и совет тренера. Приложение подготовит следующий фокус.",
                    13, Color.rgb(94, 111, 107), false));
        } else {
            card.addView(label(count + " " + entryWord(count)
                            + " сохранено. Записи ведутся отдельно по каждому виду борьбы.",
                    13, Color.rgb(94, 111, 107), false));
        }

        Button open = styledButton(count == 0 ? "Записать первую тренировку" : "Открыть дневник",
                Color.WHITE, Color.rgb(28, 104, 91), 15, 52);
        open.setOnClickListener(v -> startActivity(new Intent(this, TrainingDiaryActivity.class)));
        card.addView(open);

        int fightingIndex = page.indexOfChild(fightingCard);
        page.addView(card, Math.min(page.getChildCount(), fightingIndex + 1));
    }

    private int entryCount(SharedPreferences diary) {
        String raw = diary.getString("entries_json", "[]");
        try { return new JSONArray(raw == null ? "[]" : raw).length(); }
        catch (JSONException error) { return 0; }
    }

    private String activeSport(SharedPreferences profile) {
        String active = profile.getString("active_sport", "").trim();
        ArrayList<String> sports = new ArrayList<>();
        String saved = profile.getString("sports", "").trim();
        if (!saved.isEmpty()) {
            for (String part : saved.split("\\|")) {
                if (!part.trim().isEmpty()) sports.add(part.trim());
            }
        }
        if (sports.isEmpty()) sports.add(profile.getString("sport", "Грэпплинг / No-Gi"));
        if (active.isEmpty() || !sports.contains(active)) active = sports.get(0);
        return active;
    }

    private String entryWord(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod10 == 1 && mod100 != 11) return "запись";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "записи";
        return "записей";
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
        view.setText(value == null ? "" : value);
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
        SpacerDrawable(int size) { super(Color.TRANSPARENT); this.size = size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
