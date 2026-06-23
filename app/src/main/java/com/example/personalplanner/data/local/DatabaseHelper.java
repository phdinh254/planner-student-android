package com.example.personalplanner.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.personalplanner.data.model.PlanCategory;
import com.example.personalplanner.data.model.PlanEvaluation;
import com.example.personalplanner.data.model.PlanRangeStats;
import com.example.personalplanner.data.model.PlanReminder;
import com.example.personalplanner.data.model.RepeatRule;
import com.example.personalplanner.data.model.StudyPlan;
import com.example.personalplanner.data.model.StudyStatistics;
import com.example.personalplanner.data.model.SubTask;
import com.example.personalplanner.data.model.User;
import com.example.personalplanner.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "personal_planner.db";
    private static final int DATABASE_VERSION = 5;

    public static final int FILTER_ALL = -1;
    public static final int STATUS_UPCOMING = StudyPlan.STATUS_UPCOMING;
    public static final int STATUS_IN_PROGRESS = StudyPlan.STATUS_IN_PROGRESS;
    public static final int STATUS_COMPLETED = StudyPlan.STATUS_COMPLETED;
    public static final int STATUS_CANCELLED = StudyPlan.STATUS_CANCELLED;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_CATEGORIES = "plan_categories";
    private static final String TABLE_PLANS = "tasks";
    private static final String TABLE_SUB_TASKS = "sub_tasks";
    private static final String TABLE_REMINDERS = "reminders";
    private static final String TABLE_REPEAT_RULES = "repeat_rules";
    private static final String TABLE_PLAN_EVALUATIONS = "plan_evaluations";

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
        createCategoriesTable(db);
        createPlansTable(db);
        createSubTasksTable(db);
        createRemindersTable(db);
        createRepeatRulesTable(db);
        createPlanEvaluationsTable(db);
        createIndexes(db);
    }

    private void createCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_name TEXT NOT NULL, " +
                "category_code TEXT, " +
                "note TEXT, " +
                "color TEXT NOT NULL DEFAULT '#1F6F68', " +
                "created_at TEXT NOT NULL DEFAULT '', " +
                "updated_at TEXT NOT NULL DEFAULT '', " +
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
                "end_time TEXT, " +
                "status INTEGER NOT NULL DEFAULT 0 CHECK(status BETWEEN 0 AND 3), " +
                "category_id INTEGER DEFAULT 0, " +
                "plan_type TEXT NOT NULL DEFAULT 'PERSONAL', " +
                "priority INTEGER NOT NULL DEFAULT 1 CHECK(priority BETWEEN 0 AND 2), " +
                "duration_minutes INTEGER NOT NULL DEFAULT 60, " +
                "reminder_enabled INTEGER NOT NULL DEFAULT 0, " +
                "reminder_minutes INTEGER NOT NULL DEFAULT 0, " +
                "location TEXT, " +
                "room TEXT, " +
                "subject TEXT, " +
                "repeat_rule TEXT NOT NULL DEFAULT 'NONE', " +
                "repeat_until TEXT, " +
                "wage REAL NOT NULL DEFAULT 0, " +
                "submitted INTEGER NOT NULL DEFAULT 0, " +
                "start_time TEXT, " +
                "deadline TEXT, " +
                "created_at TEXT NOT NULL DEFAULT '', " +
                "updated_at TEXT NOT NULL DEFAULT '', " +
                "user_id INTEGER NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE)");
    }

    private void createSubTasksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUB_TASKS + " (" +
                "sub_task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "plan_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "is_completed INTEGER NOT NULL DEFAULT 0, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "FOREIGN KEY(plan_id) REFERENCES " + TABLE_PLANS + "(task_id) ON DELETE CASCADE)");
    }

    private void createRemindersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REMINDERS + " (" +
                "reminder_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "plan_id INTEGER NOT NULL UNIQUE, " +
                "reminder_time INTEGER NOT NULL, " +
                "is_enabled INTEGER NOT NULL DEFAULT 1, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "FOREIGN KEY(plan_id) REFERENCES " + TABLE_PLANS + "(task_id) ON DELETE CASCADE)");
    }

    private void createRepeatRulesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REPEAT_RULES + " (" +
                "repeat_rule_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "plan_id INTEGER NOT NULL UNIQUE, " +
                "repeat_type TEXT NOT NULL DEFAULT 'NONE', " +
                "week_days TEXT, " +
                "month_day INTEGER, " +
                "is_active INTEGER NOT NULL DEFAULT 0, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "FOREIGN KEY(plan_id) REFERENCES " + TABLE_PLANS + "(task_id) ON DELETE CASCADE)");
    }

    private void createPlanEvaluationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAN_EVALUATIONS + " (" +
                "evaluation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "plan_id INTEGER NOT NULL UNIQUE, " +
                "satisfaction_level TEXT NOT NULL, " +
                "result_note TEXT, " +
                "delay_reason TEXT, " +
                "completed_at TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "FOREIGN KEY(plan_id) REFERENCES " + TABLE_PLANS + "(task_id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            createLegacyCoursesTable(db);
            addColumnIfMissing(db, TABLE_PLANS, "course_id", "INTEGER DEFAULT 0");
            addColumnIfMissing(db, TABLE_PLANS, "priority", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfMissing(db, TABLE_PLANS, "duration_minutes", "INTEGER NOT NULL DEFAULT 60");
            addColumnIfMissing(db, TABLE_PLANS, "reminder_enabled", "INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            createCategoriesTable(db);
            migrateCoursesToCategories(db);
            addColumnIfMissing(db, TABLE_PLANS, "category_id", "INTEGER DEFAULT 0");
            if (columnExists(db, TABLE_PLANS, "course_id")) {
                db.execSQL("UPDATE " + TABLE_PLANS +
                        " SET category_id = course_id WHERE category_id = 0 AND course_id > 0");
            }
            addColumnIfMissing(db, TABLE_PLANS, "end_time", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "plan_type", "TEXT NOT NULL DEFAULT 'PERSONAL'");
            addColumnIfMissing(db, TABLE_PLANS, "reminder_minutes", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, TABLE_PLANS, "location", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "room", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "subject", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "repeat_rule", "TEXT NOT NULL DEFAULT 'NONE'");
            addColumnIfMissing(db, TABLE_PLANS, "repeat_until", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "wage", "REAL NOT NULL DEFAULT 0");
            addColumnIfMissing(db, TABLE_PLANS, "submitted", "INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 5) {
            addColumnIfMissing(db, TABLE_CATEGORIES, "created_at", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, TABLE_CATEGORIES, "updated_at", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, TABLE_PLANS, "start_time", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "deadline", "TEXT");
            addColumnIfMissing(db, TABLE_PLANS, "created_at", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, TABLE_PLANS, "updated_at", "TEXT NOT NULL DEFAULT ''");
            db.execSQL("UPDATE " + TABLE_PLANS +
                    " SET start_time = date || ' ' || time " +
                    "WHERE start_time IS NULL OR start_time = ''");
            db.execSQL("UPDATE " + TABLE_PLANS +
                    " SET deadline = date || ' ' || COALESCE(NULLIF(end_time, ''), time) " +
                    "WHERE deadline IS NULL OR deadline = ''");
            createSubTasksTable(db);
            createRemindersTable(db);
            createRepeatRulesTable(db);
            createPlanEvaluationsTable(db);
        }
        createIndexes(db);
    }

    private void createLegacyCoursesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS courses (" +
                "course_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "course_name TEXT NOT NULL, " +
                "course_code TEXT, " +
                "lecturer TEXT, " +
                "color TEXT NOT NULL DEFAULT '#1F6F68', " +
                "user_id INTEGER NOT NULL)");
    }

    private void migrateCoursesToCategories(SQLiteDatabase db) {
        if (!tableExists(db, "courses")) {
            return;
        }
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_CATEGORIES +
                "(category_id, category_name, category_code, note, color, user_id) " +
                "SELECT course_id, course_name, course_code, lecturer, color, user_id FROM courses");
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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_category ON " +
                TABLE_PLANS + "(category_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plans_type ON " +
                TABLE_PLANS + "(user_id, plan_type)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_categories_user ON " +
                TABLE_CATEGORIES + "(user_id, category_name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sub_tasks_plan ON " +
                TABLE_SUB_TASKS + "(plan_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_plan ON " +
                TABLE_REMINDERS + "(plan_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_time ON " +
                TABLE_REMINDERS + "(is_enabled, reminder_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_repeat_rules_plan ON " +
                TABLE_REPEAT_RULES + "(plan_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plan_evaluations_plan ON " +
                TABLE_PLAN_EVALUATIONS + "(plan_id)");
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
             Cursor cursor = db.query(TABLE_USERS, new String[]{"user_id", "password"},
                     "username = ? COLLATE NOCASE",
                     new String[]{username.trim()},
                     null, null, null, "1")) {
            if (!cursor.moveToFirst()) {
                return -1;
            }
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            if (!PasswordUtils.verifyPassword(password, storedHash)) {
                return -1;
            }
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
            if (PasswordUtils.needsRehash(storedHash)) {
                upgradePasswordHash(userId, password);
            }
            return userId;
        }
    }

    private void upgradePasswordHash(int userId, String password) {
        ContentValues values = new ContentValues();
        values.put("password", PasswordUtils.hashPassword(password));
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.update(TABLE_USERS, values, "user_id = ?",
                    new String[]{String.valueOf(userId)});
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

    public long addCategory(String name, String code, String note, String color, int userId) {
        ContentValues values = categoryValues(name, code, note, color, userId);
        String now = nowString();
        values.put("created_at", now);
        values.put("updated_at", now);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    public boolean updateCategory(int categoryId, String name, String code, String note,
                                  String color, int userId) {
        ContentValues values = categoryValues(name, code, note, color, userId);
        values.put("updated_at", nowString());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_CATEGORIES,
                    values,
                    "category_id = ? AND user_id = ?",
                    new String[]{String.valueOf(categoryId), String.valueOf(userId)}) > 0;
        }
    }

    public boolean deleteCategory(int categoryId, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put("category_id", 0);
                db.update(TABLE_PLANS, values, "category_id = ? AND user_id = ?",
                        new String[]{String.valueOf(categoryId), String.valueOf(userId)});
                boolean deleted = db.delete(TABLE_CATEGORIES,
                        "category_id = ? AND user_id = ?",
                        new String[]{String.valueOf(categoryId), String.valueOf(userId)}) > 0;
                db.setTransactionSuccessful();
                return deleted;
            } finally {
                db.endTransaction();
            }
        }
    }

    private ContentValues categoryValues(String name, String code, String note,
                                         String color, int userId) {
        ContentValues values = new ContentValues();
        values.put("category_name", name.trim());
        values.put("category_code", code.trim());
        values.put("note", note.trim());
        values.put("color", color);
        values.put("user_id", userId);
        return values;
    }

    public ArrayList<PlanCategory> getCategories(int userId) {
        ArrayList<PlanCategory> categories = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_CATEGORIES, null, "user_id = ?",
                     new String[]{String.valueOf(userId)}, null, null,
                     "category_name COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                categories.add(mapCategory(cursor));
            }
        }
        return categories;
    }

    public long addStudyPlan(String title, String description, String date, String time,
                             String endTime, int categoryId, String planType, int priority,
                             int durationMinutes, boolean reminderEnabled, int reminderMinutes,
                             String location, String room, String subject, String repeatRule,
                             String repeatUntil, double wage, boolean submitted, int userId) {
        ContentValues values = planValues(title, description, date, time, endTime, categoryId,
                planType, priority, durationMinutes, reminderEnabled, reminderMinutes,
                location, room, subject, repeatRule, repeatUntil, wage, submitted);
        values.put("status", STATUS_UPCOMING);
        values.put("user_id", userId);
        values.put("created_at", nowString());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_PLANS, null, values);
        }
    }

    public boolean updateStudyPlan(int planId, int userId, String title, String description,
                                   String date, String time, String endTime, int status,
                                   int categoryId, String planType, int priority,
                                   int durationMinutes, boolean reminderEnabled,
                                   int reminderMinutes, String location, String room,
                                   String subject, String repeatRule, String repeatUntil,
                                   double wage, boolean submitted) {
        ContentValues values = planValues(title, description, date, time, endTime, categoryId,
                planType, priority, durationMinutes, reminderEnabled, reminderMinutes,
                location, room, subject, repeatRule, repeatUntil, wage, submitted);
        values.put("status", status);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_PLANS, values, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
        }
    }

    private ContentValues planValues(String title, String description, String date, String time,
                                     String endTime, int categoryId, String planType, int priority,
                                     int durationMinutes, boolean reminderEnabled,
                                     int reminderMinutes, String location, String room,
                                     String subject, String repeatRule, String repeatUntil,
                                     double wage, boolean submitted) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("description", description.trim());
        values.put("date", date);
        values.put("time", time);
        values.put("end_time", endTime);
        values.put("category_id", categoryId);
        values.put("plan_type", planType);
        values.put("priority", priority);
        values.put("duration_minutes", durationMinutes);
        values.put("reminder_enabled", reminderEnabled ? 1 : 0);
        values.put("reminder_minutes", reminderMinutes);
        values.put("location", location.trim());
        values.put("room", room.trim());
        values.put("subject", subject.trim());
        values.put("repeat_rule", repeatRule);
        values.put("repeat_until", repeatUntil);
        values.put("wage", wage);
        values.put("submitted", submitted ? 1 : 0);
        values.put("start_time", combineDateTime(date, time));
        values.put("deadline", combineDateTime(date,
                endTime == null || endTime.trim().isEmpty() ? time : endTime));
        values.put("updated_at", nowString());
        return values;
    }

    private String nowString() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String combineDateTime(String date, String time) {
        String safeDate = date == null ? "" : date.trim();
        String safeTime = time == null || time.trim().isEmpty() ? "00:00" : time.trim();
        return safeDate + " " + safeTime;
    }

    public ArrayList<StudyPlan> getStudyPlans(int userId, String keyword, int statusFilter,
                                               int categoryFilter, String typeFilter) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        StringBuilder selection = new StringBuilder("p.user_id = ?");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        if (keyword != null && !keyword.trim().isEmpty()) {
            selection.append(" AND (p.title LIKE ? OR p.description LIKE ? OR pc.category_name LIKE ? OR p.subject LIKE ? OR p.location LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (statusFilter >= STATUS_UPCOMING && statusFilter <= STATUS_CANCELLED) {
            selection.append(" AND p.status = ?");
            args.add(String.valueOf(statusFilter));
        }
        if (categoryFilter > 0) {
            selection.append(" AND p.category_id = ?");
            args.add(String.valueOf(categoryFilter));
        }
        if (typeFilter != null && !typeFilter.trim().isEmpty()) {
            selection.append(" AND p.plan_type = ?");
            args.add(typeFilter);
        }

        String sql = selectPlanSql() + " WHERE " + selection + " ORDER BY p.date ASC, p.time ASC";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getStudyPlans(int userId, String keyword, int statusFilter,
                                               int categoryFilter) {
        return getStudyPlans(userId, keyword, statusFilter, categoryFilter, "");
    }

    public ArrayList<StudyPlan> getPendingReminderPlans() {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = selectPlanSql() +
                " WHERE p.status IN (0, 1) AND p.reminder_enabled = 1 ORDER BY p.date, p.time";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public StudyPlan getStudyPlanById(int planId) {
        String sql = selectPlanSql() + " WHERE p.task_id = ? LIMIT 1";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(planId)})) {
            if (cursor.moveToFirst()) {
                return mapStudyPlan(cursor);
            }
        }
        return null;
    }

    public ArrayList<StudyPlan> getStudyPlansByDate(int userId, String date) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = selectPlanSql() +
                " WHERE p.user_id = ? AND p.date = ? ORDER BY p.time ASC";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql,
                     new String[]{String.valueOf(userId), date})) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getStudyPlansBetween(int userId, String startDate, String endDate) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = selectPlanSql() +
                " WHERE p.user_id = ? AND p.date BETWEEN ? AND ? ORDER BY p.date ASC, p.time ASC";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql,
                     new String[]{String.valueOf(userId), startDate, endDate})) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getUpcomingPlans(int userId, String fromDate, int limit) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = selectPlanSql() +
                " WHERE p.user_id = ? AND p.status IN (0, 1) AND p.date >= ? " +
                "ORDER BY p.date ASC, p.time ASC LIMIT ?";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql,
                     new String[]{String.valueOf(userId), fromDate, String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public ArrayList<StudyPlan> getTodaySuggestions(int userId, String today, int limit) {
        ArrayList<StudyPlan> plans = new ArrayList<>();
        String sql = selectPlanSql() +
                " WHERE p.user_id = ? AND p.status IN (0, 1) AND p.date = ? " +
                "ORDER BY p.priority DESC, p.time ASC LIMIT ?";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql,
                     new String[]{String.valueOf(userId), today, String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                plans.add(mapStudyPlan(cursor));
            }
        }
        return plans;
    }

    public PlanRangeStats getPlanRangeStats(int userId, String startDate, String endDate,
                                            String today) {
        int total = 0;
        int completed = 0;
        int classMinutes = 0;
        int workMinutes = 0;
        int unsubmittedAssignments = 0;
        int overdue = 0;
        String nowDateTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new java.util.Date());
        String sql = "SELECT COUNT(*) total, " +
                "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) completed, " +
                "COALESCE(SUM(CASE WHEN plan_type = 'CLASS' AND status != 3 " +
                "THEN duration_minutes ELSE 0 END), 0) class_minutes, " +
                "COALESCE(SUM(CASE WHEN plan_type = 'PART_TIME' AND status != 3 " +
                "THEN duration_minutes ELSE 0 END), 0) work_minutes, " +
                "SUM(CASE WHEN plan_type = 'ASSIGNMENT' AND submitted = 0 " +
                "AND status != 2 AND status != 3 THEN 1 ELSE 0 END) unsubmitted_assignments, " +
                "SUM(CASE WHEN status IN (0, 1) AND " +
                "COALESCE(NULLIF(deadline, ''), date || ' ' || COALESCE(NULLIF(end_time, ''), time)) < ? " +
                "THEN 1 ELSE 0 END) overdue " +
                "FROM " + TABLE_PLANS + " WHERE user_id = ? AND date BETWEEN ? AND ?";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{
                     nowDateTime,
                     String.valueOf(userId),
                     startDate,
                     endDate
             })) {
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0);
                completed = cursor.getInt(1);
                classMinutes = cursor.getInt(2);
                workMinutes = cursor.getInt(3);
                unsubmittedAssignments = cursor.getInt(4);
                overdue = cursor.getInt(5);
            }
        }
        return new PlanRangeStats(total, completed, classMinutes, workMinutes,
                unsubmittedAssignments, overdue);
    }

    public boolean hasTimeConflict(int userId, String date, String startTime, String endTime,
                                   int excludePlanId) {
        String effectiveEnd = endTime == null || endTime.trim().isEmpty() ? startTime : endTime;
        String sql = "SELECT task_id FROM " + TABLE_PLANS +
                " WHERE user_id = ? AND date = ? AND status IN (0, 1) AND task_id != ? " +
                "AND time < ? AND COALESCE(NULLIF(end_time, ''), time) > ? LIMIT 1";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{
                     String.valueOf(userId), date, String.valueOf(excludePlanId),
                     effectiveEnd, startTime})) {
            return cursor.moveToFirst();
        }
    }

    public boolean deleteStudyPlan(int planId, int userId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.delete(TABLE_PLANS, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
        }
    }

    public long addSubTask(int planId, String title) {
        if (title == null || title.trim().isEmpty()) {
            return -1;
        }
        String now = nowString();
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("title", title.trim());
        values.put("is_completed", 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insert(TABLE_SUB_TASKS, null, values);
        }
    }

    public boolean updateSubTask(int subTaskId, String title, boolean completed) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("is_completed", completed ? 1 : 0);
        values.put("updated_at", nowString());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_SUB_TASKS, values, "sub_task_id = ?",
                    new String[]{String.valueOf(subTaskId)}) > 0;
        }
    }

    public boolean updateSubTaskCompleted(int subTaskId, boolean completed) {
        ContentValues values = new ContentValues();
        values.put("is_completed", completed ? 1 : 0);
        values.put("updated_at", nowString());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_SUB_TASKS, values, "sub_task_id = ?",
                    new String[]{String.valueOf(subTaskId)}) > 0;
        }
    }

    public boolean deleteSubTask(int subTaskId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.delete(TABLE_SUB_TASKS, "sub_task_id = ?",
                    new String[]{String.valueOf(subTaskId)}) > 0;
        }
    }

    public ArrayList<SubTask> getSubTasks(int planId) {
        ArrayList<SubTask> subTasks = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_SUB_TASKS, null, "plan_id = ?",
                     new String[]{String.valueOf(planId)}, null, null,
                     "created_at ASC, sub_task_id ASC")) {
            while (cursor.moveToNext()) {
                subTasks.add(mapSubTask(cursor));
            }
        }
        return subTasks;
    }

    public long upsertReminder(int planId, long reminderTime, boolean enabled) {
        String now = nowString();
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("reminder_time", reminderTime);
        values.put("is_enabled", enabled ? 1 : 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insertWithOnConflict(TABLE_REMINDERS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public boolean disableReminder(int planId) {
        ContentValues values = new ContentValues();
        values.put("is_enabled", 0);
        values.put("updated_at", nowString());
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.update(TABLE_REMINDERS, values, "plan_id = ?",
                    new String[]{String.valueOf(planId)}) > 0;
        }
    }

    public PlanReminder getPlanReminder(int planId) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_REMINDERS, null, "plan_id = ?",
                     new String[]{String.valueOf(planId)}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return mapPlanReminder(cursor);
            }
        }
        return null;
    }

    public long upsertRepeatRule(int planId, String repeatType, String weekDays,
                                 Integer monthDay, boolean active) {
        String safeType = repeatType == null || repeatType.trim().isEmpty()
                ? RepeatRule.TYPE_NONE : repeatType.trim();
        if (RepeatRule.TYPE_WEEKLY.equals(safeType)
                && active
                && (weekDays == null || weekDays.trim().isEmpty())) {
            return -1;
        }
        if (RepeatRule.TYPE_MONTHLY.equals(safeType)
                && active
                && (monthDay == null || monthDay < 1 || monthDay > 31)) {
            return -1;
        }

        String now = nowString();
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("repeat_type", safeType);
        values.put("week_days", weekDays == null ? "" : weekDays.trim());
        if (monthDay == null) {
            values.putNull("month_day");
        } else {
            values.put("month_day", monthDay);
        }
        values.put("is_active", active ? 1 : 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insertWithOnConflict(TABLE_REPEAT_RULES, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public RepeatRule getRepeatRule(int planId) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_REPEAT_RULES, null, "plan_id = ?",
                     new String[]{String.valueOf(planId)}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return mapRepeatRule(cursor);
            }
        }
        return null;
    }

    public long savePlanEvaluation(int planId, String satisfactionLevel, String resultNote,
                                   String delayReason, String completedAt) {
        StudyPlan plan = getStudyPlanById(planId);
        if (plan == null || plan.getStatus() != STATUS_COMPLETED) {
            return -1;
        }
        if (satisfactionLevel == null || satisfactionLevel.trim().isEmpty()
                || completedAt == null || completedAt.trim().isEmpty()) {
            return -1;
        }

        String now = nowString();
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("satisfaction_level", satisfactionLevel.trim());
        values.put("result_note", resultNote == null ? "" : resultNote.trim());
        values.put("delay_reason", delayReason == null ? "" : delayReason.trim());
        values.put("completed_at", completedAt.trim());
        values.put("created_at", now);
        values.put("updated_at", now);
        try (SQLiteDatabase db = getWritableDatabase()) {
            return db.insertWithOnConflict(TABLE_PLAN_EVALUATIONS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public PlanEvaluation getPlanEvaluation(int planId) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_PLAN_EVALUATIONS, null, "plan_id = ?",
                     new String[]{String.valueOf(planId)}, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return mapPlanEvaluation(cursor);
            }
        }
        return null;
    }

    public boolean updateStudyPlanStatus(int planId, int userId, int status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("updated_at", nowString());
        if (status == STATUS_COMPLETED || status == STATUS_CANCELLED) {
            values.put("reminder_enabled", 0);
        }
        try (SQLiteDatabase db = getWritableDatabase()) {
            boolean updated = db.update(TABLE_PLANS, values, "task_id = ? AND user_id = ?",
                    new String[]{String.valueOf(planId), String.valueOf(userId)}) > 0;
            if (updated && (status == STATUS_COMPLETED || status == STATUS_CANCELLED)) {
                ContentValues reminderValues = new ContentValues();
                reminderValues.put("is_enabled", 0);
                reminderValues.put("updated_at", nowString());
                db.update(TABLE_REMINDERS, reminderValues, "plan_id = ?",
                        new String[]{String.valueOf(planId)});
            }
            return updated;
        }
    }

    public StudyStatistics getStudyStatistics(int userId) {
        int total = 0;
        int completed = 0;
        int minutes = 0;
        int categories = 0;
        int overdue = 0;
        int assignments = 0;
        int classes = 0;
        int partTime = 0;
        int personal = 0;
        String nowDateTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new java.util.Date());
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor planCursor = db.rawQuery(
                     "SELECT COUNT(*) total, " +
                             "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) completed, " +
                             "COALESCE(SUM(duration_minutes), 0) minutes, " +
                             "SUM(CASE WHEN status IN (0, 1) AND " +
                             "COALESCE(NULLIF(deadline, ''), date || ' ' || COALESCE(NULLIF(end_time, ''), time)) < ? " +
                             "THEN 1 ELSE 0 END) overdue, " +
                             "SUM(CASE WHEN plan_type = 'ASSIGNMENT' THEN 1 ELSE 0 END) assignment_count, " +
                             "SUM(CASE WHEN plan_type = 'CLASS' THEN 1 ELSE 0 END) class_count, " +
                             "SUM(CASE WHEN plan_type = 'PART_TIME' THEN 1 ELSE 0 END) part_time_count, " +
                             "SUM(CASE WHEN plan_type = 'PERSONAL' THEN 1 ELSE 0 END) personal_count " +
                             "FROM " + TABLE_PLANS + " WHERE user_id = ?",
                     new String[]{nowDateTime, String.valueOf(userId)});
             Cursor categoryCursor = db.rawQuery(
                     "SELECT COUNT(*) FROM " + TABLE_CATEGORIES + " WHERE user_id = ?",
                     new String[]{String.valueOf(userId)})) {
            if (planCursor.moveToFirst()) {
                total = planCursor.getInt(0);
                completed = planCursor.getInt(1);
                minutes = planCursor.getInt(2);
                overdue = planCursor.getInt(3);
                assignments = planCursor.getInt(4);
                classes = planCursor.getInt(5);
                partTime = planCursor.getInt(6);
                personal = planCursor.getInt(7);
            }
            if (categoryCursor.moveToFirst()) {
                categories = categoryCursor.getInt(0);
            }
        }
        return new StudyStatistics(total, completed, total - completed, categories, minutes,
                overdue, assignments, classes, partTime, personal);
    }

    private String selectPlanSql() {
        return "SELECT p.*, COALESCE(pc.category_name, '') AS category_name FROM " +
                TABLE_PLANS + " p LEFT JOIN " + TABLE_CATEGORIES +
                " pc ON p.category_id = pc.category_id AND p.user_id = pc.user_id";
    }

    private PlanCategory mapCategory(Cursor cursor) {
        return new PlanCategory(
                cursor.getInt(cursor.getColumnIndexOrThrow("category_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("category_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("category_code")),
                cursor.getString(cursor.getColumnIndexOrThrow("note")),
                cursor.getString(cursor.getColumnIndexOrThrow("color")),
                cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
        );
    }

    private SubTask mapSubTask(Cursor cursor) {
        return new SubTask(
                cursor.getInt(cursor.getColumnIndexOrThrow("sub_task_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("plan_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    private PlanReminder mapPlanReminder(Cursor cursor) {
        return new PlanReminder(
                cursor.getInt(cursor.getColumnIndexOrThrow("reminder_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("plan_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("is_enabled")) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    private RepeatRule mapRepeatRule(Cursor cursor) {
        int monthDayIndex = cursor.getColumnIndexOrThrow("month_day");
        Integer monthDay = cursor.isNull(monthDayIndex) ? null : cursor.getInt(monthDayIndex);
        return new RepeatRule(
                cursor.getInt(cursor.getColumnIndexOrThrow("repeat_rule_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("plan_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("repeat_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("week_days")),
                monthDay,
                cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    private PlanEvaluation mapPlanEvaluation(Cursor cursor) {
        return new PlanEvaluation(
                cursor.getInt(cursor.getColumnIndexOrThrow("evaluation_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("plan_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("satisfaction_level")),
                cursor.getString(cursor.getColumnIndexOrThrow("result_note")),
                cursor.getString(cursor.getColumnIndexOrThrow("delay_reason")),
                cursor.getString(cursor.getColumnIndexOrThrow("completed_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    private StudyPlan mapStudyPlan(Cursor cursor) {
        return new StudyPlan(
                cursor.getInt(cursor.getColumnIndexOrThrow("task_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("date")),
                cursor.getString(cursor.getColumnIndexOrThrow("time")),
                cursor.getString(cursor.getColumnIndexOrThrow("end_time")),
                cursor.getInt(cursor.getColumnIndexOrThrow("status")),
                cursor.getInt(cursor.getColumnIndexOrThrow("category_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("category_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("plan_type")),
                cursor.getInt(cursor.getColumnIndexOrThrow("priority")),
                cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes")),
                cursor.getInt(cursor.getColumnIndexOrThrow("reminder_enabled")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("reminder_minutes")),
                cursor.getString(cursor.getColumnIndexOrThrow("location")),
                cursor.getString(cursor.getColumnIndexOrThrow("room")),
                cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                cursor.getString(cursor.getColumnIndexOrThrow("repeat_rule")),
                cursor.getString(cursor.getColumnIndexOrThrow("repeat_until")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("wage")),
                cursor.getInt(cursor.getColumnIndexOrThrow("submitted")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("user_id"))
        );
    }
}
