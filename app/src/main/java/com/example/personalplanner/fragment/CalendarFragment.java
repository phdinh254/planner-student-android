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
import com.example.personalplanner.activity.TaskDetailActivity;
import com.example.personalplanner.adapter.TaskAdapter;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.utils.SessionManager;

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
    private TaskAdapter adapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private String selectedDate = dateFormat.format(Calendar.getInstance().getTime());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        txtCalendarEmpty = view.findViewById(R.id.txtCalendarEmpty);
        recyclerCalendarTasks = view.findViewById(R.id.recyclerCalendarTasks);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            public void onTaskClick(StudyPlan plan) { openDetail(plan); }
            public void onStatusChanged(StudyPlan plan, boolean checked) {
                executorService.execute(() -> {
                    databaseHelper.updateStudyPlanStatus(plan.getPlanId(),
                            sessionManager.getUserId(), checked ? 1 : 0);
                    mainHandler.post(CalendarFragment.this::loadDate);
                });
            }
        });
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCalendarTasks.setAdapter(adapter);
        calendarView.setOnDateChangeListener((v, year, month, day) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
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
        txtSelectedDate.setText(getString(R.string.tasks_on_date, selectedDate));
        executorService.execute(() -> {
            ArrayList<StudyPlan> plans = databaseHelper.getStudyPlansByDate(
                    sessionManager.getUserId(), selectedDate);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                adapter.setData(plans);
                txtCalendarEmpty.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerCalendarTasks.setVisibility(plans.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void openDetail(StudyPlan plan) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra("plan_id", plan.getPlanId());
        intent.putExtra("title", plan.getTitle());
        intent.putExtra("description", plan.getDescription());
        intent.putExtra("date", plan.getDate());
        intent.putExtra("time", plan.getTime());
        intent.putExtra("status", plan.getStatus());
        intent.putExtra("course_id", plan.getCourseId());
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
