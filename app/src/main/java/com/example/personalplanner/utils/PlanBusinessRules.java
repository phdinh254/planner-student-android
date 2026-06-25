package com.example.personalplanner.utils;

import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.SubTask;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public final class PlanBusinessRules {
    public static final String DISPLAY_NOT_STARTED = "CHUA_BAT_DAU";
    public static final String DISPLAY_IN_PROGRESS = "DANG_THUC_HIEN";
    public static final String DISPLAY_COMPLETED = "HOAN_THANH";
    public static final String DISPLAY_OVERDUE = "QUA_HAN";
    public static final String DISPLAY_CANCELLED = "DA_HUY";

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private PlanBusinessRules() {
    }

    public static String getDisplayStatus(StudyPlan plan, long nowMillis) {
        if (plan == null) {
            return DISPLAY_NOT_STARTED;
        }
        if (plan.getStatus() == StudyPlan.STATUS_COMPLETED) {
            return DISPLAY_COMPLETED;
        }
        if (plan.getStatus() == StudyPlan.STATUS_CANCELLED) {
            return DISPLAY_CANCELLED;
        }
        if (isOverdue(plan, nowMillis)) {
            return DISPLAY_OVERDUE;
        }
        if (plan.getStatus() == StudyPlan.STATUS_IN_PROGRESS) {
            return DISPLAY_IN_PROGRESS;
        }
        return DISPLAY_NOT_STARTED;
    }

    public static String getDisplayStatusLabel(String displayStatus) {
        return resolveDisplayStatusLabelSafe(displayStatus);
    }

    private static String resolveDisplayStatusLabelSafe(String displayStatus) {
        if (DISPLAY_COMPLETED.equals(displayStatus)) {
            return "Ho\u00e0n th\u00e0nh";
        }
        if (DISPLAY_OVERDUE.equals(displayStatus)) {
            return "Qu\u00e1 h\u1ea1n";
        }
        if (DISPLAY_IN_PROGRESS.equals(displayStatus)) {
            return "\u0110ang th\u1ef1c hi\u1ec7n";
        }
        if (DISPLAY_CANCELLED.equals(displayStatus)) {
            return "\u0110\u00e3 h\u1ee7y";
        }
        return "Ch\u01b0a b\u1eaft \u0111\u1ea7u";
    }

    private static String legacyDisplayStatusLabel(String displayStatus) {
        if (DISPLAY_COMPLETED.equals(displayStatus)) {
            return "Hoàn thành";
        }
        if (DISPLAY_OVERDUE.equals(displayStatus)) {
            return "Quá hạn";
        }
        if (DISPLAY_IN_PROGRESS.equals(displayStatus)) {
            return "Đang thực hiện";
        }
        if (DISPLAY_CANCELLED.equals(displayStatus)) {
            return "Đã hủy";
        }
        return "Chưa bắt đầu";
    }

    private static String resolveDisplayStatusLabel(String displayStatus) {
        if (DISPLAY_COMPLETED.equals(displayStatus)) {
            return "Hoàn thành";
        }
        if (DISPLAY_OVERDUE.equals(displayStatus)) {
            return "Quá hạn";
        }
        if (DISPLAY_IN_PROGRESS.equals(displayStatus)) {
            return "Đang thực hiện";
        }
        if (DISPLAY_CANCELLED.equals(displayStatus)) {
            return "Đã hủy";
        }
        return "Chưa bắt đầu";
    }

    public static boolean isOverdue(StudyPlan plan, long nowMillis) {
        if (plan == null
                || plan.getStatus() == StudyPlan.STATUS_COMPLETED
                || plan.getStatus() == StudyPlan.STATUS_CANCELLED) {
            return false;
        }
        long deadlineMillis = parseDeadlineMillis(plan);
        return deadlineMillis > 0 && deadlineMillis < nowMillis;
    }

    public static int calculateProgress(StudyPlan plan, List<SubTask> subTasks) {
        if (plan == null) {
            return 0;
        }
        if (plan.getStatus() == StudyPlan.STATUS_COMPLETED) {
            return 100;
        }
        if (subTasks != null && !subTasks.isEmpty()) {
            int completed = 0;
            for (SubTask subTask : subTasks) {
                if (subTask.isCompleted()) {
                    completed++;
                }
            }
            return Math.round(completed * 100f / subTasks.size());
        }
        if (plan.getStatus() == StudyPlan.STATUS_IN_PROGRESS) {
            return 50;
        }
        return 0;
    }

    private static long parseDeadlineMillis(StudyPlan plan) {
        String date = plan.getDate();
        String time = plan.getEndTime() == null || plan.getEndTime().trim().isEmpty()
                ? plan.getTime()
                : plan.getEndTime();
        if (date == null || date.trim().isEmpty()) {
            return -1;
        }
        try {
            return DATE_TIME_FORMAT.parse(date.trim() + " " + normalizeTime(time)).getTime();
        } catch (Exception ignored) {
            try {
                return DATE_FORMAT.parse(date.trim()).getTime();
            } catch (Exception ignoredAgain) {
                return -1;
            }
        }
    }

    private static String normalizeTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return "23:59";
        }
        return time.trim();
    }
}
