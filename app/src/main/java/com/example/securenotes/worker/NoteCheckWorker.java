package com.example.securenotes.worker;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.security.CryptoManager;

public class NoteCheckWorker extends Worker {

    public NoteCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String pin = CryptoManager.getDatabasePin(getApplicationContext());

            if (pin == null) return Result.failure();

            byte[] salt = CryptoManager.getSaltDatabaseInByte(getApplicationContext());
            AppDatabase db = AppDatabase.getInstance(getApplicationContext(), CryptoManager.deriveKey(pin, salt));

            db.noteDao().deleteExpiredNotes(System.currentTimeMillis());

            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
