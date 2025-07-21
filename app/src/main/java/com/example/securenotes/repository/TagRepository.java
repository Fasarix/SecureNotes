package com.example.securenotes.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.model.dao.TagDao;
import com.example.securenotes.model.entity.TagEntity;

import java.util.List;

public class TagRepository {

    private final TagDao tagDao;
    private final LiveData<List<TagEntity>> allTags;

    public TagRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context, null);
        tagDao = db.tagDao();
        allTags = tagDao.getAllTags();
    }

    public LiveData<List<TagEntity>> getAllTags() {
        return allTags;
    }

    public Long getTagIdByNameSync(String name) {
        TagEntity tag = tagDao.findByNameSync(name);
        return (tag != null) ? tag.id : null;
    }

    public long insert(TagEntity tag) {
        return tagDao.insert(tag);
    }
}
