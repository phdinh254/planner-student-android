package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanDetailMockupActivity;
import com.example.personalplanner.adapter.TaskAdapter;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarFragment extends Fragment {
    private TextView txtSelectedDate;
    private TextView txtCalendarEmpty;
    private RecyclerView recyclerCalendarTasks;
    private RecyclerView recyclerUpcomingTasks;
    private TaskAdapter adapter;
    private TaskAdapter upcomingAdapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private String selectedDate = dateFormat.format(Calendar.getInstance().getTime());
    private int calendarMode = R.id.chipModeDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        ChipGroup chipGroupCalendarMode = view.findViewById(R.id.chipGroupCalendarMode);
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        txtCalendarEmpty = view.findViewById(R.id.txtCalendarEmpty);
        recyclerCalendarTasks = view.findViewById(R.id.recyclerCalendarTasks);
        recyclerUpcomingTasks = view.findViewById(R.id.recyclerUpcomingTasks);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            public void onTaskClick(StudyPlan plan) { openDetail(plan); }
            public void onStatusChanged(StudyPlan plan, boolean checked) {
                executorService.execute(() -> {
                    databaseHelper.updateStudyPlanStatus(plan.getPlanId(),
                            sessionManager.getUserId(),
                            checked ? StudyPlan.STATUS_COMPLETED : StudyPlan.STATUS_UPCOMING);
                    mainHandler.post(CalendarFragment.this::loadDate);
                });
            }
        });
        upcomingAdapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            public void onTaskClick(StudyPlan plan) { openDetail(plan); }
            public void onStatusChanged(StudyPlan plan, boolean checked) {
                executorService.execute(() -> {
                    databaseHelper.updateStudyPlanStatus(plan.getPlanId(),
                            sessionManager.getUserId(),
                            checked ? StudyPlan.STATUS_COMPLETED : StudyPlan.STATUS_UPCOMING);
                    mainHandler.post(CalendarFragment.this::loadDate);
                });
            }
        });
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCalendarTasks.setAdapter(adapter);
        recyclerUpcomingTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerUpcomingTasks.setAdapter(upcomingAdapter);
        calendarView.setOnDateChangeListener((v, year, month, day) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            loadDate();
        });
        view.findViewById(R.id.btnTodayCalendar).setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            selectedDate = dateFormat.format(today.getTime());
            calendarView.setDate(today.getTimeInMillis(), true, true);
            loadDate();
        });
        chipGroupCalendarMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            calendarMode = checkedIds.isEmpty() ? R.id.chipModeDay : checkedIds.get(0);
            loadDate();
        });
        loadDate();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) loadDate();
    }

    private void loadDate() {
        if (executorService == null || executorService.isShutdown()) return;
        DateRange range = resolveDateRange();
        txtSelectedDate.setText(range.startDate.equals(range.endDate)
                ? getString(R.string.tasks_on_date, selectedDate)
                : getString(R.string.plans_in_range, range.startDate, range.endDate));
        executorService.execute(() -> {
            ArrayList<StudyPlan> plans = range.startDate.equals(range.endDate)
                    ? databaseHelper.getStudyPlansByDate(sessionManager.getUserId(), selectedDate)
                    : databaseHelper.getStudyPlansBetween(
                    sessionManager.getUserId(), range.startDate, range.endDate);
            ArrayList<StudyPlan> upcoming = databaseHelper.getUpcomingPlans(
                    sessionManager.getUserId(), selectedDate, 5);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                adapter.setData(plans);
                upcomingAdapter.setData(upcoming);
                txtCalendarEmpty.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerCalendarTasks.setVisibility(plans.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private DateRange resolveDateRange() {
        Calendar start = Calendar.getInstance();
        try {
            start.setTime(dateFormat.parse(selectedDate));
        } catch (Exception ignored) {
            return new DateRange(selectedDate, selectedDate);
        }
        Calendar end = (Calendar) start.clone();
        if (calendarMode == R.id.chipModeWeek) {
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            end.setTime(start.getTime());
            end.add(Calendar.DAY_OF_MONTH, 6);
        } else if (calendarMode == R.id.chipModeMonth) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end.setTime(start.getTime());
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        return new DateRange(dateFormat.format(start.getTime()), dateFormat.format(end.getTime()));
    }

    private static class DateRange {
        final String startDate;
        final String endDate;

        DateRange(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private void openDetail(StudyPlan plan) {
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

    @Override
    public void onDestroyView() {
        if (executorService != null) executorService.shutdownNow();
        super.onDestroyView();
    }
}
