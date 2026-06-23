package com.example.personalplanner.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.PlanCategory;
import com.example.personalplanner.data.model.RepeatRule;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.notification.ReminderScheduler;
import com.example.personalplanner.notification.ReminderType;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskDetailActivity extends AppCompatActivity {
    private static final int REQUEST_POST_NOTIFICATIONS = 2002;

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
    private Button btnUpdateTask;
    private Button btnDeleteTask;
    private CheckBox chkSubmitted;
    private View layoutLocation;
    private View layoutRoom;
    private View layoutSubject;
    private View layoutWage;
    private Spinner spinnerCategory;
    private Spinner spinnerPlanType;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private Spinner spinnerRepeatRule;
    private Spinner spinnerReminderLead;
    private SwitchMaterial switchReminder;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ArrayList<PlanCategory> categories = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int planId;
    private int selectedCategoryId;
    private String selectedDate;
    private String selectedTime;
    private String selectedEndTime;
    private String selectedType;
    private String selectedRepeatRule;
    private int selectedReminderMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

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
        btnUpdateTask = findViewById(R.id.btnUpdateTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        chkSubmitted = findViewById(R.id.chkSubmitted);
        layoutLocation = findViewById(R.id.layoutLocation);
        layoutRoom = findViewById(R.id.layoutRoom);
        layoutSubject = findViewById(R.id.layoutSubject);
        layoutWage = findViewById(R.id.layoutWage);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPlanType = findViewById(R.id.spinnerPlanType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerRepeatRule = findViewById(R.id.spinnerRepeatRule);
        spinnerReminderLead = findViewById(R.id.spinnerReminderLead);
        switchReminder = findViewById(R.id.switchReminder);
        Button btnBack = findViewById(R.id.btnBack);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        bindSpinner(spinnerPriority, R.array.priority_names);
        bindSpinner(spinnerPlanType, R.array.plan_type_names);
        bindSpinner(spinnerStatus, R.array.status_names);
        bindSpinner(spinnerRepeatRule, R.array.repeat_rule_names);
        bindSpinner(spinnerReminderLead, R.array.reminder_lead_names);

        if (!readPlan()) {
            finish();
            return;
        }
        loadCategories();
        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker(calendar, true));
        btnChooseEndTime.setOnClickListener(v -> showTimePicker(endCalendar, false));
        btnUpdateTask.setOnClickListener(v -> updatePlan());
        btnDeleteTask.setOnClickListener(v -> confirmDelete());
        btnBack.setOnClickListener(v -> finish());
    }

    private void bindSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean readPlan() {
        planId = getIntent().getIntExtra("plan_id", -1);
        if (planId == -1) {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }
        edtTitle.setText(value(getIntent().getStringExtra("title")));
        edtDescription.setText(value(getIntent().getStringExtra("description")));
        edtDuration.setText(String.valueOf(getIntent().getIntExtra("duration", 60)));
        edtLocation.setText(value(getIntent().getStringExtra("location")));
        edtRoom.setText(value(getIntent().getStringExtra("room")));
        edtSubject.setText(value(getIntent().getStringExtra("subject")));
        edtRepeatUntil.setText(value(getIntent().getStringExtra("repeat_until")));
        edtWage.setText(String.valueOf(getIntent().getDoubleExtra("wage", 0)));
        selectedDate = valueOrDefault(getIntent().getStringExtra("date"),
                dateFormat.format(calendar.getTime()));
        selectedTime = valueOrDefault(getIntent().getStringExtra("time"),
                timeFormat.format(calendar.getTime()));
        selectedEndTime = valueOrDefault(getIntent().getStringExtra("end_time"), selectedTime);
        selectedCategoryId = getIntent().getIntExtra("category_id", 0);
        selectedType = valueOrDefault(getIntent().getStringExtra("plan_type"), StudyPlan.TYPE_PERSONAL);
        selectedRepeatRule = valueOrDefault(getIntent().getStringExtra("repeat_rule"), "NONE");
        selectedReminderMinutes = getIntent().getIntExtra("reminder_minutes", 0);
        spinnerPriority.setSelection(getIntent().getIntExtra("priority", 1));
        spinnerStatus.setSelection(getIntent().getIntExtra("status", 0));
        setSelectionByValue(spinnerPlanType, R.array.plan_type_values, selectedType);
        setSelectionByValue(spinnerRepeatRule, R.array.repeat_rule_values, selectedRepeatRule);
        setSelectionByValue(spinnerReminderLead, R.array.reminder_lead_values,
                String.valueOf(selectedReminderMinutes));
        switchReminder.setChecked(getIntent().getBooleanExtra("reminder_enabled", false));
        chkSubmitted.setChecked(getIntent().getBooleanExtra("submitted", false));
        spinnerPlanType.setOnItemSelectedListener(new SimpleItemSelectedListener(this::updateTypeFields));
        updateTypeFields();
        syncCalendar();
        updateDateTimeLabels();
        return true;
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
                for (int index = 0; index < categories.size(); index++) {
                    if (categories.get(index).getCategoryId() == selectedCategoryId) {
                        spinnerCategory.setSelection(index);
                        break;
                    }
                }
            });
        });
    }

    private void syncCalendar() {
        try {
            calendar.setTime(dateFormat.parse(selectedDate));
            endCalendar.setTime(dateFormat.parse(selectedDate));
            applyTime(calendar, selectedTime);
            applyTime(endCalendar, selectedEndTime);
        } catch (ParseException | NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            selectedDate = dateFormat.format(calendar.getTime());
            selectedTime = timeFormat.format(calendar.getTime());
            selectedEndTime = selectedTime;
        }
    }

    private void applyTime(Calendar target, String time) {
        String[] parts = time.split(":");
        target.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        target.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            endCalendar.set(year, month, day);
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

    private void updatePlan() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            return;
        }
        int duration = parseInt(edtDuration, -1);
        if (duration <= 0 || categories.isEmpty()) {
            edtDuration.setError(getString(R.string.error_duration));
            return;
        }
        PlanCategory category = categories.get(spinnerCategory.getSelectedItemPosition());
        int status = spinnerStatus.getSelectedItemPosition();
        int reminderMinutes = Integer.parseInt(valueFromArray(
                R.array.reminder_lead_values, spinnerReminderLead.getSelectedItemPosition()));
        boolean reminderActive = switchReminder.isChecked()
                && status != DatabaseHelper.STATUS_COMPLETED
                && status != DatabaseHelper.STATUS_CANCELLED;
        if (reminderActive) {
            if (!validateReminderSettings(reminderMinutes)
                    || !ensureNotificationPermission()
                    || !ensureExactAlarmPermission()) {
                return;
            }
        }
        String planType = valueFromArray(R.array.plan_type_values, spinnerPlanType.getSelectedItemPosition());
        String repeatRule = valueFromArray(R.array.repeat_rule_values, spinnerRepeatRule.getSelectedItemPosition());
        if (databaseHelper.hasTimeConflict(sessionManager.getUserId(), selectedDate,
                selectedTime, selectedEndTime, planId)) {
            Toast.makeText(this, R.string.schedule_conflict_warning, Toast.LENGTH_SHORT).show();
        }
        setWorking(true);
        executorService.execute(() -> {
            boolean updated = databaseHelper.updateStudyPlan(
                    planId, sessionManager.getUserId(), title,
                    edtDescription.getText().toString().trim(), selectedDate, selectedTime,
                    selectedEndTime, status, category.getCategoryId(), planType,
                    spinnerPriority.getSelectedItemPosition(), duration, reminderActive,
                    reminderMinutes, edtLocation.getText().toString(),
                    edtRoom.getText().toString(), edtSubject.getText().toString(),
                    repeatRule, edtRepeatUntil.getText().toString().trim(),
                    parseDouble(edtWage), chkSubmitted.isChecked());
            if (updated) {
                saveRepeatRule(planId, repeatRule, selectedDate);
            }
            runOnUiThread(() -> {
                if (updated) {
                    if (reminderActive) {
                        boolean scheduled = ReminderScheduler.schedule(this, planId, title, category.getCategoryName(),
                                selectedDate, selectedTime, reminderMinutes);
                        if (scheduled) {
                            long reminderTime = ReminderScheduler.resolveTriggerTimeMillis(
                                    selectedDate, selectedTime, reminderMinutes);
                            databaseHelper.upsertReminder(planId, reminderTime, true);
                        } else {
                            databaseHelper.disableReminder(planId);
                        }
                    } else {
                        ReminderScheduler.cancel(this, planId);
                        databaseHelper.disableReminder(planId);
                    }
                    Toast.makeText(this, R.string.task_updated, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    setWorking(false);
                    Toast.makeText(this, R.string.task_update_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePlan())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePlan() {
        executorService.execute(() -> {
            boolean deleted = databaseHelper.deleteStudyPlan(planId, sessionManager.getUserId());
            runOnUiThread(() -> {
                if (deleted) {
                    ReminderScheduler.cancel(this, planId);
                    Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.task_delete_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveRepeatRule(int planId, String repeatRule, String startDate) {
        String repeatType = RepeatRule.TYPE_NONE;
        String weekDays = "";
        Integer monthDay = null;
        boolean active = repeatRule != null && !"NONE".equals(repeatRule);
        if ("DAILY".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_DAILY;
        } else if ("WEEKLY".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_WEEKLY;
            weekDays = weekDayOf(startDate);
        } else if ("MON_WED_FRI".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_WEEKLY;
            weekDays = "2,4,6";
        } else if ("TUE_THU".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_WEEKLY;
            weekDays = "3,5";
        } else if ("WEEKEND".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_WEEKLY;
            weekDays = "7,1";
        } else if ("MONTHLY".equals(repeatRule)) {
            repeatType = RepeatRule.TYPE_MONTHLY;
            monthDay = monthDayOf(startDate);
        }
        databaseHelper.upsertRepeatRule(planId, repeatType, weekDays, monthDay, active);
    }

    private String weekDayOf(String date) {
        try {
            Calendar selected = Calendar.getInstance();
            selected.setTime(dateFormat.parse(date));
            return String.valueOf(selected.get(Calendar.DAY_OF_WEEK));
        } catch (ParseException ignored) {
            return "";
        }
    }

    private Integer monthDayOf(String date) {
        try {
            Calendar selected = Calendar.getInstance();
            selected.setTime(dateFormat.parse(date));
            return selected.get(Calendar.DAY_OF_MONTH);
        } catch (ParseException ignored) {
            return null;
        }
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

    private boolean ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
        Toast.makeText(this,
                "Hay cap quyen thong bao roi cap nhat lai de kich hoat nhac lich.",
                Toast.LENGTH_LONG).show();
        return false;
    }

    private boolean ensureExactAlarmPermission() {
        if (ReminderScheduler.hasExactAlarmPermission(this)) {
            return true;
        }
        try {
            startActivity(ReminderScheduler.createExactAlarmPermissionIntent(this));
        } catch (Exception ignored) {
            Toast.makeText(this,
                    "Vui long cap quyen bao thuc chinh xac trong cai dat ung dung.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        Toast.makeText(this,
                "Hay cap quyen bao thuc chinh xac roi cap nhat lai.",
                Toast.LENGTH_LONG).show();
        return false;
    }

    private boolean validateReminderSettings(int reminderValue) {
        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            Toast.makeText(this, "Vui long chon ngay", Toast.LENGTH_SHORT).show();
            return false;
        }
        ReminderType reminderType = ReminderType.fromStoredValue(reminderValue);
        try {
            long selectedDateMillis = ReminderScheduler.parseSelectedDateMillis(selectedDate);
            Integer selectedTimeMinutes = null;
            if (!reminderType.isAllDay()) {
                if (selectedTime == null || selectedTime.trim().isEmpty()) {
                    Toast.makeText(this,
                            "Vui long chon gio bao nhac",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                selectedTimeMinutes = ReminderScheduler.parseSelectedTimeMinutes(selectedTime);
            }
            long finalReminderTimeMillis = ReminderScheduler.calculateReminderTimeMillis(
                    selectedDateMillis,
                    selectedTimeMinutes,
                    reminderType
            );
            if (finalReminderTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(this,
                        "Thoi gian bao nhac phai lon hon thoi gian hien tai",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (IllegalArgumentException ignored) {
            Toast.makeText(this,
                    reminderType.isAllDay()
                            ? "Vui long chon ngay"
                            : "Vui long chon ngay va gio bao nhac",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setSelectionByValue(Spinner spinner, int arrayRes, String value) {
        String[] values = getResources().getStringArray(arrayRes);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String valueFromArray(int arrayRes, int position) {
        String[] values = getResources().getStringArray(arrayRes);
        return position >= 0 && position < values.length ? values[position] : values[0];
    }

    private void updateDateTimeLabels() {
        btnChooseDate.setText(getString(R.string.date_value, selectedDate));
        btnChooseTime.setText(getString(R.string.time_value, selectedTime));
        btnChooseEndTime.setText(getString(R.string.end_time_value, selectedEndTime));
    }

    private void setWorking(boolean working) {
        btnUpdateTask.setEnabled(!working);
        btnDeleteTask.setEnabled(!working);
    }

    private void updateTypeFields() {
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

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String valueOrDefault(String input, String fallback) {
        return input == null || input.trim().isEmpty() ? fallback : input;
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
