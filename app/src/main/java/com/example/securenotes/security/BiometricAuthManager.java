package com.example.securenotes.security;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.securenotes.utils.Constants;

import java.util.concurrent.Executor;

public class BiometricAuthManager {

    public interface Callback {
        void onSuccess();
        void onFailure();
        void onError(String error);
    }

    private final BiometricPrompt biometricPrompt;

    public BiometricAuthManager(FragmentActivity activity, Callback callback) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt.AuthenticationCallback authCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                callback.onFailure();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                callback.onError(errString.toString());
            }
        };
        biometricPrompt = new BiometricPrompt(activity, executor, authCallback);
    }

    public void authenticate(String title, String subtitle) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    public static boolean isAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        try {
            SecurePrefsManager.getBiometricsPrefs(context)
                    .edit()
                    .putBoolean(Constants.BIOMETRIC_ENABLED, enabled)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isBiometricEnabled(Context context) {
        try {
            return SecurePrefsManager.getBiometricsPrefs(context)
                    .getBoolean(Constants.BIOMETRIC_ENABLED, false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
