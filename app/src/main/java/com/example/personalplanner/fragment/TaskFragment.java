package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskFragment extends Fragment {

    private RecyclerView recyclerTask;
    private EditText edtSearchTask;
    private TextView txtEmptyTask;
    private ProgressBar progressTasks;
    private TaskAdapter taskAdapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::loadTasks;
    private int statusFilter = DatabaseHelper.FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);
        recyclerTask = view.findViewById(R.id.recyclerTask);
        edtSearchTask = view.findViewById(R.id.edtSearchTask);
        txtEmptyTask = view.findViewById(R.id.txtEmptyTask);
        progressTasks = view.findViewById(R.id.progressTasks);
        ChipGroup chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        taskAdapter = new TaskAdapter(requireContext(), new ArrayList<>(),
                new TaskAdapter.OnTaskActionListener() {
                    @Override
                    public void onTaskClick(Task task) {
                        openTaskDetail(task);
                    }

                    @Override
                    public void onStatusChanged(Task task, boolean isChecked) {
                        updateTaskStatus(task, isChecked);
                    }
                });
        recyclerTask.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTask.setAdapter(taskAdapter);

        edtSearchTask.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainHandler.removeCallbacks(searchRunnable);
                mainHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (checkedId == R.id.chipPending) {
                statusFilter = DatabaseHelper.STATUS_PENDING;
            } else if (checkedId == R.id.chipCompleted) {
                statusFilter = DatabaseHelper.STATUS_COMPLETED;
            } else {
                statusFilter = DatabaseHelper.FILTER_ALL;
            }
            loadTasks();
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadTasks();
        }
    }

    private void loadTasks() {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }
        int userId = sessionManager.getUserId();
        String keyword = edtSearchTask.getText().toString().trim();
        showLoading(true);
        executorService.execute(() -> {
            ArrayList<Task> tasks = databaseHelper.getTasks(userId, keyword, statusFilter);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                taskAdapter.setData(tasks);
                showLoading(false);
                boolean empty = tasks.isEmpty();
                txtEmptyTask.setText(keyword.isEmpty()
                        ? R.string.empty_tasks
                        : R.string.empty_search_result);
                txtEmptyTask.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerTask.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void updateTaskStatus(Task task, boolean checked) {
        int previousStatus = task.getStatus();
        int newStatus = checked ? DatabaseHelper.STATUS_COMPLETED : DatabaseHelper.STATUS_PENDING;
        task.setStatus(newStatus);
        int position = taskAdapter.getPosition(task);
        if (position >= 0) {
            taskAdapter.notifyItemChanged(position);
        }

        executorService.execute(() -> {
            boolean updated = databaseHelper.updateTaskStatus(
                    task.getTaskId(),
                    sessionManager.getUserId(),
                    newStatus
            );
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                if (updated) {
                    loadTasks();
                } else {
                    task.setStatus(previousStatus);
                    int currentPosition = taskAdapter.getPosition(task);
                    if (currentPosition >= 0) {
                        taskAdapter.notifyItemChanged(currentPosition);
                    }
                    Toast.makeText(requireContext(), R.string.status_update_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLoading(boolean loading) {
        progressTasks.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            txtEmptyTask.setVisibility(View.GONE);
        }
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
        mainHandler.removeCallbacks(searchRunnable);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        recyclerTask = null;
        edtSearchTask = null;
        txtEmptyTask = null;
        progressTasks = null;
        super.onDestroyView();
    }
}
