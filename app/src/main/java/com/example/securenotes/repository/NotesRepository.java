package com.example.securenotes.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.model.dao.NoteDao;
import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.relation.NoteWithTag;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class NotesRepository {
    private final NoteDao noteDao;
    private final LiveData<List<NoteWithTag>> allNotes;
    private final SecretKey aesKeyForDbEncryption;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public NotesRepository(Context context, SecretKey aesKey) {
        AppDatabase db = AppDatabase.getInstance(context, null);
        noteDao = db.noteDao();
        allNotes = noteDao.getAllNotesWithTag();
        this.aesKeyForDbEncryption = aesKey;
    }

    public LiveData<List<NoteWithTag>> getAllNotes() {
        return allNotes;
    }

    public LiveData<NoteWithTag> getNoteById(long id) {
        return noteDao.getNoteWithTagById(id);
    }

    public void insert(NoteEntity note) {
        executor.execute(() -> noteDao.insert(note));
    }

    public void update(NoteEntity note) {
        executor.execute(() -> noteDao.update(note));
    }

    public void delete(NoteEntity note) {
        executor.execute(() -> noteDao.delete(note));
    }

    public void deleteNotesByIds(List<Long> ids) {
        executor.execute(() -> noteDao.deleteNotesByIds(ids));
    }

    public List<NoteWithTag> getAllNotesSync() {
        List<NoteWithTag> result = noteDao.getAllNotesWithTagSync();
        if (aesKeyForDbEncryption != null) {
            for (NoteWithTag note : result) {
                note.note.decryptFields(aesKeyForDbEncryption);
            }
        }
        return result;
    }

}
