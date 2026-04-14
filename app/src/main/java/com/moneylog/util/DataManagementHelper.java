package com.moneylog.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.TransactionEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class DataManagementHelper {

    private final Context context;
    private final TransactionDao transactionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    public DataManagementHelper(@ApplicationContext Context context, TransactionDao transactionDao) {
        this.context = context;
        this.transactionDao = transactionDao;
    }

    public interface ResultCallback {
        void onSuccess(String message);
        void onFailure(String message);
    }

    public void exportCsv(ResultCallback callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> transactions = transactionDao.getAllForExport();
                String fileName = "moneylog_export_" +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    callback.onFailure("파일 생성 실패");
                    return;
                }

                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os == null) {
                        callback.onFailure("파일 쓰기 실패");
                        return;
                    }
                    // BOM for Excel compatibility
                    os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                    os.write("id,type,amount,categoryId,date,memo,paymentMethod,isAuto\n".getBytes("UTF-8"));
                    for (TransactionEntity tx : transactions) {
                        String memo = tx.memo != null ? tx.memo.replace("\"", "\"\"") : "";
                        String line = String.format("%d,%s,%d,%d,%s,\"%s\",%s,%b\n",
                                tx.id, tx.type, tx.amount, tx.categoryId,
                                tx.date, memo, tx.paymentMethod, tx.isAuto);
                        os.write(line.getBytes("UTF-8"));
                    }
                }
                callback.onSuccess(fileName);
            } catch (IOException e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void importCsv(Uri fileUri, ResultCallback callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> imported = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(context.getContentResolver().openInputStream(fileUri), "UTF-8"))) {
                    String header = reader.readLine(); // skip header
                    if (header == null) {
                        callback.onFailure("빈 파일입니다");
                        return;
                    }
                    String line;
                    while ((line = reader.readLine()) != null) {
                        TransactionEntity tx = parseCsvLine(line);
                        if (tx != null) imported.add(tx);
                    }
                }
                for (TransactionEntity tx : imported) {
                    tx.id = 0; // auto-generate new ID
                    transactionDao.insert(tx);
                }
                callback.onSuccess(String.valueOf(imported.size()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void cleanupOldData(int monthsBefore, ResultCallback callback) {
        executor.execute(() -> {
            String beforeDate = LocalDate.now().minusMonths(monthsBefore)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int count = transactionDao.softDeleteBefore(beforeDate, System.currentTimeMillis());
            callback.onSuccess(String.valueOf(count));
        });
    }

    private TransactionEntity parseCsvLine(String line) {
        try {
            // Simple CSV parse handling quoted memo field
            List<String> fields = new ArrayList<>();
            boolean inQuote = false;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    inQuote = !inQuote;
                } else if (c == ',' && !inQuote) {
                    fields.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            fields.add(sb.toString().trim());

            if (fields.size() < 6) return null;

            TransactionEntity tx = new TransactionEntity();
            tx.type = fields.get(1);
            tx.amount = Long.parseLong(fields.get(2));
            tx.categoryId = Long.parseLong(fields.get(3));
            tx.date = fields.get(4);
            tx.memo = fields.get(5);
            tx.paymentMethod = fields.size() > 6 ? fields.get(6) : "CARD";
            tx.isAuto = fields.size() > 7 && Boolean.parseBoolean(fields.get(7));
            tx.createdAt = System.currentTimeMillis();
            tx.updatedAt = System.currentTimeMillis();
            return tx;
        } catch (Exception e) {
            return null;
        }
    }
}
