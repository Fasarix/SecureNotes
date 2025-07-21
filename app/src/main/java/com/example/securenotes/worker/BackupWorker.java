package com.example.securenotes.worker;

import static com.example.securenotes.utils.Constants.BACKUP_PASSWORD;
import static com.example.securenotes.utils.Constants.BACKUP_SALT;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.securenotes.model.relation.FileWithTag;
import com.example.securenotes.model.relation.NoteWithTag;
import com.example.securenotes.repository.FilesRepository;
import com.example.securenotes.repository.NotesRepository;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.security.SecurePrefsManager;
import com.example.securenotes.utils.FileMetadata;
import com.example.securenotes.utils.NoteMetadata;
import com.example.securenotes.utils.ZipUtils;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        try {
            SecretKey aesKey = CryptoManager.getAESKey();

            NotesRepository noteRepo = new NotesRepository(context, aesKey);
            List<NoteWithTag> notes = noteRepo.getAllNotesSync();

            FilesRepository fileRepo = new FilesRepository(context, aesKey);
            List<FileWithTag> files = fileRepo.getAllFilesSync();

            File tempDir = new File(context.getCacheDir(), "backup_temp_worker");
            if (tempDir.exists()) deleteRecursively(tempDir);
            if (!tempDir.mkdirs()) return Result.failure();

            File notesJsonFile = new File(tempDir, "notes.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(notesJsonFile))) {
                List<NoteMetadata> exportNotes = new ArrayList<>();
                for (NoteWithTag note : notes) {
                    exportNotes.add(new NoteMetadata(
                            note.note.decryptedTitle != null ? note.note.decryptedTitle : "",
                            note.note.decryptedContent != null ? note.note.decryptedContent : "",
                            note.tag != null ? note.tag.name : "",
                            note.note.createdAt,
                            note.note.updatedAt
                    ));
                }
                writer.write(new Gson().toJson(exportNotes));
            }

            MasterKey masterKey = SecurePrefsManager.getMasterKey(context);

            for (FileWithTag file : files) {
                if (file.file.decryptedFilePath == null || file.file.decryptedFilePath.isEmpty()) continue;

                File inputFile = new File(file.file.decryptedFilePath);
                if (!inputFile.exists()) continue;

                String fileName = file.file.decryptedTitle != null ? file.file.decryptedTitle : "file_" + file.file.id;
                String extension = file.file.decryptedFileType != null ? "." + file.file.decryptedFileType : "";
                File outFile = new File(tempDir, fileName + extension);

                try {
                    EncryptedFile encryptedFile = new EncryptedFile.Builder(
                            context,
                            inputFile,
                            masterKey,
                            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build();

                    try (InputStream in = encryptedFile.openFileInput();
                         OutputStream out = new FileOutputStream(outFile)) {

                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                } catch (Exception e) {
                    Log.e("BackupWorker", "Errore decifratura file: " + inputFile.getAbsolutePath(), e);
                }
            }

            File metadataJsonFile = new File(tempDir, "files_metadata.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataJsonFile))) {
                List<FileMetadata> metadataList = new ArrayList<>();
                for (FileWithTag file : files) {
                    String title = file.file.decryptedTitle != null ? file.file.decryptedTitle : "";
                    String tag = file.tag != null ? file.tag.name : "";
                    String fileName = (file.file.decryptedFilePath != null && !file.file.decryptedFilePath.isEmpty())
                            ? new File(file.file.decryptedFilePath).getName() : "";
                    String fileType = file.file.decryptedFileType != null ? file.file.decryptedFileType : "";
                    long createdAt = file.file.creationDate != null ? file.file.creationDate : 0L;
                    long updatedAt = file.file.lastModifiedDate != null ? file.file.lastModifiedDate : 0L;

                    metadataList.add(new FileMetadata(title, tag, fileName, fileType, createdAt, updatedAt));
                }
                writer.write(new Gson().toJson(metadataList));
            }

            File zipFile = new File(context.getCacheDir(), "backup.zip");

            CountDownLatch latch = new CountDownLatch(1);
            final Exception[] zipException = {null};

            ZipUtils.zipDirectoryAsync(tempDir, zipFile, new ZipUtils.ZipCallback() {
                @Override
                public void onSuccess() {
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    zipException[0] = e;
                    latch.countDown();
                }
            });

            if (!latch.await(5, TimeUnit.MINUTES)) {
                Log.e("BackupWorker", "Timeout compressione backup");
                return Result.failure();
            }

            if (zipException[0] != null) {
                Log.e("BackupWorker", "Errore compressione backup", zipException[0]);
                return Result.failure();
            }

            SharedPreferences prefs = SecurePrefsManager.getBackupPrefs(context);
            String pw = prefs.getString(BACKUP_PASSWORD, null);
            if (pw == null) return Result.failure();

            byte[] key = CryptoManager.deriveKey(pw, CryptoManager.convertSaltStringToByte(BACKUP_SALT));
            SecretKey secretKey = CryptoManager.toSecretKey(key);

            File encryptedZip = new File(context.getExternalFilesDir(null), "backup_secure.zip.aes");
            CryptoManager.encryptZip(zipFile, encryptedZip, secretKey, CryptoManager.generateSalt());

            deleteRecursively(tempDir);
            zipFile.delete();

            return Result.success();

        } catch (Exception e) {
            Log.e("BackupWorker", "Errore durante backup", e);
            return Result.failure();
        }
    }

    private void deleteRecursively(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }
}
