package com.moneylog.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_transactions")
public class RecurringEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** "EXPENSE" | "INCOME" */
    public String type;

    public long amount;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    public String memo = "";

    @ColumnInfo(name = "payment_method")
    public String paymentMethod = "CARD";

    /** 반복 주기: "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY" */
    @NonNull
    @ColumnInfo(name = "interval_type", defaultValue = "MONTHLY")
    public String intervalType = "MONTHLY";

    /** WEEKLY: 요일 (1=월~7=일), MONTHLY: 일 (1~28), YEARLY: 월중일 (ex: 101=1월1일) */
    @ColumnInfo(name = "day_of_month")
    public int dayOfMonth;

    /** YEARLY용: 실행 월 (1~12), 다른 주기에선 0 */
    @ColumnInfo(name = "month_of_year", defaultValue = "0")
    public int monthOfYear = 0;

    @ColumnInfo(name = "is_active")
    public boolean isActive = true;

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    public int sortOrder = 0;

    /** 마지막 실행 날짜 "yyyy-MM-dd", null이면 미실행 */
    @ColumnInfo(name = "last_executed_date")
    public String lastExecutedDate;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
