package com.example.personalplanner.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanRangeStats;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.StudyStatistics;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverviewFragment extends Fragment {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;

    private TextView txtOverviewTotal;
    private TextView txtOverviewPending;
    private TextView txtOverviewCompleted;
    private TextView txtOverviewOverdue;
    private TextView txtOverviewCompletion;
    private TextView txtOverviewRange;
    private TextView txtPriorityHigh;
    private TextView txtPriorityMedium;
    private TextView txtPriorityLow;
    private TextView txtDueSoon;
    private TextView txtTodayPlan;
    private CircularProgressIndicator progressOverviewCompletion;
    private LinearProgressIndicator progressPriorityHigh;
    private LinearProgressIndicator progressPriorityMedium;
    private LinearProgressIndicator progressPriorityLow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        bindViews(view);
        loadOverview();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadOverview();
        }
    }

    private void bindViews(View view) {
        txtOverviewTotal = view.findViewById(R.id.txtOverviewTotal);
        txtOverviewPending = view.findViewById(R.id.txtOverviewPending);
        txtOverviewCompleted = view.findViewById(R.id.txtOverviewCompleted);
        txtOverviewOverdue = view.findViewById(R.id.txtOverviewOverdue);
        txtOverviewCompletion = view.findViewById(R.id.txtOverviewCompletion);
        txtOverviewRange = view.findViewById(R.id.txtOverviewRange);
        txtPriorityHigh = view.findViewById(R.id.txtPriorityHigh);
        txtPriorityMedium = view.findViewById(R.id.txtPriorityMedium);
        txtPriorityLow = view.findViewById(R.id.txtPriorityLow);
        txtDueSoon = view.findViewById(R.id.txtDueSoon);
        txtTodayPlan = view.findViewById(R.id.txtTodayPlan);
        progressOverviewCompletion = view.findViewById(R.id.progressOverviewCompletion);
        progressPriorityHigh = view.findViewById(R.id.progressPriorityHigh);
        progressPriorityMedium = view.findViewById(R.id.progressPriorityMedium);
        progressPriorityLow = view.findViewById(R.id.progressPriorityLow);
    }

    private void loadOverview() {
        int userId = sessionManager.getUserId();
        Calendar now = Calendar.getInstance();
        String today = dateFormat.format(now.getTime());
        DateRange weekRange = getWeekRange(now);
        executorService.execute(() -> {
            StudyStatistics stats = databaseHelper.getStudyStatistics(userId);
            PlanRangeStats weekStats = databaseHelper.getPlanRangeStats(
                    userId, weekRange.startDate, weekRange.endDate, today);
            ArrayList<StudyPlan> plans = databaseHelper.getStudyPlans(
                    userId, "", DatabaseHelper.FILTER_ALL, 0);
            ArrayList<StudyPlan> dueSoon = databaseHelper.getUpcomingPlans(userId, today, 1);
            ArrayList<StudyPlan> todayPlans = databaseHelper.getTodaySuggestions(userId, today, 1);

            int high = countPriority(plans, StudyPlan.PRIORITY_HIGH);
            int medium = countPriority(plans, StudyPlan.PRIORITY_MEDIUM);
            int low = countPriority(plans, StudyPlan.PRIORITY_LOW);

            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                render(stats, weekStats, high, medium, low, dueSoon, todayPlans);
            });
        });
    }

    private void render(StudyStatistics stats, PlanRangeStats weekStats, int high, int medium,
                        int low, ArrayList<StudyPlan> dueSoon,
                        ArrayList<StudyPlan> todayPlans) {
        int total = Math.max(stats.getTotalPlans(), 0);
        int percent = stats.getCompletionPercent();
        txtOverviewTotal.setText(String.valueOf(total));
        txtOverviewPending.setText(String.valueOf(stats.getPendingPlans()));
        txtOverviewCompleted.setText(String.valueOf(stats.getCompletedPlans()));
        txtOverviewOverdue.setText(String.valueOf(stats.getOverduePlans()));
        txtOverviewCompletion.setText(percent + "% hoan thanh chung");
        txtOverviewRange.setText("Tuan nay: " + weekStats.getCompletedPlans() + "/"
                + weekStats.getTotalPlans() + " viec xong, "
                + formatHours(weekStats.getClassMinutes()) + " gio hoc, "
                + formatHours(weekStats.getWorkMinutes()) + " gio lam them.");
        progressOverviewCompletion.setProgressCompat(percent, true);

        txtPriorityHigh.setText("Cao                                      " + high + " ke hoach");
        txtPriorityMedium.setText("Trung binh                         " + medium + " ke hoach");
        txtPriorityLow.setText("Thap                                    " + low + " ke hoach");
        progressPriorityHigh.setProgressCompat(percentOf(high, total), true);
        progressPriorityMedium.setProgressCompat(percentOf(medium, total), true);
        progressPriorityLow.setProgressCompat(percentOf(low, total), true);

        txtDueSoon.setText(dueSoon.isEmpty()
                ? "Chua co ke hoach sap den han."
                : dueSoon.get(0).getTitle() + "\nHan: " + dueSoon.get(0).getDate());
        txtTodayPlan.setText(todayPlans.isEmpty()
                ? "Hom nay chua co lich can uu tien."
                : todayPlans.get(0).getTitle() + "\n"
                + todayPlans.get(0).getTime() + " - " + labelForType(todayPlans.get(0).getPlanType()));
    }

    private int countPriority(ArrayList<StudyPlan> plans, int priority) {
        int count = 0;
        for (StudyPlan plan : plans) {
            if (plan.getPriority() == priority
                    && plan.getStatus() != StudyPlan.STATUS_CANCELLED) {
                count++;
            }
        }
        return count;
    }

    private int percentOf(int value, int total) {
        return total <= 0 ? 0 : Math.round(value * 100f / total);
    }

    private String formatHours(int minutes) {
        if (minutes <= 0) return "0";
        if (minutes % 60 == 0) return String.valueOf(minutes / 60);
        return String.format(Locale.US, "%.1f", minutes / 60f);
    }

    private String labelForType(String type) {
        if (StudyPlan.TYPE_ASSIGNMENT.equals(type)) return "Bai tap";
        if (StudyPlan.TYPE_CLASS.equals(type)) return "Di hoc";
        if (StudyPlan.TYPE_PART_TIME.equals(type)) return "Lam them";
        if (StudyPlan.TYPE_EXAM.equals(type)) return "Thi";
        if (StudyPlan.TYPE_PROJECT.equals(type)) return "Du an";
        return "Ca nhan";
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

    @Override
    public void onDestroyView() {
        if (executorService != null) executorService.shutdownNow();
        super.onDestroyView();
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
