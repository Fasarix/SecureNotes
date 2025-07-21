package com.example.securenotes.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.viewmodel.ChangePinViewModel;

public class ChangePinFragment extends Fragment {

    private EditText etCurrentPin, etNewPin, etConfirmNewPin;
    private ChangePinViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etCurrentPin = view.findViewById(R.id.et_current_pin);
        etNewPin = view.findViewById(R.id.et_new_pin);
        etConfirmNewPin = view.findViewById(R.id.et_confirm_new_pin);

        viewModel = new ViewModelProvider(this).get(ChangePinViewModel.class);

        view.findViewById(R.id.btn_change_pin_confirm).setOnClickListener(v -> onChangePinClicked());

        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result.success) {
                Toast.makeText(requireContext(), "PIN cambiato con successo", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            } else {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onChangePinClicked() {
        String currentPin = etCurrentPin.getText().toString().trim();
        String newPin = etNewPin.getText().toString().trim();
        String confirmPin = etConfirmNewPin.getText().toString().trim();

        if (currentPin.isEmpty()) {
            etCurrentPin.setError("Inserisci il PIN attuale");
            return;
        }
        if (newPin.length() != 6) {
            etNewPin.setError("Il nuovo PIN deve essere di 6 cifre");
            return;
        }
        if (!newPin.equals(confirmPin)) {
            etConfirmNewPin.setError("I PIN non corrispondono");
            return;
        }

        viewModel.changePin(requireContext(), currentPin, newPin, confirmPin);
    }
}
