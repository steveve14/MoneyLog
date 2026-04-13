package com.moneylog.data.db.dao;

/** 카테고리별 거래 합계 (Room 프로젝션) */
public class CategorySummary {
    public long categoryId;
    public String categoryName;
    public String type;
    public long total;
}
