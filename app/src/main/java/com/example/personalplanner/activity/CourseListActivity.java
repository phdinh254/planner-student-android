package com.example.personalplanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.adapter.CourseAdapter;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.Course;
import com.example.personalplanner.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseListActivity extends AppCompatActivity {
    private CourseAdapter adapter;
    private TextView txtEmptyCourses;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);

        RecyclerView recyclerCourses = findViewById(R.id.recyclerCourses);
        txtEmptyCourses = findViewById(R.id.txtEmptyCourses);
        MaterialButton btnAddCourse = findViewById(R.id.btnAddCourse);
        MaterialButton btnBack = findViewById(R.id.btnBack);
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        adapter = new CourseAdapter(this::openCourse);
        recyclerCourses.setLayoutManager(new LinearLayoutManager(this));
        recyclerCourses.setAdapter(adapter);

        btnAddCourse.setOnClickListener(v ->
                startActivity(new Intent(this, CourseFormActivity.class)));
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        executorService.execute(() -> {
            ArrayList<Course> courses = databaseHelper.getCourses(sessionManager.getUserId());
            runOnUiThread(() -> {
                adapter.setData(courses);
                txtEmptyCourses.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void openCourse(Course course) {
        Intent intent = new Intent(this, CourseFormActivity.class);
        intent.putExtra("course_id", course.getCourseId());
        intent.putExtra("course_name", course.getCourseName());
        intent.putExtra("course_code", course.getCourseCode());
        intent.putExtra("lecturer", course.getLecturer());
        intent.putExtra("color", course.getColor());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
