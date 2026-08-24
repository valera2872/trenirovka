package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class DarkUi {
    public static final int BG = Color.rgb(7, 17, 29);
    public static final int BG_2 = Color.rgb(10, 24, 39);
    public static final int CARD = Color.rgb(15, 31, 48);
    public static final int CARD_2 = Color.rgb(19, 38, 58);
    public static final int BORDER = Color.rgb(35, 56, 75);
    public static final int TEXT = Color.rgb(245, 247, 249);
    public static final int MUTED = Color.rgb(158, 170, 184);
    public static final int GOLD = Color.rgb(242, 183, 78);
    public static final int GOLD_DARK = Color.rgb(177, 118, 25);
    public static final int GREEN = Color.rgb(57, 201, 119);
    public static final int ORANGE = Color.rgb(244, 116, 53);
    public static final int PURPLE = Color.rgb(164, 110, 215);

    private static final Typeface REG = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    private static final Typeface DISPLAY = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private DarkUi() { }

    public static void apply(Activity a) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
    }

    public static ScrollView scroll(Context c) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setBackgroundColor(BG);
        return s;
    }

    public static LinearLayout page(Context c) {
        LinearLayout l = v(c, 14);
        l.setPadding(dp(c, 16), dp(c, 18), dp(c, 16), dp(c, 96));
        return l;
    }

    public static LinearLayout v(Context c, int gap) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        if (gap > 0) {
            l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            l.setDividerDrawable(new VSpace(dp(c, gap)));
        }
        return l;
    }

    public static LinearLayout h(Context c, int gap) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        if (gap > 0) {
            l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            l.setDividerDrawable(new HSpace(dp(c, gap)));
        }
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = v(c, 9);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        l.setBackground(round(CARD, 18, 1, BORDER));
        return l;
    }

    public static LinearLayout hero(Context c) {
        LinearLayout l = v(c, 10);
        l.setPadding(dp(c, 18), dp(c, 20), dp(c, 18), dp(c, 18));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(18, 38, 57), Color.rgb(11, 25, 40)});
        g.setCornerRadius(dp(c, 22));
        g.setStroke(dp(c, 1), Color.rgb(47, 68, 85));
        l.setBackground(g);
        return l;
    }

    public static LinearLayout goldCard(Context c) {
        LinearLayout l = v(c, 10);
        l.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(37, 43, 44), Color.rgb(26, 35, 42)});
        g.setCornerRadius(dp(c, 20));
        g.setStroke(dp(c, 1), Color.rgb(91, 73, 42));
        l.setBackground(g);
        return l;
    }

    public static TextView title(Context c, String s) { return text(c, s, 26, TEXT, DISPLAY); }
    public static TextView h1(Context c, String s) { return text(c, s, 21, TEXT, BOLD); }
    public static TextView h2(Context c, String s) { return text(c, s, 17, TEXT, BOLD); }
    public static TextView body(Context c, String s) { TextView v=text(c,s,14,MUTED,REG); v.setLineSpacing(0,1.15f); return v; }
    public static TextView bodyWhite(Context c, String s) { TextView v=text(c,s,14,TEXT,REG); v.setLineSpacing(0,1.15f); return v; }
    public static TextView small(Context c, String s) { return text(c, s, 12, MUTED, REG); }
    public static TextView gold(Context c, String s) { return text(c, s, 12, GOLD, MED); }
    public static TextView number(Context c, String s) { return text(c, s, 26, TEXT, DISPLAY); }

    public static TextView text(Context c, String s, int sp, int color, Typeface face) {
        TextView t = new TextView(c);
        t.setText(s == null ? "" : s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setIncludeFontPadding(false);
        t.setEllipsize(null);
        t.setSingleLine(false);
        return t;
    }

    public static TextView chip(Context c, String s, int color) {
        TextView t = text(c, s, 11, color, MED);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6));
        t.setBackground(round(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), 999, 1,
                Color.argb(75, Color.red(color), Color.green(color), Color.blue(color))));
        return t;
    }

    public static Button primary(Context c, String s) {
        Button b = baseButton(c, s, Color.rgb(16, 20, 24), GOLD);
        b.setTypeface(BOLD);
        return b;
    }

    public static Button secondary(Context c, String s) {
        return baseButton(c, s, TEXT, CARD_2);
    }

    public static Button outline(Context c, String s) {
        Button b = baseButton(c, s, TEXT, BG_2);
        b.setBackground(round(BG_2, 14, 1, BORDER));
        return b;
    }

    private static Button baseButton(Context c, String s, int text, int fill) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(text);
        b.setTextSize(14);
        b.setTypeface(MED);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(false);
        b.setMaxLines(3);
        b.setEllipsize(null);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(c, 52));
        b.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        b.setBackground(round(fill, 14, 0, Color.TRANSPARENT));
        return b;
    }

    public static View navItem(Context c, int iconRes, String label, boolean selected, View.OnClickListener click) {
        LinearLayout item = v(c, 4);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(c, 2), dp(c, 9), dp(c, 2), dp(c, 8));
        item.setOnClickListener(click);
        item.setClickable(true);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setColorFilter(selected ? GOLD : MUTED);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        item.addView(icon, new LinearLayout.LayoutParams(dp(c, 21), dp(c, 21)));
        TextView t = text(c, label, 10, selected ? GOLD : MUTED, selected ? MED : REG);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        item.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return item;
    }

    public static LinearLayout stat(Context c, String value, String label, int accent) {
        LinearLayout l = v(c, 4);
        l.setPadding(dp(c, 13), dp(c, 13), dp(c, 13), dp(c, 13));
        l.setBackground(round(CARD_2, 16, 1, BORDER));
        TextView marker = text(c, "●", 12, accent, BOLD);
        l.addView(marker);
        l.addView(number(c, value));
        l.addView(small(c, label));
        return l;
    }

    public static GradientDrawable round(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusDp * 3f);
        if (strokeDp > 0) g.setStroke(strokeDp * 3, strokeColor);
        return g;
    }

    public static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    private static final class VSpace extends android.graphics.drawable.ColorDrawable {
        private final int size; VSpace(int size){ super(Color.TRANSPARENT); this.size=size; }
        @Override public int getIntrinsicHeight(){ return size; }
    }
    private static final class HSpace extends android.graphics.drawable.ColorDrawable {
        private final int size; HSpace(int size){ super(Color.TRANSPARENT); this.size=size; }
        @Override public int getIntrinsicWidth(){ return size; }
    }
}
