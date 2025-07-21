package com.example.securenotes.security;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.securenotes.utils.Constants;

public class SecurePrefsManager {
    private static volatile MasterKey masterKey;

    private SecurePrefsManager() {

    }

    public static MasterKey getMasterKey(Context context) throws Exception {
        if (masterKey == null) {
            synchronized (SecurePrefsManager.class) {
                if (masterKey == null) {
                    masterKey = new MasterKey.Builder(context.getApplicationContext())
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();
                }
            }
        }
        return masterKey;
    }

    private static SharedPreferences getEncryptedPrefs(Context context, String prefName) throws Exception {
        return EncryptedSharedPreferences.create(
                context.getApplicationContext(),
                prefName,
                getMasterKey(context),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public static SharedPreferences getPinPrefs(Context context) throws Exception {
        return getEncryptedPrefs(context, Constants.PIN_PREF);
    }

    public static SharedPreferences getBiometricsPrefs(Context context) throws Exception {
        return getEncryptedPrefs(context, Constants.BIOMETRIC_PREF_NAME);
    }

    public static SharedPreferences getSettingsPrefs(Context context) throws Exception {
        return getEncryptedPrefs(context, Constants.SETTINGS_PREF);
    }

    public static SharedPreferences getDatabasePrefs(Context context) throws Exception {
        return getEncryptedPrefs(context, Constants.DATABASE_PREF);
    }

    public static SharedPreferences getBackupPrefs(Context context) throws Exception {
        return getEncryptedPrefs(context, Constants.BACKUP_PREF);
    }
}
