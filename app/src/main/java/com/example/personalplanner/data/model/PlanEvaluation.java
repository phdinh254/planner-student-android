package com.example.personalplanner.data.model;

public class PlanEvaluation {
    public static final String SATISFACTION_GOOD = "TOT";
    public static final String SATISFACTION_NORMAL = "BINH_THUONG";
    public static final String SATISFACTION_BAD = "CHUA_TOT";

    private final int evaluationId;
    private final int planId;
    private final String satisfactionLevel;
    private final String resultNote;
    private final String delayReason;
    private final String completedAt;
    private final String createdAt;
    private final String updatedAt;

    public PlanEvaluation(int evaluationId, int planId, String satisfactionLevel,
                          String resultNote, String delayReason, String completedAt,
                          String createdAt, String updatedAt) {
        this.evaluationId = evaluationId;
        this.planId = planId;
        this.satisfactionLevel = satisfactionLevel;
        this.resultNote = resultNote;
        this.delayReason = delayReason;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getEvaluationId() {
        return evaluationId;
    }

    public int getPlanId() {
        return planId;
    }

    public String getSatisfactionLevel() {
        return satisfactionLevel;
    }

    public String getResultNote() {
        return resultNote;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
