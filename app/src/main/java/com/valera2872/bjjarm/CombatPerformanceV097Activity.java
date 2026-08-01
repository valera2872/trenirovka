package com.valera2872.bjjarm;

import android.content.ComponentName;
import android.content.Intent;

/** 0.9.7 launcher: keeps 0.9.6 UI and routes all diary links to the editable diary. */
public class CombatPerformanceV097Activity extends CombatPerformanceV096Activity {
    @Override
    public void startActivity(Intent intent) {
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
