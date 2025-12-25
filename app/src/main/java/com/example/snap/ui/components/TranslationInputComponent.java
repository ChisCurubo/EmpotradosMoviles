package com.example.snap.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.example.snap.R;
import com.example.snap.utils.LanguageHelper;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Componente reutilizable para la interfaz de traducción.
 * Encapsula toda la lógica de UI relacionada con la traducción.
 */
public class TranslationInputComponent extends LinearLayout {
    
    private Spinner spinnerInput, spinnerOutput;
    private TextInputEditText etInput;
    private ImageView btnClear, btnSwap;
    private Button chip1, chip2, chip3;
    private ProgressBar progressBar;
    
    private TranslationInputListener listener;
    
    public interface TranslationInputListener {
        void onTranslateRequested(String text, String sourceLang, String targetLang);
        void onLanguageChanged(int inputPosition, int outputPosition);
        void onClearRequested();
        void onSwapRequested();
    }
    
    public TranslationInputComponent(Context context) {
        super(context);
        init(context);
    }
    
    public TranslationInputComponent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        // Nota: Este componente requiere que el layout se infle desde el XML padre
        // o que se cree un layout específico para este componente
    }
    
    /**
     * Inicializa las vistas del componente
     */
    public void initializeViews(View rootView) {
        spinnerInput = rootView.findViewById(R.id.spinnerInput);
        spinnerOutput = rootView.findViewById(R.id.spinnerOutput);
        etInput = rootView.findViewById(R.id.etInput);
        btnClear = rootView.findViewById(R.id.btnClear);
        btnSwap = rootView.findViewById(R.id.btnSwap);
        chip1 = rootView.findViewById(R.id.chip1);
        chip2 = rootView.findViewById(R.id.chip2);
        chip3 = rootView.findViewById(R.id.chip3);
        progressBar = rootView.findViewById(R.id.progressBar);
        
        setupSpinners();
        setupListeners();
        updateQuickTranslationChips();
    }
    
    /**
     * Configura los spinners de idiomas
     */
    private void setupSpinners() {
        String[] languages = LanguageHelper.getAvailableLanguages();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerInput.setAdapter(adapter);
        spinnerOutput.setAdapter(adapter);
        
        spinnerInput.setSelection(0); // Español
        spinnerOutput.setSelection(1); // Inglés
    }
    
    /**
     * Configura los listeners
     */
    private void setupListeners() {
        // Listener para actualizar chips según idioma
        spinnerInput.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateQuickTranslationChips();
                if (listener != null) {
                    listener.onLanguageChanged(spinnerInput.getSelectedItemPosition(), 
                                             spinnerOutput.getSelectedItemPosition());
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        spinnerOutput.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.onLanguageChanged(spinnerInput.getSelectedItemPosition(), 
                                             spinnerOutput.getSelectedItemPosition());
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Botón limpiar
        btnClear.setOnClickListener(v -> {
            clear();
            if (listener != null) {
                listener.onClearRequested();
            }
        });
        
        // Botón intercambiar
        btnSwap.setOnClickListener(v -> {
            swap();
            if (listener != null) {
                listener.onSwapRequested();
            }
        });
        
        // Chips de traducción rápida
        View.OnClickListener chipListener = v -> {
            Button b = (Button) v;
            etInput.setText(b.getText().toString());
            requestTranslation();
        };
        chip1.setOnClickListener(chipListener);
        chip2.setOnClickListener(chipListener);
        chip3.setOnClickListener(chipListener);
        
        // Enter en el teclado
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            requestTranslation();
            return true;
        });
    }
    
    /**
     * Solicita una traducción
     */
    private void requestTranslation() {
        String text = getInputText();
        if (listener != null && !text.isEmpty()) {
            String sourceLang = getSourceLanguageCode();
            String targetLang = getTargetLanguageCode();
            listener.onTranslateRequested(text, sourceLang, targetLang);
        }
    }
    
    /**
     * Actualiza los chips de traducción rápida
     */
    private void updateQuickTranslationChips() {
        String[] phrases = LanguageHelper.getQuickPhrasesByPosition(
            spinnerInput.getSelectedItemPosition()
        );
        chip1.setText(phrases[0]);
        chip2.setText(phrases[1]);
        chip3.setText(phrases[2]);
    }
    
    /**
     * Limpia los campos de entrada
     */
    public void clear() {
        etInput.setText("");
    }
    
    /**
     * Intercambia los idiomas
     */
    public void swap() {
        int inputPos = spinnerInput.getSelectedItemPosition();
        int outputPos = spinnerOutput.getSelectedItemPosition();
        spinnerInput.setSelection(outputPos);
        spinnerOutput.setSelection(inputPos);
    }
    
    /**
     * Obtiene el texto de entrada
     */
    public String getInputText() {
        return etInput.getText() != null ? etInput.getText().toString().trim() : "";
    }
    
    /**
     * Establece el texto de entrada
     */
    public void setInputText(String text) {
        etInput.setText(text);
    }
    
    /**
     * Obtiene el código del idioma de origen
     */
    public String getSourceLanguageCode() {
        return LanguageHelper.getLanguageCode(spinnerInput.getSelectedItemPosition());
    }
    
    /**
     * Obtiene el código del idioma de destino
     */
    public String getTargetLanguageCode() {
        return LanguageHelper.getLanguageCode(spinnerOutput.getSelectedItemPosition());
    }
    
    /**
     * Muestra el indicador de progreso
     */
    public void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnSwap.setEnabled(false);
    }
    
    /**
     * Oculta el indicador de progreso
     */
    public void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnSwap.setEnabled(true);
    }
    
    /**
     * Establece el listener
     */
    public void setListener(TranslationInputListener listener) {
        this.listener = listener;
    }
}
