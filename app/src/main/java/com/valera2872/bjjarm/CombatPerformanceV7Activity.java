package com.valera2872.bjjarm;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Version 0.6 dashboard: one clear task for today and access to My Week. */
public class CombatPerformanceV7Activity extends CombatPerformanceV6Activity {
    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private boolean installingWeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::installWeekExperience);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        installWeekExperience();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::installWeekExperience);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) installWeekExperience();
    }

    private void installWeekExperience() {
        if (installingWeek) return;
        installingWeek = true;
        try {
            View root = getWindow().getDecorView();
            TextView todayLabel = findText(root, "СЕГОДНЯ");
            if (todayLabel == null || !(todayLabel.getParent() instanceof LinearLayout)) return;
            adaptTodayHero((LinearLayout) todayLabel.getParent());
            addWeekCard(root);
        } finally {
            installingWeek = false;
        }
    }

    private void adaptTodayHero(LinearLayout hero) {
        WeekPlanEngine.Task task = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        TextView title = hero.getChildCount() > 1 && hero.getChildAt(1) instanceof TextView
                ? (TextView) hero.getChildAt(1) : null;
        TextView details = hero.getChildCount() > 2 && hero.getChildAt(2) instanceof TextView
                ? (TextView) hero.getChildAt(2) : null;
        Button action = findFirstButton(hero);

        if (title != null) title.setText(task.title);
        if (details != null) details.setText(task.details);
        if (action != null) {
            action.setText(task.actionLabel);
            action.setTag("weekly-today-action");
            action.setOnClickListener(v -> runTodayAction(task));
        }
    }

    private void runTodayAction(WeekPlanEngine.Task task) {
        if ("arms".equals(task.kind)) {
            startActivity(new Intent(this, GrapplingV3Activity.class));
        } else if ("base".equals(task.kind)) {
            startActivity(new Intent(this, BaseStrengthV2Activity.class));
        } else {
            startActivity(new Intent(this, WeeklyPlanActivity.class));
        }
    }

    private void addWeekCard(View root) {
        boolean configured = WeekPlanEngine.isConfigured(this);
        int[] strengthDays = configured ? WeekPlanEngine.chooseStrengthDays(this) : new int[0];
        WeekPlanEngine.Task today = WeekPlanEngine.taskForDay(this, WeekPlanEngine.todayIndex());
        String state = configured + "|" + strengthDays.length + "|" + today.title;

        View existing = root.findViewWithTag("my-week-card");
        if (existing != null && state.contentEquals(existing.getContentDescription())) return;
        if (existing != null && existing.getParent() instanceof ViewGroup) {
            ((ViewGroup) existing.getParent()).removeView(existing);
        }

        TextView physicalTitle = findText(root, "Твой физический профиль");
        if (physicalTitle == null || !(physicalTitle.getParent() instanceof LinearLayout)) return;
        View physicalCard = (View) physicalTitle.getParent();
        if (!(physicalCard.getParent() instanceof LinearLayout)) return;
        LinearLayout page = (LinearLayout) physicalCard.getParent();

        LinearLayout card = vertical(7);
        card.setTag("my-week-card");
        card.setContentDescription(state);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(Color.WHITE, 18, 1, Color.rgb(226, 231, 229)));
        card.setElevation(dp(1));
        card.addView(label("Моя неделя", 18, TEXT, true));

        if (configured) {
            card.addView(label(
                    "План составлен: " + strengthDays.length + " силовых занятий. Сегодня — «" + today.title + "».",
                    14, MUTED, false));
        } else {
            card.addView(label(
                    "Укажи дни ковра и тяжёлые тренировки. Приложение само распределит силовую работу и техническую миссию.",
                    14, MUTED, false));
        }

        Button open = baseButton(configured ? "Открыть мою неделю" : "Настроить мою неделю");
        open.setOnClickListener(v -> startActivity(new Intent(this, WeeklyPlanActivity.class)));
        card.addView(open);

        int index = page.indexOfChild(physicalCard);
        page.addView(card, Math.max(0, index));
    }

    private TextView findText(View view, String exact) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (exact.equals(value)) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findText(group.getChildAt(i), exact);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Button findFirstButton(View view) {
        if (view instanceof Button) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findFirstButton(group.getChildAt(i));
                if (result != null) return result;
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
        view.setLineSpacing(0, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button baseButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(PRIMARY);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setMinHeight(dp(48));
        button.setBackground(rounded(PRIMARY_SOFT, 13, 0, Color.TRANSPARENT));
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

    private static class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawable(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}