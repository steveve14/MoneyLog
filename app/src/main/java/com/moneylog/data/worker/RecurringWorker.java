package com.moneylog.data.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.dao.TransactionDao;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.util.DateUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * WorkManager Worker — 매일 1회 실행하여 오늘 날짜가 dayOfMonth인 활성 반복 거래를
 * transactions 테이블에 자동으로 삽입합니다.
 * 밀린 달이 있으면 순서대로 처리합니다.
 */
@HiltWorker
public class RecurringWorker extends Worker {

    private final RecurringDao recurringDao;
    private final TransactionDao transactionDao;

    @AssistedInject
    public RecurringWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            AppDatabase db) {
        super(context, params);
        this.recurringDao  = db.recurringDao();
        this.transactionDao = db.transactionDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<RecurringEntity> actives = recurringDao.getAllActiveSync();
            if (actives == null || actives.isEmpty()) return Result.success();

            LocalDate today = LocalDate.now();

            for (RecurringEntity recurring : actives) {
                processRecurring(recurring, today);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * 마지막 실행 날짜를 기준으로 오늘까지 밀린 주기를 모두 처리합니다.
     */
    private void processRecurring(RecurringEntity recurring, LocalDate today) {
        String interval = recurring.intervalType != null ? recurring.intervalType : "MONTHLY";

        LocalDate startFrom;
        if (recurring.lastExecutedDate == null) {
            LocalDate createdDate = LocalDate.ofEpochDay(
                recurring.createdAt / (24L * 60 * 60 * 1000));
            startFrom = firstExecutionDate(createdDate, recurring, interval);
        } else {
            LocalDate last = DateUtils.parseDate(recurring.lastExecutedDate);
            startFrom = nextDate(last, recurring, interval);
        }

        if (startFrom.isAfter(today)) return;

        LocalDate cursor = startFrom;
        LocalDate lastProcessed = null;
        while (!cursor.isAfter(today)) {
            insertAutoTransaction(recurring, cursor);
            lastProcessed = cursor;
            cursor = nextDate(cursor, recurring, interval);
        }

        if (lastProcessed != null) {
            recurring.lastExecutedDate = DateUtils.formatDate(lastProcessed);
            recurring.updatedAt = System.currentTimeMillis();
            recurringDao.update(recurring);
        }
    }

    private LocalDate nextDate(LocalDate from, RecurringEntity recurring, String interval) {
        switch (interval) {
            case "DAILY":
                return from.plusDays(1);
            case "WEEKLY":
                return from.plusWeeks(1);
            case "YEARLY":
                LocalDate nextYear = from.plusYears(1);
                int month = recurring.monthOfYear > 0 ? recurring.monthOfYear : 1;
                return nextYear.withMonth(month)
                    .withDayOfMonth(clampDay(nextYear.withMonth(month), recurring.dayOfMonth));
            case "MONTHLY":
            default:
                LocalDate next = from.plusMonths(1);
                return next.withDayOfMonth(clampDay(next, recurring.dayOfMonth));
        }
    }

    private LocalDate firstExecutionDate(LocalDate createdDate, RecurringEntity recurring, String interval) {
        switch (interval) {
            case "DAILY":
                return createdDate.plusDays(1);
            case "WEEKLY": {
                int targetDow = recurring.dayOfMonth; // 1=월~7=일
                DayOfWeek target = DayOfWeek.of(targetDow > 0 ? targetDow : 1);
                LocalDate candidate = createdDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(target));
                return candidate.isAfter(createdDate) ? candidate
                    : candidate.with(java.time.temporal.TemporalAdjusters.next(target));
            }
            case "YEARLY": {
                int month = recurring.monthOfYear > 0 ? recurring.monthOfYear : 1;
                LocalDate candidate = LocalDate.of(createdDate.getYear(), month, 1);
                candidate = candidate.withDayOfMonth(clampDay(candidate, recurring.dayOfMonth));
                if (!candidate.isAfter(createdDate)) {
                    candidate = LocalDate.of(createdDate.getYear() + 1, month, 1);
                    candidate = candidate.withDayOfMonth(clampDay(candidate, recurring.dayOfMonth));
                }
                return candidate;
            }
            case "MONTHLY":
            default: {
                LocalDate candidate = createdDate.withDayOfMonth(
                    clampDay(createdDate, recurring.dayOfMonth));
                if (!candidate.isAfter(createdDate)) {
                    LocalDate nextMonth = createdDate.plusMonths(1);
                    candidate = nextMonth.withDayOfMonth(clampDay(nextMonth, recurring.dayOfMonth));
                }
                return candidate;
            }
        }
    }

    private void insertAutoTransaction(RecurringEntity recurring, LocalDate date) {
        TransactionEntity tx = new TransactionEntity();
        tx.type            = recurring.type;
        tx.amount          = recurring.amount;
        tx.categoryId      = recurring.categoryId;
        tx.date            = DateUtils.formatDate(date);
        tx.memo            = recurring.memo;
        tx.paymentMethod   = recurring.paymentMethod;
        tx.recurringId     = recurring.id;
        tx.isAuto          = true;
        tx.createdAt       = System.currentTimeMillis();
        tx.updatedAt       = System.currentTimeMillis();
        transactionDao.insert(tx);
    }

    /** dayOfMonth가 해당 월 최대 일수를 초과하면 마지막 날로 클램프 */
    private int clampDay(LocalDate monthRef, int dayOfMonth) {
        int maxDay = monthRef.lengthOfMonth();
        return Math.min(dayOfMonth, maxDay);
    }
}
