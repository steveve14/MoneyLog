package com.moneylog.di;

import android.content.Context;

import androidx.room.Room;

import com.moneylog.data.db.AppDatabase;
import com.moneylog.data.db.dao.CategoryDao;
import com.moneylog.data.db.dao.RecurringDao;
import com.moneylog.data.db.dao.TransactionDao;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.createSeedCallback(context))
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build();
    }

    @Provides
    public TransactionDao provideTransactionDao(AppDatabase db) {
        return db.transactionDao();
    }

    @Provides
    public CategoryDao provideCategoryDao(AppDatabase db) {
        return db.categoryDao();
    }

    @Provides
    public RecurringDao provideRecurringDao(AppDatabase db) {
        return db.recurringDao();
    }
}
