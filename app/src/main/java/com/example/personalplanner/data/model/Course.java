package com.example.personalplanner.data.model;

public class Course {
    private final int courseId;
    private final String courseName;
    private final String courseCode;
    private final String lecturer;
    private final String color;
    private final int userId;

    public Course(int courseId, String courseName, String courseCode, String lecturer,
                  String color, int userId) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.lecturer = lecturer;
        this.color = color;
        this.userId = userId;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getLecturer() {
        return lecturer;
    }

    public String getColor() {
        return color;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return courseName;
        }
        return courseCode + " - " + courseName;
    }
}
