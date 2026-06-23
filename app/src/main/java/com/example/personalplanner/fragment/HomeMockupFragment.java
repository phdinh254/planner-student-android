package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanDetailMockupActivity;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.StudyStatistics;
import com.example.personalplanner.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HomeMockupFragment extends Fragment {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private TextView txtUserName;
    private TextView txtTotalPlans;
    private TextView txtInProgress;
    private TextView txtCompleted;
    private TextView txtOverdue;
    private TextView txtEmptyPlans;
    private View[] planRows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_mockup, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        bindViews(view);
        setupActions(view);
        renderHome();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (databaseHelper != null) {
            renderHome();
        }
    }

    private void bindViews(View view) {
        txtUserName = view.findViewById(R.id.txtUserName);
        txtTotalPlans = view.findViewById(R.id.txtTotalPlans);
        txtInProgress = view.findViewById(R.id.txtInProgress);
        txtCompleted = view.findViewById(R.id.txtCompleted);
        txtOverdue = view.findViewById(R.id.txtOverdue);
        txtEmptyPlans = view.findViewById(R.id.txtEmptyPlans);
        planRows = new View[]{
                view.findViewById(R.id.rowPlanToday1),
                view.findViewById(R.id.rowPlanToday2),
                view.findViewById(R.id.rowPlanToday3)
        };
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnViewAllPlans).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new TaskFragment())
                        .addToBackStack("home_tasks")
                        .commit());
    }

    private void renderHome() {
        int userId = sessionManager.getUserId();
        String username = sessionManager.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "An Nguy\u1ec5n";
        }
        txtUserName.setText(username);

        String today = dateFormat.format(Calendar.getInstance().getTime());
        StudyStatistics statistics = databaseHelper.getStudyStatistics(userId);
        ArrayList<StudyPlan> plans = databaseHelper.getTodaySuggestions(userId, today, 3);
        if (plans.isEmpty()) {
            plans = databaseHelper.getUpcomingPlans(userId, today, 3);
        }

        txtTotalPlans.setText(String.valueOf(statistics.getTotalPlans()));
        txtInProgress.setText(String.valueOf(statistics.getPendingPlans()));
        txtCompleted.setText(String.valueOf(statistics.getCompletedPlans()));
        txtOverdue.setText(String.valueOf(statistics.getOverduePlans()));
        bindPlanRows(plans);
    }

    private void bindPlanRows(ArrayList<StudyPlan> plans) {
        txtEmptyPlans.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < planRows.length; i++) {
            View row = planRows[i];
            if (i >= plans.size()) {
                row.setVisibility(View.GONE);
                continue;
            }
            StudyPlan plan = plans.get(i);
            row.setVisibility(View.VISIBLE);
            TextView title = row.findViewById(R.id.txtPlanTitle);
            TextView meta = row.findViewById(R.id.txtPlanMeta);
            TextView chip = row.findViewById(R.id.txtPlanType);
            title.setText(plan.getTitle());
            meta.setText(buildMeta(plan));
            chip.setText(labelForPriority(plan.getPriority()));
            stylePriorityChip(chip, plan.getPriority());
            row.setOnClickListener(v -> openPlanDetail(plan));
        }
    }

    private void stylePriorityChip(TextView chip, int priority) {
        if (priority == StudyPlan.PRIORITY_HIGH) {
            chip.setBackgroundResource(R.drawable.bg_danger_pill);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
        } else if (priority == StudyPlan.PRIORITY_LOW) {
            chip.setBackgroundResource(R.drawable.bg_stat_primary);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
        } else {
            chip.setBackgroundResource(R.drawable.bg_warning_pill);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        }
    }

    private String buildMeta(StudyPlan plan) {
        String category = plan.getCategoryName();
        if (category == null || category.trim().isEmpty()) {
            category = "Ch\u01b0a ph\u00e2n nh\u00f3m";
        }
        String dayLabel = plan.getDate().equals(dateFormat.format(Calendar.getInstance().getTime()))
                ? "H\u00f4m nay" : plan.getDate();
        return dayLabel + ", " + plan.getTime() + " - " + category;
    }

    private String labelForPriority(int priority) {
        if (priority == StudyPlan.PRIORITY_HIGH) {
            return "Cao";
        }
        if (priority == StudyPlan.PRIORITY_LOW) {
            return "Th\u1ea5p";
        }
        return "Trung b\u00ecnh";
    }

    private void openPlanDetail(StudyPlan plan) {
        Intent intent = new Intent(requireContext(), PlanDetailMockupActivity.class);
        intent.putExtra("plan_id", plan.getPlanId());
        intent.putExtra("title", plan.getTitle());
        intent.putExtra("description", plan.getDescription());
        intent.putExtra("date", plan.getDate());
        intent.putExtra("time", plan.getTime());
        intent.putExtra("status", plan.getStatus());
        intent.putExtra("category_id", plan.getCategoryId());
        intent.putExtra("plan_type", plan.getPlanType());
        intent.putExtra("end_time", plan.getEndTime());
        intent.putExtra("location", plan.getLocation());
        intent.putExtra("room", plan.getRoom());
        intent.putExtra("subject", plan.getSubject());
        intent.putExtra("repeat_rule", plan.getRepeatRule());
        intent.putExtra("repeat_until", plan.getRepeatUntil());
        intent.putExtra("reminder_minutes", plan.getReminderMinutes());
        intent.putExtra("wage", plan.getWage());
        intent.putExtra("submitted", plan.isSubmitted());
        intent.putExtra("priority", plan.getPriority());
        intent.putExtra("duration", plan.getDurationMinutes());
        intent.putExtra("reminder_enabled", plan.isReminderEnabled());
        startActivity(intent);
    }
}
