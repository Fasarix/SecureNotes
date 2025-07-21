package com.example.securenotes.ui.auth;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotes.R;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.viewmodel.LoginViewModel;

public class AuthenticationWrapperActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication_wrapper);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LoginViewModel loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        Constants.AuthRequestType requestType = Constants.AuthRequestType.LOGIN; // default
        if (getIntent().hasExtra(Constants.AUTH_REQUEST_TYPE)) {
            String typeName = getIntent().getStringExtra(Constants.AUTH_REQUEST_TYPE);
            try {
                requestType = Constants.AuthRequestType.valueOf(typeName);
            } catch (IllegalArgumentException ignored) {}
        }

        Fragment fragmentToLoad;
        switch (requestType) {
            case UPDATE_PIN:
                fragmentToLoad = new ChangePinFragment();
                setupToolbar("Modifica PIN", true);
                break;
            case DECRYPT_FILE:
                fragmentToLoad = LoginFragment.newInstance(Constants.AuthRequestType.DECRYPT_FILE);
                setupToolbar(null, false);
                break;
            case LOGIN:
            default:
                fragmentToLoad = LoginFragment.newInstance(Constants.AuthRequestType.LOGIN);
                setupToolbar(null, false);
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.auth_fragment_container, fragmentToLoad)
                .commit();

        loginViewModel.authResult.observe(this, result -> {
            if (result.status == Constants.AuthResult.Status.SUCCESS) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setupToolbar(@Nullable String title, boolean showBackButton) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        if (getSupportActionBar() != null) {
            if (title != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().show();
            } else {
                getSupportActionBar().hide();
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
