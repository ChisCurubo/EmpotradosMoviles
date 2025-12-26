package com.example.snap.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.example.snap.R;
import com.example.snap.utils.LanguageHelper;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Componente reutilizable para la entrada de texto.
 * Responsabilidad: Manejar la escritura, limpiado y sugerencias (chips).
 *
 */
public class TranslationInputComponent extends LinearLayout {
    
    private TextInputEditText etInput;
    private ImageView btnClear;
    private Button chip1, chip2, chip3;
    private ProgressBar progressBar;
    
    private TranslationInputListener listener;
    
    public interface TranslationInputListener {
        void onTranslateRequested(String text);
        void onClearRequested();
    }
    
    public TranslationInputComponent(Context context) {
        super(context);
        init();
    }
    
    public TranslationInputComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);
    }
    
    /**
     * Inicializa las vistas buscando en la vista raíz proporcionada.
     * Útil cuando el componente no infla su propio layout sino que usa el de la Activity/Fragment.
     */
    public void initializeViews(View rootView) {
        etInput = rootView.findViewById(R.id.etInput);
        btnClear = rootView.findViewById(R.id.btnClear);
        
        chip1 = rootView.findViewById(R.id.chip1);
        chip2 = rootView.findViewById(R.id.chip2);
        chip3 = rootView.findViewById(R.id.chip3);
        
        progressBar = rootView.findViewById(R.id.progressBar);
        
        setupListeners();
        // Cargar chips por defecto (Español = 0)
        updateQuickTranslationChips(0); 
    }
    
    private void setupListeners() {
        // Botón limpiar
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                clear();
                if (listener != null) listener.onClearRequested();
            });
        }
        
        // Listener para los Chips
        View.OnClickListener chipListener = v -> {
            Button b = (Button) v;
            if (etInput != null) {
                etInput.setText(b.getText().toString());
                if (etInput.getText() != null) {
                    etInput.setSelection(etInput.getText().length()); // Mover cursor al final
                }
                requestTranslation();
            }
        };
        
        if (chip1 != null) chip1.setOnClickListener(chipListener);
        if (chip2 != null) chip2.setOnClickListener(chipListener);
        if (chip3 != null) chip3.setOnClickListener(chipListener);
        
        // Enter en el teclado
        if (etInput != null) {
            etInput.setOnEditorActionListener((v, actionId, event) -> {
                requestTranslation();
                return true;
            });
        }
    }
    
    private void requestTranslation() {
        String text = getInputText();
        if (listener != null && !text.isEmpty()) {
            listener.onTranslateRequested(text);
        }
    }
    
    /**
     * Actualiza los chips de traducción rápida.
     * Debe ser llamado externamente cuando cambia el idioma en LanguageSelector.
     */
    public void updateQuickTranslationChips(int languageIndex) {
        if (chip1 == null || chip2 == null || chip3 == null) return;
        
        try {
            String[] phrases = LanguageHelper.getQuickPhrasesByPosition(languageIndex);
            chip1.setText(phrases[0]);
            chip2.setText(phrases[1]);
            chip3.setText(phrases[2]);
        } catch (Exception e) {
            // Fallback por seguridad
            chip1.setText("Hola");
            chip2.setText("Gracias");
            chip3.setText("Adiós");
        }
    }
    
    public void clear() {
        if (etInput != null) etInput.setText("");
    }
    
    public String getInputText() {
        return (etInput != null && etInput.getText() != null) ? etInput.getText().toString().trim() : "";
    }
    
    public void setInputText(String text) {
        if (etInput != null) etInput.setText(text);
    }
    
    public void showProgress() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }
    
    public void hideProgress() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }
    
    public void setListener(TranslationInputListener listener) {
        this.listener = listener;
    }
}