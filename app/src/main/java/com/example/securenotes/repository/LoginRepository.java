package com.example.securenotes.repository;

import android.content.Context;

import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;

import com.example.securenotes.security.BiometricAuthManager;
import com.example.securenotes.security.PinAuthManager;
import com.example.securenotes.utils.Constants.AuthResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginRepository {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void authenticatePin(Context context, String pin, Consumer<AuthResult> callback) {
        executor.execute(() -> {
            try {
                boolean verified = PinAuthManager.verifyPIN(context, pin);
                if (verified) {
                    callback.accept(AuthResult.success());
                } else {
                    callback.accept(AuthResult.error("PIN errato"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(AuthResult.error("Errore interno"));
            }
        });
    }

    public static boolean isBiometricAvailable(Context context) {
        return BiometricAuthManager.isAvailable(context) && BiometricAuthManager.isBiometricEnabled(context);
    }

    public static void authenticateBiometric(FragmentActivity activity, Consumer<AuthResult> callback) {
        BiometricAuthManager biometric = new BiometricAuthManager(activity, new BiometricAuthManager.Callback() {
            @Override
            public void onSuccess() {
                callback.accept(AuthResult.success());
            }

            @Override
            public void onFailure() {
                callback.accept(AuthResult.error("Autenticazione fallita"));
            }

            @Override
            public void onError(String error) {
                callback.accept(AuthResult.error(error));
            }
        });
        biometric.authenticate("Autenticazione richiesta", "Usa impronta o viso");
    }
}
