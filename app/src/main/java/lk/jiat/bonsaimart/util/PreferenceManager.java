package lk.jiat.bonsaimart.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import lk.jiat.bonsaimart.model.User;

public class PreferenceManager {

    // ============================================================
    // SHARED PREFERENCES KEYS
    // ============================================================
    private static final String PREF_NAME = "bonsai_prefs";
    private static final String KEY_USER = "user_data";
    private static final String KEY_LAST_SEARCH = "last_search";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences prefs;
    private final Gson gson;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // ============================================================
    // SAVE USER AS JSON (Gson)
    // ============================================================
    public void saveUser(User user) {
        if (user == null) return;
        String json = gson.toJson(user);  // Convert User → JSON
        prefs.edit().putString(KEY_USER, json).apply();
    }

    // ============================================================
    // GET USER FROM JSON (Gson)
    // ============================================================
    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);  // Convert JSON → User
    }

    // ============================================================
    // CLEAR USER (on logout)
    // ============================================================
    public void clearUser() {
        prefs.edit().remove(KEY_USER).apply();
    }

    // ============================================================
    // SAVE LAST SEARCH
    // ============================================================
    public void saveLastSearch(String query) {
        prefs.edit().putString(KEY_LAST_SEARCH, query).apply();
    }

    public String getLastSearch() {
        return prefs.getString(KEY_LAST_SEARCH, "");
    }

    // ============================================================
    // SEARCH HISTORY (comma-separated)
    // ============================================================
    public void addToSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) return;

        String history = prefs.getString(KEY_SEARCH_HISTORY, "");
        String newHistory = query + "," + history;

        // Keep only last 10 searches
        String[] parts = newHistory.split(",");
        StringBuilder sb = new StringBuilder();
        int count = Math.min(parts.length, 10);
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append(parts[i]);
        }

        prefs.edit().putString(KEY_SEARCH_HISTORY, sb.toString()).apply();
    }

    public String getSearchHistory() {
        return prefs.getString(KEY_SEARCH_HISTORY, "");
    }

    public void clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply();
    }

    // ============================================================
    // REMEMBER ME
    // ============================================================
    public void setRememberMe(boolean remember) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply();
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    // ============================================================
    // PROFILE IMAGE UPDATE FLAG
    // ============================================================
    private static final String KEY_PROFILE_UPDATED = "profile_updated";

    public void setProfileImageUpdated(boolean updated) {
        prefs.edit().putBoolean(KEY_PROFILE_UPDATED, updated).apply();
    }

    public boolean isProfileImageUpdated() {
        return prefs.getBoolean(KEY_PROFILE_UPDATED, false);
    }

    public void clearProfileImageUpdated() {
        prefs.edit().putBoolean(KEY_PROFILE_UPDATED, false).apply();
    }
}