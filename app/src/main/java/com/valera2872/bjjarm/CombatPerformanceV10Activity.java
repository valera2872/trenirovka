package com.valera2872.bjjarm;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Version 0.6.3 removes generic motivational copy and replaces it with a
 * personal pre-bout routine written by the athlete together with a coach or parent.
 */
public class CombatPerformanceV10Activity extends CombatPerformanceV9Activity {
    private static final String ROUTINE_PREFS = "combat_personal_routine";
    private static final String PROFILE_PREFS = "combat_performance_profile";

    private static final int TEXT = Color.rgb(25, 36, 34);
    private static final int MUTED = Color.rgb(94, 111, 107);
    private static final int PRIMARY = Color.rgb(28, 104, 91);
    private static final int PRIMARY_SOFT = Color.rgb(226, 240, 235);
    private static final int BORDER = Color.rgb(222, 230, 227);

    private SharedPreferences routine;
    private SharedPreferences athleteProfile;
    private boolean updating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routine = getSharedPreferences(ROUTINE_PREFS, MODE_PRIVATE);
        athleteProfile = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        View decor = getWindow().getDecorView();
        decor.post(this::replaceGenericStateBlock);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        replaceGenericStateBlock();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::replaceGenericStateBlock);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) replaceGenericStateBlock();
    }

    private void replaceGenericStateBlock() {
        if (updating) return;
        updating = true;
        try {
            View root = getWindow().getDecorView();
            if (findTaggedView(root, "personal-routine-card") != null) return;

            TextView oldTitle = findExactText(root, "Состояние перед борьбой");
            if (oldTitle == null) return;
            ViewParent cardParent = oldTitle.getParent();
            if (!(cardParent instanceof View)) return;
            View oldCard = (View) cardParent;
            ViewParent pageParent = oldCard.getParent();
            if (!(pageParent instanceof ViewGroup)) return;

            ViewGroup page = (ViewGroup) pageParent;
            int index = page.indexOfChild(oldCard);
            ViewGroup.LayoutParams params = oldCard.getLayoutParams();
            page.removeView(oldCard);

            LinearLayout personalCard = buildPersonalRoutineCard();
            personalCard.setTag("personal-routine-card");
            if (params != null) page.addView(personalCard, index, params);
            else page.addView(personalCard, index);
        } finally {
            updating = false;
        }
    }

    private LinearLayout buildPersonalRoutineCard() {
        LinearLayout card = vertical(9);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(Color.WHITE, 18, 1, BORDER));
        card.setElevation(dp(1));

        card.addView(label("Моя настройка перед выходом", 18, TEXT, true));
        card.addView(label(
                "Здесь нет готовых лозунгов. Сохрани то, что действительно помогает именно тебе, лучше вместе с тренером или родителем.",
                14, MUTED, false));

        if (isRoutineConfigured()) {
            String first = routine.getString("first_action", "").trim();
            String prepare = routine.getString("prepare", "").trim();
            if (!first.isEmpty()) {
                card.addView(summaryRow("Первое действие", first));
            } else if (!prepare.isEmpty()) {
                card.addView(summaryRow("Перед выходом", prepare));
            }

            Button open = primaryButton("Открыть мой план");
            open.setOnClickListener(v -> showPersonalRoutine());
            card.addView(open);

            Button edit = secondaryButton("Изменить настройку");
            edit.setOnClickListener(v -> showRoutineEditor());
            card.addView(edit);
        } else {
            Button setup = primaryButton("Настроить мой план");
            setup.setOnClickListener(v -> showRoutineEditor());
            card.addView(setup);
        }
        return card;
    }

    private boolean isRoutineConfigured() {
        return routine.getBoolean("configured", false)
                && (!routine.getString("prepare", "").trim().isEmpty()
                || !routine.getString("first_action", "").trim().isEmpty()
                || !routine.getString("after_error", "").trim().isEmpty());
    }

    private void showRoutineEditor() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = vertical(8);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        scroll.addView(form);

        form.addView(label(
                "Запиши только то, что уже помогает на тренировках или было согласовано с тренером.",
                14, MUTED, false));

        EditText problem = input(
                "Что обычно мешает перед выходом?",
                routine.getString("problem", ""));
        EditText prepare = input(
                "Что помогает собраться? Музыка, слова тренера, действие",
                routine.getString("prepare", ""));
        EditText firstAction = input(
                "Первое действие в схватке",
                routine.getString("first_action", ""));
        EditText afterError = input(
                "Что делать сразу после ошибки?",
                routine.getString("after_error", ""));

        form.addView(problem);
        form.addView(prepare);
        form.addView(firstAction);
        form.addView(afterError);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Личная настройка")
                .setView(scroll)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String prepareText = prepare.getText().toString().trim();
                    String firstText = firstAction.getText().toString().trim();
                    String errorText = afterError.getText().toString().trim();
                    if (prepareText.isEmpty() && firstText.isEmpty() && errorText.isEmpty()) {
                        Toast.makeText(this,
                                "Запиши хотя бы одно действие, которое действительно помогает.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    routine.edit()
                            .putBoolean("configured", true)
                            .putString("problem", problem.getText().toString().trim())
                            .putString("prepare", prepareText)
                            .putString("first_action", firstText)
                            .putString("after_error", errorText)
                            .apply();
                    dialog.dismiss();
                    recreate();
                }));
        dialog.show();
    }

    private void showPersonalRoutine() {
        if (!isRoutineConfigured()) {
            showRoutineEditor();
            return;
        }

        LinearLayout content = vertical(8);
        content.setPadding(dp(18), dp(6), dp(18), dp(6));

        addPlanItem(content, "Что обычно мешает", routine.getString("problem", ""));
        addPlanItem(content, "Перед выходом", routine.getString("prepare", ""));
        addPlanItem(content, "Первое действие", routine.getString("first_action", ""));
        addPlanItem(content, "После ошибки", routine.getString("after_error", ""));
        addPlanItem(content, "Техника, которую закрепляешь",
                athleteProfile.getString("mission", ""));

        new AlertDialog.Builder(this)
                .setTitle("Твой план перед выходом")
                .setView(content)
                .setNegativeButton("Изменить", (dialog, which) -> showRoutineEditor())
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void addPlanItem(LinearLayout parent, String title, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return;
        parent.addView(summaryRow(title, clean));
    }

    private LinearLayout summaryRow(String title, String value) {
        LinearLayout row = vertical(2);
        row.setPadding(0, dp(6), 0, dp(6));
        row.addView(label(title, 12, PRIMARY, true));
        row.addView(label(value, 15, TEXT, false));
        return row;
    }

    private EditText input(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setMinLines(2);
        field.setMaxLines(4);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setTextSize(15);
        field.setTextColor(TEXT);
        field.setHintTextColor(Color.rgb(132, 147, 143));
        field.setPadding(dp(13), dp(11), dp(13), dp(11));
        field.setBackground(rounded(Color.rgb(248, 250, 249), 12, 1, BORDER));
        return field;
    }

    private TextView findExactText(View view, String exact) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (exact.equals(value)) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findExactText(group.getChildAt(i), exact);
                if (result != null) return result;
            }
        }
        return null;
    }

    private View findTaggedView(View view, String tag) {
        if (tag.equals(view.getTag())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = findTaggedView(group.getChildAt(i), tag);
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
        view.setLineSpacing(0, 1.13f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button primaryButton(String value) {
        return styledButton(value, Color.WHITE, PRIMARY, 15, 52);
    }

    private Button secondaryButton(String value) {
        return styledButton(value, PRIMARY, PRIMARY_SOFT, 14, 48);
    }

    private Button styledButton(String value, int textColor, int backgroundColor,
                                int textSize, int height) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setTextSize(textSize);
        button.setAutoSizeTextTypeUniformWithConfiguration(
                10, textSize, 1, TypedValue.COMPLEX_UNIT_SP);
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
