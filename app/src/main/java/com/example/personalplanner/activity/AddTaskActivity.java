package com.example.personalplanner.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanCategory;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.notification.ReminderScheduler;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskActivity extends AppCompatActivity {
    private EditText edtTitle;
    private EditText edtDescription;
    private EditText edtDuration;
    private EditText edtLocation;
    private EditText edtRoom;
    private EditText edtSubject;
    private EditText edtRepeatUntil;
    private EditText edtWage;
    private Button btnChooseDate;
    private Button btnChooseTime;
    private Button btnChooseEndTime;
    private Button btnSaveTask;
    private Spinner spinnerCategory;
    private Spinner spinnerPlanType;
    private Spinner spinnerPriority;
    private Spinner spinnerRepeatRule;
    private Spinner spinnerReminderLead;
    private SwitchMaterial switchReminder;
    private SwitchMaterial switchAllDay;
    private TextView txtConflictWarning;
    private CheckBox chkSubmitted;
    private View layoutLocation;
    private View layoutRoom;
    private View layoutSubject;
    private View layoutWage;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ArrayList<PlanCategory> categories = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String selectedDate;
    private String selectedTime;
    private String selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule_mockup);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDuration = findViewById(R.id.edtDuration);
        edtLocation = findViewById(R.id.edtLocation);
        edtRoom = findViewById(R.id.edtRoom);
        edtSubject = findViewById(R.id.edtSubject);
        edtRepeatUntil = findViewById(R.id.edtRepeatUntil);
        edtWage = findViewById(R.id.edtWage);
        btnChooseDate = findViewById(R.id.btnChooseDate);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnChooseEndTime = findViewById(R.id.btnChooseEndTime);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPlanType = findViewById(R.id.spinnerPlanType);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerRepeatRule = findViewById(R.id.spinnerRepeatRule);
        spinnerReminderLead = findViewById(R.id.spinnerReminderLead);
        ChipGroup chipGroupPriority = findViewById(R.id.chipGroupPriority);
        switchReminder = findViewById(R.id.switchReminder);
        switchAllDay = findViewById(R.id.switchAllDay);
        txtConflictWarning = findViewById(R.id.txtConflictWarning);
        chkSubmitted = findViewById(R.id.chkSubmitted);
        layoutLocation = findViewById(R.id.layoutLocation);
        layoutRoom = findViewById(R.id.layoutRoom);
        layoutSubject = findViewById(R.id.layoutSubject);
        layoutWage = findViewById(R.id.layoutWage);
        Button btnManageCategories = findViewById(R.id.btnManageCategories);
        Button btnCancel = findViewById(R.id.btnCancel);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        bindSpinner(spinnerPriority, R.array.priority_names);
        bindSpinner(spinnerPlanType, R.array.plan_type_names);
        bindSpinner(spinnerRepeatRule, R.array.repeat_rule_names);
        bindSpinner(spinnerReminderLead, R.array.reminder_lead_names);
        spinnerPriority.setSelection(1);
        if (switchAllDay != null) {
            spinnerPlanType.setSelection(3);
        }
        chipGroupPriority.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipPriorityMedium : checkedIds.get(0);
            if (id == R.id.chipPriorityHigh) {
                spinnerPriority.setSelection(StudyPlan.PRIORITY_HIGH);
            } else if (id == R.id.chipPriorityLow) {
                spinnerPriority.setSelection(StudyPlan.PRIORITY_LOW);
            } else {
                spinnerPriority.setSelection(StudyPlan.PRIORITY_MEDIUM);
            }
        });
        spinnerPlanType.setOnItemSelectedListener(new SimpleItemSelectedListener(this::updateTypeFields));
        updateTypeFields();

        selectedDate = dateFormat.format(calendar.getTime());
        selectedTime = timeFormat.format(calendar.getTime());
        endCalendar.setTime(calendar.getTime());
        endCalendar.add(Calendar.MINUTE, 60);
        selectedEndTime = timeFormat.format(endCalendar.getTime());
        updateDateTimeLabels();

        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker(calendar, true));
        btnChooseEndTime.setOnClickListener(v -> showTimePicker(endCalendar, false));
        btnSaveTask.setOnClickListener(v -> savePlan());
        findViewById(R.id.btnSaveTop).setOnClickListener(v -> savePlan());
        if (switchAllDay != null) {
            switchAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTime = "00:00";
                    selectedEndTime = "23:59";
                    btnChooseTime.setVisibility(View.GONE);
                } else {
                    btnChooseTime.setVisibility(View.VISIBLE);
                }
                updateDateTimeLabels();
            });
        }
        btnManageCategories.setOnClickListener(v ->
                startActivity(new Intent(this, PlanCategoryListActivity.class)));
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void bindSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadCategories() {
        executorService.execute(() -> {
            ArrayList<PlanCategory> result = databaseHelper.getCategories(sessionManager.getUserId());
            result.add(0, new PlanCategory(0, getString(R.string.uncategorized_course),
                    "", "", "#607D8B", sessionManager.getUserId()));
            runOnUiThread(() -> {
                categories.clear();
                categories.addAll(result);
                ArrayAdapter<PlanCategory> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
            });
        });
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            selectedDate = dateFormat.format(calendar.getTime());
            updateDateTimeLabels();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Calendar target, boolean startTime) {
        new TimePickerDialog(this, (view, hour, minute) -> {
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            if (startTime) {
                selectedTime = timeFormat.format(target.getTime());
            } else {
                selectedEndTime = timeFormat.format(target.getTime());
            }
            updateDateTimeLabels();
        }, target.get(Calendar.HOUR_OF_DAY), target.get(Calendar.MINUTE), true).show();
    }

    private void savePlan() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            edtTitle.requestFocus();
            return;
        }
        int duration = parseInt(edtDuration, -1);
        if (duration <= 0) {
            edtDuration.setError(getString(R.string.error_duration));
            edtDuration.requestFocus();
            return;
        }
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.category_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = sessionManager.getUserId();
        PlanCategory category = categories.get(spinnerCategory.getSelectedItemPosition());
        String description = edtDescription.getText().toString().trim();
        int priority = spinnerPriority.getSelectedItemPosition();
        String planType = valueFromArray(R.array.plan_type_values, spinnerPlanType.getSelectedItemPosition());
        String repeatRule = valueFromArray(R.array.repeat_rule_values, spinnerRepeatRule.getSelectedItemPosition());
        int reminderMinutes = Integer.parseInt(valueFromArray(
                R.array.reminder_lead_values, spinnerReminderLead.getSelectedItemPosition()));
        boolean reminderEnabled = switchReminder.isChecked();
        double wage = parseDouble(edtWage);
        String repeatUntil = edtRepeatUntil.getText().toString().trim();
        ArrayList<String> dates = buildRepeatDates(selectedDate, repeatUntil, repeatRule);
        if (txtConflictWarning != null
                && dates.size() == 1
                && databaseHelper.hasTimeConflict(userId, selectedDate, selectedTime, selectedEndTime, -1)) {
            txtConflictWarning.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.schedule_conflict_warning, Toast.LENGTH_SHORT).show();
            return;
        }
        setSaving(true);
        executorService.execute(() -> {
            int saved = 0;
            int conflicted = 0;
            long firstPlanId = -1;
            for (String date : dates) {
                if (databaseHelper.hasTimeConflict(userId, date, selectedTime, selectedEndTime, -1)) {
                    conflicted++;
                    continue;
                }
                long planId = databaseHelper.addStudyPlan(
                        title, description, date, selectedTime, selectedEndTime,
                        category.getCategoryId(), planType, priority, duration,
                        reminderEnabled, reminderMinutes,
                        edtLocation.getText().toString(), edtRoom.getText().toString(),
                        edtSubject.getText().toString(), repeatRule, repeatUntil,
                        wage, chkSubmitted.isChecked(), userId);
                if (planId != -1) {
                    saved++;
                    if (firstPlanId == -1) {
                        firstPlanId = planId;
                    }
                    if (reminderEnabled) {
                        ReminderScheduler.schedule(this, (int) planId, title,
                                category.getCategoryName(), date, selectedTime, reminderMinutes);
                    }
                }
            }
            long firstId = firstPlanId;
            int savedCount = saved;
            int conflictCount = conflicted;
            runOnUiThread(() -> {
                if (savedCount > 0) {
                    String message = dates.size() > 1
                            ? getString(R.string.repeat_save_summary, savedCount,
                            dates.size(), conflictCount)
                            : getString(R.string.task_added);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    setSaving(false);
                    Toast.makeText(this,
                            firstId == -1 ? R.string.schedule_conflict_warning : R.string.task_add_failed,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private ArrayList<String> buildRepeatDates(String startDate, String repeatUntil, String repeatRule) {
        ArrayList<String> dates = new ArrayList<>();
        dates.add(startDate);
        if ("NONE".equals(repeatRule) || repeatUntil.trim().isEmpty()) {
            return dates;
        }
        try {
            Calendar current = Calendar.getInstance();
            current.setTime(dateFormat.parse(startDate));
            Calendar until = Calendar.getInstance();
            until.setTime(dateFormat.parse(repeatUntil));
            for (int count = 0; count < 365; count++) {
                current.add(Calendar.DAY_OF_MONTH, 1);
                if (current.after(until)) {
                    break;
                }
                if (shouldIncludeRepeatDate(current, repeatRule, startDate)) {
                    dates.add(dateFormat.format(current.getTime()));
                }
            }
        } catch (ParseException ignored) {
            return dates;
        }
        return dates;
    }

    private boolean shouldIncludeRepeatDate(Calendar date, String repeatRule, String startDate) {
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        if ("DAILY".equals(repeatRule)) {
            return true;
        }
        if ("WEEKLY".equals(repeatRule)) {
            try {
                Calendar start = Calendar.getInstance();
                start.setTime(dateFormat.parse(startDate));
                return dayOfWeek == start.get(Calendar.DAY_OF_WEEK);
            } catch (ParseException ignored) {
                return false;
            }
        }
        if ("MON_WED_FRI".equals(repeatRule)) {
            return dayOfWeek == Calendar.MONDAY
                    || dayOfWeek == Calendar.WEDNESDAY
                    || dayOfWeek == Calendar.FRIDAY;
        }
        if ("TUE_THU".equals(repeatRule)) {
            return dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY;
        }
        if ("WEEKEND".equals(repeatRule)) {
            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
        }
        return false;
    }

    private int parseInt(EditText editText, int fallback) {
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double parseDouble(EditText editText) {
        try {
            String value = editText.getText().toString().trim();
            return value.isEmpty() ? 0 : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String valueFromArray(int arrayRes, int position) {
        String[] values = getResources().getStringArray(arrayRes);
        return position >= 0 && position < values.length ? values[position] : values[0];
    }

    private void updateDateTimeLabels() {
        if (switchAllDay != null) {
            btnChooseDate.setText("B\u1eaft \u0111\u1ea7u\n" + selectedDate + " - " + selectedTime);
            btnChooseTime.setText("Gi\u1edd b\u1eaft \u0111\u1ea7u: " + selectedTime);
            btnChooseEndTime.setText("K\u1ebft th\u00fac\n" + selectedDate + " - " + selectedEndTime);
            if (txtConflictWarning != null) {
                txtConflictWarning.setVisibility(View.GONE);
            }
        } else {
            btnChooseDate.setText(getString(R.string.date_value, selectedDate));
            btnChooseTime.setText(getString(R.string.time_value, selectedTime));
            btnChooseEndTime.setText(getString(R.string.end_time_value, selectedEndTime));
        }
    }

    private void setSaving(boolean saving) {
        btnSaveTask.setEnabled(!saving);
        btnSaveTask.setText(saving ? R.string.saving : R.string.save_study_plan);
    }

    private void updateTypeFields() {
        if (switchAllDay != null) {
            layoutLocation.setVisibility(View.GONE);
            layoutRoom.setVisibility(View.GONE);
            layoutWage.setVisibility(View.GONE);
            layoutSubject.setVisibility(View.GONE);
            chkSubmitted.setVisibility(View.GONE);
            return;
        }
        String planType = valueFromArray(R.array.plan_type_values, spinnerPlanType.getSelectedItemPosition());
        boolean isAssignment = StudyPlan.TYPE_ASSIGNMENT.equals(planType);
        boolean isClass = StudyPlan.TYPE_CLASS.equals(planType);
        boolean isPartTime = StudyPlan.TYPE_PART_TIME.equals(planType);
        boolean isExam = StudyPlan.TYPE_EXAM.equals(planType);
        boolean isProject = StudyPlan.TYPE_PROJECT.equals(planType);

        layoutLocation.setVisibility((isClass || isPartTime || isExam) ? View.VISIBLE : View.GONE);
        layoutRoom.setVisibility(isClass ? View.VISIBLE : View.GONE);
        layoutWage.setVisibility(isPartTime ? View.VISIBLE : View.GONE);
        layoutSubject.setVisibility((isAssignment || isClass || isExam || isProject)
                ? View.VISIBLE : View.GONE);
        chkSubmitted.setVisibility(isAssignment ? View.VISIBLE : View.GONE);
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

        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
