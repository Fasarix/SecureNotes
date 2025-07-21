package com.example.securenotes.ui.files;

import static com.example.securenotes.utils.Constants.FILE_ID;
import static com.example.securenotes.utils.Constants.EDIT;

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

public class FileEditorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_editor);

        setupWindowInsetsAndStatusBar();

        setupToolbar();

        launchAppropriateFragment();
    }

    private void setupWindowInsetsAndStatusBar() {
        Window window = getWindow();

        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(ContextCompat.getColor(this, R.color.green_dark));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            int topInset = insets.getInsets(Type.systemBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            boolean isEdit = getIntent().getBooleanExtra(EDIT, false);
            getSupportActionBar().setTitle(isEdit ? "Modifica File" : "Nuovo File");
        }
    }

    private void launchAppropriateFragment() {
        boolean isEdit = getIntent().getBooleanExtra(EDIT, false);
        long fileId = getIntent().getLongExtra(FILE_ID, -1);

        Fragment fragment;
        if (isEdit && fileId != -1) {
            fragment = EditFileFragment.newInstance(fileId);
        } else {
            fragment = new NewFileFragment();
        }

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
