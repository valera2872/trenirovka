package com.valera2872.bjjarm;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 0.9.6 UI stabilization layer.
 * Replaces narrow Android buttons with real navigation tiles and turns exact
 * English technique entry into local search instead of unreliable EN speech.
 */
public class CombatPerformanceV096Activity extends CombatPerformanceV093Activity {
    private boolean applyingFixes;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View decor = getWindow() == null ? null : getWindow().getDecorView();
        if (decor != null) decor.post(this::applyFixes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decor = getWindow().getDecorView();
        decor.post(this::applyFixes);
    }

    @Override
    public void startActivity(Intent intent) {
        redirectLegacyScreen(intent);
        super.startActivity(intent);
    }

    private void redirectLegacyScreen(Intent intent) {
        if (intent == null) return;
        ComponentName component = intent.getComponent();
        if (component == null) return;
        String className = component.getClassName();
        if (GrapplingV4Activity.class.getName().equals(className)
                || GrapplingV3Activity.class.getName().equals(className)) {
            intent.setClass(this, GrapplingV5Activity.class);
        } else if (BaseStrengthV2Activity.class.getName().equals(className)
                || BaseStrengthActivity.class.getName().equals(className)) {
            intent.setClass(this, BaseStrengthV3Activity.class);
        } else if (WeeklyPlanV3Activity.class.getName().equals(className)
                || WeeklyPlanV2Activity.class.getName().equals(className)
                || WeeklyPlanActivity.class.getName().equals(className)) {
            intent.setClass(this, WeeklyPlanV4Activity.class);
        }
    }

    private void applyFixes() {
        if (applyingFixes) return;
        applyingFixes = true;
        try {
            View root = getWindow().getDecorView();
            normalizeButtons(root);
            rewriteProfileHelp(root);
            replaceTechniqueControls(root);
            replaceDashboardNavigation(root);
            replaceStrengthChoices(root);
        } finally {
            applyingFixes = false;
        }
    }

    private void normalizeButtons(View view) {
        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
            button.setEllipsize(null);
            button.setHorizontallyScrolling(false);
            button.setSingleLine(false);
            button.setMaxLines(3);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) normalizeButtons(group.getChildAt(i));
        }
    }

    private void rewriteProfileHelp(View view) {
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            String value = text.getText() == null ? "" : text.getText().toString();
            if (value.startsWith("Каждое нажатие запускает отдельное системное распознавание")) {
                text.setText("Обычные слова можно надиктовать по-русски. Для точного названия приёма используй поиск по русскому произношению или английскому написанию.");
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) rewriteProfileHelp(group.getChildAt(i));
        }
    }

    private void replaceTechniqueControls(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            View child = group.getChildAt(i);
            replaceTechniqueControls(child);
        }

        if (!(view instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) view;
        if ("v096-technique-row".equals(row.getTag())) return;

        EditText field = directEditText(row);
        LinearLayout controls = directControls(row);
        if (field == null || controls == null) return;
        Button ru = findDirectButton(controls, "RU");
        Button en = findDirectButton(controls, "EN");
        if (ru == null || en == null) return;

        boolean replaceExisting = field.getHint() != null
                && field.getHint().toString().contains("30 дней");
        row.setTag("v096-technique-row");
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.FILL_HORIZONTAL);
        row.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        row.removeAllViews();

        field.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(field);

        ru.setText("Голосом по-русски");
        ru.setSingleLine(false);
        ru.setMaxLines(2);
        ru.setMinWidth(0);
        ru.setMinimumWidth(0);
        ru.setLayoutParams(buttonParams(8));
        row.addView(ru);

        en.setText("Найти технику");
        en.setCompoundDrawables(null, null, null, null);
        en.setSingleLine(false);
        en.setMaxLines(2);
        en.setMinWidth(0);
        en.setMinimumWidth(0);
        en.setOnClickListener(v -> TechniqueCatalog.show(this, field, replaceExisting));
        en.setLayoutParams(buttonParams(8));
        row.addView(en);
    }

    private void replaceDashboardNavigation(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            replaceDashboardNavigation(group.getChildAt(i));
        }
        if (!(view instanceof LinearLayout)) return;
        LinearLayout bar = (LinearLayout) view;
        if ("v096-main-nav".equals(bar.getTag())) return;

        List<String> expected = Arrays.asList("Сегодня", "Неделя", "Дневник", "Профиль");
        ArrayList<Button> originals = directButtons(bar);
        if (originals.size() != 4) return;
        ArrayList<String> labels = new ArrayList<>();
        for (Button button : originals) labels.add(button.getText().toString());
        if (!labels.equals(expected)) return;

        bar.setTag("v096-main-nav");
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        bar.removeAllViews();
        for (int i = 0; i < originals.size(); i++) {
            Button original = originals.get(i);
            String label = expected.get(i);
            View tile = navTile(label, navIcon(label), i == 0, original::performClick);
            bar.addView(tile, new LinearLayout.LayoutParams(
                    0, PremiumUi.dp(this, 70), 1));
        }
    }

    private void replaceStrengthChoices(View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) replaceStrengthChoices(group.getChildAt(i));
        if (!(view instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) view;
        if ("v096-strength-choices".equals(parent.getTag())) return;

        Button arms = findDirectButton(parent, "Руки и хват");
        Button base = findDirectButton(parent, "Ноги и корпус");
        if (arms == null || base == null) return;

        parent.setTag("v096-strength-choices");
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        parent.removeAllViews();
        parent.addView(strengthTile(
                "Руки и хват",
                "Хват, предплечья, тяговые движения и устойчивый контроль руками.",
                arms::performClick));
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, PremiumUi.dp(this, 10)));
        parent.addView(strengthTile(
                "Ноги и корпус",
                "Сила ног, устойчивость, баланс и контроль корпуса в борьбе.",
                base::performClick));
    }

    private View navTile(String label, int iconRes, boolean selected, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(PremiumUi.dp(this, 2), PremiumUi.dp(this, 7),
                PremiumUi.dp(this, 2), PremiumUi.dp(this, 5));
        tile.setBackground(PremiumUi.rounded(
                selected ? PremiumUi.ACCENT_SOFT : PremiumUi.SURFACE,
                15, selected ? 0 : 1,
                selected ? android.graphics.Color.TRANSPARENT : PremiumUi.BORDER));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tile.addView(icon, new LinearLayout.LayoutParams(
                PremiumUi.dp(this, 23), PremiumUi.dp(this, 23)));

        TextView text = PremiumUi.text(this, label, 11, PremiumUi.ACCENT_DARK,
                Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(false);
        text.setMaxLines(2);
        text.setEllipsize(null);
        text.setHorizontallyScrolling(false);
        text.setIncludeFontPadding(false);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = PremiumUi.dp(this, 3);
        tile.addView(text, textParams);
        return tile;
    }

    private View strengthTile(String title, String description, Runnable action) {
        LinearLayout tile = PremiumUi.softCard(this);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> action.run());
        LinearLayout head = PremiumUi.horizontal(this, 8);
        TextView titleView = PremiumUi.cardTitle(this, title);
        titleView.setMaxLines(3);
        titleView.setEllipsize(null);
        head.addView(titleView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = PremiumUi.cardTitle(this, "›");
        arrow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        head.addView(arrow, new LinearLayout.LayoutParams(
                PremiumUi.dp(this, 28), ViewGroup.LayoutParams.WRAP_CONTENT));
        tile.addView(head);
        tile.addView(PremiumUi.body(this, description));
        return tile;
    }

    private LinearLayout.LayoutParams buttonParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = PremiumUi.dp(this, topMarginDp);
        return params;
    }

    private EditText directEditText(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            if (row.getChildAt(i) instanceof EditText) return (EditText) row.getChildAt(i);
        }
        return null;
    }

    private LinearLayout directControls(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout candidate = (LinearLayout) child;
            if (findDirectButton(candidate, "RU") != null && findDirectButton(candidate, "EN") != null) {
                return candidate;
            }
        }
        return null;
    }

    private Button findDirectButton(LinearLayout parent, String text) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof Button) {
                CharSequence value = ((Button) child).getText();
                if (value != null && text.equals(value.toString())) return (Button) child;
            }
        }
        return null;
    }

    private ArrayList<Button> directButtons(LinearLayout parent) {
        ArrayList<Button> result = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof Button) result.add((Button) parent.getChildAt(i));
        }
        return result;
    }

    private int navIcon(String label) {
        if ("Неделя".equals(label)) return R.drawable.ic_nav_week;
        if ("Дневник".equals(label)) return R.drawable.ic_nav_diary;
        if ("Профиль".equals(label)) return R.drawable.ic_nav_profile;
        return R.drawable.ic_nav_today;
    }
}
