package com.example.securenotes.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotes.R;
import com.example.securenotes.security.CryptoManager;
import com.example.securenotes.utils.FileUtils;
import com.example.securenotes.adapter.FilesAdapter;
import com.example.securenotes.ui.files.FileEditorActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.FilesViewModel;
import com.example.securenotes.ui.auth.AuthenticationWrapperActivity;
import com.example.securenotes.model.relation.FileWithTag;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

public class FilesFragment extends Fragment {

    private FilesViewModel filesViewModel;
    private FloatingActionButton fabAddFile;
    private FilesAdapter adapter;
    private ActionMode actionMode;

    private FileWithTag pendingFileToOpen;

    private ActivityResultLauncher<Intent> loginLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && pendingFileToOpen != null) {
                        openFile(pendingFileToOpen);
                    } else {
                        Toast.makeText(requireContext(), "Autenticazione richiesta per aprire il file annullata.", Toast.LENGTH_SHORT).show();
                    }
                    pendingFileToOpen = null;
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_files);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new FilesAdapter();
        recyclerView.setAdapter(adapter);

        filesViewModel = new ViewModelProvider(this).get(FilesViewModel.class);
        filesViewModel.init();
        filesViewModel.getDecryptedFilesWithTag().observe(getViewLifecycleOwner(), files -> {
            adapter.submitList(files);
            if (actionMode != null && files.isEmpty()) {
                actionMode.finish();
            }
        });

        fabAddFile = view.findViewById(R.id.fab_add_file);
        fabAddFile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FileEditorActivity.class);
            intent.putExtra(Constants.EDIT, false);
            startActivity(intent);
        });

        adapter.setOnFileClickListener(fileWithTag -> {
            if (actionMode == null) {
                pendingFileToOpen = fileWithTag;
                requestAuthenticationForFile();
            } else {
                toggleSelection(fileWithTag.file.id);
            }
        });

        adapter.setOnFileOptionsClickListener(fileWithTag -> {
            if (actionMode != null) {
                toggleSelection(fileWithTag.file.id);
            } else {
                Intent intent = new Intent(requireContext(), FileEditorActivity.class);
                intent.putExtra(Constants.EDIT, true);
                intent.putExtra(Constants.FILE_ID, fileWithTag.file.id);
                startActivity(intent);
            }
        });

        adapter.setOnSelectionChangedListener(selectedCount -> {
            if (selectedCount > 0) {
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
                }
                assert actionMode != null;
                actionMode.setTitle(selectedCount + " selezionati");
            } else if (actionMode != null) {
                actionMode.finish();
            }
        });
    }

    private void requestAuthenticationForFile() {
        Intent intent = new Intent(requireContext(), AuthenticationWrapperActivity.class);
        intent.putExtra(Constants.AUTH_REQUEST_TYPE, Constants.AuthRequestType.DECRYPT_FILE.name());
        loginLauncher.launch(intent);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openFile(FileWithTag fileWithTag) {
        try {
            Log.d("FILE_OPEN_DEBUG", "Aprendo file: " + fileWithTag.file.decryptedTitle);
            File encryptedFile = new File(fileWithTag.file.decryptedFilePath);
            if (!encryptedFile.exists()) {
                Toast.makeText(requireContext(), "File criptato non trovato.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri decryptedUri = CryptoManager.decryptFile(
                    requireContext(),
                    fileWithTag.file.decryptedFilePath,
                    fileWithTag.file.decryptedTitle
            );

            String mimeType = FileUtils.getMimeTypeFromExtension(fileWithTag.file.decryptedFileType);
            if (mimeType.isEmpty()) {
                Toast.makeText(requireContext(), "Impossibile determinare il tipo di file.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(decryptedUri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Apri con"));
            } else {
                Toast.makeText(requireContext(), "Nessuna app disponibile per aprire questo tipo di file: " + mimeType, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("FILE_OPEN_DEBUG", "Errore apertura file", e);
            Toast.makeText(requireContext(), "Errore apertura file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleSelection(long fileId) {
        adapter.toggleSelection(fileId);
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_files_selection, menu);
            fabAddFile.setVisibility(View.GONE);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                List<Long> selectedIds = adapter.getSelectedFileIds();
                if (!selectedIds.isEmpty()) {
                    filesViewModel.deleteByIds(selectedIds);
                    Toast.makeText(requireContext(), "File eliminati.", Toast.LENGTH_SHORT).show();
                }
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            actionMode = null;
            fabAddFile.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().invalidateOptionsMenu();
    }
}
