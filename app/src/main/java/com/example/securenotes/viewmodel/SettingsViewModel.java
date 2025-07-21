package com.example.securenotes.viewmodel;

import static com.example.securenotes.utils.Constants.SESSION_TIMEOUT;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.relation.FileWithTag;
import com.example.securenotes.model.relation.NoteWithTag;
import com.example.securenotes.repository.FilesRepository;
import com.example.securenotes.repository.NotesRepository;
import com.example.securenotes.repository.TagRepository;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.security.SecurePrefsManager;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.utils.FileMetadata;
import com.example.securenotes.utils.NoteMetadata;
import com.example.securenotes.utils.ZipUtils;
import com.google.gson.Gson;

import java.io.*;
import java.util.*;
import javax.crypto.SecretKey;

public class SettingsViewModel extends ViewModel {

    private final MutableLiveData<String> _backupStatus = new MutableLiveData<>();
    public LiveData<String> backupStatus = _backupStatus;

    private final MutableLiveData<String> _importStatus = new MutableLiveData<>();
    public LiveData<String> importStatus = _importStatus;

    public void setDarkMode(Context context, boolean enabled) {
        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("dark_mode", enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public boolean isBackupPasswordSet(Context context) {
        try {
            SharedPreferences pref = SecurePrefsManager.getBackupPrefs(context);
            String pw = pref.getString(Constants.BACKUP_PASSWORD, null);
            return pw == null || pw.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    public void showSetPasswordDialog(Context context) {
        final EditText input = getEditText(context);

        new AlertDialog.Builder(context)
                .setTitle("Imposta password per il backup")
                .setMessage("Inserisci una password di 16 caratteri")
                .setView(input)
                .setPositiveButton("Conferma", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (password.length() != 16) {
                        Toast.makeText(context, "La password deve contenere 16 caratteri", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    salvaPasswordEncrypted(context, password);
                    Toast.makeText(context, "Password salvata", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancella", null)
                .show();
    }

    @NonNull
    private static EditText getEditText(Context context) {
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(16),
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (!Character.isLetterOrDigit(c)) {
                            return "";
                        }
                    }
                    return null;
                }
        });
        return input;
    }

    private void salvaPasswordEncrypted(Context context, String password) {
        try {
            SharedPreferences backupPrefs = SecurePrefsManager.getBackupPrefs(context);
            backupPrefs.edit().putString(Constants.BACKUP_PASSWORD, password).apply();
            backupPrefs.edit().putString(Constants.BACKUP_SALT, CryptoManager.convertSaltByteToString(CryptoManager.generateSalt())).apply();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Errore", Toast.LENGTH_SHORT).show();
        }
    }

    public static int getSessionTimeout(Context context) throws Exception {
        SharedPreferences prefs = SecurePrefsManager.getSettingsPrefs(context);
        return prefs.getInt(SESSION_TIMEOUT, 3);
    }

    public static void setSessionTimeout(Context context, int minutes) throws Exception {
        SharedPreferences prefs = SecurePrefsManager.getSettingsPrefs(context);
        prefs.edit().putInt(SESSION_TIMEOUT, minutes).apply();
    }

    public void startBackup(Context context) {
        new Thread(() -> {
            try {
                SecretKey aesKey = CryptoManager.getAESKey();
                NotesRepository noteRepo = new NotesRepository(context, aesKey);
                FilesRepository fileRepo = new FilesRepository(context, aesKey);

                List<NoteWithTag> notes = noteRepo.getAllNotesSync();
                List<FileWithTag> files = fileRepo.getAllFilesSync();

                File tempDir = new File(context.getCacheDir(), "backup_temp");
                if (tempDir.exists()) deleteRecursively(tempDir);
                if (!tempDir.mkdirs()) throw new RuntimeException("Errore creazione directory");

                writeNotesJson(tempDir, notes);
                writeFilesMetadataJson(tempDir, files);
                decryptFilesToTemp(context, tempDir, files);

                File zipFile = new File(context.getCacheDir(), "backup.zip");

                ZipUtils.zipDirectoryAsync(tempDir, zipFile, new ZipUtils.ZipCallback() {
                    @Override
                    public void onSuccess() {
                        try {
                            File encryptedZip = new File(context.getCacheDir(), "backup_secure.zip.aes");
                            SharedPreferences prefs = SecurePrefsManager.getBackupPrefs(context);
                            byte[] key = CryptoManager.deriveKey(Objects.requireNonNull(prefs.getString(Constants.BACKUP_PASSWORD, null)), CryptoManager.convertSaltStringToByte(Constants.BACKUP_SALT));
                            CryptoManager.encryptZip(zipFile, encryptedZip, CryptoManager.toSecretKey(key), CryptoManager.generateSalt());

                            saveEncryptedZipToDownloads(context, encryptedZip);

                            deleteRecursively(tempDir);
                            zipFile.delete();
                            encryptedZip.delete();

                            postStatus(_backupStatus, "Backup salvato in Download");
                        } catch (Exception e) {
                            e.printStackTrace();
                            postStatus(_backupStatus, "Errore durante il backup: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        postStatus(_backupStatus, "Errore durante compressione backup: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                postStatus(_backupStatus, "Errore durante il backup: " + e.getMessage());
            }
        }).start();
    }

    public void importBackupFromUri(Context context, Uri uri) {
        new Thread(() -> {
            try {
                SecretKey aesKey = CryptoManager.getAESKey();

                File zipEncrypted = new File(context.getCacheDir(), "import_backup.zip");
                try (InputStream in = context.getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(zipEncrypted)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while (true) {
                        assert in != null;
                        if ((len = in.read(buffer)) == -1) break;
                        out.write(buffer, 0, len);
                    }
                }

                File decryptedZip = new File(context.getCacheDir(), "import_backup_decrypted.zip");
                SharedPreferences prefs = SecurePrefsManager.getBackupPrefs(context);
                byte[] key = CryptoManager.deriveKey(Objects.requireNonNull(prefs.getString(Constants.BACKUP_PASSWORD, null)), CryptoManager.convertSaltStringToByte(Constants.BACKUP_SALT));
                CryptoManager.decryptZip(zipEncrypted, decryptedZip, CryptoManager.toSecretKey(key));

                File extractDir = new File(context.getCacheDir(), "import_backup_extract");
                if (extractDir.exists()) deleteRecursively(extractDir);
                extractDir.mkdirs();

                ZipUtils.unzipAsync(decryptedZip, extractDir, new ZipUtils.ZipCallback() {
                    @Override
                    public void onSuccess() {
                        try {
                            importNotes(context, aesKey, extractDir);
                            importFiles(context, aesKey, extractDir);
                            postStatus(_importStatus, "Importazione completata");
                        } catch (Exception e) {
                            e.printStackTrace();
                            postStatus(_importStatus, "Errore durante importazione: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        postStatus(_importStatus, "Errore estrazione backup: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                postStatus(_importStatus, "Errore importazione backup: " + e.getMessage());
            }
        }).start();
    }

    private void writeNotesJson(File dir, List<NoteWithTag> notes) throws IOException {
        File file = new File(dir, "notes.json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<NoteMetadata> exportNotes = new ArrayList<>();
            for (NoteWithTag note : notes) {
                exportNotes.add(new NoteMetadata(
                        note.note.decryptedTitle != null ? note.note.decryptedTitle : "",
                        note.note.decryptedContent != null ? note.note.decryptedContent : "",
                        note.tag != null ? note.tag.name : "",
                        note.note.createdAt,
                        note.note.updatedAt
                ));
            }
            writer.write(new Gson().toJson(exportNotes));
        }
    }

    private void writeFilesMetadataJson(File dir, List<FileWithTag> files) throws IOException {
        File file = new File(dir, "files_metadata.json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<FileMetadata> metadataList = new ArrayList<>();
            for (FileWithTag f : files) {
                metadataList.add(new FileMetadata(
                        f.file.decryptedTitle != null ? f.file.decryptedTitle : "",
                        f.tag != null ? f.tag.name : "",
                        f.file.decryptedFilePath != null ? new File(f.file.decryptedFilePath).getName() : "",
                        f.file.decryptedFileType != null ? f.file.decryptedFileType : "",
                        f.file.creationDate != null ? f.file.creationDate : 0L,
                        f.file.lastModifiedDate != null ? f.file.lastModifiedDate : 0L
                ));
            }
            writer.write(new Gson().toJson(metadataList));
        }
    }

    private void decryptFilesToTemp(Context context, File dir, List<FileWithTag> files) {
        try {
            MasterKey key = SecurePrefsManager.getMasterKey(context);
            for (FileWithTag file : files) {
                if (file.file.decryptedFilePath == null || file.file.decryptedFilePath.isEmpty()) continue;

                File input = new File(file.file.decryptedFilePath);
                if (!input.exists()) continue;

                String fileName = file.file.decryptedTitle != null ? file.file.decryptedTitle : "file_" + file.file.id;
                String extension = file.file.decryptedFileType != null ? "." + file.file.decryptedFileType : "";
                File output = new File(dir, fileName + extension);

                EncryptedFile encryptedFile = new EncryptedFile.Builder(
                        context,
                        input,
                        key,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();

                try (InputStream in = encryptedFile.openFileInput();
                     OutputStream out = new FileOutputStream(output)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void saveEncryptedZipToDownloads(Context context, File encryptedZip) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, "backup_secure.zip.aes");
        values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                : null;

        if (uri == null) throw new IOException("Errore creazione MediaStore");

        try (InputStream in = new FileInputStream(encryptedZip);
             OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("OutputStream nullo");
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
            out.flush();
        }
    }

    private void importNotes(Context context, SecretKey key, File dir) throws Exception {
        File json = new File(dir, "notes.json");
        if (!json.exists()) return;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(json))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        NoteMetadata[] array = new Gson().fromJson(sb.toString(), NoteMetadata[].class);
        NotesRepository notesRepo = new NotesRepository(context, key);
        TagRepository tagRepo = new TagRepository(context);

        for (NoteMetadata n : array) {
            Long tagId = null;
            if (n.tag != null && !n.tag.trim().isEmpty()) {
                tagId = tagRepo.getTagIdByNameSync(n.tag.trim());
            }

            NoteEntity newNote = new NoteEntity(
                    n.title != null ? n.title : "",
                    n.content != null ? n.content : "",
                    tagId,
                    n.createdAt,
                    n.updatedAt,
                    key,
                    false
            );
            notesRepo.insert(newNote);
        }
    }

    private void importFiles(Context context, SecretKey key, File dir) throws Exception {
        File json = new File(dir, "files_metadata.json");
        if (!json.exists()) return;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(json))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        FileMetadata[] array = new Gson().fromJson(sb.toString(), FileMetadata[].class);
        FilesRepository fileRepo = new FilesRepository(context, key);
        TagRepository tagRepo = new TagRepository(context);

        for (FileMetadata meta : array) {
            Long tagId = null;
            if (meta.tag != null && !meta.tag.trim().isEmpty()) {
                tagId = tagRepo.getTagIdByNameSync(meta.tag.trim());
            }

            File originalFile = new File(dir, meta.fileName);
            if (!originalFile.exists()) continue;

            try {
                Uri src = Uri.fromFile(originalFile);
                String newName = UUID.randomUUID().toString();
                String encryptedPath = CryptoManager.saveEncryptedFile(context, src, newName);

                FileEntity entity = new FileEntity(
                        CryptoManager.encrypt(meta.title != null ? meta.title : "", key),
                        CryptoManager.encrypt(encryptedPath, key),
                        tagId,
                        CryptoManager.encrypt(meta.fileType != null ? meta.fileType : "", key),
                        meta.createdAt,
                        meta.updatedAt
                );

                fileRepo.insert(entity);
            } catch (Exception ignored) {}
        }
    }

    private void deleteRecursively(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void postStatus(MutableLiveData<String> liveData, String message) {
        new Handler(Looper.getMainLooper()).post(() -> liveData.setValue(message));
    }
}
