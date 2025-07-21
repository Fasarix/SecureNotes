package com.example.securenotes.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.securenotes.model.dao.FileDao;
import com.example.securenotes.model.dao.NoteDao;
import com.example.securenotes.model.dao.TagDao;
import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.utils.TagIcon;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;
// Non più necessario, possiamo rimuoverli se non usati altrove
// import net.zetetic.database.sqlcipher.SQLiteStatement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {NoteEntity.class, TagEntity.class, FileEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();
    public abstract TagDao tagDao();
    public abstract FileDao fileDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(Context context, byte[] passphrase) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    final SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "secure_notes.db")
                            .openHelperFactory(factory)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        AppDatabase instance = AppDatabase.getInstance(context, passphrase);
                                        TagDao tagDao = instance.tagDao();
                                        for (TagIcon icon : TagIcon.values()) {
                                            TagEntity tag = new TagEntity(icon.name, icon.resId);
                                            tagDao.insert(tag);
                                        }
                                    });
                                }
                            })
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void initDB(Context context) throws Exception {
        AppDatabase.getInstance(context, CryptoManager.getDatabaseSecretKeyInByte(context));
        Log.e("CONFIG", "DB CREATO CON CHIAVE AES BASATA SU PBKDF2");
    }

}


