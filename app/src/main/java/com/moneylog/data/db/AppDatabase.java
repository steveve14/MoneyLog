package com.moneylog.data.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.moneylog.data.db.dao.CategoryDao;
import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.db.entity.TransactionEntity;

import androidx.room.migration.Migration;

import java.util.concurrent.Executors;

@Database(
    entities = {
        TransactionEntity.class,
        CategoryEntity.class,
        RecurringEntity.class
    },
    version = 3,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    // ─────────────────────────────────────────────────────────
    // v1 → v2: 반복 거래 주기 단위 컬럼 추가
    // ─────────────────────────────────────────────────────────
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE recurring_transactions ADD COLUMN interval_type TEXT NOT NULL DEFAULT 'MONTHLY'");
            database.execSQL(
                "ALTER TABLE recurring_transactions ADD COLUMN month_of_year INTEGER NOT NULL DEFAULT 0");
        }
    };

    // ─────────────────────────────────────────────────────────
    // v2 → v3: 미사용 budgets 테이블 제거
    // ─────────────────────────────────────────────────────────
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS budgets");
        }
    };

    public static final String DATABASE_NAME = "moneylog.db";

    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();
    public abstract RecurringDao recurringDao();

    // ─────────────────────────────────────────────────────────
    // 첫 실행 시 기본 카테고리 시드
    // ─────────────────────────────────────────────────────────

    public static final Callback SEED_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Executors.newSingleThreadExecutor().execute(() -> seedDefaultCategories(db));
        }
    };

    private static void seedDefaultCategories(SupportSQLiteDatabase db) {
        String[][] expense = {
            {"식비",        "restaurant"},
            {"교통",        "directions_bus"},
            {"주거",        "home"},
            {"엔터테인먼트", "sports_esports"},
            {"의류",        "checkroom"},
            {"의료·건강",   "local_hospital"},
            {"교육",        "menu_book"},
            {"선물·경조사", "redeem"},
            {"카페",        "local_cafe"},
            {"기타 지출",   "inventory_2"},
        };
        String[][] income = {
            {"급여",      "payments"},
            {"용돈",      "account_balance_wallet"},
            {"저축 인출", "savings"},
            {"투자 수익", "trending_up"},
            {"기타 수입", "inventory_2"},
        };

        for (int i = 0; i < expense.length; i++) {
            db.execSQL(
                "INSERT INTO categories (name, type, icon_name, sort_order, is_default, is_deleted)" +
                " VALUES (?, 'EXPENSE', ?, ?, 1, 0)",
                new Object[]{expense[i][0], expense[i][1], i}
            );
        }
        for (int i = 0; i < income.length; i++) {
            db.execSQL(
                "INSERT INTO categories (name, type, icon_name, sort_order, is_default, is_deleted)" +
                " VALUES (?, 'INCOME', ?, ?, 1, 0)",
                new Object[]{income[i][0], income[i][1], i}
            );
        }
    }
}
