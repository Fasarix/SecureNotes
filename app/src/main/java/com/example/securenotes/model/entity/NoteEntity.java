package com.example.securenotes.model.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore; // Import for @Ignore
import android.util.Log;    // Import for Log

import com.example.securenotes.security.CryptoManager; // Import for CryptoUtils
import javax.crypto.SecretKey; // Import for SecretKey

@Entity(tableName = "notes",
        foreignKeys = @ForeignKey(entity = TagEntity.class,
                parentColumns = "id",
                childColumns = "tagId",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("tagId")})
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String titleEncrypted;
    public String contentEncrypted;

    public Long tagId;

    public long createdAt;
    public long updatedAt;
    public Long selfDestructAt;

    @Ignore
    public String decryptedTitle;
    @Ignore
    public String decryptedContent;


    public NoteEntity(long id, String titleEncrypted, String contentEncrypted, Long tagId,
                      long createdAt, long updatedAt, Long selfDestructAt) {
        this.id = id;
        this.titleEncrypted = titleEncrypted;
        this.contentEncrypted = contentEncrypted;
        this.tagId = tagId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.selfDestructAt = selfDestructAt;
    }


    @Ignore
    public NoteEntity(@NonNull String title, @NonNull String content, Long tagId,
                      long createdAt, long updatedAt, SecretKey key, boolean selfDestruct) {
        try {
            this.titleEncrypted = CryptoManager.encrypt(title, key);
            this.contentEncrypted = CryptoManager.encrypt(content, key);
        } catch (Exception e) {
            Log.e("NoteEntity", "Encryption failed: " + e.getMessage());
            this.titleEncrypted = "";
            this.contentEncrypted = "";
        }
        this.tagId = tagId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.selfDestructAt = selfDestruct ? createdAt +  60 * 1000 : null;
    }

    @Ignore
    public NoteEntity(@NonNull String title, @NonNull String content, Long tagId,
                      long createdAt, long updatedAt, SecretKey key, Long selfDestructAt) {
        try {
            this.titleEncrypted = CryptoManager.encrypt(title, key);
            this.contentEncrypted = CryptoManager.encrypt(content, key);
        } catch (Exception e) {
            Log.e("NoteEntity", "Encryption failed: " + e.getMessage());
            this.titleEncrypted = "";
            this.contentEncrypted = "";
        }
        this.tagId = tagId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.selfDestructAt = selfDestructAt;
    }

    public void decryptFields(SecretKey key) {
        if (key == null) {
            Log.e("NoteEntity", "Decryption attempted with null AES key.");
            this.decryptedTitle = "Error: Key missing";
            this.decryptedContent = "Error: Key missing";
            return;
        }
        try {
            if (this.titleEncrypted != null) {
                this.decryptedTitle = CryptoManager.decrypt(this.titleEncrypted, key);
            } else {
                this.decryptedTitle = "";
            }

            if (this.contentEncrypted != null) {
                this.decryptedContent = CryptoManager.decrypt(this.contentEncrypted, key);
            } else {
                this.decryptedContent = "";
            }
        } catch (Exception e) {
            Log.e("NoteEntity", "Decryption failed for note ID: " + id + ", Error: " + e.getMessage(), e);
            this.decryptedTitle = "Decryption Failed";
            this.decryptedContent = "Decryption Failed";
        }
    }
}