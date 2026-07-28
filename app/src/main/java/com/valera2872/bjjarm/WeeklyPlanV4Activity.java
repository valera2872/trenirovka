package com.valera2872.bjjarm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

/** 0.9.6 readable weekly plan with repaired strength-module routing. */
public class WeeklyPlanV4Activity extends WeeklyPlanV3Activity {
    private boolean repairing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decor = getWindow().getDecorView();
        decor.post(this::repair);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() { repair(); }
                });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) repair();
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component != null) {
                String className = component.getClassName();
                if (GrapplingV4Activity.class.getName().equals(className)
                        || GrapplingV3Activity.class.getName().equals(className)) {
                    intent.setClass(this, GrapplingV5Activity.class);
                } else if (BaseStrengthV2Activity.class.getName().equals(className)
                        || BaseStrengthActivity.class.getName().equals(className)) {
                    intent.setClass(this, BaseStrengthV3Activity.class);
                }
            }
        }
        super.startActivity(intent);
    }

    private void repair() {
        if (repairing) return;
        repairing = true;
        try {
            LegacyLayoutRepair.apply(this, getWindow().getDecorView());
        } finally {
            repairing = false;
        }
    }
}
