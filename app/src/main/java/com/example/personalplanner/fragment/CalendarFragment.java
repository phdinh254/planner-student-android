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
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.model.Task;
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

        adapter = new TaskAdapter(requireContext(), new ArrayList<>(),
                new TaskAdapter.OnTaskActionListener() {
                    @Override
                    public void onTaskClick(Task task) {
                        openTaskDetail(task);
                    }

                    @Override
                    public void onStatusChanged(Task task, boolean isChecked) {
                        updateStatus(task, isChecked);
                    }
                });
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCalendarTasks.setAdapter(adapter);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadSelectedDate();
        });
        loadSelectedDate();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadSelectedDate();
        }
    }

    private void loadSelectedDate() {
        txtSelectedDate.setText(getString(R.string.tasks_on_date, selectedDate));
        executorService.execute(() -> {
            ArrayList<Task> tasks = databaseHelper.getTasksByDate(
                    sessionManager.getUserId(),
                    selectedDate
            );
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                adapter.setData(tasks);
                boolean empty = tasks.isEmpty();
                txtCalendarEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerCalendarTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void updateStatus(Task task, boolean checked) {
        executorService.execute(() -> {
            databaseHelper.updateTaskStatus(
                    task.getTaskId(),
                    sessionManager.getUserId(),
                    checked ? 1 : 0
            );
            mainHandler.post(() -> {
                if (isAdded() && getView() != null
                        && executorService != null && !executorService.isShutdown()) {
                    loadSelectedDate();
                }
            });
        });
    }

    private void openTaskDetail(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra("task_id", task.getTaskId());
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("date", task.getDate());
        intent.putExtra("time", task.getTime());
        intent.putExtra("status", task.getStatus());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.onDestroyView();
    }
}
