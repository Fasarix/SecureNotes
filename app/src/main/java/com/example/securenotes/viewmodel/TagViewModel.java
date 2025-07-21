package com.example.securenotes.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.repository.TagRepository;

import java.util.List;

public class TagViewModel extends ViewModel {

    private TagRepository repository;
    private LiveData<List<TagEntity>> allTags;

    public void init(Context context) {
        if (repository != null) return;

        repository = new TagRepository(context);
        allTags = repository.getAllTags();
    }

    public LiveData<List<TagEntity>> getAllTags() {
        return allTags;
    }
}
