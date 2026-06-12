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
                ArrayList<StudyPlan> plans =
                        new DatabaseHelper(context.getApplicationContext())
                                .getPendingReminderPlans();
                for (StudyPlan plan : plans) {
                    ReminderScheduler.schedule(
                            context,
                            plan.getPlanId(),
                            plan.getTitle(),
                            plan.getCourseName(),
                            plan.getDate(),
                            plan.getTime()
                    );
                }
            } finally {
                pendingResult.finish();
                executor.shutdown();
            }
        });
    }
}
