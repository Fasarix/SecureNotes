package com.example.securenotes.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.securenotes.model.entity.TagEntity;

import java.util.List;

@Dao
public interface TagDao {
    @Insert
    long insert(TagEntity tag);

    @Update
    int update(TagEntity tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<TagEntity>> getAllTags();

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    TagEntity findByNameSync(String name);

}
