package com.example.personalplanner.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                DatabaseHelper databaseHelper =
                        new DatabaseHelper(context.getApplicationContext());
                ArrayList<StudyPlan> plans = databaseHelper.getPendingReminderPlans();
                for (StudyPlan plan : plans) {
                    boolean scheduled = ReminderScheduler.schedule(
                            context,
                            plan.getPlanId(),
                            plan.getTitle(),
                            plan.getCategoryName(),
                            plan.getDate(),
                            plan.getTime(),
                            plan.getReminderMinutes()
                    );
                    if (scheduled) {
                        long reminderTime = ReminderScheduler.resolveTriggerTimeMillis(
                                plan.getDate(),
                                plan.getTime(),
                                plan.getReminderMinutes()
                        );
                        databaseHelper.upsertReminder(plan.getPlanId(), reminderTime, true);
                    }
                }
            } finally {
                pendingResult.finish();
                executor.shutdown();
            }
        });
    }
}
