package com.moneylog.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.moneylog.R;
import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.dao.CategoryDao;
import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.db.entity.TransactionEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class DataManagementHelper {

    private final Context context;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final RecurringDao recurringDao;
    private final AppDatabase appDatabase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public DataManagementHelper(@ApplicationContext Context context,
                                TransactionDao transactionDao,
                                CategoryDao categoryDao,
                                RecurringDao recurringDao,
                                AppDatabase appDatabase) {
        this.context = context;
        this.transactionDao = transactionDao;
        this.categoryDao = categoryDao;
        this.recurringDao = recurringDao;
        this.appDatabase = appDatabase;
    }

    /** CSV 가져오기 모드 */
    public enum ImportMode {
        /** 기존 데이터에 추가 (덮어쓰기) */
        MERGE,
        /** 기존 거래 데이터 삭제 후 가져오기 */
        CLEAR_DATA,
        /** 카테고리 + 기존 거래 데이터 모두 삭제 후 가져오기 */
        CLEAR_ALL
    }

    public interface ResultCallback {
        void onSuccess(String message);
        void onFailure(String message);
    }

    public void exportCsv(ResultCallback callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> transactions = transactionDao.getAllForExport();

                // 카테고리 ID → 이름 매핑 테이블 구축
                Map<Long, CategoryEntity> categoryMap = new HashMap<>();
                for (CategoryEntity cat : categoryDao.getAllSync()) {
                    categoryMap.put(cat.id, cat);
                }

                String fileName = "moneylog_export_" +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    callback.onFailure(context.getString(R.string.file_create_failed));
                    return;
                }

                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os == null) {
                        callback.onFailure(context.getString(R.string.file_write_failed));
                        return;
                    }
                    // BOM for Excel compatibility
                    os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                    os.write("id,type,amount,categoryId,categoryName,date,memo,paymentMethod,isAuto\n".getBytes("UTF-8"));
                    for (TransactionEntity tx : transactions) {
                        String memo = tx.memo != null ? tx.memo.replace("\"", "\"\"") : "";
                        CategoryEntity cat = categoryMap.get(tx.categoryId);
                        String categoryName = cat != null ? cat.name.replace("\"", "\"\"") : "";
                        String line = String.format("%d,%s,%d,%d,\"%s\",%s,\"%s\",%s,%b\n",
                                tx.id, tx.type, tx.amount, tx.categoryId,
                                categoryName, tx.date, memo, tx.paymentMethod, tx.isAuto);
                        os.write(line.getBytes("UTF-8"));
                    }
                }
                callback.onSuccess(fileName);
            } catch (IOException e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void importCsv(Uri fileUri, ImportMode mode, ResultCallback callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> imported = new ArrayList<>();
                List<String[]> rawRows = new ArrayList<>();
                boolean hasNameColumn = false;

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(context.getContentResolver().openInputStream(fileUri), "UTF-8"))) {
                    String header = reader.readLine(); // read header
                    if (header == null) {
                        callback.onFailure(context.getString(R.string.file_empty));
                        return;
                    }
                    // BOM 제거
                    if (header.startsWith("\uFEFF")) header = header.substring(1);
                    // 새 포맷(categoryName 포함) 여부 판별
                    hasNameColumn = header.toLowerCase().contains("categoryname");

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parsed = parseCsvFields(line);
                        if (parsed != null) rawRows.add(parsed);
                    }
                }

                // 모드에 따라 기존 데이터 삭제
                if (mode == ImportMode.CLEAR_ALL) {
                    transactionDao.deleteAll();
                    recurringDao.deleteAll();
                    categoryDao.deleteAll();
                } else if (mode == ImportMode.CLEAR_DATA) {
                    transactionDao.deleteAll();
                    recurringDao.deleteAll();
                }

                // 카테고리 이름→ID 캐시 (DB 조회 최소화)
                Map<String, Long> categoryCache = new HashMap<>();

                for (String[] fields : rawRows) {
                    TransactionEntity tx = buildTransactionFromFields(fields, hasNameColumn, categoryCache);
                    if (tx != null) imported.add(tx);
                }

                for (TransactionEntity tx : imported) {
                    tx.id = 0; // auto-generate new ID
                    transactionDao.insert(tx);
                }

                // isAuto 거래에 대해 RecurringEntity 생성
                Map<String, Boolean> recurringDupCheck = new HashMap<>();
                for (TransactionEntity tx : imported) {
                    if (tx.isAuto) {
                        String key = tx.type + ":" + tx.categoryId + ":" + tx.amount;
                        if (recurringDupCheck.containsKey(key)) continue;
                        recurringDupCheck.put(key, true);

                        RecurringEntity rec = new RecurringEntity();
                        rec.type = tx.type;
                        rec.amount = tx.amount;
                        rec.categoryId = tx.categoryId;
                        rec.memo = tx.memo;
                        rec.paymentMethod = tx.paymentMethod;
                        rec.intervalType = "MONTHLY";
                        rec.dayOfMonth = 1;
                        try {
                            String dayStr = tx.date.substring(8, 10);
                            rec.dayOfMonth = Math.min(Integer.parseInt(dayStr), 28);
                        } catch (Exception ignored) { }
                        recurringDao.insert(rec);
                    }
                }

                callback.onSuccess(String.valueOf(imported.size()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    /**
     * CSV 필드 배열로부터 TransactionEntity를 생성한다.
     * categoryName이 있으면 이름+타입으로 카테고리를 조회/생성하여 매핑한다.
     */
    private TransactionEntity buildTransactionFromFields(String[] fields, boolean hasNameColumn,
                                                         Map<String, Long> categoryCache) {
        try {
            // 새 포맷: id,type,amount,categoryId,categoryName,date,memo,paymentMethod,isAuto
            // 구 포맷: id,type,amount,categoryId,date,memo,paymentMethod,isAuto
            int minFields = hasNameColumn ? 7 : 6;
            if (fields.length < minFields) return null;

            TransactionEntity tx = new TransactionEntity();
            tx.type = fields[1];

            if (hasNameColumn) {
                tx.amount = Long.parseLong(fields[2]);
                String categoryName = fields[4];
                tx.categoryId = resolveCategoryId(categoryName, tx.type, Long.parseLong(fields[3]), categoryCache);
                tx.date = fields[5];
                tx.memo = fields[6];
                tx.paymentMethod = fields.length > 7 ? fields[7] : "CARD";
                tx.isAuto = fields.length > 8 && Boolean.parseBoolean(fields[8]);
            } else {
                // 구 포맷 (categoryName 없음) — categoryId를 그대로 사용
                tx.amount = Long.parseLong(fields[2]);
                tx.categoryId = Long.parseLong(fields[3]);
                tx.date = fields[4];
                tx.memo = fields[5];
                tx.paymentMethod = fields.length > 6 ? fields[6] : "CARD";
                tx.isAuto = fields.length > 7 && Boolean.parseBoolean(fields[7]);
            }

            tx.createdAt = System.currentTimeMillis();
            tx.updatedAt = System.currentTimeMillis();
            return tx;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 카테고리 이름으로 ID를 찾는다. 없으면 새 카테고리를 생성한다.
     * @param name 카테고리 이름
     * @param type EXPENSE 또는 INCOME
     * @param fallbackId CSV에 기록된 원래 categoryId (이름이 비어있을 때 폴백)
     * @param cache 이름→ID 캐시
     */
    private long resolveCategoryId(String name, String type, long fallbackId,
                                   Map<String, Long> cache) {
        if (name == null || name.trim().isEmpty()) return fallbackId;

        String key = type + ":" + name.trim();
        Long cached = cache.get(key);
        if (cached != null) return cached;

        // DB에서 이름+타입으로 조회
        CategoryEntity existing = categoryDao.getByNameAndType(name.trim(), type);
        if (existing != null) {
            cache.put(key, existing.id);
            return existing.id;
        }

        // 없으면 새 카테고리 생성
        CategoryEntity newCat = new CategoryEntity();
        newCat.name = name.trim();
        newCat.type = type;
        newCat.iconName = "INCOME".equals(type) ? "payments" : "inventory_2";
        newCat.isDefault = false;
        long newId = categoryDao.insert(newCat);
        cache.put(key, newId);
        return newId;
    }

    public void cleanupOldData(int monthsBefore, ResultCallback callback) {
        executor.execute(() -> {
            String beforeDate = LocalDate.now().minusMonths(monthsBefore)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int count = transactionDao.softDeleteBefore(beforeDate, System.currentTimeMillis());
            callback.onSuccess(String.valueOf(count));
        });
    }

    public void resetAllData(ResultCallback callback) {
        executor.execute(() -> {
            try {
                transactionDao.deleteAll();
                recurringDao.deleteAll();
                categoryDao.deleteAll();
                AppDatabase.reseedDefaultCategories(
                        appDatabase.getOpenHelper().getWritableDatabase(), context);
                callback.onSuccess("");
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void deleteAllCategories(ResultCallback callback) {
        executor.execute(() -> {
            try {
                categoryDao.deleteAll();
                callback.onSuccess("");
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void resetCategoriesToDefault(ResultCallback callback) {
        executor.execute(() -> {
            try {
                categoryDao.deleteAll();
                AppDatabase.reseedDefaultCategories(
                        appDatabase.getOpenHelper().getWritableDatabase(), context);
                callback.onSuccess("");
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    /** 언어 변경 시 기본 카테고리 이름을 현재 로케일에 맞게 업데이트 */
    public void updateDefaultCategoryNames() {
        executor.execute(() -> {
            Map<String, Integer> iconToStringRes = new HashMap<>();
            iconToStringRes.put("restaurant", R.string.cat_food);
            iconToStringRes.put("directions_bus", R.string.cat_transport);
            iconToStringRes.put("home", R.string.cat_housing);
            iconToStringRes.put("sports_esports", R.string.cat_entertainment);
            iconToStringRes.put("checkroom", R.string.cat_clothing);
            iconToStringRes.put("local_hospital", R.string.cat_medical);
            iconToStringRes.put("menu_book", R.string.cat_education);
            iconToStringRes.put("redeem", R.string.cat_gifts);
            iconToStringRes.put("local_cafe", R.string.cat_cafe);
            iconToStringRes.put("payments", R.string.cat_salary);
            iconToStringRes.put("account_balance_wallet", R.string.cat_allowance);
            iconToStringRes.put("savings", R.string.cat_savings_withdrawal);
            iconToStringRes.put("trending_up", R.string.cat_investment);

            List<CategoryEntity> defaults = categoryDao.getDefaultCategoriesSync();
            for (CategoryEntity cat : defaults) {
                Integer resId = iconToStringRes.get(cat.iconName);
                if (resId != null) {
                    categoryDao.updateDefaultCategoryName(cat.id, context.getString(resId));
                } else if ("inventory_2".equals(cat.iconName)) {
                    int fallbackRes = "EXPENSE".equals(cat.type)
                            ? R.string.cat_other_expense : R.string.cat_other_income;
                    categoryDao.updateDefaultCategoryName(cat.id, context.getString(fallbackRes));
                }
            }
        });
    }

    /**
     * CSV 한 줄을 필드 배열로 파싱한다. RFC 4180 인용부호 처리.
     */
    private String[] parseCsvFields(String line) {
        try {
            List<String> fields = new ArrayList<>();
            boolean inQuote = false;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // escaped quote ""
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (c == ',' && !inQuote) {
                    fields.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            fields.add(sb.toString().trim());

            if (fields.size() < 6) return null;
            return fields.toArray(new String[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
