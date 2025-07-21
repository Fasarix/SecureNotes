package com.example.securenotes.ui.auth;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.securenotes.R;
import com.example.securenotes.model.AppDatabase;
import com.example.securenotes.security.PinAuthManager;
import com.example.securenotes.utils.Constants;
import com.scottyab.rootbeer.RootBeer;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.loadLibrary("sqlcipher");

        setContentView(R.layout.activity_welcome);

        checkRoot();

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
    private void checkRoot() {
        RootBeer rootBeer = new RootBeer(this);

        if (rootBeer.isRooted()) {
            new AlertDialog.Builder(this)
                    .setTitle("Security Alert")
                    .setMessage("This device appears to be rooted. For your security, the app will now close.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                    .show();
        }
    }
}
