package com.example.securenotes.model.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.entity.TagEntity;

public class FileWithTag {

    @Embedded
    public FileEntity file;

    @Relation(parentColumn = "tagId", entityColumn = "id")
    public TagEntity tag;
}
