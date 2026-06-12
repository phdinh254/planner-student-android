package com.example.personalplanner.data.model;

public class StudyPlan {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    private final int planId;
    private final String title;
    private final String description;
    private final String date;
    private final String time;
    private int status;
    private final int courseId;
    private final String courseName;
    private final int priority;
    private final int durationMinutes;
    private final boolean reminderEnabled;
    private final int userId;

    public StudyPlan(int planId, String title, String description, String date, String time,
                     int status, int courseId, String courseName, int priority,
                     int durationMinutes, boolean reminderEnabled, int userId) {
        this.planId = planId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.status = status;
        this.courseId = courseId;
        this.courseName = courseName;
        this.priority = priority;
        this.durationMinutes = durationMinutes;
        this.reminderEnabled = reminderEnabled;
        this.userId = userId;
    }

    public int getPlanId() {
        return planId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getPriority() {
        return priority;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public int getUserId() {
        return userId;
    }
}
