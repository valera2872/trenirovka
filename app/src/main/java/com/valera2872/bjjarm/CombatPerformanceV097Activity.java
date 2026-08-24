package com.valera2872.bjjarm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Legacy 0.9.7 component kept for update compatibility.
 * Some OEM launchers keep the old ComponentName after an in-place update,
 * so any direct launch of V097 must hand off to the current premium launcher.
 */
public class CombatPerformanceV097Activity extends CombatPerformanceV096Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent premium = new Intent(this, CombatPerformanceV100Activity.class);
        premium.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(premium);
        finish();
    }

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
