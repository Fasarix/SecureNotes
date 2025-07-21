package com.example.securenotes.utils;

public class NoteMetadata {
    public String title;
    public String content;
    public String tag;
    public long createdAt;
    public long updatedAt;

    public NoteMetadata(String title, String content, String tag, long createdAt, long updatedAt) {
        this.title = title;
        this.content = content;
        this.tag = tag;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
