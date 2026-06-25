package com.example.personalplanner.notification;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReminderScheduler {
    public static final long MINUTE_MILLIS = 60_000L;
    public static final long DAY_MILLIS = 24L * 60L * MINUTE_MILLIS;
    private static final int SNOOZE_REQUEST_OFFSET = 1_000_000;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    static {
        DATE_FORMAT.setLenient(false);
    }

    private ReminderScheduler() {
    }

    public static boolean schedule(Context context, int planId, String title, String categoryName,
                                   String date, String time, int reminderValue) {
        try {
            ReminderType type = ReminderType.fromStoredValue(reminderValue);
            long triggerAt = resolveTriggerTimeMillis(date, time, reminderValue);
            return scheduleExactAt(context, planId, title, categoryName, triggerAt, type);
        } catch (IllegalArgumentException ignored) {
            cancel(context, planId);
            return false;
        }
    }

    public static boolean schedule(Context context, int planId, String title, String categoryName,
                                   String date, String time) {
        return schedule(context, planId, title, categoryName, date, time,
                ReminderType.ON_TIME.getStoredValue());
    }

    public static long resolveTriggerTimeMillis(String date, String time, int reminderValue) {
        ReminderType type = ReminderType.fromStoredValue(reminderValue);
        long selectedDateMillis = parseSelectedDateMillis(date);
        Integer selectedTimeMinutes = type.isAllDay() ? null : parseSelectedTimeMinutes(time);
        long triggerAt = calculateReminderTimeMillis(
                selectedDateMillis,
                selectedTimeMinutes,
                type
        );
        if (type == ReminderType.EVERY_24_HOURS) {
            return nextFuture24HourTime(triggerAt, System.currentTimeMillis());
        }
        return triggerAt;
    }

    public static boolean scheduleNext24Hours(Context context, int planId, String title,
                                              String categoryName, long currentReminderTimeMillis) {
        long nextReminderTimeMillis = resolveNext24HourTriggerTime(currentReminderTimeMillis);
        return scheduleExactAt(
                context,
                planId,
                title,
                categoryName,
                nextReminderTimeMillis,
                ReminderType.EVERY_24_HOURS
        );
    }

    public static boolean scheduleSnooze(Context context, int planId, String title,
                                         String categoryName, int snoozeMinutes) {
        if (snoozeMinutes <= 0) {
            return false;
        }
        long triggerAt = System.currentTimeMillis() + snoozeMinutes * MINUTE_MILLIS;
        return scheduleExactAt(
                context,
                planId,
                title,
                categoryName,
                triggerAt,
                ReminderType.ON_TIME,
                planId + SNOOZE_REQUEST_OFFSET
        );
    }

    public static long resolveNext24HourTriggerTime(long currentReminderTimeMillis) {
        long nextReminderTimeMillis = currentReminderTimeMillis + DAY_MILLIS;
        return nextFuture24HourTime(nextReminderTimeMillis, System.currentTimeMillis());
    }

    public static long calculateReminderTimeMillis(long selectedDateMillis,
                                                   Integer selectedTimeMinutes,
                                                   ReminderType type) {
        if (type == ReminderType.EVERY_24_HOURS) {
            return selectedDateMillis + DAY_MILLIS;
        }
        if (selectedTimeMinutes == null) {
            throw new IllegalArgumentException("Manual reminder requires selected time");
        }
        long baseReminderTimeMillis =
                selectedDateMillis + selectedTimeMinutes * MINUTE_MILLIS;
        return baseReminderTimeMillis + type.getOffsetMillis();
    }

    public static long parseSelectedDateMillis(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing date");
        }
        try {
            Date parsedDate = DATE_FORMAT.parse(date);
            if (parsedDate == null) {
                throw new IllegalArgumentException("Invalid date");
            }
            return parsedDate.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date", e);
        }
    }

    public static int parseSelectedTimeMinutes(String time) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing time");
        }
        String[] parts = time.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time");
        }
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Invalid time");
        }
        return hour * 60 + minute;
    }

    public static boolean hasExactAlarmPermission(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmManager == null
                || alarmManager.canScheduleExactAlarms();
    }

    @SuppressLint("InlinedApi")
    public static Intent createExactAlarmPermissionIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static void cancel(Context context, int planId) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createPendingIntent(
                    context,
                    planId,
                    "",
                    "",
                    ReminderType.ON_TIME,
                    0L,
                    planId
            ));
            alarmManager.cancel(createPendingIntent(
                    context,
                    planId,
                    "",
                    "",
                    ReminderType.ON_TIME,
                    0L,
                    planId + SNOOZE_REQUEST_OFFSET
            ));
        }
    }

    private static boolean scheduleExactAt(Context context, int planId, String title,
                                           String categoryName, long triggerAt,
                                           ReminderType type) {
        return scheduleExactAt(context, planId, title, categoryName, triggerAt, type, planId);
    }

    private static boolean scheduleExactAt(Context context, int planId, String title,
                                           String categoryName, long triggerAt,
                                           ReminderType type, int requestCode) {
        if (triggerAt <= System.currentTimeMillis() || !hasExactAlarmPermission(context)) {
            return false;
        }
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }
        PendingIntent alarmIntent = createPendingIntent(
                context,
                planId,
                title,
                categoryName,
                type,
                triggerAt,
                requestCode
        );
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                alarmIntent
        );
        return true;
    }

    private static long nextFuture24HourTime(long reminderTimeMillis, long nowMillis) {
        long next = reminderTimeMillis;
        while (next <= nowMillis) {
            next += DAY_MILLIS;
        }
        return next;
    }

    private static PendingIntent createPendingIntent(Context context, int planId, String title,
                                                     String courseName, ReminderType type,
                                                     long triggerAt, int requestCode) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_PLAN_ID, planId);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_COURSE, courseName);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type.name());
        intent.putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAt);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
