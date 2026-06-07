package com.example.personalplanner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.personalplanner.model.Task;
import com.example.personalplanner.model.User;
import com.example.personalplanner.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "personal_planner.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_CATEGORIES = "categories";

    public static final int FILTER_ALL = -1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_COMPLETED = 1;

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

        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_name TEXT NOT NULL, " +
                "user_id INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_TASKS + " (" +
                "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "date TEXT NOT NULL, " +
                "time TEXT NOT NULL, " +
                "status INTEGER NOT NULL DEFAULT 0 CHECK(status IN (0, 1)), " +
                "category_id INTEGER DEFAULT 0, " +
                "user_id INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE)");

        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Keep existing user data and only add indexes for common list/search queries.
            createIndexes(db);
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_user_date ON " +
                TABLE_TASKS + "(user_id, date, time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_user_status ON " +
                TABLE_TASKS + "(user_id, status)");
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
        return valueExists("username", username.trim());
    }

    public boolean checkEmailExists(String email) {
        return valueExists("email", email.trim());
    }

    private boolean valueExists(String column, String value) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_USERS,
                     new String[]{"user_id"},
                     column + " = ? COLLATE NOCASE",
                     new String[]{value},
                     null,
                     null,
                     null,
                     "1"
             )) {
            return cursor.moveToFirst();
        }
    }

    public int loginUser(String username, String password) {
        String hashedPassword = PasswordUtils.hashPassword(password);

        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_USERS,
                     new String[]{"user_id"},
                     "username = ? COLLATE NOCASE AND password = ?",
                     new String[]{username.trim(), hashedPassword},
                     null,
                     null,
                     null,
                     "1"
             )) {
            return cursor.moveToFirst()
                    ? cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
                    : -1;
        }
    }

    public User getUser(int userId) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_USERS,
                     new String[]{"user_id", "username", "email"},
                     "user_id = ?",
                     new String[]{String.valueOf(userId)},
                     null,
                     null,
                     null,
                     "1"
             )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new User(
                    cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    ""
            );
        }
    }

    public boolean addTask(String title, String description, String date, String time,
                           int categoryId, int userId) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("description", description.trim());
        values.put("date", date);
        values.put("time", time);
        values.put("status", STATUS_PENDING);
        values.put("category_id", categoryId);
        values.put("user_id", userId);

        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_TASKS, null, values) != -1;
        }
    }

    public ArrayList<Task> getAllTasks(int userId) {
        return getTasks(userId, "", FILTER_ALL);
    }

    public ArrayList<Task> getTasks(int userId, String keyword, int statusFilter) {
        ArrayList<Task> tasks = new ArrayList<>();
        StringBuilder selection = new StringBuilder("user_id = ?");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        if (keyword != null && !keyword.trim().isEmpty()) {
            selection.append(" AND (title LIKE ? OR description LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
        }

        if (statusFilter == STATUS_PENDING || statusFilter == STATUS_COMPLETED) {
            selection.append(" AND status = ?");
            args.add(String.valueOf(statusFilter));
        }

        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_TASKS,
                     null,
                     selection.toString(),
                     args.toArray(new String[0]),
                     null,
                     null,
                     "date ASC, time ASC"
             )) {
            while (cursor.moveToNext()) {
                tasks.add(mapCursorToTask(cursor));
            }
        }
        return tasks;
    }

    public ArrayList<Task> getTasksByDate(int userId, String date) {
        ArrayList<Task> tasks = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_TASKS,
                     null,
                     "user_id = ? AND date = ?",
                     new String[]{String.valueOf(userId), date},
                     null,
                     null,
                     "time ASC"
             )) {
            while (cursor.moveToNext()) {
                tasks.add(mapCursorToTask(cursor));
            }
        }
        return tasks;
    }

    public boolean updateTask(int taskId, int userId, String title, String description,
                              String date, String time, int status) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("description", description.trim());
        values.put("date", date);
        values.put("time", time);
        values.put("status", status);

        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(
                    TABLE_TASKS,
                    values,
                    "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(taskId), String.valueOf(userId)}
            ) > 0;
        }
    }

    public boolean deleteTask(int taskId, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.delete(
                    TABLE_TASKS,
                    "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(taskId), String.valueOf(userId)}
            ) > 0;
        }
    }

    public boolean updateTaskStatus(int taskId, int userId, int status) {
        ContentValues values = new ContentValues();
        values.put("status", status);

        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(
                    TABLE_TASKS,
                    values,
                    "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(taskId), String.valueOf(userId)}
            ) > 0;
        }
    }

    private Task mapCursorToTask(Cursor cursor) {
        return new Task(
                cursor.getInt(cursor.getColumnIndexOrThrow("task_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("status")),
                cursor.getInt(cursor.getColumnIndexOrThrow("category_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
        );
    }
}
