package com.focusflow.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("migration-test")
        db = context.openOrCreateDatabase("migration-test", 0, null)
    }

    @After
    fun tearDown() {
        db.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("migration-test")
    }

    private fun exec(sql: String) = db.execSQL(sql)
    private fun query(sql: String) = db.rawQuery(sql, null)

    // ── MG-01: MIGRATION_1_2 ──

    @Test
    fun `MIGRATION_1_2_adds_defaultTimerMode_and_defaultDurationMinutes_columns`() {
        exec("""
            CREATE TABLE task_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        exec("""
            CREATE TABLE timer_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                date TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                duration INTEGER NOT NULL,
                mode TEXT NOT NULL,
                targetDuration INTEGER NOT NULL
            )
        """)
        exec("INSERT INTO task_labels (name, color, sortOrder, createdAt) VALUES ('Test', '#FF0000', 0, 1000)")

        // MIGRATION_1_2
        exec("ALTER TABLE task_labels ADD COLUMN defaultTimerMode TEXT NOT NULL DEFAULT 'COUNT_UP'")
        exec("ALTER TABLE task_labels ADD COLUMN defaultDurationMinutes INTEGER DEFAULT NULL")

        val cursor = query("SELECT * FROM task_labels WHERE name = 'Test'")
        cursor.moveToFirst()
        assertEquals("COUNT_UP", cursor.getString(cursor.getColumnIndex("defaultTimerMode")))
        assertEquals(null, cursor.getString(cursor.getColumnIndex("defaultDurationMinutes")))
        cursor.close()
    }

    // ── MG-02: MIGRATION_2_3 ──

    @Test
    fun `MIGRATION_2_3_creates_daily_goals_table`() {
        exec("""
            CREATE TABLE task_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                defaultTimerMode TEXT NOT NULL DEFAULT 'COUNT_UP',
                defaultDurationMinutes INTEGER DEFAULT NULL
            )
        """)
        exec("""
            CREATE TABLE timer_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                date TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                duration INTEGER NOT NULL,
                mode TEXT NOT NULL,
                targetDuration INTEGER NOT NULL
            )
        """)

        // MIGRATION_2_3
        exec("""
            CREATE TABLE IF NOT EXISTS daily_goals (
                date TEXT NOT NULL PRIMARY KEY,
                goalSeconds INTEGER NOT NULL,
                achievedSeconds INTEGER NOT NULL,
                achieved INTEGER NOT NULL DEFAULT 0
            )
        """)

        var cursor = query("SELECT name FROM sqlite_master WHERE type='table' AND name='daily_goals'")
        assertTrue("daily_goals table should exist", cursor.moveToFirst())
        cursor.close()

        exec("INSERT INTO daily_goals (date, goalSeconds, achievedSeconds, achieved) VALUES ('2024-01-01', 28800, 10000, 0)")

        cursor = query("SELECT * FROM daily_goals WHERE date = '2024-01-01'")
        cursor.moveToFirst()
        assertEquals("2024-01-01", cursor.getString(cursor.getColumnIndex("date")))
        assertEquals(28800, cursor.getLong(cursor.getColumnIndex("goalSeconds")))
        assertEquals(10000, cursor.getLong(cursor.getColumnIndex("achievedSeconds")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("achieved")))
        cursor.close()
    }

    // ── MG-03: MIGRATION_3_4 ──

    @Test
    fun `MIGRATION_3_4_adds_taskName_and_taskColor_columns_to_timer_records`() {
        exec("""
            CREATE TABLE task_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                defaultTimerMode TEXT NOT NULL DEFAULT 'COUNT_UP',
                defaultDurationMinutes INTEGER DEFAULT NULL
            )
        """)
        exec("""
            CREATE TABLE timer_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                date TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                duration INTEGER NOT NULL,
                mode TEXT NOT NULL,
                targetDuration INTEGER NOT NULL
            )
        """)
        exec("""
            CREATE TABLE daily_goals (
                date TEXT NOT NULL PRIMARY KEY,
                goalSeconds INTEGER NOT NULL,
                achievedSeconds INTEGER NOT NULL,
                achieved INTEGER NOT NULL DEFAULT 0
            )
        """)
        exec("INSERT INTO timer_records (taskId, date, startTime, endTime, duration, mode, targetDuration) VALUES (1, '2024-01-01', 1000, 2000, 1000, 'COUNT_UP', 0)")

        // MIGRATION_3_4
        exec("ALTER TABLE timer_records ADD COLUMN taskName TEXT NOT NULL DEFAULT ''")
        exec("ALTER TABLE timer_records ADD COLUMN taskColor TEXT NOT NULL DEFAULT ''")

        val cursor = query("SELECT * FROM timer_records WHERE taskId = 1")
        cursor.moveToFirst()
        assertTrue("taskName column should exist", cursor.getColumnIndex("taskName") >= 0)
        assertTrue("taskColor column should exist", cursor.getColumnIndex("taskColor") >= 0)
        assertEquals("", cursor.getString(cursor.getColumnIndex("taskName")))
        assertEquals("", cursor.getString(cursor.getColumnIndex("taskColor")))
        cursor.close()
    }

    // ── MG-04: Full chain MIGRATION 1→4 ──

    @Test
    fun `full_MIGRATION_1_to_4_with_data_preservation`() {
        exec("""
            CREATE TABLE task_labels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        exec("""
            CREATE TABLE timer_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                date TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                duration INTEGER NOT NULL,
                mode TEXT NOT NULL,
                targetDuration INTEGER NOT NULL
            )
        """)
        exec("INSERT INTO task_labels (name, color, sortOrder, createdAt) VALUES ('OldTask', '#00FF00', 1, 5000)")
        exec("INSERT INTO timer_records (taskId, date, startTime, endTime, duration, mode, targetDuration) VALUES (1, '2024-01-01', 100, 200, 100, 'COUNT_UP', 0)")

        // MIGRATION_1_2
        exec("ALTER TABLE task_labels ADD COLUMN defaultTimerMode TEXT NOT NULL DEFAULT 'COUNT_UP'")
        exec("ALTER TABLE task_labels ADD COLUMN defaultDurationMinutes INTEGER DEFAULT NULL")

        // MIGRATION_2_3
        exec("""
            CREATE TABLE IF NOT EXISTS daily_goals (
                date TEXT NOT NULL PRIMARY KEY,
                goalSeconds INTEGER NOT NULL,
                achievedSeconds INTEGER NOT NULL,
                achieved INTEGER NOT NULL DEFAULT 0
            )
        """)

        // MIGRATION_3_4
        exec("ALTER TABLE timer_records ADD COLUMN taskName TEXT NOT NULL DEFAULT ''")
        exec("ALTER TABLE timer_records ADD COLUMN taskColor TEXT NOT NULL DEFAULT ''")

        // Verify task data preserved
        var cursor = query("SELECT * FROM task_labels WHERE name = 'OldTask'")
        cursor.moveToFirst()
        assertEquals("OldTask", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("#00FF00", cursor.getString(cursor.getColumnIndex("color")))
        assertEquals(1, cursor.getLong(cursor.getColumnIndex("sortOrder")))
        assertEquals("COUNT_UP", cursor.getString(cursor.getColumnIndex("defaultTimerMode")))
        cursor.close()

        // Verify timer data preserved
        cursor = query("SELECT * FROM timer_records WHERE taskId = 1")
        cursor.moveToFirst()
        assertEquals(1, cursor.getLong(cursor.getColumnIndex("taskId")))
        assertEquals(100, cursor.getLong(cursor.getColumnIndex("duration")))
        assertEquals("", cursor.getString(cursor.getColumnIndex("taskName")))
        assertEquals("", cursor.getString(cursor.getColumnIndex("taskColor")))
        cursor.close()

        // Verify daily_goals table exists
        cursor = query("SELECT name FROM sqlite_master WHERE type='table' AND name='daily_goals'")
        assertTrue("daily_goals table should exist after MIGRATION_2_3", cursor.moveToFirst())
        cursor.close()
    }
}
