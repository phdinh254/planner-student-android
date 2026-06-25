package com.example.personalplanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanEvaluation;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.SubTask;
import com.example.personalplanner.notification.ReminderScheduler;
import com.example.personalplanner.utils.PlanBusinessRules;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PlanDetailMockupActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int planId;
    private StudyPlan currentPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail_mockup);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        if (!readPlanId() || !loadPlanFromDatabase()) {
            finish();
            return;
        }
        setupActions();
        bindContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (databaseHelper == null || planId <= 0 || currentPlan == null) {
            return;
        }
        if (!loadPlanFromDatabase()) {
            finish();
            return;
        }
        bindContent();
    }

    private boolean readPlanId() {
        planId = getIntent().getIntExtra("plan_id", -1);
        if (planId == -1) {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean loadPlanFromDatabase() {
        StudyPlan plan = databaseHelper.getStudyPlanById(planId);
        if (plan == null || plan.getUserId() != sessionManager.getUserId()) {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }
        currentPlan = plan;
        return true;
    }

    private void bindContent() {
        TextView txtStatus = findViewById(R.id.txtStatus);
        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtSubtitle = findViewById(R.id.txtSubtitle);
        TextView txtProgress = findViewById(R.id.txtProgress);
        TextView txtPlanLink = findViewById(R.id.txtPlanLink);
        TextView txtNote = findViewById(R.id.txtNote);
        LinearProgressIndicator progressDetail = findViewById(R.id.progressDetail);

        ArrayList<SubTask> subTasks = databaseHelper.getSubTasks(planId);
        PlanEvaluation evaluation = databaseHelper.getPlanEvaluation(planId);
        int progress = PlanBusinessRules.calculateProgress(currentPlan, subTasks);
        String displayStatus = PlanBusinessRules.getDisplayStatus(
                currentPlan, System.currentTimeMillis());

        txtStatus.setText(labelForDisplayStatus(displayStatus));
        txtTitle.setText(value(currentPlan.getTitle()));
        txtSubtitle.setText(labelForType(currentPlan.getPlanType())
                + " - H\u1ea1n " + value(currentPlan.getDate()));
        txtProgress.setText(progress + "%");
        progressDetail.setProgressCompat(progress, false);
        txtPlanLink.setText(value(currentPlan.getTitle()) + "\n"
                + value(currentPlan.getTime()) + " - "
                + valueOrDefault(currentPlan.getEndTime(), currentPlan.getTime()));
        txtNote.setText(value(currentPlan.getDescription()).trim().isEmpty()
                ? "Ch\u01b0a c\u00f3 ghi ch\u00fa cho k\u1ebf ho\u1ea1ch n\u00e0y."
                : currentPlan.getDescription());

        bindInfoRow(R.id.rowStart, "Ng\u00e0y b\u1eaft \u0111\u1ea7u",
                value(currentPlan.getDate()) + " " + value(currentPlan.getTime()));
        bindInfoRow(R.id.rowEnd, "Ng\u00e0y k\u1ebft th\u00fac",
                value(currentPlan.getDate()) + " "
                        + valueOrDefault(currentPlan.getEndTime(), currentPlan.getTime()));
        bindInfoRow(R.id.rowPriority, "M\u1ee9c \u01b0u ti\u00ean",
                labelForPriority(currentPlan.getPriority()));
        bindInfoRow(R.id.rowProgress, "Ti\u1ebfn \u0111\u1ed9", progress + "%");
        bindSubTasks(subTasks);
        bindEvaluation(evaluation);

        View btnComplete = findViewById(R.id.btnComplete);
        btnComplete.setVisibility(currentPlan.getStatus() == StudyPlan.STATUS_COMPLETED
                || currentPlan.getStatus() == StudyPlan.STATUS_CANCELLED
                ? View.GONE : View.VISIBLE);

        View btnAddSubTask = findViewById(R.id.btnAddSubTask);
        btnAddSubTask.setVisibility(currentPlan.getStatus() == StudyPlan.STATUS_COMPLETED
                || currentPlan.getStatus() == StudyPlan.STATUS_CANCELLED
                ? View.GONE : View.VISIBLE);
    }

    private void bindInfoRow(int rowId, String label, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.txtLabel)).setText(label);
        ((TextView) row.findViewById(R.id.txtValue)).setText(value);
    }

    private void setupActions() {
        findViewById(R.id.btnAddSubTask).setOnClickListener(v -> showAddSubTaskDialog());
        findViewById(R.id.btnEditBottom).setOnClickListener(v -> openEditForm());
        findViewById(R.id.btnDeleteBottom).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnComplete).setOnClickListener(v -> markCompleted());
    }

    private void openEditForm() {
        if (currentPlan == null) {
            return;
        }
        Intent intent = new Intent(this, TaskDetailActivity.class);
        putPlanExtras(intent, currentPlan);
        startActivity(intent);
    }

    private void markCompleted() {
        if (currentPlan == null || currentPlan.getStatus() == StudyPlan.STATUS_COMPLETED) {
            return;
        }
        boolean wasOverdue = PlanBusinessRules.isOverdue(currentPlan, System.currentTimeMillis());
        if (completePlan()) {
            showCompletionEvaluationDialog(wasOverdue);
        }
    }

    private void showCompletionEvaluationDialog(boolean wasOverdue) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, 8, padding, 0);

        Spinner spinnerSatisfaction = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"T\u1ed1t", "B\u00ecnh th\u01b0\u1eddng", "Ch\u01b0a t\u1ed1t"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSatisfaction.setAdapter(adapter);
        layout.addView(spinnerSatisfaction);

        EditText edtResultNote = new EditText(this);
        edtResultNote.setHint("Ghi ch\u00fa k\u1ebft qu\u1ea3 / r\u00fat kinh nghi\u1ec7m");
        edtResultNote.setMinLines(2);
        layout.addView(edtResultNote);

        EditText edtDelayReason = new EditText(this);
        edtDelayReason.setHint("L\u00fd do tr\u1ec5 h\u1ea1n n\u1ebfu c\u00f3");
        edtDelayReason.setMinLines(2);
        edtDelayReason.setVisibility(wasOverdue ? View.VISIBLE : View.GONE);
        layout.addView(edtDelayReason);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("\u0110\u00e1nh gi\u00e1 sau ho\u00e0n th\u00e0nh")
                .setView(layout)
                .setPositiveButton("L\u01b0u \u0111\u00e1nh gi\u00e1", null)
                .setNegativeButton("B\u1ecf qua", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                saveCompletionEvaluation(
                        satisfactionValue(spinnerSatisfaction.getSelectedItemPosition()),
                        edtResultNote.getText().toString(),
                        edtDelayReason.getText().toString());
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setOnClickListener(v -> dialog.dismiss());
        });
        dialog.show();
    }

    private boolean completePlan() {
        boolean updated = databaseHelper.updateStudyPlanStatus(
                planId, sessionManager.getUserId(), StudyPlan.STATUS_COMPLETED);
        if (updated) {
            ReminderScheduler.cancel(this, planId);
            NotificationManagerCompat.from(this).cancel(planId);
            Toast.makeText(this, R.string.completed_status, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            if (loadPlanFromDatabase()) {
                bindContent();
            }
            return true;
        } else {
            Toast.makeText(this, R.string.status_update_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void saveCompletionEvaluation(String satisfactionLevel, String resultNote,
                                          String delayReason) {
        long result = databaseHelper.savePlanEvaluation(planId, satisfactionLevel, resultNote,
                delayReason, completedAtNow());
        if (result == -1) {
            Toast.makeText(this, R.string.status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (loadPlanFromDatabase()) {
            bindContent();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePlan())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePlan() {
        boolean deleted = databaseHelper.deleteStudyPlan(planId, sessionManager.getUserId());
        if (deleted) {
            ReminderScheduler.cancel(this, planId);
            Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.task_delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddSubTaskDialog() {
        if (currentPlan == null
                || currentPlan.getStatus() == StudyPlan.STATUS_COMPLETED
                || currentPlan.getStatus() == StudyPlan.STATUS_CANCELLED) {
            return;
        }
        EditText input = new EditText(this);
        input.setHint(getString(R.string.sub_task_title_hint));
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, 8, padding, 8);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_sub_task)
                .setView(input)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        input.setError(getString(R.string.error_sub_task_required));
                        input.requestFocus();
                        return;
                    }
                    long result = databaseHelper.addSubTask(planId, title);
                    if (result == -1) {
                        Toast.makeText(this, R.string.sub_task_add_failed,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, R.string.sub_task_added, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    bindContent();
                }));
        dialog.show();
    }

    private void bindSubTasks(ArrayList<SubTask> subTasks) {
        LinearLayout container = findViewById(R.id.layoutSubTasks);
        container.removeAllViews();
        if (subTasks == null || subTasks.isEmpty()) {
            TextView empty = supportingText(
                    "Ch\u01b0a c\u00f3 c\u00f4ng vi\u1ec7c nh\u1ecf cho k\u1ebf ho\u1ea1ch n\u00e0y.");
            container.addView(empty);
            return;
        }

        for (SubTask subTask : subTasks) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(subTask.getTitle());
            checkBox.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            checkBox.setTextSize(14);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary)));
            checkBox.setChecked(subTask.isCompleted());
            checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
                if (!buttonView.isPressed()) {
                    return;
                }
                boolean updated = databaseHelper.updateSubTaskCompleted(
                        subTask.getSubTaskId(), checked);
                if (updated) {
                    bindContent();
                } else {
                    buttonView.setChecked(!checked);
                    Toast.makeText(this, R.string.status_update_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
            container.addView(checkBox);
        }
    }

    private void bindEvaluation(PlanEvaluation evaluation) {
        View section = findViewById(R.id.layoutEvaluationSection);
        TextView txtEvaluation = findViewById(R.id.txtEvaluation);
        if (currentPlan.getStatus() != StudyPlan.STATUS_COMPLETED) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);
        if (evaluation == null) {
            txtEvaluation.setText("K\u1ebf ho\u1ea1ch \u0111\u00e3 ho\u00e0n th\u00e0nh, "
                    + "ch\u01b0a c\u00f3 ghi ch\u00fa \u0111\u00e1nh gi\u00e1.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("M\u1ee9c \u0111\u00e1nh gi\u00e1: ")
                .append(labelForSatisfaction(evaluation.getSatisfactionLevel()));
        builder.append("\nNg\u00e0y ho\u00e0n th\u00e0nh: ")
                .append(value(evaluation.getCompletedAt()));
        if (!value(evaluation.getResultNote()).trim().isEmpty()) {
            builder.append("\nGhi ch\u00fa sau ho\u00e0n th\u00e0nh: ")
                    .append(evaluation.getResultNote());
        }
        if (!value(evaluation.getDelayReason()).trim().isEmpty()) {
            builder.append("\nL\u00fd do tr\u1ec5: ").append(evaluation.getDelayReason());
        }
        txtEvaluation.setText(builder.toString());
    }

    private TextView supportingText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        textView.setTextSize(14);
        textView.setPadding(4, 8, 4, 8);
        return textView;
    }

    private void putPlanExtras(Intent intent, StudyPlan plan) {
        intent.putExtra("plan_id", plan.getPlanId());
        intent.putExtra("title", plan.getTitle());
        intent.putExtra("description", plan.getDescription());
        intent.putExtra("date", plan.getDate());
        intent.putExtra("time", plan.getTime());
        intent.putExtra("end_time", plan.getEndTime());
        intent.putExtra("category_id", plan.getCategoryId());
        intent.putExtra("plan_type", plan.getPlanType());
        intent.putExtra("priority", plan.getPriority());
        intent.putExtra("status", plan.getStatus());
        intent.putExtra("duration", plan.getDurationMinutes());
        intent.putExtra("reminder_enabled", plan.isReminderEnabled());
        intent.putExtra("reminder_minutes", plan.getReminderMinutes());
        intent.putExtra("location", plan.getLocation());
        intent.putExtra("room", plan.getRoom());
        intent.putExtra("subject", plan.getSubject());
        intent.putExtra("repeat_rule", plan.getRepeatRule());
        intent.putExtra("repeat_until", plan.getRepeatUntil());
        intent.putExtra("wage", plan.getWage());
        intent.putExtra("submitted", plan.isSubmitted());
    }

    private String labelForDisplayStatus(String value) {
        if (PlanBusinessRules.DISPLAY_COMPLETED.equals(value)) {
            return "\u0110\u00e3 ho\u00e0n th\u00e0nh";
        }
        if (PlanBusinessRules.DISPLAY_OVERDUE.equals(value)) {
            return "Qu\u00e1 h\u1ea1n";
        }
        if (PlanBusinessRules.DISPLAY_IN_PROGRESS.equals(value)) {
            return "\u0110ang th\u1ef1c hi\u1ec7n";
        }
        if (PlanBusinessRules.DISPLAY_CANCELLED.equals(value)) {
            return "\u0110\u00e3 h\u1ee7y";
        }
        return "Ch\u01b0a b\u1eaft \u0111\u1ea7u";
    }

    private String labelForPriority(int value) {
        if (value == StudyPlan.PRIORITY_HIGH) return "Cao";
        if (value == StudyPlan.PRIORITY_LOW) return "Th\u1ea5p";
        return "Trung b\u00ecnh";
    }

    private String labelForType(String type) {
        if (StudyPlan.TYPE_ASSIGNMENT.equals(type)) return "B\u00e0i t\u1eadp";
        if (StudyPlan.TYPE_CLASS.equals(type)) return "\u0110i h\u1ecdc";
        if (StudyPlan.TYPE_PART_TIME.equals(type)) return "L\u00e0m th\u00eam";
        if (StudyPlan.TYPE_EXAM.equals(type)) return "Thi";
        if (StudyPlan.TYPE_PROJECT.equals(type)) return "D\u1ef1 \u00e1n";
        return "C\u00e1 nh\u00e2n";
    }

    private String labelForSatisfaction(String value) {
        if (PlanEvaluation.SATISFACTION_GOOD.equals(value)) {
            return "T\u1ed1t";
        }
        if (PlanEvaluation.SATISFACTION_BAD.equals(value)) {
            return "Ch\u01b0a t\u1ed1t";
        }
        return "B\u00ecnh th\u01b0\u1eddng";
    }

    private String satisfactionValue(int position) {
        if (position == 0) {
            return PlanEvaluation.SATISFACTION_GOOD;
        }
        if (position == 2) {
            return PlanEvaluation.SATISFACTION_BAD;
        }
        return PlanEvaluation.SATISFACTION_NORMAL;
    }

    private String completedAtNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    }

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String valueOrDefault(String input, String fallback) {
        return input == null || input.trim().isEmpty() ? fallback : input;
    }
}
