package com.example.securenotes.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.security.BiometricAuthManager;
import com.example.securenotes.security.SessionManager;
import com.example.securenotes.ui.home.HomeActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.SetupViewModel;

public class SetupFragment extends Fragment {

    private SetupViewModel setupViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText pinEditText = view.findViewById(R.id.et_pin);
        EditText confirmPinEditText = view.findViewById(R.id.et_pin_confirm);
        Button confirmButton = view.findViewById(R.id.btn_confirm_pin);

        setupViewModel = new ViewModelProvider(this).get(SetupViewModel.class);

        setupViewModel.authResult.observe(getViewLifecycleOwner(), result -> {
            if (result.status == Constants.AuthResult.Status.SUCCESS) {
                handleSuccessfulSetup();
            } else {
                handleSetupError(result.message, pinEditText, confirmPinEditText);
            }
        });

        confirmButton.setOnClickListener(v -> {
            String pin = pinEditText.getText().toString().trim();
            String confirmPin = confirmPinEditText.getText().toString().trim();
            setupViewModel.init(pin, confirmPin, requireContext());
        });
    }

    private void handleSuccessfulSetup() {
        Intent intent = new Intent(requireContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (BiometricAuthManager.isAvailable(requireContext())) {
            BiometricAuthManager biometricManager = new BiometricAuthManager(requireActivity(), new BiometricAuthManager.Callback() {
                @Override
                public void onSuccess() {
                    BiometricAuthManager.setBiometricEnabled(requireContext(), true);
                    SessionManager.startSession();
                    startActivity(intent);
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onError(String error) {
                    SessionManager.startSession();
                    startActivity(intent);
                }
            });
            biometricManager.authenticate("Permetti autenticazione biometrica", "Usa impronta o viso");
        } else {
            SessionManager.startSession();
            startActivity(intent);
        }
    }

    private void handleSetupError(String errorMessage, EditText pinEditText, EditText confirmPinEditText) {
        switch (errorMessage) {
            case Constants.SIZE:
                pinEditText.setError("Il PIN deve avere 6 cifre");
                break;
            case Constants.NO_MATCH:
                confirmPinEditText.setError("I PIN non corrispondono");
                break;
            default:
                break;
        }
    }
}
