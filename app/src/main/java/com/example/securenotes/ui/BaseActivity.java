package com.example.securenotes.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.securenotes.security.SessionManager;
import com.example.securenotes.ui.auth.AuthenticationWrapperActivity;
import com.example.securenotes.utils.Constants;
import com.example.securenotes.utils.Constants.AuthRequestType;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean lockTriggered = false;
    private boolean isAppInBackground = false;

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (SessionManager.shouldShowLockScreen(this) && !lockTriggered) {
                lockTriggered = true;
                launchLockScreen(AuthRequestType.SESSION_TIMEOUT);
            }
        } catch (Exception e) {
            Log.e("BaseActivity", "Error in onResume", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isAppInBackground) {
            isAppInBackground = false;

            try {
                if (SessionManager.shouldShowLockScreen(this)) {
                    launchLockScreen(AuthRequestType.SESSION_TIMEOUT);
                }
            } catch (Exception e) {
                Log.e("BaseActivity", "Error in onStart", e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            isAppInBackground = true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            if (SessionManager.isSessionExpired(getApplicationContext())) {
                SessionManager.clearSession();
                launchLockScreen(AuthRequestType.LOGIN);
                return false;
            } else {
                long remaining = SessionManager.getRemainingTime(getApplicationContext());
                Log.d("SESSION_TIME", "Remaining time: " + (remaining / 1000) + " sec");
            }
        } catch (Exception e) {
            Log.e("BaseActivity", "Error in dispatchTouchEvent", e);
        }

        return super.dispatchTouchEvent(ev);
    }

    private void launchLockScreen(AuthRequestType type) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(this, AuthenticationWrapperActivity.class);
            intent.putExtra(Constants.AUTH_REQUEST_TYPE, type.name());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    public static void applySavedTheme(Context context) {
        boolean isDark = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
