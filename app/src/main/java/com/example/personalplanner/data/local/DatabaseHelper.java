package com.example.personalplanner.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.personalplanner.data.model.Course;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.StudyStatistics;
import com.example.personalplanner.data.model.User;
import com.example.personalplanner.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "personal_planner.db";
    private static final int DATABASE_VERSION = 3;

    public static final int FILTER_ALL = -1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_COMPLETED = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_PLANS = "tasks";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                "email TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                "password TEXT NOT NULL, " +
                "created_at TEXT NOT NULL)");

        createCoursesTable(db);
        createPlansTable(db);
        createIndexes(db);
    }

    private void createCoursesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COURSES + " (" +
                "course_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_name TEXT NOT NULL, " +
                "course_code TEXT, " +
                "lecturer TEXT, " +
                "color TEXT NOT NULL DEFAULT '#1F6F68', " +
                "user_id INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE)");
    }

    private void createPlansTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PLANS + " (" +
                "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "date TEXT NOT NULL, " +
                "time TEXT NOT NULL, " +
                "status INTEGER NOT NULL DEFAULT 0 CHECK(status IN (0, 1)), " +
                "course_id INTEGER DEFAULT 0, " +
                "priority INTEGER NOT NULL DEFAULT 1 CHECK(priority BETWEEN 0 AND 2), " +
                "duration_minutes INTEGER NOT NULL DEFAULT 60, " +
                "reminder_enabled INTEGER NOT NULL DEFAULT 0, " +
                "user_id INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            createCoursesTable(db);
            addColumnIfMissing(db, TABLE_PLANS, "course_id", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, TABLE_PLANS, "priority", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfMissing(db, TABLE_PLANS, "duration_minutes", "INTEGER NOT NULL DEFAULT 60");
            addColumnIfMissing(db, TABLE_PLANS, "reminder_enabled", "INTEGER NOT NULL DEFAULT 0");

            if (tableExists(db, "categories")) {
                db.execSQL("INSERT INTO " + TABLE_COURSES +
                        "(course_id, course_name, course_code, lecturer, color, user_id) " +
                        "SELECT category_id, category_name, '', '', '#1F6F68', user_id FROM categories " +
                        "WHERE user_id IS NOT NULL");
                if (columnExists(db, TABLE_PLANS, "category_id")) {
                    db.execSQL("UPDATE " + TABLE_PLANS +
                            " SET course_id = category_id WHERE category_id > 0");
                }
            }
        }
        createIndexes(db);
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column,
                                    String definition) {
        if (!columnExists(db, table, column)) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean tableExists(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private boolean columnExists(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_user_date ON " +
                TABLE_PLANS + "(user_id, date, time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_user_status ON " +
                TABLE_PLANS + "(user_id, status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_course ON " +
                TABLE_PLANS + "(course_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_courses_user ON " +
                TABLE_COURSES + "(user_id, course_name)");
    }

    public boolean registerUser(String username, String email, String password) {
        ContentValues values = new ContentValues();
        values.put("username", username.trim());
        values.put("email", email.trim().toLowerCase(Locale.ROOT));
        values.put("password", PasswordUtils.hashPassword(password));
        values.put("created_at", String.valueOf(System.currentTimeMillis()));
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_USERS, null, values) != -1;
        }
    }

    public boolean checkUsernameExists(String username) {
        return userValueExists("username", username.trim());
    }

    public boolean checkEmailExists(String email) {
        return userValueExists("email", email.trim());
    }

    private boolean userValueExists(String column, String value) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_USERS, new String[]{"user_id"},
                     column + " = ? COLLATE NOCASE", new String[]{value},
                     null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public int loginUser(String username, String password) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_USERS, new String[]{"user_id"},
                     "username = ? COLLATE NOCASE AND password = ?",
                     new String[]{username.trim(), PasswordUtils.hashPassword(password)},
                     null, null, null, "1")) {
            return cursor.moveToFirst()
                    ? cursor.getInt(cursor.getColumnIndexOrThrow("user_id")) : -1;
        }
    }

    public User getUser(int userId) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_USERS,
                     new String[]{"user_id", "username", "email"},
                     "user_id = ?", new String[]{String.valueOf(userId)},
                     null, null, null, "1")) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new User(
                    cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email"))
            );
        }
    }

    public long addCourse(String name, String code, String lecturer, String color, int userId) {
        ContentValues values = courseValues(name, code, lecturer, color, userId);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_COURSES, null, values);
        }
    }

    public boolean updateCourse(int courseId, String name, String code, String lecturer,
                                String color, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_COURSES,
                    courseValues(name, code, lecturer, color, userId),
                    "course_id = ? AND user_id = ?",
                    new String[]{String.valueOf(courseId), String.valueOf(userId)}) > 0;
        }
    }

    public boolean deleteCourse(int courseId, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("course_id", 0);
                db.update(TABLE_PLANS, values, "course_id = ? AND user_id = ?",
                        new String[]{String.valueOf(courseId), String.valueOf(userId)});
                boolean deleted = db.delete(TABLE_COURSES,
                        "course_id = ? AND user_id = ?",
                        new String[]{String.valueOf(courseId), String.valueOf(userId)}) > 0;
                db.setTransactionSuccessful();
                return deleted;
            } finally {
                db.endTransaction();
            }
        }
    }

    private ContentValues courseValues(String name, String code, String lecturer,
                                       String color, int userId) {
        ContentValues values = new ContentValues();
        values.put("course_name", name.trim());
        values.put("course_code", code.trim());
        values.put("lecturer", lecturer.trim());
        values.put("color", color);
        values.put("user_id", userId);
        return values;
    }

    public ArrayList<Course> getCourses(int userId) {
        ArrayList<Course> courses = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_COURSES, null, "user_id = ?",
                     new String[]{String.valueOf(userId)}, null, null,
                     "course_name COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                courses.add(mapCourse(cursor));
            }
        }
        return courses;
    }

    public long addStudyPlan(String title, String description, String date, String time,
                             int courseId, int priority, int durationMinutes,
                             boolean reminderEnabled, int userId) {
        ContentValues values = planValues(title, description, date, time, courseId,
                priority, durationMinutes, reminderEnabled);
        values.put("status", STATUS_PENDING);
        values.put("user_id", userId);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_PLANS, null, values);
        }
    }

    public boolean updateStudyPlan(int planId, int userId, String title, String description,
                                   String date, String time, int status, int courseId,
                                   int priority, int durationMinutes, boolean reminderEnabled) {
        ContentValues values = planValues(title, description, date, time, courseId,
                priority, durationMinutes, reminderEnabled);
        values.put("status", status);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_PLANS, values, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
        }
    }

    private ContentValues planValues(String title, String description, String date, String time,
                                     int courseId, int priority, int durationMinutes,
                                     boolean reminderEnabled) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("description", description.trim());
        values.put("date", date);
        values.put("time", time);
        values.put("course_id", courseId);
        values.put("priority", priority);
        values.put("duration_minutes", durationMinutes);
        values.put("reminder_enabled", reminderEnabled ? 1 : 0);
        return values;
    }

    public ArrayList<StudyPlan> getStudyPlans(int userId, String keyword, int statusFilter,
                                               int courseFilter) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        StringBuilder selection = new StringBuilder("p.user_id = ?");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        if (keyword != null && !keyword.trim().isEmpty()) {
            selection.append(" AND (p.title LIKE ? OR p.description LIKE ? OR c.course_name LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (statusFilter == STATUS_PENDING || statusFilter == STATUS_COMPLETED) {
            selection.append(" AND p.status = ?");
            args.add(String.valueOf(statusFilter));
        }
        if (courseFilter > 0) {
            selection.append(" AND p.course_id = ?");
            args.add(String.valueOf(courseFilter));
        }

        String sql = "SELECT p.*, COALESCE(c.course_name, '') AS course_name FROM " +
                TABLE_PLANS + " p LEFT JOIN " + TABLE_COURSES +
                " c ON p.course_id = c.course_id AND p.user_id = c.user_id WHERE " +
                selection + " ORDER BY p.date ASC, p.time ASC";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getAllStudyPlans(int userId) {
        return getStudyPlans(userId, "", FILTER_ALL, 0);
    }

    public ArrayList<StudyPlan> getPendingReminderPlans() {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(c.course_name, '') AS course_name FROM " +
                TABLE_PLANS + " p LEFT JOIN " + TABLE_COURSES +
                " c ON p.course_id = c.course_id AND p.user_id = c.user_id " +
                "WHERE p.status = 0 AND p.reminder_enabled = 1 ORDER BY p.date, p.time";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getStudyPlansByDate(int userId, String date) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(c.course_name, '') AS course_name FROM " +
                TABLE_PLANS + " p LEFT JOIN " + TABLE_COURSES +
                " c ON p.course_id = c.course_id AND p.user_id = c.user_id " +
                "WHERE p.user_id = ? AND p.date = ? ORDER BY p.time ASC";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql,
                     new String[]{String.valueOf(userId), date})) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public boolean deleteStudyPlan(int planId, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.delete(TABLE_PLANS, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
        }
    }

    public boolean updateStudyPlanStatus(int planId, int userId, int status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_PLANS, values, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
        }
    }

    public StudyStatistics getStudyStatistics(int userId) {
        int total = 0;
        int completed = 0;
        int minutes = 0;
        int courses = 0;
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor planCursor = db.rawQuery(
                     "SELECT COUNT(*) total, SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) completed," +
                             " COALESCE(SUM(duration_minutes), 0) minutes FROM " + TABLE_PLANS +
                             " WHERE user_id = ?", new String[]{String.valueOf(userId)});
             Cursor courseCursor = db.rawQuery(
                     "SELECT COUNT(*) FROM " + TABLE_COURSES + " WHERE user_id = ?",
                     new String[]{String.valueOf(userId)})) {
            if (planCursor.moveToFirst()) {
                total = planCursor.getInt(0);
                completed = planCursor.getInt(1);
                minutes = planCursor.getInt(2);
            }
            if (courseCursor.moveToFirst()) {
                courses = courseCursor.getInt(0);
            }
        }
        return new StudyStatistics(total, completed, total - completed, courses, minutes);
    }

    private Course mapCourse(Cursor cursor) {
        return new Course(
                cursor.getInt(cursor.getColumnIndexOrThrow("course_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("course_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("course_code")),
                cursor.getString(cursor.getColumnIndexOrThrow("lecturer")),
                cursor.getString(cursor.getColumnIndexOrThrow("color")),
                cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
        );
    }

    private StudyPlan mapStudyPlan(Cursor cursor) {
        return new StudyPlan(
                cursor.getInt(cursor.getColumnIndexOrThrow("task_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("status")),
                cursor.getInt(cursor.getColumnIndexOrThrow("course_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("course_name")),
                cursor.getInt(cursor.getColumnIndexOrThrow("priority")),
                cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes")),
                cursor.getInt(cursor.getColumnIndexOrThrow("reminder_enabled")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
        );
    }
}
