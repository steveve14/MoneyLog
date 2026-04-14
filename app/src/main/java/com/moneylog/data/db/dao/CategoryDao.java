package com.moneylog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.moneylog.data.db.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Query("UPDATE categories SET is_deleted = 1 WHERE id = :id")
    void softDelete(long id);

    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY sort_order ASC")
    LiveData<List<CategoryEntity>> getAll();

    @Query("SELECT * FROM categories WHERE type = :type AND is_deleted = 0 ORDER BY sort_order ASC")
    LiveData<List<CategoryEntity>> getByType(String type);

    @Query("SELECT * FROM categories WHERE id = :id AND is_deleted = 0")
    CategoryEntity getById(long id);

    @Query("SELECT COUNT(*) FROM categories WHERE is_default = 1")
    int getDefaultCount();

    @Query("UPDATE categories SET sort_order = :sortOrder WHERE id = :id")
    void updateSortOrder(long id, int sortOrder);

    /** 기본 카테고리 동기 조회 (언어 변경 시 이름 업데이트용) */
    @Query("SELECT * FROM categories WHERE is_default = 1 AND is_deleted = 0")
    List<CategoryEntity> getDefaultCategoriesSync();

    /** 기본 카테고리 이름 업데이트 */
    @Query("UPDATE categories SET name = :name WHERE id = :id AND is_default = 1")
    void updateDefaultCategoryName(long id, String name);

    /** 전체 카테고리 삭제 (데이터 초기화용) */
    @Query("DELETE FROM categories")
    void deleteAll();
}
