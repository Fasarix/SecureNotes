package com.example.securenotes.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotes.R;
import com.example.securenotes.adapter.NotesAdapter;
import com.example.securenotes.ui.notes.NoteEditorActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.NotesViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class NotesFragment extends Fragment {

    private NotesViewModel noteViewModel;
    private FloatingActionButton addNoteFab;
    private NotesAdapter adapter;

    private ActionMode actionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true); // per menu azione
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        addNoteFab = view.findViewById(R.id.fab_add_note);

        adapter = new NotesAdapter();
        recyclerView.setAdapter(adapter);

        noteViewModel = new ViewModelProvider(this).get(NotesViewModel.class);
        noteViewModel.init();

        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            if (actionMode != null && notes.isEmpty()) {
                actionMode.finish();
            }
        });

        addNoteFab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NoteEditorActivity.class);
            intent.putExtra(Constants.EDIT, false);
            startActivity(intent);
        });

        adapter.setOnNoteClickListener(note -> {
            if (actionMode == null) {
                Intent intent = new Intent(requireContext(), NoteEditorActivity.class);
                intent.putExtra(Constants.EDIT, true);
                intent.putExtra(Constants.NOTE_ID, note.note.id);
                startActivity(intent);
            } else {
                toggleSelection(note.note.id);
            }
        });

        adapter.setOnNoteLongClickListener(note -> toggleSelection(note.note.id));

        adapter.setOnSelectionChangedListener(selectedCount -> {
            if (selectedCount > 0 && actionMode == null) {
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
                animateFab(false);
            } else if (selectedCount == 0 && actionMode != null) {
                actionMode.finish();
            }
            if (actionMode != null) {
                actionMode.setTitle(selectedCount + " selezionate");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.clearSelection();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void toggleSelection(long noteId) {
        adapter.toggleSelection(noteId);
    }

    private void animateFab(boolean show) {
        if (show) {
            addNoteFab.setVisibility(View.VISIBLE);
            addNoteFab.setAlpha(0f);
            addNoteFab.animate().alpha(1f).setDuration(200).start();
        } else {
            addNoteFab.animate().alpha(0f).setDuration(200).withEndAction(() ->
                    addNoteFab.setVisibility(View.GONE)
            ).start();
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_notes_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                List<Long> selectedIds = adapter.getSelectedNoteIds();
                noteViewModel.deleteNotesByIds(selectedIds);
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            actionMode = null;
            animateFab(true);
        }
    };
}
