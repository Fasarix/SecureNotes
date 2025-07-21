package com.example.securenotes.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.relation.FileWithTag;
import com.example.securenotes.repository.FilesRepository;
import com.example.securenotes.security.CryptoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class FilesViewModel extends AndroidViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FilesRepository repository;
    private LiveData<List<FileWithTag>> decryptedFiles;
    private SecretKey aesKey;

    public FilesViewModel(Application application) {
        super(application);
    }

    public void init() {
        Application application = getApplication();
        try {
            CryptoManager.initializeAESKey();
            aesKey = CryptoManager.getAESKey();
            Log.d("FilesViewModel", "AES key loaded.");
        } catch (Exception e) {
            Log.e("FilesViewModel", "Failed to load AES key: " + e.getMessage());
            aesKey = null;
        }

        repository = new FilesRepository(application.getApplicationContext(), aesKey);

        decryptedFiles = Transformations.map(repository.getAllFiles(), encryptedList -> {
            List<FileWithTag> decryptedList = new ArrayList<>();
            if (encryptedList != null) {
                for (FileWithTag fileWithTag : encryptedList) {
                    if (fileWithTag.file != null) {
                        fileWithTag.file.decryptFields(aesKey);
                    }
                    decryptedList.add(fileWithTag);
                }
            }
            return decryptedList;
        });
    }

    public LiveData<List<FileWithTag>> getDecryptedFilesWithTag() {
        return decryptedFiles;
    }

    public LiveData<FileWithTag> getDecryptedFileById(long id) {
        return Transformations.map(repository.getFileById(id), fileWithTag -> {
            if (fileWithTag != null && fileWithTag.file != null) {
                fileWithTag.file.decryptFields(aesKey);
            }
            return fileWithTag;
        });
    }

    public void insert(FileEntity file) {
        executor.execute(() -> repository.insert(file));
    }

    public void update(FileEntity file) {
        executor.execute(() -> repository.update(file));
    }

    public void delete(FileEntity file) {
        executor.execute(() -> repository.delete(file));
    }

    public void deleteByIds(List<Long> ids) {
        executor.execute(() -> repository.deleteByIds(ids));
    }
}
