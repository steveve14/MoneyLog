package com.moneylog.data.repository;

import android.content.Context;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Google Drive 백업·복원 (Phase 5 scaffold).
 * 실제 Drive API 연동은 Phase 5-4 이후 구현.
 */
@Singleton
public class BackupRepository {

    private final Context context;

    @Inject
    public BackupRepository(@ApplicationContext Context context) {
        this.context = context;
    }

    /** Google Drive에 DB 백업 — 미구현 stub */
    public void backupToGoogleDrive(BackupCallback callback) {
        callback.onFailure("Google Drive 연동은 아직 구현되지 않았습니다.");
    }

    /** Google Drive에서 DB 복원 — 미구현 stub */
    public void restoreFromGoogleDrive(BackupCallback callback) {
        callback.onFailure("Google Drive 연동은 아직 구현되지 않았습니다.");
    }

    public interface BackupCallback {
        void onSuccess();
        void onFailure(String message);
    }
}
