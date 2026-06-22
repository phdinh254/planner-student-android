package com.example.personalplanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.notification.ReminderScheduler;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class PlanDetailMockupActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private int planId;
    private String title;
    private String description;
    private String date;
    private String time;
    private String endTime;
    private String planType;
    private int priority;
    private int status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail_mockup);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        if (!readExtras()) {
            finish();
            return;
        }
        bindContent();
        setupActions();
    }

    private boolean readExtras() {
        planId = getIntent().getIntExtra("plan_id", -1);
        if (planId == -1) {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }
        title = value(getIntent().getStringExtra("title"));
        description = value(getIntent().getStringExtra("description"));
        date = value(getIntent().getStringExtra("date"));
        time = value(getIntent().getStringExtra("time"));
        endTime = valueOrDefault(getIntent().getStringExtra("end_time"), time);
        planType = valueOrDefault(getIntent().getStringExtra("plan_type"), StudyPlan.TYPE_PERSONAL);
        priority = getIntent().getIntExtra("priority", StudyPlan.PRIORITY_MEDIUM);
        status = getIntent().getIntExtra("status", StudyPlan.STATUS_UPCOMING);
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

        int progress = status == StudyPlan.STATUS_COMPLETED ? 100 : 68;
        txtStatus.setText(labelForStatus(status));
        txtTitle.setText(title);
        txtSubtitle.setText(labelForType(planType) + " - H\u1ea1n " + date);
        txtProgress.setText(progress + "%");
        progressDetail.setProgressCompat(progress, false);
        txtPlanLink.setText(title + "\n" + time + " - " + endTime);
        txtNote.setText(description.trim().isEmpty()
                ? "Ch\u01b0a c\u00f3 ghi ch\u00fa cho k\u1ebf ho\u1ea1ch n\u00e0y."
                : description);

        bindInfoRow(R.id.rowStart, "Ng\u00e0y b\u1eaft \u0111\u1ea7u", date);
        bindInfoRow(R.id.rowEnd, "Ng\u00e0y k\u1ebft th\u00fac", date);
        bindInfoRow(R.id.rowPriority, "M\u1ee9c \u01b0u ti\u00ean", labelForPriority(priority));
        bindInfoRow(R.id.rowProgress, "Ti\u1ebfn \u0111\u1ed9", progress + "%");
    }

    private void bindInfoRow(int rowId, String label, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.txtLabel)).setText(label);
        ((TextView) row.findViewById(R.id.txtValue)).setText(value);
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v -> openEditForm());
        findViewById(R.id.btnEditBottom).setOnClickListener(v -> openEditForm());
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnDeleteBottom).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnComplete).setOnClickListener(v -> markCompleted());
    }

    private void openEditForm() {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
    }

    private void markCompleted() {
        boolean updated = databaseHelper.updateStudyPlanStatus(
                planId, sessionManager.getUserId(), StudyPlan.STATUS_COMPLETED);
        if (updated) {
            ReminderScheduler.cancel(this, planId);
            Toast.makeText(this, R.string.completed_status, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.status_update_failed, Toast.LENGTH_SHORT).show();
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

    private String labelForStatus(int value) {
        if (value == StudyPlan.STATUS_COMPLETED) return "\u0110\u00e3 ho\u00e0n th\u00e0nh";
        if (value == StudyPlan.STATUS_IN_PROGRESS) return "\u0110ang di\u1ec5n ra";
        if (value == StudyPlan.STATUS_CANCELLED) return "\u0110\u00e3 h\u1ee7y";
        return "\u0110ang th\u1ef1c hi\u1ec7n";
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

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String valueOrDefault(String input, String fallback) {
        return input == null || input.trim().isEmpty() ? fallback : input;
    }
}
