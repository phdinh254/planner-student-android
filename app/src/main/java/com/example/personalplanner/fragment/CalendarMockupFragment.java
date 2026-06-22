package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanDetailMockupActivity;
import com.example.personalplanner.adapter.CalendarEventAdapter;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarMockupFragment extends Fragment {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Calendar visibleMonth = Calendar.getInstance();
    private final Set<String> datesWithEvents = new HashSet<>();

    private TextView txtMonthTitle;
    private TextView txtSelectedDate;
    private TextView txtCalendarEmpty;
    private GridLayout gridCalendarDays;
    private RecyclerView recyclerCalendarTasks;
    private CalendarEventAdapter adapter;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private String selectedDate = dateFormat.format(Calendar.getInstance().getTime());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_mockup, container, false);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        bindViews(view);
        setupActions(view);
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CalendarEventAdapter(this::openDetail);
        recyclerCalendarTasks.setAdapter(adapter);
        syncVisibleMonthWithSelectedDate();
        loadMonthAndDate();
        return view;
    }

    private void bindViews(View view) {
        txtMonthTitle = view.findViewById(R.id.txtMonthTitle);
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        txtCalendarEmpty = view.findViewById(R.id.txtCalendarEmpty);
        gridCalendarDays = view.findViewById(R.id.gridCalendarDays);
        recyclerCalendarTasks = view.findViewById(R.id.recyclerCalendarTasks);
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnTodayCalendar).setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            selectedDate = dateFormat.format(today.getTime());
            visibleMonth.setTime(today.getTime());
            loadMonthAndDate();
        });
        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, -1);
            selectedDate = firstDayOfVisibleMonth();
            loadMonthAndDate();
        });
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, 1);
            selectedDate = firstDayOfVisibleMonth();
            loadMonthAndDate();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (executorService != null && !executorService.isShutdown()) {
            loadMonthAndDate();
        }
    }

    private void loadMonthAndDate() {
        if (executorService == null || executorService.isShutdown()) return;
        txtMonthTitle.setText(String.format(Locale.US, "Th\u00e1ng %d, %d",
                visibleMonth.get(Calendar.MONTH) + 1,
                visibleMonth.get(Calendar.YEAR)));
        String start = firstDayOfVisibleMonth();
        String end = lastDayOfVisibleMonth();
        executorService.execute(() -> {
            ArrayList<StudyPlan> monthPlans = databaseHelper.getStudyPlansBetween(
                    sessionManager.getUserId(), start, end);
            ArrayList<StudyPlan> dayPlans = databaseHelper.getStudyPlansByDate(
                    sessionManager.getUserId(), selectedDate);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                datesWithEvents.clear();
                for (StudyPlan plan : monthPlans) {
                    datesWithEvents.add(plan.getDate());
                }
                renderCalendarGrid();
                renderSelectedDate(dayPlans);
            });
        });
    }

    private void renderCalendarGrid() {
        gridCalendarDays.removeAllViews();
        Calendar cursor = (Calendar) visibleMonth.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOffset = cursor.get(Calendar.DAY_OF_WEEK) - 1;
        cursor.add(Calendar.DAY_OF_MONTH, -firstDayOffset);
        String today = dateFormat.format(Calendar.getInstance().getTime());
        int currentMonth = visibleMonth.get(Calendar.MONTH);
        for (int i = 0; i < 42; i++) {
            Calendar day = (Calendar) cursor.clone();
            String dateValue = dateFormat.format(day.getTime());
            TextView dayView = createDayView(day, dateValue, today, currentMonth);
            dayView.setOnClickListener(v -> {
                selectedDate = dateValue;
                visibleMonth.set(Calendar.YEAR, day.get(Calendar.YEAR));
                visibleMonth.set(Calendar.MONTH, day.get(Calendar.MONTH));
                loadMonthAndDate();
            });
            gridCalendarDays.addView(dayView);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private TextView createDayView(Calendar day, String dateValue, String today, int currentMonth) {
        TextView view = new TextView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(36);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 2, 0, 2);
        view.setLayoutParams(params);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(12);
        view.setTextColor(ContextCompat.getColor(requireContext(),
                day.get(Calendar.MONTH) == currentMonth ? R.color.text_primary : R.color.text_secondary));
        view.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        if (datesWithEvents.contains(dateValue)) {
            view.setText(day.get(Calendar.DAY_OF_MONTH) + "\n\u2022");
            view.setLineSpacing(-4f, 1f);
        }
        if (dateValue.equals(selectedDate)) {
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
            view.setBackgroundResource(R.drawable.bg_calendar_selected);
        } else if (dateValue.equals(today)) {
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            view.setBackgroundResource(R.drawable.bg_calendar_today);
        }
        return view;
    }

    private void renderSelectedDate(ArrayList<StudyPlan> dayPlans) {
        txtSelectedDate.setText(formatSelectedDateLabel());
        adapter.setData(dayPlans);
        boolean empty = dayPlans.isEmpty();
        txtCalendarEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerCalendarTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String formatSelectedDateLabel() {
        try {
            Calendar selected = Calendar.getInstance();
            selected.setTime(dateFormat.parse(selectedDate));
            String[] weekdays = {"Ch\u1ee7 nh\u1eadt", "Th\u1ee9 hai", "Th\u1ee9 ba", "Th\u1ee9 t\u01b0",
                    "Th\u1ee9 n\u0103m", "Th\u1ee9 s\u00e1u", "Th\u1ee9 b\u1ea3y"};
            return String.format(Locale.US, "%s, %d th\u00e1ng %d",
                    weekdays[selected.get(Calendar.DAY_OF_WEEK) - 1],
                    selected.get(Calendar.DAY_OF_MONTH),
                    selected.get(Calendar.MONTH) + 1);
        } catch (Exception ignored) {
            return selectedDate;
        }
    }

    private void syncVisibleMonthWithSelectedDate() {
        try {
            visibleMonth.setTime(dateFormat.parse(selectedDate));
        } catch (Exception ignored) {
            visibleMonth.setTime(Calendar.getInstance().getTime());
        }
    }

    private String firstDayOfVisibleMonth() {
        Calendar start = (Calendar) visibleMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        return dateFormat.format(start.getTime());
    }

    private String lastDayOfVisibleMonth() {
        Calendar end = (Calendar) visibleMonth.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return dateFormat.format(end.getTime());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
