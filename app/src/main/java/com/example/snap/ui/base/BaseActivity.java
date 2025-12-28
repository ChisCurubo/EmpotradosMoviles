package com.example.snap.ui.base;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar idioma antes de crear la actividad
        applyAppLanguage();
        
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Muestra un mensaje largo
     */
    protected void showLongMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Cierra sesión y navega a la pantalla principal
     */
    protected void performLogout() {
        navigationManager.logoutAndNavigateToMain();
        showMessage("Sesión cerrada - Modo Invitado");
    }
    
    /**
     * Aplica el idioma de la aplicación según las preferencias del usuario
     */
    private void applyAppLanguage() {
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            userId = getSharedPreferences("session_prefs", MODE_PRIVATE).getString("active_user", "guest");
        }
        
        String prefsName = "SnapPrefs_" + userId;
        android.content.SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "es");
        
        java.util.Locale locale;
        if (languageCode.equals("zh")) {
            locale = java.util.Locale.SIMPLIFIED_CHINESE;
        } else if (languageCode.equals("ko")) {
            locale = java.util.Locale.KOREAN;
        } else {
            locale = new java.util.Locale(languageCode);
        }
        
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
