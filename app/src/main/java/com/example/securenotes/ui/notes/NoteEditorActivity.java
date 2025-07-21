package com.example.securenotes.ui.notes;

import static com.example.securenotes.utils.Constants.*;

import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.fragment.app.Fragment;

import com.example.securenotes.R;
import com.example.securenotes.ui.BaseActivity;

public class NoteEditorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        setupStatusBar();
        setupToolbar();
        loadNoteFragment();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green_dark));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            boolean isEdit = getIntent().getBooleanExtra(EDIT, false);
            getSupportActionBar().setTitle(isEdit ? "Modifica Nota" : "Nuova Nota");
        }
    }

    private void loadNoteFragment() {
        boolean isEdit = getIntent().getBooleanExtra(EDIT, false);
        long noteId = getIntent().getLongExtra(NOTE_ID, -1);

        Fragment fragment = (isEdit && noteId != -1)
                ? EditNoteFragment.newInstance(noteId)
                : new NewNoteFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
