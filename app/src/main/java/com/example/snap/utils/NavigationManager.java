package com.example.snap.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.example.snap.TextActivity;
import com.example.snap.LoginActivity;
import com.example.snap.StatisticsActivity;
import com.example.snap.camara.Camara;

/**
 * Gestiona la navegación entre pantallas.
 * Centraliza la lógica de navegación y paso de datos.
 */
public class NavigationManager {
    
    private final Context context;
    private final SessionManager sessionManager;
    
    public NavigationManager(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }
    
    /**
     * Navega a la pantalla principal (TextActivity)
     */
    public void navigateToMain() {
        navigateToMain(false);
    }
    
    /**
     * Navega a la pantalla principal con opción de limpiar stack
     */
    public void navigateToMain(boolean clearStack) {
        Intent intent = new Intent(context, TextActivity.class);
        String userId = sessionManager.getActiveUser();
        if (userId != null) {
            intent.putExtra("USER_ID", userId);
        }
        
        if (clearStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        
        context.startActivity(intent);
        
        if (clearStack && context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * Navega específicamente a la pantalla de cámara
     */
    public void navigateToCamera() {
        Intent intent = new Intent(context, Camara.class);
        String userId = sessionManager.getActiveUser();
        if (userId != null) {
            intent.putExtra("USER_ID", userId);
        }
        context.startActivity(intent);
    }
    
    /**
     * Navega a la pantalla de estadísticas
     */
    public void navigateToStatistics() {
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        Intent intent = new Intent(context, StatisticsActivity.class);
        intent.putExtra("USER_ID", sessionManager.getActiveUser());
        context.startActivity(intent);
    }
    
    /**
     * Navega a la pantalla de login
     */
    public void navigateToLogin() {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }
    
    /**
     * Navega a una actividad específica con usuario
     */
    public void navigateToActivityWithUser(Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        String userId = sessionManager.getActiveUser();
        if (userId != null) {
            intent.putExtra("USER_ID", userId);
        }
        context.startActivity(intent);
    }
    
    /**
     * Cierra sesión y navega a la pantalla principal
     */
    public void logoutAndNavigateToMain() {
        sessionManager.logout();
        navigateToMain(true);
    }
}