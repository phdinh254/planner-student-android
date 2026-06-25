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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanDetailMockupActivity;
import com.example.personalplanner.adapter.TaskAdapter;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanCategory;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.notification.ReminderScheduler;
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
    private Spinner spinnerCategoryFilter;
    private TaskAdapter taskAdapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private final ArrayList<PlanCategory> categories = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::loadPlans;
    private int statusFilter = DatabaseHelper.FILTER_ALL;
    private String typeFilter = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);
        recyclerTask = view.findViewById(R.id.recyclerTask);
        edtSearchTask = view.findViewById(R.id.edtSearchTask);
        txtEmptyTask = view.findViewById(R.id.txtEmptyTask);
        progressTasks = view.findViewById(R.id.progressTasks);
        spinnerCategoryFilter = view.findViewById(R.id.spinnerCategoryFilter);
        ChipGroup chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        ChipGroup chipGroupTypeFilter = view.findViewById(R.id.chipGroupTypeFilter);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(StudyPlan plan) {
                openPlanDetail(plan);
            }

            @Override
            public void onStatusChanged(StudyPlan plan, boolean checked) {
                updatePlanStatus(plan, checked);
            }
        });
        recyclerTask.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTask.setAdapter(taskAdapter);

        edtSearchTask.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainHandler.removeCallbacks(searchRunnable);
                mainHandler.postDelayed(searchRunnable, 300);
            }
        });
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            statusFilter = id == R.id.chipPending ? DatabaseHelper.STATUS_UPCOMING
                    : id == R.id.chipCompleted ? DatabaseHelper.STATUS_COMPLETED
                    : DatabaseHelper.FILTER_ALL;
            loadPlans();
        });
        chipGroupTypeFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipTypeAll : checkedIds.get(0);
            typeFilter = resolveTypeFilter(id);
            loadPlans();
        });
        spinnerCategoryFilter.setOnItemSelectedListener(new SimpleItemSelectedListener(this::loadPlans));
        loadCategories();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadCategories();
            loadPlans();
        }
    }

    private void loadCategories() {
        executorService.execute(() -> {
            ArrayList<PlanCategory> result = databaseHelper.getCategories(sessionManager.getUserId());
            result.add(0, new PlanCategory(0, getString(R.string.all_courses),
                    "", "", "#607D8B", sessionManager.getUserId()));
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                int selectedId = getCategoryFilter();
                categories.clear();
                categories.addAll(result);
                ArrayAdapter<PlanCategory> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategoryFilter.setAdapter(adapter);
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getCategoryId() == selectedId) {
                        spinnerCategoryFilter.setSelection(i);
                        break;
                    }
                }
            });
        });
    }

    private void loadPlans() {
        if (executorService == null || executorService.isShutdown() || edtSearchTask == null) return;
        String keyword = edtSearchTask.getText().toString().trim();
        int categoryId = getCategoryFilter();
        showLoading(true);
        executorService.execute(() -> {
            ArrayList<StudyPlan> plans = databaseHelper.getStudyPlans(
                    sessionManager.getUserId(), keyword, statusFilter, categoryId, typeFilter);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                taskAdapter.setData(plans);
                showLoading(false);
                boolean empty = plans.isEmpty();
                txtEmptyTask.setText(keyword.isEmpty()
                        ? R.string.empty_study_plans : R.string.empty_search_result);
                txtEmptyTask.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerTask.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private int getCategoryFilter() {
        int position = spinnerCategoryFilter == null ? 0 : spinnerCategoryFilter.getSelectedItemPosition();
        return position >= 0 && position < categories.size() ? categories.get(position).getCategoryId() : 0;
    }

    private String resolveTypeFilter(int chipId) {
        if (chipId == R.id.chipTypeAssignment) {
            return StudyPlan.TYPE_ASSIGNMENT;
        }
        if (chipId == R.id.chipTypeClass) {
            return StudyPlan.TYPE_CLASS;
        }
        if (chipId == R.id.chipTypePartTime) {
            return StudyPlan.TYPE_PART_TIME;
        }
        if (chipId == R.id.chipTypePersonal) {
            return StudyPlan.TYPE_PERSONAL;
        }
        if (chipId == R.id.chipTypeExam) {
            return StudyPlan.TYPE_EXAM;
        }
        if (chipId == R.id.chipTypeProject) {
            return StudyPlan.TYPE_PROJECT;
        }
        return "";
    }

    private void updatePlanStatus(StudyPlan plan, boolean checked) {
        int oldStatus = plan.getStatus();
        int newStatus = checked ? StudyPlan.STATUS_COMPLETED : StudyPlan.STATUS_UPCOMING;
        plan.setStatus(newStatus);
        executorService.execute(() -> {
            boolean updated = databaseHelper.updateStudyPlanStatus(
                    plan.getPlanId(), sessionManager.getUserId(), newStatus);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                if (updated) {
                    if (checked) {
                        ReminderScheduler.cancel(requireContext(), plan.getPlanId());
                        NotificationManagerCompat.from(requireContext()).cancel(plan.getPlanId());
                    }
                    loadPlans();
                } else {
                    plan.setStatus(oldStatus);
                    Toast.makeText(requireContext(), R.string.status_update_failed,
                            Toast.LENGTH_SHORT).show();
                    loadPlans();
                }
            });
        });
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

    private void showLoading(boolean loading) {
        progressTasks.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) txtEmptyTask.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(searchRunnable);
        if (executorService != null) executorService.shutdownNow();
        super.onDestroyView();
    }

    private static class SimpleItemSelectedListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;
        SimpleItemSelectedListener(Runnable callback) {
            this.callback = callback;
        }
        public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                   int position, long id) {
            callback.run();
        }
        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
