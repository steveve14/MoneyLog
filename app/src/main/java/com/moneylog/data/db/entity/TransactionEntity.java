package com.moneylog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "transactions",
    indices = {
        @Index(value = {"date"},        name = "idx_tx_date"),
        @Index(value = {"category_id"}, name = "idx_tx_category"),
        @Index(value = {"type"},        name = "idx_tx_type"),
        @Index(value = {"is_deleted"},  name = "idx_tx_deleted")
    }
)
public class TransactionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** "EXPENSE" | "INCOME" */
    public String type;

    public long amount;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    /** "yyyy-MM-dd" */
    public String date;

    public String memo = "";

    /** "CARD" | "CASH" | "TRANSFER" | "OTHER" */
    @ColumnInfo(name = "payment_method")
    public String paymentMethod = "CARD";

    /** recurring_transactions.id — null이면 수동 등록 */
    @ColumnInfo(name = "recurring_id")
    public Long recurringId;

    /** WorkManager 자동 등록 여부 */
    @ColumnInfo(name = "is_auto")
    public boolean isAuto = false;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted = false;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
