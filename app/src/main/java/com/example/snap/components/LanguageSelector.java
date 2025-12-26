package com.example.snap.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import com.example.snap.R;

public class LanguageSelector extends LinearLayout {

    private Spinner spinnerSourceLanguage;
    private Spinner spinnerTargetLanguage;
    private ImageButton btnSwapLanguages;

    /**
     * Lista de idiomas
     */
    private String[] languages = {
            "Español", "Inglés", "Francés", "Alemán",
            "Italiano", "Portugués", "Chino", "Japonés"
    };

    /**
     * Devuelve el código de idioma a partir del nombre
     * @param language
     * @return
     */
    public String getLanguageCode(String language) {
        switch (language) {
            case "Español": return "es";
            case "Inglés": return "en";
            case "Francés": return "fr";
            case "Alemán": return "de";
            case "Italiano": return "it";
            case "Portugués": return "pt";
            case "Chino": return "zh";
            case "Japonés": return "ja";
            default: return "en";
        }
    }


    /**
     * Constructor
     * @param context
     */
    public LanguageSelector(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     */
    public LanguageSelector(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public LanguageSelector(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inicialización
     * @param context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_language_selector, this, true);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        spinnerSourceLanguage = findViewById(R.id.spinnerSourceLanguage);
        spinnerTargetLanguage = findViewById(R.id.spinnerTargetLanguage);
        btnSwapLanguages = findViewById(R.id.btnSwapLanguages);

        setupSpinners(context);
        setupSwapButton();
    }

    /**
     * Configura los spinners
     * @param context
     */
    private void setupSpinners(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSourceLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setAdapter(adapter);

        spinnerTargetLanguage.setSelection(1); // Inglés por defecto
    }

    /**
     * Configura el botón para intercambiar idiomas
     */
    private void setupSwapButton() {
        btnSwapLanguages.setOnClickListener(v -> swapLanguages());
    }

    /**
     * Intercambia los idiomas
     */
    private void swapLanguages() {
        int sourceIndex = spinnerSourceLanguage.getSelectedItemPosition();
        int targetIndex = spinnerTargetLanguage.getSelectedItemPosition();

        spinnerSourceLanguage.setSelection(targetIndex);
        spinnerTargetLanguage.setSelection(sourceIndex);
    }

    /**
     * Getters
     * @return
     */
    public Spinner getSpinnerSourceLanguage() {
        return spinnerSourceLanguage;
    }

    public Spinner getSpinnerTargetLanguage() {
        return spinnerTargetLanguage;
    }

    public ImageButton getBtnSwapLanguages() {
        return btnSwapLanguages;
    }
    public String getSourceLangCode() {
        return getLanguageCode(
                spinnerSourceLanguage.getSelectedItem().toString()
        );
    }

    public String getTargetLangCode() {
        return getLanguageCode(
                spinnerTargetLanguage.getSelectedItem().toString()
        );
    }

}