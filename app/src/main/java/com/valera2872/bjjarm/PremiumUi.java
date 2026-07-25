package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared visual system: warm background, strong typography and adaptive controls. */
public final class PremiumUi {
    public static final int BG = Color.rgb(244, 241, 235);
    public static final int SURFACE = Color.rgb(255, 253, 249);
    public static final int INK = Color.rgb(26, 33, 31);
    public static final int MUTED = Color.rgb(101, 111, 107);
    public static final int ACCENT = Color.rgb(29, 105, 91);
    public static final int ACCENT_DARK = Color.rgb(15, 62, 56);
    public static final int ACCENT_SOFT = Color.rgb(221, 237, 231);
    public static final int PAPER = Color.rgb(238, 232, 221);
    public static final int BORDER = Color.rgb(225, 219, 208);
    public static final int GOLD = Color.rgb(176, 137, 76);
    public static final int DANGER = Color.rgb(152, 60, 55);

    private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    private static final Typeface DISPLAY = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private PremiumUi() { }

    public static void applyWindow(Activity activity) {
        activity.getWindow().setStatusBarColor(ACCENT_DARK);
        activity.getWindow().setNavigationBarColor(SURFACE);
    }

    public static ScrollView scroll(Context context) {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BG);
        return scroll;
    }

    public static LinearLayout page(Context context) {
        LinearLayout page = vertical(context, 16);
        page.setPadding(dp(context, 18), dp(context, 22), dp(context, 18), dp(context, 34));
        return page;
    }

    public static LinearLayout vertical(Context context, int spacingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        if (spacingDp > 0) layout.setDividerDrawable(new SpacerDrawable(dp(context, spacingDp)));
        return layout;
    }

    public static LinearLayout horizontal(Context context, int spacingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        if (spacingDp > 0) layout.setDividerDrawable(new HorizontalSpacerDrawable(dp(context, spacingDp)));
        return layout;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = vertical(context, 10);
        card.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        card.setBackground(rounded(SURFACE, 22, 1, BORDER));
        card.setElevation(dp(context, 2));
        return card;
    }

    public static LinearLayout softCard(Context context) {
        LinearLayout card = vertical(context, 9);
        card.setPadding(dp(context, 17), dp(context, 16), dp(context, 17), dp(context, 16));
        card.setBackground(rounded(ACCENT_SOFT, 20, 0, Color.TRANSPARENT));
        return card;
    }

    public static LinearLayout hero(Context context) {
        LinearLayout card = vertical(context, 11);
        card.setPadding(dp(context, 21), dp(context, 22), dp(context, 21), dp(context, 22));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{ACCENT_DARK, ACCENT});
        background.setCornerRadius(dp(context, 25));
        card.setBackground(background);
        card.setElevation(dp(context, 4));
        return card;
    }

    public static TextView eyebrow(Context context, String value) {
        TextView view = text(context, value, 12, ACCENT, MEDIUM);
        view.setLetterSpacing(0.12f);
        view.setAllCaps(true);
        return view;
    }

    public static TextView heroEyebrow(Context context, String value) {
        TextView view = text(context, value, 12, Color.rgb(214, 237, 230), MEDIUM);
        view.setLetterSpacing(0.12f);
        view.setAllCaps(true);
        return view;
    }

    public static TextView title(Context context, String value) {
        TextView view = text(context, value, 29, INK, DISPLAY);
        view.setLineSpacing(0, 1.02f);
        return view;
    }

    public static TextView heroTitle(Context context, String value) {
        TextView view = text(context, value, 25, Color.WHITE, BOLD);
        view.setLineSpacing(0, 1.04f);
        return view;
    }

    public static TextView cardTitle(Context context, String value) {
        return text(context, value, 19, INK, BOLD);
    }

    public static TextView sectionTitle(Context context, String value) {
        return text(context, value, 17, INK, BOLD);
    }

    public static TextView body(Context context, String value) {
        TextView view = text(context, value, 15, MUTED, REGULAR);
        view.setLineSpacing(0, 1.18f);
        return view;
    }

    public static TextView bodyDark(Context context, String value) {
        TextView view = text(context, value, 15, INK, REGULAR);
        view.setLineSpacing(0, 1.18f);
        return view;
    }

    public static TextView heroBody(Context context, String value) {
        TextView view = text(context, value, 14, Color.rgb(226, 240, 236), REGULAR);
        view.setLineSpacing(0, 1.18f);
        return view;
    }

    public static TextView small(Context context, String value) {
        TextView view = text(context, value, 13, MUTED, REGULAR);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    public static TextView accentText(Context context, String value) {
        return text(context, value, 14, ACCENT, MEDIUM);
    }

    public static TextView chip(Context context, String value) {
        TextView view = text(context, value, 12, ACCENT_DARK, MEDIUM);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7));
        view.setBackground(rounded(ACCENT_SOFT, 999, 0, Color.TRANSPARENT));
        return view;
    }

    public static TextView numberBadge(Context context, String value) {
        TextView view = text(context, value, 13, Color.WHITE, BOLD);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(context, 34));
        view.setMinHeight(dp(context, 34));
        view.setBackground(rounded(ACCENT, 999, 0, Color.TRANSPARENT));
        return view;
    }

    public static Button primaryButton(Context context, String value) {
        return button(context, value, Color.WHITE, ACCENT, 15, 56, 0, Color.TRANSPARENT);
    }

    public static Button secondaryButton(Context context, String value) {
        Button button = button(context, value, ACCENT_DARK, ACCENT_SOFT, 14, 54, 0, Color.TRANSPARENT);
        if ("RU".equals(value)) {
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_premium, 0, 0, 0);
            button.setCompoundDrawablePadding(dp(context, 4));
            button.setContentDescription("Голосовой ввод на русском языке");
        }
        return button;
    }

    public static Button outlineButton(Context context, String value) {
        return button(context, value, ACCENT_DARK, SURFACE, 14, 54, 1, BORDER);
    }

    public static Button lightButton(Context context, String value) {
        return button(context, value, ACCENT_DARK, Color.WHITE, 15, 54, 0, Color.TRANSPARENT);
    }

    public static Button navButton(Context context, String value, boolean selected) {
        return button(context, value,
                selected ? Color.WHITE : MUTED,
                selected ? ACCENT : SURFACE,
                12, 50, selected ? 0 : 1, selected ? Color.TRANSPARENT : BORDER);
    }

    public static Button dangerButton(Context context, String value) {
        return button(context, value, DANGER, Color.rgb(250, 236, 234), 14, 52, 0, Color.TRANSPARENT);
    }

    private static Button button(Context context, String value, int textColor, int fill,
                                 int textSize, int minHeight, int stroke, int strokeColor) {
        Button button = new Button(context);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSize);
        button.setTypeface(MEDIUM);
        button.setGravity(Gravity.CENTER);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setMinHeight(dp(context, minHeight));
        button.setMinimumHeight(dp(context, minHeight));
        button.setPadding(dp(context, 13), dp(context, 7), dp(context, 13), dp(context, 7));
        button.setAutoSizeTextTypeUniformWithConfiguration(12, textSize, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setBackground(rounded(fill, 16, stroke, strokeColor));
        return button;
    }

    public static EditText input(Context context, String hint, String value, int inputType) {
        EditText field = new EditText(context);
        field.setHint(hint);
        field.setText(value == null ? "" : value);
        field.setTextColor(INK);
        field.setHintTextColor(Color.rgb(132, 141, 137));
        field.setTextSize(15);
        field.setTypeface(REGULAR);
        field.setInputType(inputType);
        field.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        field.setMinHeight(dp(context, 52));
        field.setBackground(rounded(Color.WHITE, 14, 1, BORDER));
        return field;
    }

    public static EditText multiline(Context context, String hint, String value) {
        EditText field = input(context, hint, value,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setMinLines(2);
        field.setMaxLines(7);
        field.setGravity(Gravity.TOP | Gravity.START);
        return field;
    }

    public static TextView text(Context context, String value, int size, int color, Typeface face) {
        TextView view = new TextView(context);
        view.setText(value == null ? "" : value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(face == null ? REGULAR : face);
        view.setIncludeFontPadding(false);
        view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        view.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NORMAL);
        return view;
    }

    public static GradientDrawable rounded(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radiusDp * 3f);
        if (strokeDp > 0) drawable.setStroke(strokeDp * 3, strokeColor);
        return drawable;
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout.LayoutParams fullWidthWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static final class SpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawable(int size) { super(Color.TRANSPARENT); this.size = size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }

    private static final class HorizontalSpacerDrawable extends android.graphics.drawable.ColorDrawable {
        private final int size;
        HorizontalSpacerDrawable(int size) { super(Color.TRANSPARENT); this.size = size; }
        @Override public int getIntrinsicWidth() { return size; }
        @Override public int getIntrinsicHeight() { return 1; }
    }
}
