package com.example.personalplanner.notification;

public enum ReminderType {
    ON_TIME(0, false),
    AFTER_1_MIN(1, false),
    AFTER_3_MIN(3, false),
    AFTER_5_MIN(5, false),
    AFTER_10_MIN(10, false),
    AFTER_20_MIN(20, false),
    AFTER_30_MIN(30, false),
    AFTER_1_HOUR(60, false),
    EVERY_24_HOURS(-1440, true);

    private final int storedValue;
    private final boolean allDay;

    ReminderType(int storedValue, boolean allDay) {
        this.storedValue = storedValue;
        this.allDay = allDay;
    }

    public int getStoredValue() {
        return storedValue;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public long getOffsetMillis() {
        return Math.abs((long) storedValue) * 60_000L;
    }

    public static ReminderType fromStoredValue(int storedValue) {
        for (ReminderType type : values()) {
            if (type.storedValue == storedValue) {
                return type;
            }
        }
        return ON_TIME;
    }
}
