package com.example.personalplanner.data.model;

public class SubTask {
    private final int subTaskId;
    private final int planId;
    private final String title;
    private final boolean completed;
    private final String createdAt;
    private final String updatedAt;

    public SubTask(int subTaskId, int planId, String title, boolean completed,
                   String createdAt, String updatedAt) {
        this.subTaskId = subTaskId;
        this.planId = planId;
        this.title = title;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getSubTaskId() {
        return subTaskId;
    }

    public int getPlanId() {
        return planId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
