package com.valera2872.bjjarm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small 0.6.7 compatibility layer over the stable 0.6.6 screen.
 * It does not replace cards or listen to global layout changes. It only:
 * - corrects one heading;
 * - adds optional English speech recognition to the two technique fields;
 * - normalizes a small glossary of common grappling terms;
 * - explains how the saved technique is currently used.
 */
public class CombatPerformanceV067Activity extends CombatPerformanceStableActivity {
    private static final int REQUEST_ENGLISH_AUDIO = 9701;
    private static final String TAG_GLOSSARY = "technique-glossary-attached";
    private static final String TAG_ENGLISH_BUTTON = "english-technique-mic";
    private static final String TAG_USAGE_NOTE = "technique-usage-note";

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
        root.post(this::applySafeTextAndVoiceTweaks);
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
            } catch (RuntimeException ignored) {
                // Speech services differ between Android vendors.
            }
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

    private void applySafeTextAndVoiceTweaks() {
        View root = getWindow().getDecorView();
        replaceExactText(root, "Что здесь можно делать", "Чем может помочь приложение?");

        EditText favorite = findEditTextByHint(root, "Любимые позиции и техники");
        EditText focus = findEditTextByHint(root, "Какую технику хочешь закрепить за 30 дней?");
        prepareTechniqueField(favorite);
        prepareTechniqueField(focus);

        if (focus != null) addTechniqueUsageNote(focus);
    }

    private void prepareTechniqueField(EditText field) {
        if (field == null) return;

        if (!TAG_GLOSSARY.equals(field.getTag())) {
            field.setTag(TAG_GLOSSARY);
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable editable) {
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
        note.setText("Сейчас название техники используется как личная цель на 30 дней и для учёта попыток, успешных применений и финишей. База техник и AI-разбор пока не подключены.");
        note.setTextSize(13);
        note.setTextColor(android.graphics.Color.rgb(94, 111, 107));
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

    private int dpLocal(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toastLocal(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
