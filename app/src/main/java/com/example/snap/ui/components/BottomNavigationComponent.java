package com.example.snap.ui.components;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.snap.R;
import com.example.snap.utils.NavigationManager;
import com.example.snap.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class BottomNavigationComponent extends LinearLayout {

    private MaterialButton btnTexto, btnCamara, btnAudio, btnUsuario;
    private NavigationManager navigationManager;
    private SessionManager sessionManager;
    private NavigationListener navigationListener;
    private String currentScreen = "";

    private int activeColor;
    private int inactiveColor;

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
        navigationManager = new NavigationManager(context);
        sessionManager = new SessionManager(context);

        activeColor   = ContextCompat.getColor(context, R.color.purple_header);
        inactiveColor = ContextCompat.getColor(context, R.color.text_gray);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        btnTexto   = findViewById(R.id.nav_texto);
        btnCamara  = findViewById(R.id.nav_camara);
        btnAudio   = findViewById(R.id.nav_audio);
        btnUsuario = findViewById(R.id.nav_usuario);

        setupListeners();
    }

    private void setupListeners() {
        btnTexto.setOnClickListener(v -> navigateTo("texto"));
        btnCamara.setOnClickListener(v -> navigateTo("camara"));
        btnAudio.setOnClickListener(v -> navigateTo("audio"));
        btnUsuario.setOnClickListener(v -> navigateTo("usuario"));
    }

    private void navigateTo(String targetScreen) {

        //Si ya estamos en la pantalla → no hace nada
        if (targetScreen.equals(currentScreen)) return;

        //Notificar listener (si existe) pero NO cancelar navegación
        if (navigationListener != null) {
            switch (targetScreen) {
                case "texto": navigationListener.onTextoClicked(); break;
                case "camara": navigationListener.onCamaraClicked(); break;
                case "audio": navigationListener.onAudioClicked(); break;
                case "usuario": navigationListener.onUsuarioClicked(); break;
            }
        }

        //Navegación centralizada dentro del componente
        switch (targetScreen) {
            case "texto":
                navigationManager.navigateToMain();
                break;

            case "camara":
                navigationManager.navigateToCamera();
                break;

            case "audio":
                navigationManager.navigateToVoz();
                break;

            case "usuario":
                navigationManager.navigateToStatistics();
                break;
        }

        finishCurrentActivity();
    }

    /** Cierra la activity actual para simular tabs */
    private void finishCurrentActivity() {
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).finish();
        }
    }

    /** Marca qué tab está activa */
    public void setActiveScreen(String screenName) {
        this.currentScreen = screenName;
        updateButtonsVisuals();
    }

    private void updateButtonsVisuals() {
        setButtonState(btnTexto, false);
        setButtonState(btnCamara, false);
        setButtonState(btnAudio, false);
        setButtonState(btnUsuario, false);

        switch (currentScreen) {
            case "texto":   setButtonState(btnTexto, true); break;
            case "camara":  setButtonState(btnCamara, true); break;
            case "audio":   setButtonState(btnAudio, true); break;
            case "usuario": setButtonState(btnUsuario, true); break;
        }
    }

    private void setButtonState(MaterialButton btn, boolean isActive) {
        if (btn == null) return;
        int color = isActive ? activeColor : inactiveColor;
        btn.setTextColor(color);
        btn.setIconTint(android.content.res.ColorStateList.valueOf(color));
    }

    /** Listener opcional (solo notifica, no controla navegación) */
    public void setNavigationListener(NavigationListener listener) {
        this.navigationListener = listener;
    }

    /** Helper recomendado para Activities */
    public void attachToScreen(String screenName) {
        setActiveScreen(screenName);
        updateUserButtonState();
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public void updateUserButtonState() {
        if (btnUsuario != null) {
            Context context = getContext();
            String text = sessionManager.isLoggedIn() 
                ? context.getString(R.string.perfil) 
                : context.getString(R.string.usuario);
            btnUsuario.setText(text);
        }
    }
}
