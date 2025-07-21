package com.example.securenotes.security;

import static com.example.securenotes.security.CryptoManager.deriveKey;
import static com.example.securenotes.security.CryptoManager.generateSalt;
import static com.example.securenotes.utils.Constants.PIN_HASH;
import static com.example.securenotes.utils.Constants.SALT_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

public class PinAuthManager {

    private static final String TAG = "PinAuthManager";

    public static void savePIN(Context context, String pin) throws Exception {
        byte[] salt = generateSalt();
        byte[] hashed = deriveKey(pin, salt);

        SharedPreferences prefs = SecurePrefsManager.getPinPrefs(context);
        prefs.edit()
                .putString(PIN_HASH, Base64.encodeToString(hashed, Base64.NO_WRAP))
                .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply();

        Log.d(TAG, "PIN salvato correttamente");
    }

    public static boolean isPinSet(Context context) {
        try {
            SharedPreferences prefs = SecurePrefsManager.getPinPrefs(context);
            return prefs.contains(PIN_HASH) && prefs.contains(SALT_KEY);
        } catch (Exception e) {
            Log.e(TAG, "Errore nel controllo PIN impostato", e);
            return false;
        }
    }

    public static boolean verifyPIN(Context context, String inputPin) throws Exception {
        SharedPreferences prefs = SecurePrefsManager.getPinPrefs(context);
        String storedHashBase64 = prefs.getString(PIN_HASH, null);
        String saltBase64 = prefs.getString(SALT_KEY, null);

        if (storedHashBase64 == null || saltBase64 == null) {
            Log.w(TAG, "Hash o salt PIN non trovati");
            return false;
        }

        byte[] storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP);
        byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
        byte[] inputHash = deriveKey(inputPin, salt);

        return constantTimeEquals(storedHash, inputHash);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
