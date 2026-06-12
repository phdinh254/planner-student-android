package com.example.personalplanner.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.Course;
import com.example.personalplanner.notification.ReminderScheduler;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private Button btnChooseDate;
    private Button btnChooseTime;
    private Button btnSaveTask;
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
    private String selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDuration = findViewById(R.id.edtDuration);
        btnChooseDate = findViewById(R.id.btnChooseDate);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        switchReminder = findViewById(R.id.switchReminder);
        Button btnManageCourses = findViewById(R.id.btnManageCourses);
        Button btnCancel = findViewById(R.id.btnCancel);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_names, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1);

        selectedDate = dateFormat.format(calendar.getTime());
        selectedTime = timeFormat.format(calendar.getTime());
        updateDateTimeLabels();

        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker());
        btnSaveTask.setOnClickListener(v -> savePlan());
        btnManageCourses.setOnClickListener(v ->
                startActivity(new Intent(this, CourseListActivity.class)));
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
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

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            selectedTime = timeFormat.format(calendar.getTime());
            updateDateTimeLabels();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void savePlan() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            edtTitle.requestFocus();
            return;
        }
        int duration = parseDuration();
        if (duration <= 0) {
            edtDuration.setError(getString(R.string.error_duration));
            edtDuration.requestFocus();
            return;
        }
        if (courses.isEmpty()) {
            Toast.makeText(this, R.string.course_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = sessionManager.getUserId();
        Course course = courses.get(spinnerCourse.getSelectedItemPosition());
        String description = edtDescription.getText().toString().trim();
        int priority = spinnerPriority.getSelectedItemPosition();
        boolean reminderEnabled = switchReminder.isChecked();
        setSaving(true);
        executorService.execute(() -> {
            long planId = databaseHelper.addStudyPlan(
                    title, description, selectedDate, selectedTime, course.getCourseId(),
                    priority, duration, reminderEnabled, userId);
            runOnUiThread(() -> {
                if (planId != -1) {
                    if (reminderEnabled) {
                        ReminderScheduler.schedule(this, (int) planId, title,
                                course.getCourseName(), selectedDate, selectedTime);
                    }
                    Toast.makeText(this, R.string.task_added, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    setSaving(false);
                    Toast.makeText(this, R.string.task_add_failed, Toast.LENGTH_SHORT).show();
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

    private void setSaving(boolean saving) {
        btnSaveTask.setEnabled(!saving);
        btnSaveTask.setText(saving ? R.string.saving : R.string.save_study_plan);
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
