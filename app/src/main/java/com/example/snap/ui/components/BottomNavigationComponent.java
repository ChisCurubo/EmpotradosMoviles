package com.example.snap.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.snap.R;
import com.example.snap.utils.NavigationManager;
import com.example.snap.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

/**
 * Componente reutilizable para la barra de navegación inferior.
 * Puede ser usado en cualquier Activity para proporcionar navegación consistente.
 */
public class BottomNavigationComponent extends LinearLayout {
    
    private MaterialButton btnTexto, btnCamara, btnAudio, btnUsuario;
    private NavigationManager navigationManager;
    private SessionManager sessionManager;
    private NavigationListener navigationListener;
    private String currentScreen;
    
    public interface NavigationListener {
        void onTextoClicked();
        void onCamaraClicked();
        void onAudioClicked();
        void onUsuarioClicked();
    }
    
    public BottomNavigationComponent(Context context) {
        super(context);
        init(context);
    }
    
    public BottomNavigationComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public BottomNavigationComponent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // NO inflar el layout aquí - el componente YA contiene los hijos del XML
        // Solo inicializar managers
        navigationManager = new NavigationManager(context);
        sessionManager = new SessionManager(context);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Inicializar vistas después de que el XML se haya inflado completamente
        btnTexto = findViewById(R.id.nav_texto);
        btnCamara = findViewById(R.id.nav_camara);
        btnAudio = findViewById(R.id.nav_audio);
        btnUsuario = findViewById(R.id.nav_usuario);
        
        // Configurar listeners por defecto
        setupDefaultListeners();
    }
    
    /**
     * Configura los listeners por defecto para navegación
     */
    private void setupDefaultListeners() {
        btnTexto.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onTextoClicked();
            } else {
                navigationManager.navigateToMain();
            }
        });
        
        btnCamara.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onCamaraClicked();
            } else {
                // Por defecto redirige a texto
                navigationManager.navigateToMain();
                Toast.makeText(getContext(), "Modo Cámara (Próximamente)", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnAudio.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onAudioClicked();
            } else {
                Toast.makeText(getContext(), "Modo Audio (Próximamente)", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnUsuario.setOnClickListener(v -> {
            if (navigationListener != null) {
                navigationListener.onUsuarioClicked();
            } else {
                handleDefaultUserNavigation();
            }
        });
    }
    
    /**
     * Maneja la navegación por defecto del botón usuario
     */
    private void handleDefaultUserNavigation() {
        // Navegar a estadísticas siempre, sin requerir login
        navigationManager.navigateToStatistics();
    }
    
    /**
     * Establece un listener personalizado para la navegación
     */
    public void setNavigationListener(NavigationListener listener) {
        this.navigationListener = listener;
    }
    
    /**
     * Marca una pantalla como activa
     */
    public void setActiveScreen(String screenName) {
        this.currentScreen = screenName;
        // Aquí podrías agregar lógica para resaltar el botón activo
        resetButtonStates();
        
        switch (screenName) {
            case "texto":
                // Resaltar botón texto si quieres
                break;
            case "camara":
                // Resaltar botón cámara
                break;
            case "audio":
                // Resaltar botón audio
                break;
            case "usuario":
                // Resaltar botón usuario
                break;
        }
    }
    
    /**
     * Resetea el estado visual de todos los botones
     */
    private void resetButtonStates() {
        // Puedes agregar lógica para resetear colores/estilos
    }
    
    /**
     * Habilita o deshabilita la navegación
     */
    public void setNavigationEnabled(boolean enabled) {
        btnTexto.setEnabled(enabled);
        btnCamara.setEnabled(enabled);
        btnAudio.setEnabled(enabled);
        btnUsuario.setEnabled(enabled);
    }
    
    /**
     * Actualiza el estado del botón usuario basado en la sesión
     */
    public void updateUserButtonState() {
        if (sessionManager.isLoggedIn()) {
            btnUsuario.setText("Perfil");
        } else {
            btnUsuario.setText("Usuario");
        }
    }
}
