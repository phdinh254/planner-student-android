package com.example.personalplanner.data.model;

public class RepeatRule {
    public static final String TYPE_NONE = "NONE";
    public static final String TYPE_DAILY = "DAILY";
    public static final String TYPE_WEEKLY = "WEEKLY";
    public static final String TYPE_MONTHLY = "MONTHLY";

    private final int repeatRuleId;
    private final int planId;
    private final String repeatType;
    private final String weekDays;
    private final Integer monthDay;
    private final boolean active;
    private final String createdAt;
    private final String updatedAt;

    public RepeatRule(int repeatRuleId, int planId, String repeatType, String weekDays,
                      Integer monthDay, boolean active, String createdAt, String updatedAt) {
        this.repeatRuleId = repeatRuleId;
        this.planId = planId;
        this.repeatType = repeatType;
        this.weekDays = weekDays;
        this.monthDay = monthDay;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getRepeatRuleId() {
        return repeatRuleId;
    }

    public int getPlanId() {
        return planId;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public String getWeekDays() {
        return weekDays;
    }

    public Integer getMonthDay() {
        return monthDay;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
