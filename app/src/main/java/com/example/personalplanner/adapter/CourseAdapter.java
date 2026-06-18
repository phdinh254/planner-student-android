package com.example.personalplanner.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalplanner.R;
import com.example.personalplanner.data.model.Course;

import java.util.ArrayList;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    private final ArrayList<Course> courses = new ArrayList<>();
    private final OnCourseClickListener listener;

    public CourseAdapter(OnCourseClickListener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<Course> newCourses) {
        int oldSize = courses.size();
        courses.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        if (newCourses != null) {
            courses.addAll(newCourses);
            if (!newCourses.isEmpty()) {
                notifyItemRangeInserted(0, newCourses.size());
            }
        }
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.txtCourseName.setText(course.getCourseName());
        holder.txtCourseCode.setText(course.getCourseCode().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.no_course_code)
                : course.getCourseCode());
        holder.txtLecturer.setText(course.getLecturer().trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.no_lecturer)
                : course.getLecturer());
        try {
            holder.viewCourseColor.setBackgroundColor(Color.parseColor(course.getColor()));
        } catch (IllegalArgumentException ignored) {
            holder.viewCourseColor.setBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.primary)
            );
        }
        holder.itemView.setOnClickListener(v -> listener.onCourseClick(course));
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        final View viewCourseColor;
        final TextView txtCourseName;
        final TextView txtCourseCode;
        final TextView txtLecturer;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCourseColor = itemView.findViewById(R.id.viewCourseColor);
            txtCourseName = itemView.findViewById(R.id.txtCourseName);
            txtCourseCode = itemView.findViewById(R.id.txtCourseCode);
            txtLecturer = itemView.findViewById(R.id.txtLecturer);
        }
    }
}
