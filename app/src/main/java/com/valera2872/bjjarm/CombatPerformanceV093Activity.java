package com.valera2872.bjjarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Legacy 0.9.3 entry retained for compatibility only.
 * Any stale internal route must return to the current premium 0.10.1 experience
 * instead of rendering the old light dashboard.
 */
public class CombatPerformanceV093Activity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent current = new Intent(this, CombatPerformanceV101Activity.class);
        current.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(current);
        finish();
    }
}
