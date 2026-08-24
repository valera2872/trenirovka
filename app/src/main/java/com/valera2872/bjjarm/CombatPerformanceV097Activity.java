package com.valera2872.bjjarm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/** Legacy 0.9.7 component retained only for update compatibility. */
public class CombatPerformanceV097Activity extends CombatPerformanceV096Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent premium = new Intent(this, CombatPerformanceV101Activity.class);
        premium.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(premium);
        finish();
    }

    @Override public void startActivity(Intent intent) {
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component != null) {
                String name = component.getClassName();
                if (PremiumTrainingDiaryActivity.class.getName().equals(name)
                        || TrainingDiaryActivity.class.getName().equals(name)) {
                    intent.setClass(this, PremiumTrainingDiaryV2Activity.class);
                }
            }
        }
        super.startActivity(intent);
    }
}
