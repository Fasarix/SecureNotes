package com.example.securenotes.ui.files;

import static com.example.securenotes.utils.Constants.FILE_ID;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.model.entity.FileEntity;
import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.viewmodel.FilesViewModel;
import com.example.securenotes.viewmodel.TagViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class EditFileFragment extends Fragment {

    private EditText editTitle;
    private Spinner tagSpinner;

    private FilesViewModel filesViewModel;
    private List<TagEntity> tagList;

    private long fileId = -1L;
    private FileEntity loadedFile;

    private SecretKey aesKeyForDbEncryption;

    public static EditFileFragment newInstance(long fileId) {
        EditFileFragment fragment = new EditFileFragment();
        Bundle args = new Bundle();
        args.putLong(FILE_ID, fileId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            fileId = getArguments().getLong(FILE_ID, -1L);
        }

        try {
            CryptoManager.initializeAESKey();
            aesKeyForDbEncryption = CryptoManager.getAESKey();
        } catch (Exception e) {
            Log.e("EditFileFragment", "Errore caricamento chiave AES: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore caricamento chiave sicurezza.", Toast.LENGTH_LONG).show();
            requireActivity().finish();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTitle = view.findViewById(R.id.edit_file_title);
        tagSpinner = view.findViewById(R.id.spinner_file_tag);
        Button btnSave = view.findViewById(R.id.button_save_file);

        View btnPickFile = view.findViewById(R.id.button_pick_file);
        if (btnPickFile != null) btnPickFile.setVisibility(View.GONE);
        View selectedFileTextView = view.findViewById(R.id.text_selected_file);
        if (selectedFileTextView != null) selectedFileTextView.setVisibility(View.GONE);

        filesViewModel = new ViewModelProvider(this).get(FilesViewModel.class);
        filesViewModel.init();

        TagViewModel tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        tagViewModel.init(requireContext());

        tagViewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            tagList = tags;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, getTagNames(tags));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(adapter);

            loadAndPopulateFileData();
        });

        btnSave.setOnClickListener(v -> saveEditedFile());
    }

    private void loadAndPopulateFileData() {
        if (fileId == -1L) {
            Toast.makeText(requireContext(), "ID file non valido", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        filesViewModel.getDecryptedFileById(fileId).observe(getViewLifecycleOwner(), fileWithTag -> {
            if (fileWithTag != null && fileWithTag.file != null) {
                loadedFile = fileWithTag.file;
                editTitle.setText(loadedFile.decryptedTitle);

                if (fileWithTag.tag != null && tagList != null) {
                    for (int i = 0; i < tagList.size(); i++) {
                        if (tagList.get(i).id == fileWithTag.tag.id) {
                            tagSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "File non trovato o errore decrittazione", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        });
    }

    private void saveEditedFile() {
        String newTitle = editTitle.getText().toString().trim();
        if (newTitle.isEmpty()) {
            Toast.makeText(requireContext(), "Il titolo non può essere vuoto", Toast.LENGTH_SHORT).show();
            return;
        }
        if (loadedFile == null) {
            Toast.makeText(requireContext(), "File non caricato", Toast.LENGTH_SHORT).show();
            return;
        }
        if (aesKeyForDbEncryption == null) {
            Toast.makeText(requireContext(), "Chiave di sicurezza non disponibile", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String encryptedTitle = CryptoManager.encrypt(newTitle, aesKeyForDbEncryption);

            Long newTagId = null;
            if (tagList != null && !tagList.isEmpty()) {
                int pos = tagSpinner.getSelectedItemPosition();
                if (pos >= 0 && pos < tagList.size()) {
                    newTagId = tagList.get(pos).id;
                }
            }

            FileEntity updatedFile = new FileEntity(
                    loadedFile.id,
                    encryptedTitle,
                    loadedFile.filePathEncrypted,
                    newTagId,
                    loadedFile.fileTypeEncrypted,
                    loadedFile.creationDate,
                    System.currentTimeMillis()
            );

            filesViewModel.update(updatedFile);
            Toast.makeText(requireContext(), "File aggiornato con successo", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();

        } catch (Exception e) {
            Log.e("EditFileFragment", "Errore salvataggio: ", e);
            Toast.makeText(requireContext(), "Errore salvataggio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getTagNames(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        if (tags != null) {
            for (TagEntity t : tags) {
                names.add(t.name);
            }
        }
        return names;
    }
}
