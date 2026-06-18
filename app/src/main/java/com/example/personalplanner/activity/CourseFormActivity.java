package com.example.personalplanner.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseFormActivity extends AppCompatActivity {
    private static final String[] COLORS =
            {"#1F6F68", "#3568A8", "#8A5A9E", "#C06A3D", "#3C7A57", "#A04A59"};

    private EditText edtCourseName;
    private EditText edtCourseCode;
    private EditText edtLecturer;
    private Spinner spinnerCourseColor;
    private MaterialButton btnSaveCourse;
    private MaterialButton btnDeleteCourse;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int courseId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_form);

        edtCourseName = findViewById(R.id.edtCourseName);
        edtCourseCode = findViewById(R.id.edtCourseCode);
        edtLecturer = findViewById(R.id.edtLecturer);
        spinnerCourseColor = findViewById(R.id.spinnerCourseColor);
        btnSaveCourse = findViewById(R.id.btnSaveCourse);
        btnDeleteCourse = findViewById(R.id.btnDeleteCourse);
        MaterialButton btnBack = findViewById(R.id.btnBack);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.course_color_names,
                android.R.layout.simple_spinner_item
        );
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseColor.setAdapter(colorAdapter);
        readCourse();

        btnSaveCourse.setOnClickListener(v -> saveCourse());
        btnDeleteCourse.setOnClickListener(v -> confirmDelete());
        btnBack.setOnClickListener(v -> finish());
    }

    private void readCourse() {
        courseId = getIntent().getIntExtra("course_id", -1);
        if (courseId == -1) {
            btnDeleteCourse.setVisibility(View.GONE);
            return;
        }
        edtCourseName.setText(getIntent().getStringExtra("course_name"));
        edtCourseCode.setText(getIntent().getStringExtra("course_code"));
        edtLecturer.setText(getIntent().getStringExtra("lecturer"));
        String color = getIntent().getStringExtra("color");
        for (int index = 0; index < COLORS.length; index++) {
            if (COLORS[index].equals(color)) {
                spinnerCourseColor.setSelection(index);
                break;
            }
        }
    }

    private void saveCourse() {
        String name = edtCourseName.getText().toString().trim();
        if (name.isEmpty()) {
            edtCourseName.setError(getString(R.string.error_course_name_required));
            edtCourseName.requestFocus();
            return;
        }
        btnSaveCourse.setEnabled(false);
        int userId = sessionManager.getUserId();
        String code = edtCourseCode.getText().toString().trim();
        String lecturer = edtLecturer.getText().toString().trim();
        String color = COLORS[spinnerCourseColor.getSelectedItemPosition()];
        executorService.execute(() -> {
            boolean success = courseId == -1
                    ? databaseHelper.addCourse(name, code, lecturer, color, userId) != -1
                    : databaseHelper.updateCourse(courseId, name, code, lecturer, color, userId);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, R.string.course_saved, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnSaveCourse.setEnabled(true);
                    Toast.makeText(this, R.string.course_save_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_course)
                .setMessage(R.string.confirm_delete_course)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCourse())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteCourse() {
        executorService.execute(() -> {
            boolean deleted = databaseHelper.deleteCourse(courseId, sessionManager.getUserId());
            runOnUiThread(() -> {
                Toast.makeText(this,
                        deleted ? R.string.course_deleted : R.string.course_delete_failed,
                        Toast.LENGTH_SHORT).show();
                if (deleted) {
                    finish();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
