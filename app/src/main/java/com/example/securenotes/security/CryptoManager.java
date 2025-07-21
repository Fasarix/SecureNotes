package com.example.securenotes.security;

import static com.example.securenotes.security.SecurePrefsManager.getDatabasePrefs;
import static com.example.securenotes.security.SecurePrefsManager.getPinPrefs;
import static com.example.securenotes.utils.Constants.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {

    private static final String TAG = "CryptoManager";

    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        byte[] iv = cipher.getIV();
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    public static String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[IV_SIZE];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext);
    }

    public static byte[] deriveKey(String pin, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 100_000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static SecretKey toSecretKey(byte[] rawKey) {
        return new SecretKeySpec(rawKey, "AES");
    }

    public static String saveEncryptedFile(Context context, Uri sourceUri, String fileName) throws Exception {
        MasterKey masterKey = SecurePrefsManager.getMasterKey(context);

        File dir = new File(context.getFilesDir(), "encrypted_files");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Impossibile creare directory encrypted_files");
        }

        File targetFile = new File(dir, fileName);
        if (targetFile.exists() && !targetFile.delete()) {
            Log.w(TAG, "Impossibile eliminare file esistente: " + targetFile.getAbsolutePath());
        }

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                targetFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = encryptedFile.openFileOutput()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (true) {
                assert in != null;
                if ((bytesRead = in.read(buffer)) == -1) break;
                out.write(buffer, 0, bytesRead);
            }
        }

        return targetFile.getAbsolutePath();
    }

    public static Uri decryptFile(Context context, String encryptedFilePath, String fileName) throws Exception {
        File inFile = new File(encryptedFilePath);

        File tempDir = new File(context.getCacheDir(), "decrypted");
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IllegalStateException("Impossibile creare directory decrypted");
        }

        File outFile = new File(tempDir, fileName);
        if (outFile.exists() && !outFile.delete()) {
            Log.w(TAG, "Impossibile eliminare file decriptato esistente: " + outFile.getAbsolutePath());
        }

        MasterKey masterKey = SecurePrefsManager.getMasterKey(context);

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                inFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream in = encryptedFile.openFileInput();
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", outFile);
    }

    public static boolean isKeyPresent() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.containsAlias(KEY_ALIAS);
    }

    public static void initializeAESKey() throws Exception {
        if (!isKeyPresent()) {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build();

            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(spec);
            generator.generateKey();
            Log.i(TAG, "AES key generata e salvata nel KeyStore");
        }
    }

    public static SecretKey getAESKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    public static boolean deleteFileInEncryptedDir(Context context, String encryptedFileName) {
        File filesDir = new File(context.getFilesDir(), "encrypted_files");
        File fileToDelete = new File(filesDir, encryptedFileName);

        if (fileToDelete.exists()) {
            boolean deleted = fileToDelete.delete();
            if (deleted) {
                Log.d(TAG, "File crittografato eliminato: " + fileToDelete.getAbsolutePath());
                return true;
            } else {
                Log.e(TAG, "Impossibile eliminare il file crittografato: " + fileToDelete.getAbsolutePath());
                return false;
            }
        } else {
            Log.w(TAG, "Il file crittografato non esiste: " + fileToDelete.getAbsolutePath());
            return true;
        }
    }

    public static void encryptZip(File inputZip, File outputFile, SecretKey key, byte[] salt) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(salt);
            fos.write(iv);

            try (InputStream is = new FileInputStream(inputZip);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static void decryptZip(File encryptedFile, File outputZip, SecretKey key) throws Exception {
        try (FileInputStream fis = new FileInputStream(encryptedFile)) {
            byte[] salt = new byte[16];
            int readSalt = fis.read(salt);
            if (readSalt != salt.length) throw new IllegalStateException("Salt mancante o file corrotto");

            byte[] iv = new byte[12];
            int readIv = fis.read(iv);
            if (readIv != iv.length) throw new IllegalStateException("IV mancante o file corrotto");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 FileOutputStream fos = new FileOutputStream(outputZip)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }
    }

    public static byte[] getSaltDatabaseInByte(Context context) throws Exception {
        SharedPreferences prefs = getDatabasePrefs(context);
        String saltBase64 = prefs.getString(DATABASE_SALT, null);
        if (saltBase64 != null) {
            return Base64.decode(saltBase64, Base64.NO_WRAP);
        } else {
            byte[] newSalt = generateSalt();
            prefs.edit().putString(DATABASE_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP)).apply();
            return newSalt;
        }
    }

    public static String getSaltDatabaseInString(Context context) throws Exception {
        SharedPreferences prefs = getPinPrefs(context);
        return prefs.getString(SALT_KEY, null);
    }

    public static String getDatabasePin(Context context) throws Exception {
        SharedPreferences pref = SecurePrefsManager.getDatabasePrefs(context);
        return pref.getString(DATABASE_PIN, null);
    }

    public static byte[] getDatabaseSecretKeyInByte(Context context) throws Exception {
        String pin = getDatabasePin(context);
        byte[] salt = getSaltDatabaseInByte(context);
        KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 100_000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    public static String convertSaltByteToString(byte[] salt) {
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static byte[] convertSaltStringToByte(String saltBase64) {
        return Base64.decode(saltBase64, Base64.NO_WRAP);
    }
}
