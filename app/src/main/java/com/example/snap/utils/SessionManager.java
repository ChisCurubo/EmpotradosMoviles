package com.example.snap.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Gestiona sesiones de usuario en toda la app.
 * Centraliza toda la lógica de persistencia de sesión.
 */
public class SessionManager {
    
    private static final String PREF_NAME = "user_session";
    private static final String KEY_ACTIVE_EMAIL = "active_email";
    private static final String TAG = "SessionManager";
    
    private final SharedPreferences prefs;
    private final Context context;
    
    public SessionManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Guarda la sesión del usuario
     */
    public void saveSession(String email) {
        prefs.edit().putString(KEY_ACTIVE_EMAIL, email).apply();
        
        // También actualizar session_prefs para sincronizar el idioma por usuario
        SharedPreferences sessionPrefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE);
        sessionPrefs.edit().putString("active_user", email).apply();
        
        Log.d(TAG, "Sesión guardada: " + email + " (sincronizada en session_prefs)");
    }
    
    /**
     * Obtiene el usuario activo
     */
    public String getActiveUser() {
        return prefs.getString(KEY_ACTIVE_EMAIL, null);
    }
    
    /**
     * Verifica si hay una sesión activa
     */
    public boolean isLoggedIn() {
        String email = getActiveUser();
        return email != null && !email.trim().isEmpty();
    }
    
    /**
     * Cierra la sesión actual
     */
    public void logout() {
        prefs.edit().remove(KEY_ACTIVE_EMAIL).apply();
        
        // También restaurar session_prefs a guest para que use el idioma por defecto
        SharedPreferences sessionPrefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE);
        sessionPrefs.edit().putString("active_user", "guest").apply();
        
        // Limpiar idiomas actuales para que use los idiomas por defecto del invitado
        SharedPreferences languagePrefs = context.getSharedPreferences("current_languages", Context.MODE_PRIVATE);
        languagePrefs.edit().clear().apply();
        
        Log.d(TAG, "Sesión cerrada (restaurado a guest y limpiados idiomas actuales)");
    }
    
    /**
     * Limpia completamente las preferencias de sesión
     */
    public void clearSession() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Sesión completamente limpiada");
    }
}
