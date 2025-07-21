package com.example.securenotes.ui.files;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.utils.FileUtils;
import com.example.securenotes.viewmodel.FilesViewModel;
import com.example.securenotes.viewmodel.TagViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class NewFileFragment extends Fragment {

    private EditText editTitle;
    private Spinner tagSpinner;
    private TextView selectedFileTextView;
    private Uri selectedFileUri;

    private FilesViewModel filesViewModel;
    private List<TagEntity> tagList = new ArrayList<>();

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private SecretKey aesKeyForDbEncryption;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editTitle = view.findViewById(R.id.edit_file_title);
        tagSpinner = view.findViewById(R.id.spinner_file_tag);
        selectedFileTextView = view.findViewById(R.id.text_selected_file);
        Button btnPickFile = view.findViewById(R.id.button_pick_file);
        Button btnSave = view.findViewById(R.id.button_save_file);

        filesViewModel = new ViewModelProvider(this).get(FilesViewModel.class);
        filesViewModel.init();

        TagViewModel tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        tagViewModel.init(requireContext());
        tagViewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            tagList = tags != null ? tags : new ArrayList<>();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item,
                    getTagNames(tagList));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(adapter);
        });

        // File picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        selectedFileTextView.setText("File selezionato: " + queryFileName(uri));
                    }
                });

        btnPickFile.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"*/*"}));

        btnSave.setOnClickListener(v -> saveEncryptedFile());

        try {
            CryptoManager.initializeAESKey();
            aesKeyForDbEncryption = CryptoManager.getAESKey();
        } catch (Exception e) {
            Log.e("NewFileFragment", "Errore caricamento chiave AES: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore caricamento chiave di sicurezza.", Toast.LENGTH_LONG).show();
            btnSave.setEnabled(false);
        }
    }

    private void saveEncryptedFile() {
        String title = editTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci un titolo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Seleziona un file", Toast.LENGTH_SHORT).show();
            return;
        }
        if (aesKeyForDbEncryption == null) {
            Toast.makeText(requireContext(), "Chiave di sicurezza non disponibile. Riprova.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Context context = requireContext();

            String fileType = FileUtils.detectFileType(context, selectedFileUri);

            String encryptedFilePath = CryptoManager.saveEncryptedFile(context, selectedFileUri, title + "." + fileType);

            Long tagId = null;
            int pos = tagSpinner.getSelectedItemPosition();
            if (pos >= 0 && !tagList.isEmpty()) {
                tagId = tagList.get(pos).id;
            }

            String encryptedTitle = CryptoManager.encrypt(title, aesKeyForDbEncryption);
            String encryptedFileType = CryptoManager.encrypt(fileType, aesKeyForDbEncryption);
            String encryptedFilePathForDb = CryptoManager.encrypt(encryptedFilePath, aesKeyForDbEncryption);

            long now = System.currentTimeMillis();

            FileEntity newFile = new FileEntity(encryptedTitle, encryptedFilePathForDb, tagId, encryptedFileType, now, now);
            filesViewModel.insert(newFile);

            Toast.makeText(context, "File salvato con successo", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();

        } catch (Exception e) {
            Log.e("NewFileFragment", "Errore salvataggio file", e);
            Toast.makeText(requireContext(), "Errore nel salvataggio del file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String queryFileName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) name = cursor.getString(index);
                }
            }
        }
        if (name == null) name = uri.getLastPathSegment();
        return name;
    }

    private List<String> getTagNames(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        for (TagEntity tag : tags) {
            names.add(tag.name);
        }
        return names;
    }
}
