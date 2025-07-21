package com.example.securenotes.ui.auth;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.securenotes.R;
import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.security.PinAuthManager;
import com.example.securenotes.utils.Constants;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.loadLibrary("sqlcipher");

        setContentView(R.layout.activity_welcome);

        boolean pinSet = PinAuthManager.isPinSet(this);
        Log.d(TAG, "PIN già impostato? " + pinSet);

        if (pinSet) {
            try {
                AppDatabase.initDB(getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Errore inizializzazione DB", e);
                throw new RuntimeException("Errore inizializzazione DB", e);
            }
        }

        Fragment fragment = pinSet
                ? LoginFragment.newInstance(Constants.AuthRequestType.LOGIN)
                : new SetupFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.welcome_fragment_container, fragment)
                .commit();
    }
}
