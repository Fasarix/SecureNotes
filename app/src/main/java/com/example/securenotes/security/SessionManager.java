package com.example.securenotes.security;

import android.content.Context;

import com.example.securenotes.viewmodel.SettingsViewModel;

public class SessionManager {
    private static long lastActiveTime = 0;

    public static void startSession() {
        lastActiveTime = System.currentTimeMillis();
    }

    public static boolean isSessionExpired(Context context) throws Exception {
        int timeoutMinutes = SettingsViewModel.getSessionTimeout(context);
        long timeoutMillis = timeoutMinutes * 60_000L;
        long elapsed = System.currentTimeMillis() - lastActiveTime;
        return elapsed > timeoutMillis;
    }

    public static boolean isSessionStarted() {
        return lastActiveTime > 0;
    }

    public static void clearSession() {
        lastActiveTime = 0;
    }

    public static long getRemainingTime(Context context) throws Exception {
        int timeoutMinutes = SettingsViewModel.getSessionTimeout(context);
        long timeoutMillis = timeoutMinutes * 60_000L;
        long elapsed = System.currentTimeMillis() - lastActiveTime;
        return Math.max(timeoutMillis - elapsed, 0);
    }

    public static boolean shouldShowLockScreen(Context context) throws Exception {
        return !isSessionStarted() || isSessionExpired(context);
    }
}
