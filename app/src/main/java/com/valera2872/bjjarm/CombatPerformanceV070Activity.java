package com.valera2872.bjjarm;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * 0.7.0 dashboard layer over the standalone stable screen.
 * Keeps the bilingual technique input from 0.6.7 and adds one isolated entry
 * to the personal fighting-system module.
 */
public class CombatPerformanceV070Activity extends CombatPerformanceStableActivity {
    private static final int REQUEST_ENGLISH_AUDIO = 9701;
    private static final String TAG_GLOSSARY = "technique-glossary-attached";
    private static final String TAG_ENGLISH_BUTTON = "english-technique-mic";
    private static final String TAG_USAGE_NOTE = "technique-usage-note";
    private static final String TAG_FIGHTING_CARD = "fighting-system-card";
    private static final String SYSTEM_PREFS = "combat_fighting_system";

    private SpeechRecognizer englishRecognizer;
    private EditText englishTarget;
    private Button englishButton;
    private EditText pendingEnglishTarget;
    private Button pendingEnglishButton;
    private String englishPrefix = "";
    private boolean englishListening;
    private boolean normalizing;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View root = getWindow().getDecorView();
        root.post(this::applySafeDashboardTweaks);
    }

    @Override
    protected void onPause() {
        stopEnglishVoice();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (englishRecognizer != null) {
            try {
                englishRecognizer.cancel();
                englishRecognizer.destroy();
            } catch (RuntimeException ignored) { }
            englishRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_ENGLISH_AUDIO) return;

        EditText target = pendingEnglishTarget;
        Button button = pendingEnglishButton;
        pendingEnglishTarget = null;
        pendingEnglishButton = null;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (target != null && button != null) startEnglishVoice(target, button);
        } else {
            toastLocal("Без разрешения на микрофон голосовой ввод не работает.");
        }
    }

    private void applySafeDashboardTweaks() {
        View root = getWindow().getDecorView();
        replaceExactText(root, "Что здесь можно делать", "Чем может помочь приложение?");

        EditText favorite = findEditTextByHint(root, "Любимые позиции и техники");
        EditText focus = findEditTextByHint(root, "Какую технику хочешь закрепить за 30 дней?");
        prepareTechniqueField(favorite);
        prepareTechniqueField(focus);
        if (focus != null) addTechniqueUsageNote(focus);

        addFightingSystemCard(root);
    }

    private void addFightingSystemCard(View root) {
        if (findTaggedView(root, TAG_FIGHTING_CARD) != null) return;
        TextView techniqueTitle = findExactText(root, "Техника на 30 дней");
        if (techniqueTitle == null) return;

        ViewParent cardParent = techniqueTitle.getParent();
        if (!(cardParent instanceof View)) return;
        View techniqueCard = (View) cardParent;
        ViewParent pageParent = techniqueCard.getParent();
        if (!(pageParent instanceof LinearLayout)) return;

        LinearLayout page = (LinearLayout) pageParent;
        LinearLayout card = verticalLocal(9);
        card.setTag(TAG_FIGHTING_CARD);
        card.setPadding(dpLocal(18), dpLocal(17), dpLocal(18), dpLocal(17));
        card.setBackground(roundedLocal(Color.WHITE, 18, 1, Color.rgb(222, 230, 227)));
        card.setElevation(dpLocal(1));

        card.addView(labelLocal("Моя борцовская система", 18, Color.rgb(25, 36, 34), true));
        SharedPreferences prefs = getSharedPreferences(SYSTEM_PREFS, MODE_PRIVATE);
        if (prefs.getBoolean("configured", false)) {
            String sport = prefs.getString("sport", "Борьба");
            String compact = compactSystem(prefs);
            card.addView(labelLocal(sport, 12, Color.rgb(28, 104, 91), true));
            if (!compact.isEmpty()) {
                card.addView(labelLocal(compact, 15, Color.rgb(25, 36, 34), false));
            }
            card.addView(labelLocal(
                    "Открой цепочку, чтобы изменить этапы или выбрать новую цель на 30 дней.",
                    13, Color.rgb(94, 111, 107), false));
        } else {
            card.addView(labelLocal(
                    "Свяжи позицию, вход, основное продолжение, запасной вариант и возврат после ошибки.",
                    14, Color.rgb(94, 111, 107), false));
        }

        Button open = styledLocal(prefs.getBoolean("configured", false)
                        ? "Открыть систему" : "Собрать систему",
                Color.WHITE, Color.rgb(28, 104, 91), 15, 52);
        open.setOnClickListener(v -> startActivity(new Intent(this, FightingSystemActivity.class)));
        card.addView(open);

        int index = page.indexOfChild(techniqueCard);
        page.addView(card, Math.min(index + 1, page.getChildCount()));
    }

    private String compactSystem(SharedPreferences prefs) {
        ArrayList<String> parts = new ArrayList<>();
        String start = prefs.getString("start_position", "").trim();
        String entry = prefs.getString("entry", "").trim();
        String main = prefs.getString("main_action", "").trim();
        if (!start.isEmpty()) parts.add(start);
        if (!entry.isEmpty()) parts.add(entry);
        if (!main.isEmpty()) parts.add(main);
        return TextUtils.join("  →  ", parts);
    }

    private void prepareTechniqueField(EditText field) {
        if (field == null) return;

        if (!TAG_GLOSSARY.equals(field.getTag())) {
            field.setTag(TAG_GLOSSARY);
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable editable) {
                    if (normalizing || editable == null) return;
                    String current = editable.toString();
                    String normalized = normalizeTechniqueTerms(current);
                    if (current.equals(normalized)) return;
                    normalizing = true;
                    field.setText(normalized);
                    field.setSelection(normalized.length());
                    normalizing = false;
                }
            });
        }

        ViewParent parent = field.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) parent;
        if (findChildWithTag(row, TAG_ENGLISH_BUTTON) != null) return;

        Button english = new Button(this);
        english.setText("EN");
        english.setAllCaps(false);
        english.setSingleLine(true);
        english.setTextSize(11);
        english.setContentDescription("Голосовой ввод английских названий техник");
        english.setTag(TAG_ENGLISH_BUTTON);
        english.setPadding(dpLocal(4), 0, dpLocal(4), 0);
        english.setOnClickListener(v -> startEnglishVoice(field, english));
        row.addView(english, new LinearLayout.LayoutParams(dpLocal(48), dpLocal(52)));
    }

    private void addTechniqueUsageNote(EditText focus) {
        ViewParent rowParent = focus.getParent();
        if (!(rowParent instanceof View)) return;
        ViewParent cardParent = ((View) rowParent).getParent();
        if (!(cardParent instanceof LinearLayout)) return;

        LinearLayout card = (LinearLayout) cardParent;
        if (findChildWithTag(card, TAG_USAGE_NOTE) != null) return;

        TextView note = new TextView(this);
        note.setTag(TAG_USAGE_NOTE);
        note.setText("Название техники используется как текущая цель на 30 дней и для учёта попыток. Теперь её также можно выбрать из своей рабочей цепочки.");
        note.setTextSize(13);
        note.setTextColor(Color.rgb(94, 111, 107));
        note.setLineSpacing(0, 1.12f);
        note.setPadding(0, dpLocal(6), 0, 0);
        card.addView(note);
    }

    private void startEnglishVoice(EditText target, Button button) {
        if (englishListening) {
            if (englishTarget == target) {
                try {
                    if (englishRecognizer != null) englishRecognizer.stopListening();
                } catch (RuntimeException ignored) { }
                button.setText("…");
                return;
            }
            stopEnglishVoice();
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingEnglishTarget = target;
            pendingEnglishButton = button;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_ENGLISH_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toastLocal("На телефоне не найден сервис распознавания речи.");
            return;
        }

        try {
            if (englishRecognizer == null) {
                englishRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                englishRecognizer.setRecognitionListener(new EnglishRecognitionListener());
            }
            englishTarget = target;
            englishButton = button;
            englishPrefix = target.getText().toString().trim();
            englishListening = true;
            button.setText("●");
            target.requestFocus();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            englishRecognizer.startListening(intent);
        } catch (RuntimeException error) {
            stopEnglishVoice();
            toastLocal("Английский голосовой ввод не запустился. Название можно написать вручную.");
        }
    }

    private void applyEnglishRecognition(Bundle results) {
        if (englishTarget == null || results == null) return;
        ArrayList<String> values = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (values == null || values.isEmpty()) return;
        String spoken = values.get(0).trim();
        if (spoken.isEmpty()) return;
        String combined = englishPrefix.isEmpty() ? spoken : englishPrefix + " " + spoken;
        combined = normalizeTechniqueTerms(combined);
        englishTarget.setText(combined);
        englishTarget.setSelection(combined.length());
    }

    private void stopEnglishVoice() {
        if (englishRecognizer != null && englishListening) {
            try { englishRecognizer.cancel(); } catch (RuntimeException ignored) { }
        }
        englishListening = false;
        if (englishButton != null) englishButton.setText("EN");
        englishTarget = null;
        englishButton = null;
        englishPrefix = "";
    }

    private final class EnglishRecognitionListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) {
            if (englishButton != null) englishButton.setText("●");
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (englishButton != null) englishButton.setText("…");
        }
        @Override public void onError(int error) {
            boolean silent = error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    || error == SpeechRecognizer.ERROR_CLIENT;
            stopEnglishVoice();
            if (!silent) toastLocal("Не удалось распознать английское название. Его можно поправить вручную.");
        }
        @Override public void onResults(Bundle results) {
            applyEnglishRecognition(results);
            stopEnglishVoice();
        }
        @Override public void onPartialResults(Bundle partialResults) {
            applyEnglishRecognition(partialResults);
        }
        @Override public void onEvent(int eventType, Bundle params) { }
    }

    private String normalizeTechniqueTerms(String value) {
        String result = value == null ? "" : value;
        result = replaceTerm(result, "вильямс\\s*гард|вильямсгард", "Williams Guard");
        result = replaceTerm(result, "багги\\s*чок|баггичок|баги\\s*чок|багичок", "Buggy Choke");
        result = replaceTerm(result, "де\\s*ла\\s*рива", "De La Riva");
        result = replaceTerm(result, "икс\\s*гард", "X-Guard");
        result = replaceTerm(result, "сингл\\s*лег\\s*икс", "Single Leg X");
        result = replaceTerm(result, "баттерфляй\\s*гард", "Butterfly Guard");
        result = replaceTerm(result, "спайдер\\s*гард", "Spider Guard");
        result = replaceTerm(result, "аши\\s*гарами", "Ashi Garami");
        result = replaceTerm(result, "беримболо", "Berimbolo");
        result = replaceTerm(result, "дарс", "D'Arce");
        return result;
    }

    private String replaceTerm(String value, String expression, String replacement) {
        return Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])(?:" + expression + ")(?![\\p{L}\\p{N}])")
                .matcher(value)
                .replaceAll(replacement);
    }

    private void replaceExactText(View view, String oldValue, String newValue) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            if (value != null && oldValue.contentEquals(value)) text.setText(newValue);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                replaceExactText(group.getChildAt(i), oldValue, newValue);
            }
        }
    }

    private TextView findExactText(View view, String exact) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            if (value != null && exact.contentEquals(value)) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findExactText(group.getChildAt(i), exact);
                if (found != null) return found;
            }
        }
        return null;
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

    private EditText findEditTextByHint(View view, String hint) {
        if (view instanceof EditText) {
            EditText field = (EditText) view;
            CharSequence value = field.getHint();
            if (value != null && hint.contentEquals(value)) return field;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEditTextByHint(group.getChildAt(i), hint);
                if (found != null) return found;
            }
        }
        return null;
    }

    private View findChildWithTag(ViewGroup group, String tag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (tag.equals(child.getTag())) return child;
        }
        return null;
    }

    private LinearLayout verticalLocal(int spacing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        if (spacing > 0) layout.setDividerDrawable(new SpacerDrawableLocal(dpLocal(spacing)));
        return layout;
    }

    private TextView labelLocal(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.13f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button styledLocal(String value, int textColor, int backgroundColor,
                               int textSize, int height) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setTextSize(textSize);
        button.setAutoSizeTextTypeUniformWithConfiguration(
                9, textSize, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(textColor);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpLocal(8), 0, dpLocal(8), 0);
        button.setMinHeight(dpLocal(height));
        button.setBackground(roundedLocal(backgroundColor, 13, 0, Color.TRANSPARENT));
        return button;
    }

    private GradientDrawable roundedLocal(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dpLocal(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dpLocal(strokeDp), strokeColor);
        return drawable;
    }

    private int dpLocal(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toastLocal(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private static final class SpacerDrawableLocal extends android.graphics.drawable.ColorDrawable {
        private final int size;
        SpacerDrawableLocal(int size) {
            super(Color.TRANSPARENT);
            this.size = size;
        }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public int getIntrinsicWidth() { return size; }
    }
}
