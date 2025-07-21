package com.example.securenotes.utils;

public final class Constants {

    // === Biometric Authentication ===
    public static final String BIOMETRIC_PREF_NAME = "biometric_secure_prefs";
    public static final String BIOMETRIC_ENABLED = "biometric_enabled";

    // === PIN Authentication ===
    public static final String PIN_PREF = "pin_secure_prefs";
    public static final String PIN_HASH = "pin_hash";
    public static final String SALT_KEY = "pin_salt";

    // === Encryption ===
    public static final String KEY_ALIAS = "main_aes_key";
    public static final int IV_SIZE = 12;
    public static final int TAG_LENGTH = 128;

    // === App Settings ===
    public static final String SETTINGS_PREF = "user_settings";
    public static final String SESSION_TIMEOUT = "session_timeout_minutes";

    // === Extras for Intents/Fragments ===
    public static final String EDIT = "is_edit";
    public static final String NOTE_ID = "note_id";
    public static final String FILE_ID = "file_id";
    public static final String AUTH_REQUEST_TYPE = "auth_request_type";

    // === Database ===
    public static final String DATABASE_PREF = "db_pref";
    public static final String DATABASE_PIN = "db_pin";
    public static final String DATABASE_SALT = "db_salt";

    // === Backup ===
    public static final String BACKUP_PREF = "backup_pref";
    public static final String BACKUP_PASSWORD = "backup_pw";
    public static final String BACKUP_SALT = "backup_salt";
    public static final int ZIP_BUFFER_SIZE = 4096;

    // === Misc ===
    public static final String SIZE = "Size";
    public static final String NO_MATCH = "NoMatch";
    public static final long INITIAL_DELAY_SECONDS = 60;

    private Constants() {
    }

    public enum AuthRequestType {
        LOGIN,
        DECRYPT_FILE,
        UPDATE_PIN,
        SESSION_TIMEOUT
    }

    public static final class AuthResult {

        public enum Status { SUCCESS, ERROR }

        public final Status status;
        public final String message;

        private AuthResult(Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public static AuthResult success() {
            return new AuthResult(Status.SUCCESS, null);
        }

        public static AuthResult error(String msg) {
            return new AuthResult(Status.ERROR, msg);
        }
    }
}
