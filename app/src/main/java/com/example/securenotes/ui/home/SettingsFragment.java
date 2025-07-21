package com.example.securenotes.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.securenotes.R;
import com.example.securenotes.ui.auth.AuthenticationWrapperActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.SettingsViewModel;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SettingsViewModel viewModel;
    private ActivityResultLauncher<Intent> importBackupLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        importBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            viewModel.importBackupFromUri(requireContext(), uri);
                        } else {
                            Toast.makeText(requireContext(), "Backup non selezionato", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        viewModel.backupStatus.observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.importStatus.observe(this, status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_LONG).show();
            }
        });

        // Dark Mode
        SwitchPreferenceCompat darkModePref = findPreference("dark_mode");
        if (darkModePref != null) {
            darkModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                viewModel.setDarkMode(requireContext(), enabled);
                return true;
            });
        }

        // Session Timeout
        ListPreference timeoutPreference = findPreference("session_timeout");
        if (timeoutPreference != null) {
            try {
                int currentTimeout = SettingsViewModel.getSessionTimeout(requireContext());
                timeoutPreference.setValue(String.valueOf(currentTimeout));
                timeoutPreference.setSummary(currentTimeout + " minuti");
            } catch (Exception e) {
                e.printStackTrace();
            }

            timeoutPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    int minutes = Integer.parseInt(newValue.toString());
                    SettingsViewModel.setSessionTimeout(requireContext(), minutes);
                    preference.setSummary(minutes + " minuti");
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }

        // Set Password
        Preference setPasswordPref = findPreference("set_password");
        if (setPasswordPref != null) {
            setPasswordPref.setOnPreferenceClickListener(preference -> {
                viewModel.showSetPasswordDialog(requireContext());
                return true;
            });
        }

        // Perform Backup
        Preference backupPref = findPreference("perform_backup");
        if (backupPref != null) {
            backupPref.setOnPreferenceClickListener(preference -> {
                if (viewModel.isBackupPasswordSet(requireContext())) {
                    Toast.makeText(requireContext(), "Imposta prima la password per il backup", Toast.LENGTH_LONG).show();
                    return true;
                }
                viewModel.startBackup(requireContext());
                return true;
            });
        }

        // Import Backup
        Preference importPref = findPreference("import_backup");
        if (importPref != null) {
            importPref.setOnPreferenceClickListener(preference -> {
                if (viewModel.isBackupPasswordSet(requireContext())) {
                    Toast.makeText(requireContext(), "Imposta prima la password per il backup", Toast.LENGTH_LONG).show();
                    return true;
                }
                openFilePicker();
                return true;
            });
        }

        // Change PIN
        Preference changePinPref = findPreference("change_pin");
        if (changePinPref != null) {
            changePinPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(requireContext(), AuthenticationWrapperActivity.class);
                intent.putExtra(Constants.AUTH_REQUEST_TYPE, Constants.AuthRequestType.UPDATE_PIN.name());
                startActivity(intent);
                return true;
            });
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        importBackupLauncher.launch(intent);
    }
}
