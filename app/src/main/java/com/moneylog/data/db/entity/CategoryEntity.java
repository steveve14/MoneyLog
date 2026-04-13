package com.moneylog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    /** "EXPENSE" | "INCOME" */
    public String type;

    /** Material Icons 이름 (예: "restaurant", "payments") */
    @ColumnInfo(name = "icon_name")
    public String iconName;

    @ColumnInfo(name = "sort_order")
    public int sortOrder = 0;

    /** 기본 제공 카테고리 여부 (삭제 불가) */
    @ColumnInfo(name = "is_default")
    public boolean isDefault = false;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted = false;
}
