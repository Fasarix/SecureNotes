package com.example.securenotes.utils;

public class FileMetadata {
    public String title;
    public String tag;
    public String fileName;
    public String fileType;
    public long createdAt;
    public long updatedAt;

    public FileMetadata(String title, String tag, String fileName, String fileType, long createdAt, long updatedAt) {
        this.title = title;
        this.tag = tag;
        this.fileName = fileName;
        this.fileType = fileType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
