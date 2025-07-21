package com.example.securenotes.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.model.dao.FileDao;
import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.relation.FileWithTag;
import com.example.securenotes.security.CryptoManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class FilesRepository {
    private final FileDao fileDao;
    private final LiveData<List<FileWithTag>> allFiles;
    private final Context applicationContext;
    private final ExecutorService databaseWriteExecutor;
    private final SecretKey aesKeyForDbEncryption;

    public FilesRepository(Context context, SecretKey aesKey) {
        AppDatabase db = AppDatabase.getInstance(context, null);
        fileDao = db.fileDao();
        allFiles = fileDao.getAllFilesWithTag();
        this.applicationContext = context.getApplicationContext();
        this.databaseWriteExecutor = Executors.newSingleThreadExecutor();
        this.aesKeyForDbEncryption = aesKey;
    }

    public LiveData<List<FileWithTag>> getAllFiles() {
        return allFiles;
    }

    public LiveData<FileWithTag> getFileById(long id) {
        return fileDao.getFileById(id);
    }

    public FileEntity getFileByIdSync(long id) {
        return fileDao.getFileByIdSync(id);
    }

    public void insert(FileEntity file) {
        databaseWriteExecutor.execute(() -> fileDao.insert(file));
    }

    public void update(FileEntity file) {
        databaseWriteExecutor.execute(() -> fileDao.update(file));
    }

    public void delete(FileEntity file) {
        databaseWriteExecutor.execute(() -> {
            if (file == null) {
                Log.w("FilesRepository", "Attempted to delete a null File object.");
                return;
            }

            String encryptedFilePathBase64 = file.filePathEncrypted;
            if (encryptedFilePathBase64 != null && !encryptedFilePathBase64.isEmpty()) {
                try {
                    String decryptedFilePath = CryptoManager.decrypt(encryptedFilePathBase64, aesKeyForDbEncryption);
                    File physicalFile = new File(decryptedFilePath);
                    String encryptedFileName = physicalFile.getName();

                    boolean fileDeleted = CryptoManager.deleteFileInEncryptedDir(applicationContext, encryptedFileName);
                    if (fileDeleted) {
                        fileDao.delete(file);
                        Log.d("FilesRepository", "Deleted physical file '" + encryptedFileName + "' and DB record for ID: " + file.id);
                    } else {
                        Log.e("FilesRepository", "Failed to delete physical file '" + encryptedFileName + "'. DB record not deleted.");
                    }
                } catch (Exception e) {
                    Log.e("FilesRepository", "Error during file deletion for ID: " + file.id, e);
                }
            } else {
                Log.w("FilesRepository", "Encrypted file path empty for ID: " + file.id + ". Deleting DB record only.");
                fileDao.delete(file);
            }
        });
    }

    public void deleteByIds(List<Long> ids) {
        databaseWriteExecutor.execute(() -> {
            if (ids != null && !ids.isEmpty()) {
                for (Long id : ids) {
                    FileEntity fileToDelete = getFileByIdSync(id);
                    if (fileToDelete != null) {
                        delete(fileToDelete);
                    } else {
                        Log.w("FilesRepository", "No file found with ID: " + id + " in batch delete");
                    }
                }
            }
        });
    }

    public List<FileWithTag> getAllFilesSync() {
        List<FileWithTag> filesWithTags = fileDao.getAllFilesWithTagSync();
        if (filesWithTags != null) {
            for (FileWithTag fwt : filesWithTags) {
                if (fwt.file != null) {
                    fwt.file.decryptFields(aesKeyForDbEncryption);
                }
            }
        }
        return filesWithTags;
    }
}
