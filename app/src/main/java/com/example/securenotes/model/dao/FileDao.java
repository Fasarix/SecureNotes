package com.example.securenotes.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.relation.FileWithTag;

import java.util.List;

@Dao
public interface FileDao {

    @Insert
    long insert(FileEntity file);

    @Update
    int update(FileEntity file);

    @Delete
    void delete(FileEntity file);

    @Transaction
    @Query("SELECT * FROM files WHERE id = :id")
    LiveData<FileWithTag> getFileById(long id);

    @Query("SELECT * FROM files WHERE id = :fileId")
    FileEntity getFileByIdSync(long fileId);

    @Transaction
    @Query("SELECT * FROM files ORDER BY id DESC")
    LiveData<List<FileWithTag>> getAllFilesWithTag();

    @Transaction
    @Query("SELECT * FROM files ORDER BY id DESC")
    List<FileWithTag> getAllFilesWithTagSync();

}