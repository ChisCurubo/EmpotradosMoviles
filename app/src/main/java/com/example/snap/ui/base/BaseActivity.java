package com.example.snap.ui.base;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.snap.R;
import com.example.snap.SnapLinguaApplication;
import com.example.snap.presentation.viewmodel.TranslationViewModel;
import com.example.snap.utils.NavigationManager;
import com.example.snap.utils.SessionManager;

/**
 * Actividad base que contiene funcionalidad común para todas las pantallas.
 * Proporciona acceso a managers y métodos utilitarios.
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    protected SessionManager sessionManager;
    protected NavigationManager navigationManager;
    protected TranslationViewModel viewModel;
    private String currentLanguage; // Track current language
    private android.content.Context languageContext; // Cache del contexto con idioma
    
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        // Aplicar idioma ANTES de crear el contexto base
        languageContext = applyLanguageToContext(newBase);
        super.attachBaseContext(languageContext);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentLanguage = getCurrentAppLanguage();
        
        super.onCreate(savedInstanceState);
        
        // Inicializar managers compartidos
        sessionManager = new SessionManager(this);
        navigationManager = new NavigationManager(this);
        viewModel = new ViewModelProvider(this).get(TranslationViewModel.class);
        
        // Obtener usuario del Intent o de sesión
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId != null) {
            sessionManager.saveSession(userId);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Verificar si el idioma cambió mientras estábamos en otra actividad
        String newLanguage = getCurrentAppLanguage();
        if (currentLanguage != null && !currentLanguage.equals(newLanguage)) {
            // El idioma cambió, recargar la actividad
            recreate();
            return;
        }
        
        // Las subclases pueden override para actualizar UI con sesión actualizada
        onSessionUpdated();
    }
    
    /**
     * Método que se llama cuando la sesión se actualiza.
     * Las subclases deben implementar para actualizar su UI.
     */
    protected void onSessionUpdated() {
        // Por defecto no hace nada, las subclases lo implementan si lo necesitan
    }
    
    /**
     * Obtiene el usuario actualmente logueado
     */
    protected String getCurrentUser() {
        return sessionManager.getActiveUser();
    }
    
    /**
     * Verifica si hay un usuario logueado
     */
    protected boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn();
    }
    
    /**
     * Muestra un mensaje corto
     */
    protected void showMessage(String message) {
        Toast.makeText(SnapLinguaApplication.getLanguageContext(this), message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Muestra un mensaje largo
     */
    protected void showLongMessage(String message) {
        Toast.makeText(SnapLinguaApplication.getLanguageContext(this), message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Cierra sesión y navega a la pantalla principal
     */
    protected void performLogout() {
        navigationManager.logoutAndNavigateToMain();
        showMessage(getString(R.string.sesion_cerrada));
    }
    
    /**
     * Aplica el idioma al contexto y devuelve un nuevo contexto con el idioma configurado
     */
    private android.content.Context applyLanguageToContext(android.content.Context context) {
        // Obtener userId desde session_prefs
        android.content.SharedPreferences sessionPrefs = context.getSharedPreferences("session_prefs", android.content.Context.MODE_PRIVATE);
        String userId = sessionPrefs.getString("active_user", "guest");
        
        // Obtener idioma de las preferencias del usuario
        String prefsName = "SnapPrefs_" + userId;
        android.content.SharedPreferences prefs = context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "es");
        
        // Log para debugging
        android.util.Log.d("BaseActivity", "===== APPLYING LANGUAGE =====");
        android.util.Log.d("BaseActivity", "User ID: " + userId);
        android.util.Log.d("BaseActivity", "Language Code: " + languageCode);
        android.util.Log.d("BaseActivity", "Prefs Name: " + prefsName);
        
        // Crear locale
        java.util.Locale locale;
        if (languageCode.equals("zh")) {
            locale = java.util.Locale.SIMPLIFIED_CHINESE;
        } else if (languageCode.equals("ko")) {
            locale = java.util.Locale.KOREAN;
        } else {
            locale = new java.util.Locale(languageCode);
        }
        
        java.util.Locale.setDefault(locale);
        android.util.Log.d("BaseActivity", "Locale set to: " + locale.toString());
        
        // Crear configuración con el locale  
        android.content.res.Configuration config = context.getResources().getConfiguration();
        android.content.res.Configuration newConfig = new android.content.res.Configuration(config);
        newConfig.setLocale(locale);
        
        android.util.Log.d("BaseActivity", "Config locale: " + newConfig.locale.toString());
        
        // Retornar contexto con el idioma aplicado
        android.content.Context newContext = context.createConfigurationContext(newConfig);
        android.util.Log.d("BaseActivity", "Context locale after creation: " + newContext.getResources().getConfiguration().locale.toString());
        android.util.Log.d("BaseActivity", "=============================");
        
        return newContext;
    }
    
    /**
     * Obtiene el idioma actual de las preferencias
     */
    private String getCurrentAppLanguage() {
        android.content.SharedPreferences sessionPrefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
        String userId = sessionPrefs.getString("active_user", "guest");
        
        String prefsName = "SnapPrefs_" + userId;
        android.content.SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        return prefs.getString("app_language", "es");
    }
}
