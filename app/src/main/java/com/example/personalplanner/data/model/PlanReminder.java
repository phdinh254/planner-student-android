package com.example.personalplanner.data.model;

public class PlanReminder {
    private final int reminderId;
    private final int planId;
    private final long reminderTime;
    private final boolean enabled;
    private final String createdAt;
    private final String updatedAt;

    public PlanReminder(int reminderId, int planId, long reminderTime, boolean enabled,
                        String createdAt, String updatedAt) {
        this.reminderId = reminderId;
        this.planId = planId;
        this.reminderTime = reminderTime;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getReminderId() {
        return reminderId;
    }

    public int getPlanId() {
        return planId;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
