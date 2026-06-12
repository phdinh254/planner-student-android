package com.example.personalplanner.data.model;

public class StudyStatistics {
    private final int totalPlans;
    private final int completedPlans;
    private final int pendingPlans;
    private final int courseCount;
    private final int plannedMinutes;

    public StudyStatistics(int totalPlans, int completedPlans, int pendingPlans,
                           int courseCount, int plannedMinutes) {
        this.totalPlans = totalPlans;
        this.completedPlans = completedPlans;
        this.pendingPlans = pendingPlans;
        this.courseCount = courseCount;
        this.plannedMinutes = plannedMinutes;
    }

    public int getTotalPlans() {
        return totalPlans;
    }

    public int getCompletedPlans() {
        return completedPlans;
    }

    public int getPendingPlans() {
        return pendingPlans;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public int getCompletionPercent() {
        return totalPlans == 0 ? 0 : Math.round(completedPlans * 100f / totalPlans);
    }
}
