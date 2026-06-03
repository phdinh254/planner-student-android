package com.example.personalplanner.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.model.Task;
import com.example.personalplanner.utils.SessionManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView txtWelcome, txtTotalTasks, txtCompletedTasks, txtPendingTasks;

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initObjects();
        showWelcome();
        loadStatistics();

        return view;
    }

    private void initViews(View view) {
        txtWelcome = view.findViewById(R.id.txtWelcome);
        txtTotalTasks = view.findViewById(R.id.txtTotalTasks);
        txtCompletedTasks = view.findViewById(R.id.txtCompletedTasks);
        txtPendingTasks = view.findViewById(R.id.txtPendingTasks);
    }

    private void initObjects() {
        sessionManager = new SessionManager(requireContext());
        databaseHelper = new DatabaseHelper(requireContext());
    }

    private void showWelcome() {
        txtWelcome.setText("Xin chào, " + sessionManager.getUsername());
    }

    private void loadStatistics() {
        int userId = sessionManager.getUserId();

        executorService.execute(() -> {
            ArrayList<Task> tasks = databaseHelper.getAllTasks(userId);

            int completed = 0;

            for (Task task : tasks) {
                if (task.getStatus() == 1) {
                    completed++;
                }
            }

            final int total = tasks.size();
            final int completedTasks = completed;
            final int pending = total - completedTasks;

            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }

                txtTotalTasks.setText("Tổng kế hoạch: " + total);
                txtCompletedTasks.setText("Đã hoàn thành: " + completedTasks);
                txtPendingTasks.setText("Chưa hoàn thành: " + pending);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && databaseHelper != null) {
            loadStatistics();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
