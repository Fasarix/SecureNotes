package com.example.securenotes.ui.notes;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.model.entity.NoteEntity;
import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.model.relation.NoteWithTag;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.NotesViewModel;
import com.example.securenotes.viewmodel.TagViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class NewNoteFragment extends Fragment {

    private static final long SELF_DESTRUCT_DELAY_MS = 10 * 1000;

    private EditText titleEditText, contentEditText;
    private Spinner tagSpinner;
    private CheckBox selfDestructCheckbox;

    private NotesViewModel notesViewModel;
    private TagViewModel tagViewModel;

    private List<TagEntity> tagList = new ArrayList<>();
    private SecretKey aesKeyForDbEncryption;

    private long existingNoteId = -1;
    private NoteWithTag loadedNote;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    private boolean isSaving = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupViewModels();
        setupCryptoKey();
        setupObservers();
    }

    private void initViews(View view) {
        titleEditText = view.findViewById(R.id.edit_title);
        contentEditText = view.findViewById(R.id.edit_content);
        tagSpinner = view.findViewById(R.id.spinner_tags);
        selfDestructCheckbox = view.findViewById(R.id.checkbox_self_destruct);

        titleEditText.addTextChangedListener(autoSaveTextWatcher);
        contentEditText.addTextChangedListener(autoSaveTextWatcher);
        tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { scheduleAutoSave(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupViewModels() {
        notesViewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        notesViewModel.init();

        tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        tagViewModel.init(requireContext());
    }

    private void setupCryptoKey() {
        try {
            CryptoManager.initializeAESKey();
            aesKeyForDbEncryption = CryptoManager.getAESKey();
            if (aesKeyForDbEncryption == null) {
                Toast.makeText(requireContext(), "Errore: Chiave di crittografia non disponibile.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Errore chiave crittografia: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupObservers() {
        tagViewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            tagList = tags;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, getTagNames(tags));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(adapter);

            if (loadedNote != null && loadedNote.tag != null) {
                selectTagInSpinner(loadedNote.tag);
            }
        });

        if (getArguments() != null) {
            existingNoteId = getArguments().getLong(Constants.NOTE_ID, -1);
            boolean isEdit = getArguments().getBoolean(Constants.EDIT, false);

            if (isEdit && existingNoteId != -1) {
                notesViewModel.getDecryptedNoteById(existingNoteId).observe(getViewLifecycleOwner(), noteWithTag -> {
                    if (noteWithTag != null && noteWithTag.note != null) {
                        loadedNote = noteWithTag;
                        titleEditText.setText(noteWithTag.note.decryptedTitle);
                        contentEditText.setText(noteWithTag.note.decryptedContent);
                        if (tagList != null && noteWithTag.tag != null) {
                            selectTagInSpinner(noteWithTag.tag);
                        }
                    }
                });
            }
        }
    }

    private void selectTagInSpinner(TagEntity tagToSelect) {
        for (int i = 0; i < tagList.size(); i++) {
            if (tagList.get(i).id == tagToSelect.id) {
                tagSpinner.setSelection(i);
                break;
            }
        }
    }

    private List<String> getTagNames(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        for (TagEntity tag : tags) names.add(tag.name);
        return names;
    }

    private final TextWatcher autoSaveTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { scheduleAutoSave(); }
    };

    private void scheduleAutoSave() {
        if (autoSaveRunnable != null) handler.removeCallbacks(autoSaveRunnable);
        autoSaveRunnable = this::autoSave;
        handler.postDelayed(autoSaveRunnable, 1000);
    }

    private void autoSave() {
        if (isSaving || aesKeyForDbEncryption == null || tagList == null || tagList.isEmpty()) return;

        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        int selectedPosition = tagSpinner.getSelectedItemPosition();

        if (title.isEmpty() || content.isEmpty() || selectedPosition == Spinner.INVALID_POSITION) return;

        isSaving = true;
        long now = System.currentTimeMillis();

        try {
            if (existingNoteId != -1 && loadedNote != null && loadedNote.note != null) {
                // Aggiorna nota esistente
                loadedNote.note.titleEncrypted = CryptoManager.encrypt(title, aesKeyForDbEncryption);
                loadedNote.note.contentEncrypted = CryptoManager.encrypt(content, aesKeyForDbEncryption);
                loadedNote.note.tagId = tagList.get(selectedPosition).id;
                loadedNote.note.updatedAt = now;
                notesViewModel.update(loadedNote.note);
                Log.d("NewNoteFragment", "Nota aggiornata automaticamente");
            } else {
                // Crea nuova nota
                Long selfDestructAt = selfDestructCheckbox.isChecked()
                        ? now + SELF_DESTRUCT_DELAY_MS
                        : null;

                NoteEntity newNote = new NoteEntity(
                        title, content,
                        tagList.get(selectedPosition).id,
                        now, now,
                        aesKeyForDbEncryption,
                        selfDestructAt
                );
                notesViewModel.insert(newNote);
                Log.d("NewNoteFragment", "Nuova nota salvata automaticamente");
            }
        } catch (Exception e) {
            Log.e("NewNoteFragment", "Errore durante salvataggio automatico: " + e.getMessage());
        }

        isSaving = false;
    }
}
