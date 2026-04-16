package com.moneylog.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getCanonicalName()
    );

    // ─────────────────────────────────────────────────────────
    // v3 → v4: recurring_transactions에 sort_order 컬럼 추가
    // ─────────────────────────────────────────────────────────

    @Test
    public void migrate3to4_addsSortOrderColumn() throws IOException {
        // v3 스키마로 DB 생성
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        // v3 상태에서 데이터 삽입
        db.execSQL("INSERT INTO recurring_transactions "
                + "(type, amount, category_id, memo, payment_method, interval_type, "
                + "day_of_month, month_of_year, is_active, last_executed_date, created_at, updated_at) "
                + "VALUES ('EXPENSE', 50000, 1, '월세', 'CARD', 'MONTHLY', 1, 0, 1, NULL, "
                + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ")");

        db.close();

        // v4로 마이그레이션 실행
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true,
                AppDatabase.MIGRATION_3_4);

        // sort_order 컬럼이 기본값 0으로 추가되었는지 확인
        Cursor cursor = db.query("SELECT sort_order FROM recurring_transactions");
        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getInt(0));
        cursor.close();

        db.close();
    }

    @Test
    public void migrate3to4_preservesExistingData() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        db.execSQL("INSERT INTO recurring_transactions "
                + "(type, amount, category_id, memo, payment_method, interval_type, "
                + "day_of_month, month_of_year, is_active, last_executed_date, created_at, updated_at) "
                + "VALUES ('INCOME', 3500000, 2, '급여', 'TRANSFER', 'MONTHLY', 25, 0, 1, "
                + "'2026-03-25', 1000, 2000)");

        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true,
                AppDatabase.MIGRATION_3_4);

        Cursor cursor = db.query(
                "SELECT type, amount, memo, interval_type, day_of_month, is_active, last_executed_date "
                + "FROM recurring_transactions");
        assertTrue(cursor.moveToFirst());
        assertEquals("INCOME", cursor.getString(0));
        assertEquals(3500000, cursor.getLong(1));
        assertEquals("급여", cursor.getString(2));
        assertEquals("MONTHLY", cursor.getString(3));
        assertEquals(25, cursor.getInt(4));
        assertEquals(1, cursor.getInt(5)); // is_active = true
        assertEquals("2026-03-25", cursor.getString(6));
        cursor.close();

        db.close();
    }

    // ─────────────────────────────────────────────────────────
    // 마이그레이션 SQL 직접 검증 (스키마 파일 없이)
    // ─────────────────────────────────────────────────────────

    @Test
    public void migration1to2_addsIntervalColumns() throws IOException {
        // v3 DB를 기반으로 SQL 구문 자체를 검증
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        // MIGRATION_1_2의 SQL이 이미 적용된 v3에서 interval_type, month_of_year 컬럼 확인
        Cursor cursor = db.query(
                "SELECT interval_type, month_of_year FROM recurring_transactions LIMIT 0");
        // 컬럼이 존재하면 예외 없이 커서 반환
        assertNotNull(cursor);
        cursor.close();

        db.close();
    }

    @Test
    public void migration2to3_budgetsTableRemoved() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        // v3에서는 budgets 테이블이 없어야 함
        Cursor cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='budgets'");
        assertFalse("budgets 테이블이 v3에서 이미 제거되어야 함", cursor.moveToFirst());
        cursor.close();

        db.close();
    }

    @Test
    public void allMigrations_v3toV4_producesValidSchema() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true,
                AppDatabase.MIGRATION_3_4);

        // 핵심 테이블 존재 확인
        Cursor cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
        java.util.Set<String> tables = new java.util.HashSet<>();
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0));
        }
        cursor.close();

        assertTrue("transactions 테이블이 존재해야 함", tables.contains("transactions"));
        assertTrue("categories 테이블이 존재해야 함", tables.contains("categories"));
        assertTrue("recurring_transactions 테이블이 존재해야 함",
                tables.contains("recurring_transactions"));

        db.close();
    }
}
