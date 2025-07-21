package com.example.securenotes.model.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "tags")
public class TagEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String name;
    public int iconId;

    public TagEntity(@NonNull String name, int iconId) { // Constructor updated
        this.name = name;
        this.iconId = iconId;
    }

    // Getters
    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getIconId() { // Getter updated
        return iconId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagEntity tag = (TagEntity) o;
        return id == tag.id &&
                name.equals(tag.name) &&
                iconId == tag.iconId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, iconId);
    }

    @NonNull
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                "iconId=" + iconId +
                '}';
    }
}