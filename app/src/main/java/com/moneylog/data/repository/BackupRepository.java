package com.moneylog.data.repository;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.moneylog.R;
import com.moneylog.data.db.AppDatabase;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BackupRepository {

    private static final String TAG = "BackupRepository";
    private static final String BACKUP_FILE_NAME = "moneylog_backup.db";

    private final Context context;
    private final AppDatabase appDatabase;

    @Inject
    public BackupRepository(@ApplicationContext Context context, AppDatabase appDatabase) {
        this.context = context;
        this.appDatabase = appDatabase;
    }

    /* ── Google Sign-In ───────────────────────────────── */

    public boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(context) != null;
    }

    public String getSignedInEmail() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null ? account.getEmail() : null;
    }

    public Intent getSignInIntent() {
        return getSignInClient().getSignInIntent();
    }

    public void signOut(Runnable onComplete) {
        getSignInClient().signOut()
                .addOnCompleteListener(task -> {
                    if (onComplete != null) onComplete.run();
                });
    }

    private GoogleSignInClient getSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    private Drive getDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) return null;

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(context.getString(R.string.app_name))
                .build();
    }

    /* ── Backup ───────────────────────────────────────── */

    public void backupToGoogleDrive(BackupCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure(context.getString(R.string.gdrive_not_signed_in));
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // WAL checkpoint — flush all pending writes to the main DB file
                Cursor c = appDatabase.getOpenHelper().getWritableDatabase()
                        .query("PRAGMA wal_checkpoint(TRUNCATE)");
                if (c != null) c.close();

                Drive driveService = getDriveService();
                if (driveService == null) {
                    callback.onFailure(context.getString(R.string.gdrive_not_signed_in));
                    return;
                }

                java.io.File dbFile = context.getDatabasePath("moneylog.db");
                if (!dbFile.exists()) {
                    callback.onFailure(context.getString(R.string.gdrive_backup_no_data));
                    return;
                }

                // Check for existing backup in appDataFolder
                FileList result = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "'")
                        .setFields("files(id, name)")
                        .execute();

                FileContent mediaContent = new FileContent("application/x-sqlite3", dbFile);

                if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                    String fileId = result.getFiles().get(0).getId();
                    driveService.files().update(fileId, null, mediaContent).execute();
                } else {
                    File fileMetadata = new File();
                    fileMetadata.setName(BACKUP_FILE_NAME);
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    driveService.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                }

                callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                callback.onFailure(context.getString(R.string.gdrive_backup_failed));
            }
        });
    }

    /* ── Restore ──────────────────────────────────────── */

    public void restoreFromGoogleDrive(BackupCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure(context.getString(R.string.gdrive_not_signed_in));
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Drive driveService = getDriveService();
                if (driveService == null) {
                    callback.onFailure(context.getString(R.string.gdrive_not_signed_in));
                    return;
                }

                FileList result = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '" + BACKUP_FILE_NAME + "'")
                        .setFields("files(id, name)")
                        .execute();

                List<File> files = result.getFiles();
                if (files == null || files.isEmpty()) {
                    callback.onFailure(context.getString(R.string.gdrive_no_backup_found));
                    return;
                }

                // Download to temp
                String fileId = files.get(0).getId();
                java.io.File tempFile = new java.io.File(context.getCacheDir(), "restore_temp.db");
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    driveService.files().get(fileId).executeMediaAndDownloadTo(os);
                }

                // Close Room and replace DB
                appDatabase.close();

                java.io.File dbFile = context.getDatabasePath("moneylog.db");
                java.io.File walFile = new java.io.File(dbFile.getPath() + "-wal");
                java.io.File shmFile = new java.io.File(dbFile.getPath() + "-shm");
                if (walFile.exists()) walFile.delete();
                if (shmFile.exists()) shmFile.delete();

                copyFile(tempFile, dbFile);
                tempFile.delete();

                callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                callback.onFailure(context.getString(R.string.gdrive_restore_failed));
            }
        });
    }

    private void copyFile(java.io.File src, java.io.File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    public interface BackupCallback {
        void onSuccess();
        void onFailure(String message);
    }
}
