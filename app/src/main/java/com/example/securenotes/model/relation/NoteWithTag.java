package com.example.securenotes.model.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.entity.TagEntity;

public class NoteWithTag {

    @Embedded
    public NoteEntity note;

    @Relation(parentColumn = "tagId", entityColumn = "id")
    public TagEntity tag;
}
