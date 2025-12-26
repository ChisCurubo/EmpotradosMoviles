package com.example.snap.utils;

import android.util.Patterns;

/**
 * Validaciones comunes.
 * Centraliza la lógica de validación de campos.
 */
public class ValidationHelper {
    
    /**
     * Valida que un email sea correcto
     */
    public static boolean isValidEmail(String email) {
        return email != null && 
               !email.trim().isEmpty() && 
               Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Valida que una contraseña cumpla los requisitos mínimos
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }
    
    /**
     * Valida que una contraseña cumpla requisitos específicos
     */
    public static boolean isValidPassword(String password, int minLength) {
        return password != null && password.length() >= minLength;
    }
    
    /**
     * Valida que un campo de texto no esté vacío
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * Obtiene mensaje de error para email
     */
    public static String getEmailError(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "El correo electrónico es requerido";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Correo electrónico inválido";
        }
        return null;
    }
    
    /**
     * Obtiene mensaje de error para contraseña
     */
    public static String getPasswordError(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "La contraseña es requerida";
        }
        if (password.length() < 4) {
            return "La contraseña debe tener al menos 4 caracteres";
        }
        return null;
    }
    
    /**
     * Valida campos de login
     */
    public static ValidationResult validateLoginFields(String email, String password) {
        String emailError = getEmailError(email);
        if (emailError != null) {
            return new ValidationResult(false, emailError);
        }
        
        String passwordError = getPasswordError(password);
        if (passwordError != null) {
            return new ValidationResult(false, passwordError);
        }
        
        return new ValidationResult(true, "Validación exitosa");
    }
    
    /**
     * Clase para encapsular el resultado de una validación
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
