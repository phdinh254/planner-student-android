package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanCategoryListActivity;
import com.example.personalplanner.activity.PlanDetailMockupActivity;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanRangeStats;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.StudyStatistics;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    private TextView txtWelcome;
    private TextView txtTodaySummary;
    private TextView txtCompletion;
    private TextView txtHeroDescription;
    private LinearProgressIndicator progressCompletion;
    private TextView txtTotalPlans;
    private TextView txtAssignments;
    private TextView txtClassHours;
    private TextView txtWorkHours;
    private TextView txtWeekStats;
    private TextView txtMonthStats;
    private TextView txtTodaySuggestionBadge;
    private TextView txtEmptySuggestions;
    private TextView txtOverdue;
    private TextView txtEmptyUpcoming;
    private TextView txtInsight1;
    private TextView txtInsight2;
    private TextView txtInsight3;

    private View[] suggestionRows;
    private View[] upcomingRows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        bindViews(view);
        setupActions(view);
        renderDashboard();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (databaseHelper != null) {
            renderDashboard();
        }
    }

    private void bindViews(View view) {
        txtWelcome = view.findViewById(R.id.txtWelcome);
        txtTodaySummary = view.findViewById(R.id.txtTodaySummary);
        txtCompletion = view.findViewById(R.id.txtCompletion);
        txtHeroDescription = view.findViewById(R.id.txtHeroDescription);
        progressCompletion = view.findViewById(R.id.progressCompletion);
        txtTotalPlans = view.findViewById(R.id.txtTotalPlans);
        txtAssignments = view.findViewById(R.id.txtAssignments);
        txtClassHours = view.findViewById(R.id.txtClassHours);
        txtWorkHours = view.findViewById(R.id.txtWorkHours);
        txtWeekStats = view.findViewById(R.id.txtWeekStats);
        txtMonthStats = view.findViewById(R.id.txtMonthStats);
        txtTodaySuggestionBadge = view.findViewById(R.id.txtTodaySuggestionBadge);
        txtEmptySuggestions = view.findViewById(R.id.txtEmptySuggestions);
        txtOverdue = view.findViewById(R.id.txtOverdue);
        txtEmptyUpcoming = view.findViewById(R.id.txtEmptyUpcoming);
        txtInsight1 = view.findViewById(R.id.txtInsight1);
        txtInsight2 = view.findViewById(R.id.txtInsight2);
        txtInsight3 = view.findViewById(R.id.txtInsight3);
        suggestionRows = new View[]{
                view.findViewById(R.id.rowSuggestion1),
                view.findViewById(R.id.rowSuggestion2),
                view.findViewById(R.id.rowSuggestion3)
        };
        upcomingRows = new View[]{
                view.findViewById(R.id.rowPlan1),
                view.findViewById(R.id.rowPlan2),
                view.findViewById(R.id.rowPlan3)
        };
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnMenu).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PlanCategoryListActivity.class)));
        view.findViewById(R.id.btnSearch).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new TaskFragment())
                        .commit());
        MaterialButton btnViewPlans = view.findViewById(R.id.btnViewPlans);
        btnViewPlans.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new TaskFragment())
                        .commit());
    }

    private void renderDashboard() {
        int userId = sessionManager.getUserId();
        String username = sessionManager.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "bạn";
        }

        Calendar now = Calendar.getInstance();
        String today = dateFormat.format(now.getTime());
        Calendar tomorrowCalendar = (Calendar) now.clone();
        tomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrow = dateFormat.format(tomorrowCalendar.getTime());
        DateRange weekRange = getWeekRange(now);
        DateRange monthRange = getMonthRange(now);

        StudyStatistics statistics = databaseHelper.getStudyStatistics(userId);
        ArrayList<StudyPlan> allPlans = databaseHelper.getStudyPlans(
                userId,
                "",
                DatabaseHelper.FILTER_ALL,
                0
        );
        ArrayList<StudyPlan> todaySuggestions = databaseHelper.getTodaySuggestions(userId, today, 3);
        ArrayList<StudyPlan> upcomingPlans = databaseHelper.getUpcomingPlans(userId, tomorrow, 3);
        PlanRangeStats weekStats = databaseHelper.getPlanRangeStats(
                userId,
                weekRange.startDate,
                weekRange.endDate,
                today
        );
        PlanRangeStats monthStats = databaseHelper.getPlanRangeStats(
                userId,
                monthRange.startDate,
                monthRange.endDate,
                today
        );

        int classMinutes = sumMinutesByType(allPlans, StudyPlan.TYPE_CLASS);
        int workMinutes = sumMinutesByType(allPlans, StudyPlan.TYPE_PART_TIME);

        txtWelcome.setText("Chào mừng trở lại, " + username);
        txtTodaySummary.setText("Theo dõi bài tập, buổi học, ca làm thêm và kế hoạch cá nhân trong một nơi.");

        int percent = statistics.getCompletionPercent();
        txtCompletion.setText(percent + "% hoàn thành");
        txtHeroDescription.setText(statistics.getCompletedPlans() + " / "
                + statistics.getTotalPlans() + " kế hoạch đã xong. Còn "
                + statistics.getPendingPlans() + " việc cần theo dõi.");
        progressCompletion.setProgressCompat(percent, true);

        txtTotalPlans.setText(String.valueOf(statistics.getTotalPlans()));
        txtAssignments.setText(String.valueOf(statistics.getAssignmentCount()));
        txtClassHours.setText(formatHours(classMinutes));
        txtWorkHours.setText(formatHours(workMinutes));
        txtWeekStats.setText(formatRangeStats(weekStats));
        txtMonthStats.setText(formatRangeStats(monthStats));
        txtTodaySuggestionBadge.setText(todaySuggestions.size() + " việc hôm nay");
        txtOverdue.setText(statistics.getOverduePlans() + " quá hạn");

        bindPlanRows(todaySuggestions, suggestionRows, txtEmptySuggestions, true);
        bindPlanRows(upcomingPlans, upcomingRows, txtEmptyUpcoming, false);

        txtInsight1.setText("- Hôm nay ưu tiên " + todaySuggestions.size()
                + " kế hoạch, xếp theo mức quan trọng và thời gian gần nhất.");
        txtInsight2.setText("- Tuần này còn " + weekStats.getUnsubmittedAssignments()
                + " bài tập chưa nộp, " + weekStats.getOverduePlans()
                + " kế hoạch quá hạn.");
        txtInsight3.setText("- Tháng này dự kiến " + formatHours(monthStats.getClassMinutes())
                + " giờ học và " + formatHours(monthStats.getWorkMinutes())
                + " giờ làm thêm.");
    }

    private void bindPlanRows(ArrayList<StudyPlan> plans, View[] rows, TextView emptyView,
                              boolean suggestionMode) {
        emptyView.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < rows.length; i++) {
            View row = rows[i];
            if (i >= plans.size()) {
                row.setVisibility(View.GONE);
                continue;
            }

            StudyPlan plan = plans.get(i);
            row.setVisibility(View.VISIBLE);
            TextView title = row.findViewById(R.id.txtPlanTitle);
            TextView meta = row.findViewById(R.id.txtPlanMeta);
            TextView type = row.findViewById(R.id.txtPlanType);
            title.setText(plan.getTitle());
            meta.setText(suggestionMode ? buildSuggestionMeta(plan) : buildPlanMeta(plan));
            type.setText(suggestionMode ? labelForPriority(plan.getPriority()) : labelForType(plan.getPlanType()));
            row.setOnClickListener(v -> openPlanDetail(plan));
        }
    }

    private String buildSuggestionMeta(StudyPlan plan) {
        String category = plan.getCategoryName();
        if (category == null || category.trim().isEmpty()) {
            category = "Chưa phân nhóm";
        }
        return "Hôm nay - " + plan.getTime() + " - " + labelForType(plan.getPlanType())
                + " - " + category;
    }

    private String buildPlanMeta(StudyPlan plan) {
        String category = plan.getCategoryName();
        if (category == null || category.trim().isEmpty()) {
            category = "Chưa phân nhóm";
        }
        return plan.getDate() + " - " + plan.getTime() + " - " + category;
    }

    private int sumMinutesByType(ArrayList<StudyPlan> plans, String type) {
        int minutes = 0;
        for (StudyPlan plan : plans) {
            if (type.equals(plan.getPlanType())
                    && plan.getStatus() != StudyPlan.STATUS_CANCELLED) {
                minutes += plan.getDurationMinutes();
            }
        }
        return minutes;
    }

    private String formatRangeStats(PlanRangeStats stats) {
        return "Giờ học: " + formatHours(stats.getClassMinutes())
                + "\nGiờ làm: " + formatHours(stats.getWorkMinutes())
                + "\nBT chưa nộp: " + stats.getUnsubmittedAssignments()
                + "\nHoàn thành: " + stats.getCompletionPercent() + "%";
    }

    private String formatHours(int minutes) {
        if (minutes <= 0) {
            return "0";
        }
        if (minutes % 60 == 0) {
            return String.valueOf(minutes / 60);
        }
        return String.format(Locale.US, "%.1f", minutes / 60f);
    }

    private String labelForType(String type) {
        if (StudyPlan.TYPE_ASSIGNMENT.equals(type)) {
            return "Bài tập";
        }
        if (StudyPlan.TYPE_CLASS.equals(type)) {
            return "Đi học";
        }
        if (StudyPlan.TYPE_PART_TIME.equals(type)) {
            return "Làm thêm";
        }
        if (StudyPlan.TYPE_EXAM.equals(type)) {
            return "Thi";
        }
        if (StudyPlan.TYPE_PROJECT.equals(type)) {
            return "Dự án";
        }
        return "Cá nhân";
    }

    private String labelForPriority(int priority) {
        if (priority == StudyPlan.PRIORITY_HIGH) {
            return "Ưu tiên cao";
        }
        if (priority == StudyPlan.PRIORITY_LOW) {
            return "Ưu tiên thấp";
        }
        return "Ưu tiên vừa";
    }

    private DateRange getWeekRange(Calendar current) {
        Calendar start = (Calendar) current.clone();
        int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
        int diff = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
        start.add(Calendar.DAY_OF_MONTH, diff);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        return new DateRange(dateFormat.format(start.getTime()), dateFormat.format(end.getTime()));
    }

    private DateRange getMonthRange(Calendar current) {
        Calendar start = (Calendar) current.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = (Calendar) current.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new DateRange(dateFormat.format(start.getTime()), dateFormat.format(end.getTime()));
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

    private static class DateRange {
        final String startDate;
        final String endDate;

        DateRange(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
