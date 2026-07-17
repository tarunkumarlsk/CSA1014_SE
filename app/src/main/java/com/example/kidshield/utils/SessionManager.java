package com.example.kidshield.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Unified Session Management for KidShield.
 * Use this class EXCLUSIVELY to read/write persistent state.
 */
public class SessionManager {
    private static final String PREF_NAME = "kidshield_prefs";
    
    // Key Constants
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PARENT_ID = "parent_id";
    private static final String KEY_CHILD_ID = "child_id";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_IS_CHILD_LINKED = "is_child_linked";
    private static final String KEY_IS_CHILD_SETUP_DONE = "is_child_setup_done";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_HOME_LAT = "home_lat";
    private static final String KEY_HOME_LNG = "home_lng";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private static SessionManager instance;

    private SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // --- Session Persistence ---
    public void saveParentSession(int parentId, String name) {
        editor.putInt(KEY_PARENT_ID, parentId);
        editor.putInt(KEY_USER_ID, parentId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_ROLE, "parent");
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    public void saveChildLinking(int childId, String childName, boolean isSetupDone) {
        editor.putInt(KEY_CHILD_ID, childId);
        editor.putString(KEY_NAME, childName);
        editor.putString(KEY_ROLE, "child");
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_CHILD_LINKED, true);
        editor.putBoolean(KEY_IS_CHILD_SETUP_DONE, isSetupDone);
        editor.commit();
    }

    // --- Field Specific Setters ---
    public void saveParentEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    public void saveParentPhone(String phone) {
        editor.putString(KEY_PHONE, phone);
        editor.commit();
    }

    public void saveDeviceName(String deviceName) {
        editor.putString(KEY_DEVICE_NAME, deviceName);
        editor.commit();
    }

    public void setSelectedChild(int childId, String childName) {
        editor.putInt(KEY_CHILD_ID, childId);
        editor.putString(KEY_NAME, childName); // For display title
        editor.commit();
    }

    // --- Getters ---
    public int getParentId() { return pref.getInt(KEY_PARENT_ID, -1); }
    public int getChildId() { return pref.getInt(KEY_CHILD_ID, -1); }
    public int getSelectedChildId() { return getChildId(); } // Alias for SplashActivity
    public String getRole() { return pref.getString(KEY_ROLE, ""); }
    public String getName() { return pref.getString(KEY_NAME, ""); }
    public String getEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String getPhone() { return pref.getString(KEY_PHONE, ""); }
    public String getDeviceName() { return pref.getString(KEY_DEVICE_NAME, android.os.Build.MODEL); }
    public boolean isLoggedIn() { return pref.getBoolean(KEY_IS_LOGGED_IN, false); }
    public boolean isChildLinked() { return pref.getBoolean(KEY_IS_CHILD_LINKED, false); }
    public boolean isChildSetupDone() { return pref.getBoolean(KEY_IS_CHILD_SETUP_DONE, false); }

    // --- Network / Server ---
    public void saveServerIp(String ip) {
        editor.putString(KEY_SERVER_IP, ip);
        editor.commit();
    }
    public String getServerIp() {
        return pref.getString(KEY_SERVER_IP, "");
    }

    public void saveHomeLocation(double lat, double lng) {
        editor.putFloat(KEY_HOME_LAT, (float) lat);
        editor.putFloat(KEY_HOME_LNG, (float) lng);
        editor.commit();
    }
    public double getHomeLat() { return pref.getFloat(KEY_HOME_LAT, 0.0f); }
    public double getHomeLng() { return pref.getFloat(KEY_HOME_LNG, 0.0f); }

    public void clearSession() {
        String serverIp = getServerIp();
        editor.clear();
        editor.putString(KEY_SERVER_IP, serverIp);
        editor.commit();
    }
}
