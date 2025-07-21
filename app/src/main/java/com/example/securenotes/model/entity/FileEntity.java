package com.example.securenotes.model.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore; // <-- IMPORTANT: Added import for @Ignore
import android.util.Log;    // <-- IMPORTANT: Added import for Log

import com.example.securenotes.security.CryptoManager; // <-- IMPORTANT: Added import for CryptoUtils
import javax.crypto.SecretKey; // <-- IMPORTANT: Added import for SecretKey

@Entity(tableName = "files")
public class FileEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String titleEncrypted;
    public String filePathEncrypted;
    public Long tagId;
    public String fileTypeEncrypted;
    public Long creationDate;
    public Long lastModifiedDate;

    public FileEntity(Long id, String titleEncrypted, String filePathEncrypted, Long tagId, String fileTypeEncrypted, Long creationDate, Long lastModifiedDate) {
        this.id = id;
        this.titleEncrypted = titleEncrypted;
        this.filePathEncrypted = filePathEncrypted;
        this.tagId = tagId;
        this.fileTypeEncrypted = fileTypeEncrypted;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    @Ignore
    public FileEntity(String titleEncrypted, String filePathEncrypted, Long tagId, String fileTypeEncrypted, Long creationDate, Long lastModifiedDate) {
        this(null, titleEncrypted, filePathEncrypted, tagId, fileTypeEncrypted, creationDate, lastModifiedDate);
    }

    @Ignore
    public String decryptedTitle;
    @Ignore
    public String decryptedFileType;
    @Ignore
    public String decryptedFilePath;

    public void decryptFields(SecretKey key) {
        if (key == null) {
            this.decryptedTitle = "Error: Key missing";
            this.decryptedFileType = "Error: Key missing";
            this.decryptedFilePath = "Error: Key missing";
            Log.e("FileEntity", "Decryption attempted with null AES key.");
            return;
        }
        try {
            if (this.titleEncrypted != null) {
                this.decryptedTitle = CryptoManager.decrypt(this.titleEncrypted, key);
            } else {
                this.decryptedTitle = "";
            }

            if (this.fileTypeEncrypted != null) {
                this.decryptedFileType = CryptoManager.decrypt(this.fileTypeEncrypted, key);
            } else {
                this.decryptedFileType = "";
            }

            if (this.filePathEncrypted != null) {
                this.decryptedFilePath = CryptoManager.decrypt(this.filePathEncrypted, key);
            } else {
                this.decryptedFilePath = "";
            }

        } catch (Exception e) {
            Log.e("FileEntity", "Decryption failed for file ID: " + id + ", Error: " + e.getMessage(), e);
            this.decryptedTitle = "Decryption Failed";
            this.decryptedFileType = "Decryption Failed";
            this.decryptedFilePath = "Decryption Failed";
        }
    }
}