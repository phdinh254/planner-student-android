package com.example.personalplanner.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskActivity extends AppCompatActivity {

    private EditText edtTitle;
    private EditText edtDescription;
    private Button btnChooseDate;
    private Button btnChooseTime;
    private Button btnSaveTask;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

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
        btnChooseDate = findViewById(R.id.btnChooseDate);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        Button btnCancel = findViewById(R.id.btnCancel);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        selectedDate = dateFormat.format(calendar.getTime());
        selectedTime = timeFormat.format(calendar.getTime());
        updateDateTimeLabels();

        btnChooseDate.setOnClickListener(v -> showDatePicker());
        btnChooseTime.setOnClickListener(v -> showTimePicker());
        btnSaveTask.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> finish());
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

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_task_title_required));
            edtTitle.requestFocus();
            return;
        }

        int userId = sessionManager.getUserId();
        if (userId == -1) {
            Toast.makeText(this, R.string.invalid_session, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSaving(true);
        executorService.execute(() -> {
            boolean added = databaseHelper.addTask(
                    title,
                    description,
                    selectedDate,
                    selectedTime,
                    0,
                    userId
            );
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (added) {
                    Toast.makeText(this, R.string.task_added, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    setSaving(false);
                    Toast.makeText(this, R.string.task_add_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateDateTimeLabels() {
        btnChooseDate.setText(getString(R.string.date_value, selectedDate));
        btnChooseTime.setText(getString(R.string.time_value, selectedTime));
    }

    private void setSaving(boolean saving) {
        btnSaveTask.setEnabled(!saving);
        btnSaveTask.setText(saving ? R.string.saving : R.string.save_task);
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
