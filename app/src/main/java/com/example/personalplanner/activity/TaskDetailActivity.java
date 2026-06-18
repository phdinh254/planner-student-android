package com.example.personalplanner.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.Course;
import com.example.personalplanner.notification.ReminderScheduler;
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
    private EditText edtTitle;
    private EditText edtDescription;
    private EditText edtDuration;
    private Button btnChooseDate;
    private Button btnChooseTime;
    private Button btnUpdateTask;
    private Button btnDeleteTask;
    private CheckBox chkStatus;
    private Spinner spinnerCourse;
    private Spinner spinnerPriority;
    private SwitchMaterial switchReminder;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ArrayList<Course> courses = new ArrayList<>();
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int planId;
    private int selectedCourseId;
    private String selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDuration = findViewById(R.id.edtDuration);
        btnChooseDate = findViewById(R.id.btnChooseDate);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnUpdateTask = findViewById(R.id.btnUpdateTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        chkStatus = findViewById(R.id.chkStatus);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        switchReminder = findViewById(R.id.switchReminder);
        Button btnBack = findViewById(R.id.btnBack);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_names, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        if (!readPlan()) {
            finish();
            return;
        }
        loadCourses();
        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker());
        btnUpdateTask.setOnClickListener(v -> updatePlan());
        btnDeleteTask.setOnClickListener(v -> confirmDelete());
        btnBack.setOnClickListener(v -> finish());
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
        selectedDate = valueOrDefault(getIntent().getStringExtra("date"),
                dateFormat.format(calendar.getTime()));
        selectedTime = valueOrDefault(getIntent().getStringExtra("time"),
                timeFormat.format(calendar.getTime()));
        selectedCourseId = getIntent().getIntExtra("course_id", 0);
        spinnerPriority.setSelection(getIntent().getIntExtra("priority", 1));
        switchReminder.setChecked(getIntent().getBooleanExtra("reminder_enabled", false));
        chkStatus.setChecked(getIntent().getIntExtra("status", 0) == 1);
        syncCalendar();
        updateDateTimeLabels();
        return true;
    }

    private void loadCourses() {
        executorService.execute(() -> {
            ArrayList<Course> result = databaseHelper.getCourses(sessionManager.getUserId());
            result.add(0, new Course(0, getString(R.string.uncategorized_course),
                    "", "", "#607D8B", sessionManager.getUserId()));
            runOnUiThread(() -> {
                courses.clear();
                courses.addAll(result);
                ArrayAdapter<Course> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, courses);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCourse.setAdapter(adapter);
                for (int index = 0; index < courses.size(); index++) {
                    if (courses.get(index).getCourseId() == selectedCourseId) {
                        spinnerCourse.setSelection(index);
                        break;
                    }
                }
            });
        });
    }

    private void syncCalendar() {
        try {
            calendar.setTime(dateFormat.parse(selectedDate));
            String[] parts = selectedTime.split(":");
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        } catch (ParseException | NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            selectedDate = dateFormat.format(calendar.getTime());
            selectedTime = timeFormat.format(calendar.getTime());
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            selectedDate = dateFormat.format(calendar.getTime());
            updateDateTimeLabels();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            selectedTime = timeFormat.format(calendar.getTime());
            updateDateTimeLabels();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updatePlan() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            return;
        }
        int duration = parseDuration();
        if (duration <= 0 || courses.isEmpty()) {
            edtDuration.setError(getString(R.string.error_duration));
            return;
        }
        Course course = courses.get(spinnerCourse.getSelectedItemPosition());
        String description = edtDescription.getText().toString().trim();
        int priority = spinnerPriority.getSelectedItemPosition();
        boolean reminder = switchReminder.isChecked();
        int status = chkStatus.isChecked() ? 1 : 0;
        setWorking(true);
        executorService.execute(() -> {
            boolean updated = databaseHelper.updateStudyPlan(
                    planId, sessionManager.getUserId(), title, description,
                    selectedDate, selectedTime, status, course.getCourseId(),
                    priority, duration, reminder);
            runOnUiThread(() -> {
                if (updated) {
                    if (reminder && status == DatabaseHelper.STATUS_PENDING) {
                        ReminderScheduler.schedule(this, planId, title, course.getCourseName(),
                                selectedDate, selectedTime);
                    } else {
                        ReminderScheduler.cancel(this, planId);
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

    private int parseDuration() {
        try {
            return Integer.parseInt(edtDuration.getText().toString().trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void updateDateTimeLabels() {
        btnChooseDate.setText(getString(R.string.date_value, selectedDate));
        btnChooseTime.setText(getString(R.string.time_value, selectedTime));
    }

    private void setWorking(boolean working) {
        btnUpdateTask.setEnabled(!working);
        btnDeleteTask.setEnabled(!working);
    }

    private String value(String input) {
        return input == null ? "" : input;
    }

    private String valueOrDefault(String input, String fallback) {
        return input == null || input.trim().isEmpty() ? fallback : input;
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
