package com.moneylog;

import android.app.Application;

import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.moneylog.data.worker.RecurringWorker;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.LocaleHelper;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MoneyLogApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        FormatUtils.setTextMode(LocaleHelper.isAmountTextMode(this));
        scheduleRecurringWorker();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build();
    }

    private void scheduleRecurringWorker() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            RecurringWorker.class, 1, TimeUnit.DAYS)
            .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            request);
    }
}

