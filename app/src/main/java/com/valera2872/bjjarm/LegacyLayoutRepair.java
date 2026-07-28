package com.valera2872.bjjarm;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Repairs old dynamic screens without changing their workout or persistence logic. */
public final class LegacyLayoutRepair {
    private LegacyLayoutRepair() { }

    public static void apply(Activity activity, View root) {
        normalizeButtons(activity, root);
        replaceLegacyNavigation(activity, root);
        shortenLegacyBackLabels(root);
    }

    public static void normalizeButtons(Activity activity, View view) {
        if (view instanceof Button) {
            Button button = (Button) view;
            button.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
            button.setAllCaps(false);
            button.setSingleLine(false);
            button.setMaxLines(3);
            button.setEllipsize(null);
            button.setHorizontallyScrolling(false);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setPadding(dp(activity, 10), dp(activity, 9), dp(activity, 10), dp(activity, 9));
            button.setIncludeFontPadding(false);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                normalizeButtons(activity, group.getChildAt(i));
            }
        }
    }

    private static void replaceLegacyNavigation(Activity activity, View view) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
            replaceLegacyNavigation(activity, group.getChildAt(i));
        }
        if (!(view instanceof LinearLayout)) return;
        LinearLayout bar = (LinearLayout) view;
        if ("v096-legacy-nav".equals(bar.getTag())) return;

        List<String> expected = Arrays.asList("Сегодня", "План", "Прогресс", "Профиль");
        ArrayList<Button> originals = directButtons(bar);
        if (originals.size() != 4) return;
        ArrayList<String> labels = new ArrayList<>();
        for (Button button : originals) labels.add(button.getText().toString());
        if (!labels.equals(expected)) return;

        bar.setTag("v096-legacy-nav");
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        bar.setPadding(dp(activity, 5), dp(activity, 4), dp(activity, 5), dp(activity, 5));
        bar.removeAllViews();
        for (int i = 0; i < originals.size(); i++) {
            Button original = originals.get(i);
            String label = expected.get(i);
            boolean selected = original.getTypeface() != null && original.getTypeface().isBold();
            View tile = createTile(activity, label, icon(label), selected, original::performClick);
            bar.addView(tile, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
    }

    private static View createTile(Activity activity, String label, int iconRes,
                                   boolean selected, Runnable action) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(activity, 1), dp(activity, 3), dp(activity, 1), dp(activity, 2));
        tile.setBackground(PremiumUi.rounded(
                selected ? PremiumUi.ACCENT_SOFT : android.graphics.Color.TRANSPARENT,
                13, 0, android.graphics.Color.TRANSPARENT));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(activity, 21), dp(activity, 21)));

        TextView text = PremiumUi.text(activity, label, 10, PremiumUi.ACCENT_DARK,
                Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(false);
        text.setMaxLines(2);
        text.setEllipsize(null);
        text.setHorizontallyScrolling(false);
        text.setIncludeFontPadding(false);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = dp(activity, 2);
        tile.addView(text, textParams);
        return tile;
    }

    private static void shortenLegacyBackLabels(View view) {
        if (view instanceof Button) {
            Button button = (Button) view;
            String value = button.getText() == null ? "" : button.getText().toString();
            if ("← Combat Performance".equals(value)) button.setText("← На главный экран");
            else if ("← К обзору модуля".equals(value)) button.setText("← К обзору");
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) shortenLegacyBackLabels(group.getChildAt(i));
        }
    }

    private static ArrayList<Button> directButtons(LinearLayout parent) {
        ArrayList<Button> result = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof Button) result.add((Button) parent.getChildAt(i));
        }
        return result;
    }

    private static int icon(String label) {
        if ("План".equals(label)) return R.drawable.ic_nav_week;
        if ("Прогресс".equals(label)) return R.drawable.ic_nav_progress;
        if ("Профиль".equals(label)) return R.drawable.ic_nav_profile;
        return R.drawable.ic_nav_today;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
