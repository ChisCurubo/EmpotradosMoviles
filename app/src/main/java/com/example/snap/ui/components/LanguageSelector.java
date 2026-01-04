package com.example.snap.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.example.snap.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente centralizado para la selección de idiomas.
 * Encapsula toda la lógica de spinners, swap e índices.
 */
public class LanguageSelector extends LinearLayout {

    private Spinner spinnerSourceLanguage;
    private Spinner spinnerTargetLanguage;
    private ImageButton btnSwapLanguages;

    // Mapa ordenado: Nombre visible -> Código ISO
    private final Map<String, String> languagesMap = new LinkedHashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private OnLanguageChangeListener listener;

    /**
     * Listener que notifica:
     * - códigos ISO
     * - índices seleccionados (útil para actualizar chips u otros componentes)
     */
    public interface OnLanguageChangeListener {
        void onLanguageChanged(
                String sourceCode,
                String targetCode,
                int sourceIndex,
                int targetIndex);
    }

    public LanguageSelector(Context context) {
        super(context);
        init(context);
    }

    public LanguageSelector(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LanguageSelector(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        initLanguages();

        LayoutInflater.from(context).inflate(R.layout.layout_language_selector, this, true);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        spinnerSourceLanguage = findViewById(R.id.spinnerSourceLanguage);
        spinnerTargetLanguage = findViewById(R.id.spinnerTargetLanguage);
        btnSwapLanguages = findViewById(R.id.btnSwapLanguages);

        setupSpinners(context);
        setupListeners();
    }

    private void initLanguages() {
        // Cargar nombres de idiomas desde recursos (se traducen automáticamente)
        String[] languageNamesArray = getContext().getResources().getStringArray(R.array.languages);
        String[] languageCodes = {"es", "en", "fr", "de", "it", "pt", "zh", "ja", "ko"};
        
        // Mapear nombres traducidos a códigos
        for (int i = 0; i < Math.min(languageNamesArray.length, languageCodes.length); i++) {
            languagesMap.put(languageNamesArray[i], languageCodes[i]);
        }

        languageNames.addAll(languagesMap.keySet());
    }

    private void setupSpinners(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSourceLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setAdapter(adapter);

        // Default: Español → Inglés
        spinnerSourceLanguage.setSelection(0);
        spinnerTargetLanguage.setSelection(1);
    }

    private void setupListeners() {
        btnSwapLanguages.setOnClickListener(v -> swapLanguages());

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                notifyLanguageChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerSourceLanguage.setOnItemSelectedListener(spinnerListener);
        spinnerTargetLanguage.setOnItemSelectedListener(spinnerListener);
    }

    /**
     * Intercambia los idiomas seleccionados.
     * La notificación se dispara automáticamente.
     */
    public void swapLanguages() {
        int sourceIndex = spinnerSourceLanguage.getSelectedItemPosition();
        int targetIndex = spinnerTargetLanguage.getSelectedItemPosition();

        spinnerSourceLanguage.setSelection(targetIndex);
        spinnerTargetLanguage.setSelection(sourceIndex);
    }

    /**
     * Notifica al listener siempre con:
     * - códigos ISO
     * - índices actuales
     */
    private void notifyLanguageChange() {
        if (listener == null)
            return;

        listener.onLanguageChanged(
                getSourceLangCode(),
                getTargetLangCode(),
                spinnerSourceLanguage.getSelectedItemPosition(),
                spinnerTargetLanguage.getSelectedItemPosition());
    }

    // --- Métodos públicos de acceso limpio ---

    public String getSourceLangCode() {
        String name = (String) spinnerSourceLanguage.getSelectedItem();
        return languagesMap.get(name);
    }

    public String getTargetLangCode() {
        String name = (String) spinnerTargetLanguage.getSelectedItem();
        return languagesMap.get(name);
    }

    public String getSourceLangName() {
        return (String) spinnerSourceLanguage.getSelectedItem();
    }

    public void setOnLanguageChangeListener(OnLanguageChangeListener listener) {
        this.listener = listener;
        // dispara el estado inicial para sincronizar componentes
        notifyLanguageChange();
    }

    public void setLanguages(String sourceCode, String targetCode) {
        int sourceIndex = -1;
        int targetIndex = -1;

        // Buscar índices
        for (int i = 0; i < languageNames.size(); i++) {
            String name = languageNames.get(i);
            String code = languagesMap.get(name);

            if (code != null && code.equals(sourceCode)) {
                sourceIndex = i;
            }
            if (code != null && code.equals(targetCode)) {
                targetIndex = i;
            }
        }

        if (sourceIndex != -1) {
            spinnerSourceLanguage.setSelection(sourceIndex);
        }
        if (targetIndex != -1) {
            spinnerTargetLanguage.setSelection(targetIndex);
        }
    }

    /**
     * Guarda la selección actual de idiomas en SharedPreferences.
     * Se usa una clave diferente a los idiomas "por defecto" para diferenciar
     * entre la configuración inicial y la selección actual de la sesión.
     */
    public void saveCurrentLanguages() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(
                "current_languages", android.content.Context.MODE_PRIVATE);
        
        prefs.edit()
                .putString("current_source_lang", getSourceLangCode())
                .putString("current_target_lang", getTargetLangCode())
                .apply();
    }

    /**
     * Carga los idiomas de la sesión actual desde SharedPreferences.
     * Si no existen, devuelve null para que se usen los idiomas por defecto.
     * 
     * @return String[] con [sourceCode, targetCode] o null si no hay idiomas guardados
     */
    public String[] loadCurrentLanguages() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(
                "current_languages", android.content.Context.MODE_PRIVATE);
        
        String source = prefs.getString("current_source_lang", null);
        String target = prefs.getString("current_target_lang", null);
        
        if (source != null && target != null) {
            return new String[]{source, target};
        }
        return null;
    }

    /**
     * Limpia los idiomas actuales guardados.
     * Se debe llamar al iniciar sesión, registrarse o iniciar la app por primera vez.
     */
    public void clearCurrentLanguages() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(
                "current_languages", android.content.Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public Spinner getSpinnerSource() {
        return spinnerSourceLanguage;
    }

    public Spinner getSpinnerTarget() {
        return spinnerTargetLanguage;
    }
}
