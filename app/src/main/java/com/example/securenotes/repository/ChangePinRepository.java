package com.example.securenotes.repository;

import android.content.Context;

import com.example.securenotes.security.PinAuthManager;
import com.example.securenotes.viewmodel.ChangePinViewModel;

public class ChangePinRepository {

    public static ChangePinViewModel.Result changePin(Context context, String oldPin, String newPin) {
        try {
            if (!PinAuthManager.verifyPIN(context, oldPin)) {
                return new ChangePinViewModel.Result(false, "PIN attuale errato");
            }

            PinAuthManager.savePIN(context, newPin);

            return new ChangePinViewModel.Result(true, "PIN cambiato");
        } catch (Exception e) {
            e.printStackTrace();
            return new ChangePinViewModel.Result(false, "Errore durante la procedura di cambio PIN");
        }
    }
}
