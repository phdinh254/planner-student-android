package com.example.personalplanner.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskDetailActivity extends AppCompatActivity {

    private EditText edtTitle;
    private EditText edtDescription;
    private Button btnChooseDate;
    private Button btnChooseTime;
    private Button btnUpdateTask;
    private Button btnDeleteTask;
    private CheckBox chkStatus;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int taskId;
    private String selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        btnChooseDate = findViewById(R.id.btnChooseDate);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnUpdateTask = findViewById(R.id.btnUpdateTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        Button btnBack = findViewById(R.id.btnBack);
        chkStatus = findViewById(R.id.chkStatus);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!readTask()) {
            finish();
            return;
        }

        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker());
        btnUpdateTask.setOnClickListener(v -> updateTask());
        btnDeleteTask.setOnClickListener(v -> confirmDeleteTask());
        btnBack.setOnClickListener(v -> finish());
    }

    private boolean readTask() {
        taskId = getIntent().getIntExtra("task_id", -1);
        if (taskId == -1) {
            Toast.makeText(this, R.string.task_not_found, Toast.LENGTH_SHORT).show();
            return false;
        }

        edtTitle.setText(valueOrEmpty(getIntent().getStringExtra("title")));
        edtDescription.setText(valueOrEmpty(getIntent().getStringExtra("description")));
        selectedDate = valueOrDefault(
                getIntent().getStringExtra("date"),
                dateFormat.format(calendar.getTime())
        );
        selectedTime = valueOrDefault(
                getIntent().getStringExtra("time"),
                timeFormat.format(calendar.getTime())
        );
        chkStatus.setChecked(getIntent().getIntExtra("status", 0) == 1);
        syncCalendar();
        updateDateTimeLabels();
        return true;
    }

    private void syncCalendar() {
        try {
            calendar.setTime(dateFormat.parse(selectedDate));
            String[] timeParts = selectedTime.split(":");
            if (timeParts.length == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            }
        } catch (ParseException | NumberFormatException ignored) {
            selectedDate = dateFormat.format(calendar.getTime());
            selectedTime = timeFormat.format(calendar.getTime());
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    selectedDate = dateFormat.format(calendar.getTime());
                    updateDateTimeLabels();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    selectedTime = timeFormat.format(calendar.getTime());
                    updateDateTimeLabels();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void updateTask() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            edtTitle.requestFocus();
            return;
        }

        int userId = requireUserId();
        if (userId == -1) {
            return;
        }
        String description = edtDescription.getText().toString().trim();
        int newStatus = chkStatus.isChecked() ? 1 : 0;
        setWorking(true);
        executorService.execute(() -> {
            boolean updated = databaseHelper.updateTask(
                    taskId,
                    userId,
                    title,
                    description,
                    selectedDate,
                    selectedTime,
                    newStatus
            );
            runOnUiThread(() -> handleResult(
                    updated,
                    R.string.task_updated,
                    R.string.task_update_failed
            ));
        });
    }

    private void confirmDeleteTask() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTask())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteTask() {
        int userId = requireUserId();
        if (userId == -1) {
            return;
        }
        setWorking(true);
        executorService.execute(() -> {
            boolean deleted = databaseHelper.deleteTask(taskId, userId);
            runOnUiThread(() -> handleResult(
                    deleted,
                    R.string.task_deleted,
                    R.string.task_delete_failed
            ));
        });
    }

    private int requireUserId() {
        int userId = sessionManager.getUserId();
        if (userId == -1) {
            Toast.makeText(this, R.string.invalid_session, Toast.LENGTH_SHORT).show();
            finish();
        }
        return userId;
    }

    private void handleResult(boolean success, int successMessage, int errorMessage) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, success ? successMessage : errorMessage, Toast.LENGTH_SHORT).show();
        if (success) {
            finish();
        } else {
            setWorking(false);
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
