package com.example.securenotes.ui.notes;

import static com.example.securenotes.utils.Constants.NOTE_ID;

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
import com.example.securenotes.model.entity.TagEntity;
import com.example.securenotes.model.relation.NoteWithTag;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.viewmodel.NotesViewModel;
import com.example.securenotes.viewmodel.TagViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class EditNoteFragment extends Fragment {

    private EditText titleEditText, contentEditText;
    private Spinner tagSpinner;

    private NotesViewModel notesViewModel;
    private TagViewModel tagViewModel;

    private List<TagEntity> tagList;
    private NoteWithTag loadedNote;
    private SecretKey aesKeyForDbEncryption;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    private Runnable autoSaveRunnable;
    private boolean isSaving = false;

    public static EditNoteFragment newInstance(long noteId) {
        EditNoteFragment fragment = new EditNoteFragment();
        Bundle args = new Bundle();
        args.putLong(NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleEditText = view.findViewById(R.id.edit_title);
        contentEditText = view.findViewById(R.id.edit_content);
        tagSpinner = view.findViewById(R.id.spinner_tags);
        CheckBox selfDestructCheckbox = view.findViewById(R.id.checkbox_self_destruct);
        selfDestructCheckbox.setVisibility(View.GONE);

        notesViewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        notesViewModel.init();

        tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        tagViewModel.init(requireContext());

        try {
            CryptoManager.initializeAESKey();
            aesKeyForDbEncryption = CryptoManager.getAESKey();
            if (aesKeyForDbEncryption == null) {
                Toast.makeText(requireContext(), "Errore: Chiave di crittografia non disponibile.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Errore chiave crittografia: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        long noteId = getArguments() != null ? getArguments().getLong(NOTE_ID, -1) : -1;
        if (noteId != -1) {
            loadNote(noteId);
        } else {
            Toast.makeText(requireContext(), "Errore: ID nota non valido.", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        observeTags();

        titleEditText.addTextChangedListener(autoSaveTextWatcher);
        contentEditText.addTextChangedListener(autoSaveTextWatcher);
        tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                scheduleAutoSave();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeTags() {
        tagViewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            this.tagList = tags;

            List<String> tagNames = new ArrayList<>();
            for (TagEntity tag : tags) tagNames.add(tag.name);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, tagNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(adapter);

            if (loadedNote != null && loadedNote.tag != null) {
                for (int i = 0; i < tags.size(); i++) {
                    if (tags.get(i).id == loadedNote.tag.id) {
                        tagSpinner.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void loadNote(long id) {
        notesViewModel.getDecryptedNoteById(id).observe(getViewLifecycleOwner(), noteWithTag -> {
            if (noteWithTag == null || noteWithTag.note == null) {
                Toast.makeText(requireContext(), "Nota non trovata.", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
                return;
            }

            loadedNote = noteWithTag;
            titleEditText.setText(noteWithTag.note.decryptedTitle);
            contentEditText.setText(noteWithTag.note.decryptedContent);

            if (tagList != null && loadedNote.tag != null) {
                for (int i = 0; i < tagList.size(); i++) {
                    if (tagList.get(i).id == loadedNote.tag.id) {
                        tagSpinner.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private final TextWatcher autoSaveTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            scheduleAutoSave();
        }
    };

    private void scheduleAutoSave() {
        if (autoSaveRunnable != null) handler.removeCallbacks(autoSaveRunnable);
        autoSaveRunnable = this::autoSave;
        handler.postDelayed(autoSaveRunnable, 1000);
    }

    private void autoSave() {
        if (isSaving || loadedNote == null || aesKeyForDbEncryption == null || tagList == null || tagList.isEmpty())
            return;

        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        int selectedPosition = tagSpinner.getSelectedItemPosition();

        if (title.isEmpty() || content.isEmpty() || selectedPosition == Spinner.INVALID_POSITION)
            return;

        isSaving = true;
        executor.execute(() -> {
            try {
                loadedNote.note.titleEncrypted = CryptoManager.encrypt(title, aesKeyForDbEncryption);
                loadedNote.note.contentEncrypted = CryptoManager.encrypt(content, aesKeyForDbEncryption);
                loadedNote.note.tagId = tagList.get(selectedPosition).id;
                loadedNote.note.updatedAt = System.currentTimeMillis();

                notesViewModel.update(loadedNote.note);
                Log.d("EditNoteFragment", "Salvataggio automatico completato");
            } catch (Exception e) {
                Log.e("EditNoteFragment", "Errore nel salvataggio automatico: " + e.getMessage());
            } finally {
                isSaving = false;
            }
        });
    }
}
