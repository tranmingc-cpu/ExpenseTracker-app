package com.expensetracker_manager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class TokenManager {
    private static final String PREF_NAME = "ExpenseTrackerPrefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PASSCODE = "app_passcode";

    private static TokenManager instance;
    private SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private TokenManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        editor = sharedPreferences.edit();
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(String token) {
        editor.putString(KEY_JWT_TOKEN, token).commit();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null);
    }

    public void saveUserInfo(Long userId, String email, String name, String avatarUrl) {
        editor.putLong(KEY_USER_ID, userId != null ? userId : -1L);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_AVATAR, avatarUrl);
        editor.commit();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    public String getUserAvatar() {
        return sharedPreferences.getString(KEY_USER_AVATAR, "");
    }

    public Long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1L);
    }

    public void clear() {
        editor.clear().commit();
    }

    public void savePasscode(String passcode) {
        editor.putString(KEY_PASSCODE, passcode).commit();
    }

    public String getPasscode() {
        return sharedPreferences.getString(KEY_PASSCODE, null);
    }
}
