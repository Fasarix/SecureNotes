package com.example.securenotes.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.relation.NoteWithTag;
import com.example.securenotes.repository.NotesRepository;
import com.example.securenotes.security.CryptoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class NotesViewModel extends AndroidViewModel {

    private static final String TAG = "NotesViewModel";

    private NotesRepository repository;
    private LiveData<List<NoteWithTag>> decryptedNotes;
    private SecretKey aesKey;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NotesViewModel(Application application) {
        super(application);
    }

    public void init() {
        Application application = getApplication();
        try {
            CryptoManager.initializeAESKey();
            aesKey = CryptoManager.getAESKey();
            Log.d(TAG, "AES key initialized");
        } catch (Exception e) {
            Log.e(TAG, "AES key initialization failed", e);
            return;
        }

        repository = new NotesRepository(application.getApplicationContext(), aesKey);

        decryptedNotes = Transformations.map(repository.getAllNotes(), encryptedList -> {
            if (encryptedList == null) return null;

            List<NoteWithTag> decryptedList = new ArrayList<>();
            for (NoteWithTag noteWithTag : encryptedList) {
                if (noteWithTag.note != null) {
                    noteWithTag.note.decryptFields(aesKey);
                }
                decryptedList.add(noteWithTag);
            }
            return decryptedList;
        });
    }

    public LiveData<List<NoteWithTag>> getAllNotes() {
        return decryptedNotes;
    }

    public LiveData<NoteWithTag> getDecryptedNoteById(long noteId) {
        return Transformations.map(repository.getNoteById(noteId), noteWithTag -> {
            if (noteWithTag != null && noteWithTag.note != null) {
                noteWithTag.note.decryptFields(aesKey);
            }
            return noteWithTag;
        });
    }

    public void insert(NoteEntity note) {
        executor.execute(() -> repository.insert(note));
    }

    public void update(NoteEntity note) {
        executor.execute(() -> repository.update(note));
    }

    public void delete(NoteEntity note) {
        executor.execute(() -> repository.delete(note));
    }

    public void deleteNotesByIds(List<Long> ids) {
        executor.execute(() -> repository.deleteNotesByIds(ids));
    }
}
