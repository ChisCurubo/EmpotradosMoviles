package com.example.snap.ui.components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.snap.R;
import com.example.snap.SnapLinguaApplication;

/**
 * Componente reutilizable para mostrar el resultado de la traducción.
 * Encapsula la lógica de visualización y acciones sobre el texto traducido.
 */
public class TranslationOutputComponent extends LinearLayout {
    
    private TextView tvOutput;
    private ImageView btnCopy, btnStar, btnVolume;
    
    private TranslationOutputListener listener;
    
    public interface TranslationOutputListener {
        void onSaveAsFavorite(String translatedText);
        void onPlayAudio(String translatedText);
    }
    
    public TranslationOutputComponent(Context context) {
        super(context);
        init(context);
    }
    
    public TranslationOutputComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
    }
    
    /**
     * Inicializa las vistas del componente
     */
    public void initializeViews(View rootView) {
        tvOutput = rootView.findViewById(R.id.tvOutput);
        btnCopy = rootView.findViewById(R.id.btnCopy);
        btnStar = rootView.findViewById(R.id.btnStar);
        btnVolume = rootView.findViewById(R.id.btnVolume);
        
        setupListeners();
    }
    
    /**
     * Configura los listeners
     */
    private void setupListeners() {
        // Botón copiar
        btnCopy.setOnClickListener(v -> copyToClipboard());
        
        // Botón favorito
        btnStar.setOnClickListener(v -> {
            String text = getOutputText();
            if (isValidTranslation(text) && listener != null) {
                listener.onSaveAsFavorite(text);
            } else {
                Toast.makeText(SnapLinguaApplication.getLanguageContext(getContext()), R.string.no_traduccion_guardar, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Botón audio
        btnVolume.setOnClickListener(v -> {
            String text = getOutputText();
            if (isValidTranslation(text) && listener != null) {
                listener.onPlayAudio(text);
            } else {
                Toast.makeText(SnapLinguaApplication.getLanguageContext(getContext()), R.string.no_audio_reproducir, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Establece el texto de salida
     */
    public void setOutputText(String text) {
        tvOutput.setText(text);
        // Animación de entrada
        tvOutput.setAlpha(0);
        tvOutput.animate().alpha(1).setDuration(500).start();
    }
    
    /**
     * Obtiene el texto de salida
     */
    public String getOutputText() {
        return tvOutput.getText() != null ? tvOutput.getText().toString() : "";
    }
    
    /**
     * Establece el texto por defecto
     */
    public void setDefaultText() {
        tvOutput.setText("La traducción aparecerá aquí");
    }
    
    /**
     * Muestra estado de carga
     */
    public void showLoading() {
        tvOutput.setText("Traduciendo...");
    }
    
    /**
     * Copia el texto al portapapeles
     */
    private void copyToClipboard() {
        String text = getOutputText();
        if (isValidTranslation(text)) {
            ClipboardManager clipboard = (ClipboardManager) getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Traducción", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(SnapLinguaApplication.getLanguageContext(getContext()), R.string.texto_copiado, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(getContext()), R.string.no_hay_copiar, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Verifica si el texto es una traducción válida
     */
    private boolean isValidTranslation(String text) {
        return text != null && 
               !text.isEmpty() && 
               !text.equals("La traducción aparecerá aquí") &&
               !text.equals("Traduciendo...");
    }
    
    /**
     * Establece el listener
     */
    public void setListener(TranslationOutputListener listener) {
        this.listener = listener;
    }
}
