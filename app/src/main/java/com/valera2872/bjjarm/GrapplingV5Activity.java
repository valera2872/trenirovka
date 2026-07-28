package com.valera2872.bjjarm;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

/** 0.9.6 readable version of the arms-and-grip module. */
public class GrapplingV5Activity extends GrapplingV4Activity {
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
