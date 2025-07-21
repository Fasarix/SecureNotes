package com.example.securenotes.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.repository.LoginRepository;
import com.example.securenotes.security.SessionManager;
import com.example.securenotes.ui.home.HomeActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.utils.Constants.AuthRequestType;
import com.example.securenotes.viewmodel.LoginViewModel;

import java.util.Objects;

public class LoginFragment extends Fragment {

    private LoginViewModel loginViewModel;
    private EditText pinEditText;
    private Button loginButton;
    private TextView label;
    private View bar;

    private AuthRequestType requestType = AuthRequestType.LOGIN;

    public static LoginFragment newInstance(AuthRequestType type) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(Constants.AUTH_REQUEST_TYPE, type.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
    }

    private void parseArguments() {
        if (getArguments() != null) {
            String name = getArguments().getString(Constants.AUTH_REQUEST_TYPE);
            if (name != null) {
                try {
                    requestType = AuthRequestType.valueOf(name);
                } catch (IllegalArgumentException e) {
                    requestType = AuthRequestType.LOGIN;
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupViewModel();
        attemptBiometricAuthIfAvailable();

        loginButton.setOnClickListener(v -> onLoginButtonClicked());
    }

    private void bindViews(View view) {
        pinEditText = view.findViewById(R.id.et_login_pin);
        loginButton = view.findViewById(R.id.btn_login_pin);
        label = view.findViewById(R.id.text_choose_pin);
        bar = view.findViewById(R.id.under_bar);
    }

    private void setupViewModel() {
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loginViewModel.authResult.observe(getViewLifecycleOwner(), result -> {
            if (result.status == Constants.AuthResult.Status.SUCCESS) {
                handleAuthSuccess();
            } else {
                pinEditText.setError(result.message);
            }
        });
    }

    private void attemptBiometricAuthIfAvailable() {
        // Usa post per garantire che la view sia già pronta
        pinEditText.post(() -> {
            if (LoginRepository.isBiometricAvailable(requireContext())) {
                loginViewModel.authenticateBiometric(requireActivity());
            } else {
                showPinUI();
            }
        });
    }

    private void showPinUI() {
        label.setVisibility(View.VISIBLE);
        bar.setVisibility(View.VISIBLE);
        pinEditText.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
    }

    private void onLoginButtonClicked() {
        String pin = pinEditText.getText().toString().trim();
        if (pin.isEmpty()) {
            pinEditText.setError("Inserisci il PIN");
            return;
        }
        loginViewModel.authenticatePin(requireContext(), pin);
    }

    private void handleAuthSuccess() {
        SessionManager.startSession();

        if (Objects.requireNonNull(requestType) == AuthRequestType.DECRYPT_FILE) {
            setResultAndFinish();
        } else {
            navigateToHome();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(requireContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setResultAndFinish() {
        requireActivity().setResult(AppCompatActivity.RESULT_OK);
        requireActivity().finish();
    }
}
