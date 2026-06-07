package com.example.personalplanner.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.model.Task;
import com.example.personalplanner.utils.SessionManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView txtWelcome;
    private TextView txtTotalTasks;
    private TextView txtCompletedTasks;
    private TextView txtPendingTasks;
    private TextView txtProgressSummary;
    private ProgressBar progressCompletion;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        txtWelcome = view.findViewById(R.id.txtWelcome);
        txtTotalTasks = view.findViewById(R.id.txtTotalTasks);
        txtCompletedTasks = view.findViewById(R.id.txtCompletedTasks);
        txtPendingTasks = view.findViewById(R.id.txtPendingTasks);
        txtProgressSummary = view.findViewById(R.id.txtProgressSummary);
        progressCompletion = view.findViewById(R.id.progressCompletion);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        String username = sessionManager.getUsername();
        txtWelcome.setText(getString(
                R.string.welcome_user,
                username.isEmpty() ? getString(R.string.default_user) : username
        ));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadStatistics();
        }
    }

    private void loadStatistics() {
        int userId = sessionManager.getUserId();
        executorService.execute(() -> {
            ArrayList<Task> tasks = databaseHelper.getAllTasks(userId);
            int completed = 0;
            for (Task task : tasks) {
                if (task.getStatus() == DatabaseHelper.STATUS_COMPLETED) {
                    completed++;
                }
            }
            int total = tasks.size();
            int pending = total - completed;
            int percent = total == 0 ? 0 : Math.round(completed * 100f / total);

            int finalCompleted = completed;
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                txtTotalTasks.setText(String.valueOf(total));
                txtCompletedTasks.setText(String.valueOf(finalCompleted));
                txtPendingTasks.setText(String.valueOf(pending));
                progressCompletion.setProgress(percent);
                txtProgressSummary.setText(getString(R.string.progress_summary, percent));
            });
        });
    }

    @Override
    public void onDestroyView() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.onDestroyView();
    }
}
