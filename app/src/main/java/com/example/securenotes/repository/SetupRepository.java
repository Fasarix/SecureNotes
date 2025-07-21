package com.example.securenotes.repository;

import static com.example.securenotes.utils.Constants.DATABASE_PIN;
import static com.example.securenotes.utils.Constants.DATABASE_SALT;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.util.Consumer;

import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.security.PinAuthManager;
import com.example.securenotes.security.SecurePrefsManager;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.utils.Constants.AuthResult;

public class SetupRepository {

    public static void init(String pin, String confirmPin, Context context, Consumer<AuthResult> callback) {
        if (pin.length() != 6) {
            callback.accept(AuthResult.error(Constants.SIZE));
            return;
        }

        if (!pin.equals(confirmPin)) {
            callback.accept(AuthResult.error(Constants.NO_MATCH));
            return;
        }

        try {
            PinAuthManager.savePIN(context, pin);

            SharedPreferences prefs = SecurePrefsManager.getDatabasePrefs(context);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString(DATABASE_PIN, pin);
            editor.putString(DATABASE_SALT, CryptoManager.getSaltDatabaseInString(context));
            editor.apply();

            AppDatabase.initDB(context);

            CryptoManager.initializeAESKey();

            callback.accept(AuthResult.success());

        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(AuthResult.error("Errore interno durante inizializzazione"));
        }
    }
}
