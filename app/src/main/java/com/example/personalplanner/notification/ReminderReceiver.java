package com.example.personalplanner.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.MainActivity;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_COURSE = "course";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    public static final String EXTRA_TRIGGER_AT = "trigger_at";
    private static final String CHANNEL_ID = "study_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        createChannel(context);
        int planId = intent.getIntExtra(EXTRA_PLAN_ID, 0);
        DatabaseHelper databaseHelper = new DatabaseHelper(context.getApplicationContext());
        StudyPlan plan = databaseHelper.getStudyPlanById(planId);
        if (plan == null
                || !plan.isReminderEnabled()
                || plan.getStatus() == StudyPlan.STATUS_COMPLETED
                || plan.getStatus() == StudyPlan.STATUS_CANCELLED) {
            ReminderScheduler.cancel(context, planId);
            return;
        }

        ReminderType reminderType = ReminderType.fromStoredValue(plan.getReminderMinutes());
        long triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            if (reminderType == ReminderType.EVERY_24_HOURS) {
                long nextReminderTime = ReminderScheduler.resolveNext24HourTriggerTime(triggerAt);
                boolean scheduled = ReminderScheduler.scheduleNext24Hours(
                        context,
                        plan.getPlanId(),
                        plan.getTitle(),
                        plan.getCategoryName(),
                        triggerAt
                );
                if (scheduled) {
                    databaseHelper.upsertReminder(plan.getPlanId(), nextReminderTime, true);
                }
            }
            return;
        }

        String title = plan.getTitle();
        String course = plan.getCategoryName();
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                planId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String content = course == null || course.trim().isEmpty()
                ? context.getString(R.string.reminder_content)
                : context.getString(R.string.reminder_content_course, course);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? context.getString(R.string.app_name) : title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        NotificationManagerCompat.from(context).notify(planId, builder.build());

        if (reminderType == ReminderType.EVERY_24_HOURS) {
            long nextReminderTime = ReminderScheduler.resolveNext24HourTriggerTime(triggerAt);
            boolean scheduled = ReminderScheduler.scheduleNext24Hours(
                    context,
                    plan.getPlanId(),
                    plan.getTitle(),
                    plan.getCategoryName(),
                    triggerAt
            );
            if (scheduled) {
                databaseHelper.upsertReminder(plan.getPlanId(), nextReminderTime, true);
            }
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.reminder_channel_description));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
