package com.valera2872.bjjarm;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * No-gi grappling presentation layer over the original training tracker.
 *
 * The underlying progression logic and stored preferences remain unchanged so
 * users can install this version over 0.1 without losing workout history.
 */
public class GrapplingActivity extends MainActivity {
    private final Map<String, String> exactReplacements = new LinkedHashMap<>();
    private boolean rewritingViews = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        configureReplacements();
        super.onCreate(savedInstanceState);

        View decor = getWindow().getDecorView();
        decor.post(this::applyGrapplingLanguage);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        applyGrapplingLanguage();
                    }
                }
        );
    }

    private void configureReplacements() {
        exactReplacements.put(
                "ДОПОЛНЕНИЕ К ТРЕНИРОВКАМ BJJ",
                "ДОПОЛНЕНИЕ К ГРЭППЛИНГУ / NO-GI"
        );
        exactReplacements.put(
                "Две короткие тренировки в неделю для бицепса, трицепса, предплечий и хвата. Без отказа и без тяжёлых максимальных весов.",
                "Две короткие тренировки в неделю для силы рук в грэпплинге: контроль запястий, андерхуки, рамки и сцепление рук. Без отказа и максимальных весов."
        );
        exactReplacements.put(
                "Тренировок BJJ в неделю",
                "Тренировок по грэпплингу в неделю"
        );
        exactReplacements.put(
                "Тренировка A · сгибания и хват",
                "Тренировка A · сгибания и контроль запястий"
        );
        exactReplacements.put(
                "Гантели. Локти рядом с корпусом, кисть нейтрально. Без раскачки.",
                "Гантели. Локти рядом с корпусом, кисть нейтрально. Без раскачки. Развивает тяговое усилие рук для андерхуков и подтягивания соперника."
        );
        exactReplacements.put(
                "Локти не уходят вперёд. Полное, но мягкое разгибание без удара в сустав.",
                "Локти не уходят вперёд. Полное, но мягкое разгибание без удара в сустав. Трицепс помогает удерживать рамки и создавать дистанцию."
        );
        exactReplacements.put(
                "Стой ровно, плечи опущены, не зажимай шею. Держи хват без боли в пальцах.",
                "Стой ровно, плечи опущены, не зажимай шею. Удерживай гантели без боли в пальцах. Это укрепляет ладонь и предплечье для контроля запястий и сцепления рук."
        );
        exactReplacements.put(
                "Одна лёгкая гантель двумя руками. Не прогибай поясницу, локти направлены вперёд.",
                "Одна лёгкая гантель двумя руками. Не прогибай поясницу, локти направлены вперёд. Укрепляет трицепс для рамок и отталкивания."
        );
        exactReplacements.put(
                "Держи гантель за один край. Медленно поворачивай кисть внутрь и наружу.",
                "Держи гантель за один край. Медленно поворачивай кисть внутрь и наружу. Полезно для контроля кисти и запястья соперника."
        );
        exactReplacements.put(
                "Локти прижаты. Держи гантели без раскачки и без подъёма плеч.",
                "Локти прижаты. Держи гантели без раскачки и без подъёма плеч. Развивает статическую силу согнутых рук для андерхуков и контроля головы."
        );
    }

    private void applyGrapplingLanguage() {
        if (rewritingViews) return;
        rewritingViews = true;
        try {
            rewriteTree(getWindow().getDecorView());
        } finally {
            rewritingViews = false;
        }
    }

    private void rewriteTree(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;

            CharSequence hint = textView.getHint();
            if (hint != null) {
                String rewrittenHint = rewrite(hint.toString());
                if (!rewrittenHint.contentEquals(hint)) {
                    textView.setHint(rewrittenHint);
                }
            }

            // Do not alter user-entered values in editable fields.
            if (!(textView instanceof EditText)) {
                CharSequence text = textView.getText();
                if (text != null) {
                    String rewrittenText = rewrite(text.toString());
                    if (!rewrittenText.contentEquals(text)) {
                        textView.setText(rewrittenText);
                    }
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                rewriteTree(group.getChildAt(i));
            }
        }
    }

    private String rewrite(String source) {
        String exact = exactReplacements.get(source);
        if (exact != null) return exact;

        String result = source;
        result = result.replace("тренировкам BJJ", "тренировкам по грэпплингу");
        result = result.replace("тренировки BJJ", "тренировки по грэпплингу");
        result = result.replace("тренировок BJJ", "тренировок по грэпплингу");
        result = result.replace("тяжёлого BJJ", "тяжёлого грэпплинга");
        result = result.replace("тяжёлый BJJ", "тяжёлый грэпплинг");
        result = result.replace("после BJJ", "после грэпплинга");
        result = result.replace("BJJ", "грэпплинг");
        result = result.replace("бразильским джиу-джитсу", "грэпплингом без кимоно");
        result = result.replace("бразильского джиу-джитсу", "грэпплинга без кимоно");
        return result;
    }
}
