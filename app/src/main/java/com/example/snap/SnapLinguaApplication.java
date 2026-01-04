package com.example.snap;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

public class SnapLinguaApplication extends Application {
    
    private static SnapLinguaApplication instance;
    
    public static SnapLinguaApplication getInstance() {
        return instance;
    }
    
    /**
     * Obtiene un Context con el idioma correcto aplicado.
     * Usar este método para Toast, Dialog, etc.
     */
    public static Context getLanguageContext(Context context) {
        if (instance != null) {
            return instance.createLanguageContext(context);
        }
        return context;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        android.util.Log.d("SnapLinguaApp", "Application onCreate");
        
        // Verificar si hay sesión activa
        SharedPreferences sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String activeUser = sessionPrefs.getString("active_email", null);
        
        // Verificar si es el primer inicio de la aplicación
        SharedPreferences prefs = getSharedPreferences("app_state", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        
        // Limpiar idiomas si es el primer inicio O si no hay sesión activa
        if (isFirstLaunch || activeUser == null || activeUser.trim().isEmpty()) {
            // Marcar que ya no es el primer inicio
            if (isFirstLaunch) {
                prefs.edit().putBoolean("is_first_launch", false).apply();
            }
            
            // Limpiar idiomas actuales para que use los idiomas por defecto del invitado
            clearCurrentLanguages();
            android.util.Log.d("SnapLinguaApp", "Cleared current languages (first launch or no active session)");
        }
    }
    
    /**
     * Limpia los idiomas guardados de la sesión actual.
     * Se llama al iniciar la app por primera vez.
     */
    private void clearCurrentLanguages() {
        SharedPreferences languagePrefs = getSharedPreferences("current_languages", MODE_PRIVATE);
        languagePrefs.edit().clear().apply();
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateResources(base));
    }
    
    private Context updateResources(Context context) {
        String languageCode = getLanguageCodeFromPrefs(context);
        android.util.Log.d("SnapLinguaApp", "updateResources with language: " + languageCode);
        
        java.util.Locale locale = getLocaleFromCode(languageCode);
        java.util.Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }
    
    private Context createLanguageContext(Context context) {
        String languageCode = getLanguageCodeFromPrefs(context);
        android.util.Log.d("SnapLinguaApp", "createLanguageContext - userId from session: " + getActiveUserId(context) + ", language: " + languageCode);
        
        java.util.Locale locale = getLocaleFromCode(languageCode);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        
        Context newContext = context.createConfigurationContext(config);
        
        // Verificar que el contexto tiene el locale correcto
        String testString = newContext.getResources().getString(R.string.ajustes);
        android.util.Log.d("SnapLinguaApp", "Context locale: " + newContext.getResources().getConfiguration().locale + ", test string 'ajustes': " + testString);
        
        return newContext;
    }
    
    private String getActiveUserId(Context context) {
        SharedPreferences sessionPrefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE);
        return sessionPrefs.getString("active_user", "guest");
    }
    
    private String getLanguageCodeFromPrefs(Context context) {
        String userId = getActiveUserId(context);
        
        // Si no hay usuario activo, usar español
        if (userId.equals("guest") || userId.isEmpty()) {
            android.util.Log.d("SnapLinguaApp", "No active user, using Spanish");
            return "es";
        }
        
        String prefsName = "SnapPrefs_" + userId;
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "es");
        
        android.util.Log.d("SnapLinguaApp", "User: " + userId + ", language: " + languageCode + " from prefs: " + prefsName);
        
        return languageCode;
    }
    
    private java.util.Locale getLocaleFromCode(String languageCode) {
        switch (languageCode) {
            case "zh":
                return java.util.Locale.SIMPLIFIED_CHINESE;
            case "ko":
                return java.util.Locale.KOREAN;
            default:
                return new java.util.Locale(languageCode);
        }
    }
}
