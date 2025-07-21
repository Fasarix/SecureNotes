package com.example.securenotes.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.relation.NoteWithTag;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    long insert(NoteEntity note);

    @Update
    int update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("SELECT * FROM notes WHERE id = :id")
    NoteEntity getNoteById(long id);

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<NoteWithTag> getNoteWithTagById(long id);

    @Transaction
    @Query("SELECT * FROM notes ORDER BY id DESC")
    LiveData<List<NoteWithTag>> getAllNotesWithTag();

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    void deleteNotesByIds(List<Long> ids);

    @Transaction
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<NoteWithTag> getAllNotesWithTagSync();

    @Query("DELETE FROM notes WHERE selfDestructAt IS NOT NULL AND selfDestructAt <= :now")
    void deleteExpiredNotes(long now);

}