package com.example.snap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.snap.ui.base.BaseActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseActivity {

    public static final String PREFS_NAME = "SnapPrefs";
    public static final String KEY_AUTO_TTS = "auto_tts";
    public static final String KEY_SAVE_HISTORY = "save_history";
    public static final String KEY_DEFAULT_SOURCE_LANG = "default_source_lang";
    public static final String KEY_DEFAULT_TARGET_LANG = "default_target_lang";
    public static final String KEY_APP_LANGUAGE = "app_language";

    private SwitchMaterial switchAutoTts;
    private SwitchMaterial switchSaveHistory;
    private android.widget.LinearLayout containerDefaultSource;
    private android.widget.LinearLayout containerDefaultTarget;
    private android.widget.LinearLayout containerAppLanguage;
    private android.widget.TextView tvDefaultSource;
    private android.widget.TextView tvDefaultTarget;
    private android.widget.TextView tvAppLanguage;
    private ImageView btnBack;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            userId = "guest";
        }

        initializeViews();
        loadPreferences();
        setupListeners();
    }

    private void initializeViews() {
        switchAutoTts = findViewById(R.id.switchAutoTts);
        switchSaveHistory = findViewById(R.id.switchSaveHistory);
        containerDefaultSource = findViewById(R.id.containerDefaultSource);
        containerDefaultTarget = findViewById(R.id.containerDefaultTarget);
        containerAppLanguage = findViewById(R.id.containerAppLanguage);
        tvDefaultSource = findViewById(R.id.tvDefaultSource);
        tvDefaultTarget = findViewById(R.id.tvDefaultTarget);
        tvAppLanguage = findViewById(R.id.tvAppLanguage);
        btnBack = findViewById(R.id.btnBack);
    }

    private String getPrefsName() {
        return PREFS_NAME + "_" + userId;
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);
        boolean autoTts = prefs.getBoolean(KEY_AUTO_TTS, true);
        boolean saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, true);
        String sourceLang = prefs.getString(KEY_DEFAULT_SOURCE_LANG, "es");
        String targetLang = prefs.getString(KEY_DEFAULT_TARGET_LANG, "en");
        String appLanguage = prefs.getString(KEY_APP_LANGUAGE, "es");

        switchAutoTts.setChecked(autoTts);
        switchSaveHistory.setChecked(saveHistory);
        tvDefaultSource.setText(com.example.snap.utils.LanguageHelper.getLanguageName(this, sourceLang));
        tvDefaultTarget.setText(com.example.snap.utils.LanguageHelper.getLanguageName(this, targetLang));
        tvAppLanguage.setText(getAppLanguageName(appLanguage));
    }

    private void setupListeners() {
        SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);

        switchAutoTts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_AUTO_TTS, isChecked).apply();
        });

        switchSaveHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SAVE_HISTORY, isChecked).apply();
        });

        containerDefaultSource.setOnClickListener(v -> showLanguageDialog(true));
        containerDefaultTarget.setOnClickListener(v -> showLanguageDialog(false));
        containerAppLanguage.setOnClickListener(v -> showAppLanguageDialog());

        btnBack.setOnClickListener(v -> {
            // Volver a la pantalla anterior y recargarla
            android.content.Intent intent = new android.content.Intent(this, StatisticsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void showLanguageDialog(boolean isSource) {
        String[] languages = com.example.snap.utils.LanguageHelper.getAvailableLanguages(this);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(isSource ? R.string.seleccionar_idioma_origen : R.string.seleccionar_idioma_destino)
                .setItems(languages, (dialog, which) -> {
                    String code = com.example.snap.utils.LanguageHelper.getLanguageCode(which);
                    SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);

                    if (isSource) {
                        prefs.edit().putString(KEY_DEFAULT_SOURCE_LANG, code).apply();
                        tvDefaultSource.setText(languages[which]);
                    } else {
                        prefs.edit().putString(KEY_DEFAULT_TARGET_LANG, code).apply();
                        tvDefaultTarget.setText(languages[which]);
                    }
                    
                    // Actualizar también los idiomas actuales para que se reflejen inmediatamente
                    updateCurrentLanguagesFromDefaults();
                })
                .show();
    }
    
    /**
     * Actualiza los idiomas actuales (current_languages) con los idiomas por defecto
     * para que se reflejen inmediatamente en el selector de idiomas de las otras pantallas
     */
    private void updateCurrentLanguagesFromDefaults() {
        SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);
        String sourceCode = prefs.getString(KEY_DEFAULT_SOURCE_LANG, "es");
        String targetCode = prefs.getString(KEY_DEFAULT_TARGET_LANG, "en");
        
        // Actualizar los idiomas actuales
        SharedPreferences currentLangPrefs = getSharedPreferences("current_languages", MODE_PRIVATE);
        currentLangPrefs.edit()
                .putString("current_source_lang", sourceCode)
                .putString("current_target_lang", targetCode)
                .apply();
                
        android.util.Log.d("SettingsActivity", "Updated current languages to: " + sourceCode + " -> " + targetCode);
    }

    private void showAppLanguageDialog() {
        String[] appLanguages = getResources().getStringArray(R.array.app_languages);
        String[] languageCodes = {"es", "en", "de", "fr", "it", "pt", "ja", "ko", "zh"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.seleccionar_idioma_app)
                .setItems(appLanguages, (dialog, which) -> {
                    String selectedCode = languageCodes[which];
                    
                    // Guardar idioma en las preferencias del usuario
                    SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);
                    prefs.edit().putString(KEY_APP_LANGUAGE, selectedCode).apply();
                    
                    // Asegurar que session_prefs tenga el userId correcto
                    SharedPreferences sessionPrefs = getSharedPreferences("session_prefs", MODE_PRIVATE);
                    sessionPrefs.edit().putString("active_user", userId).apply();
                    
                    // Log para verificar
                    android.util.Log.d("SettingsActivity", "Saved language: " + selectedCode + " for user: " + userId);
                    
                    // Actualizar la UI inmediatamente
                    tvAppLanguage.setText(appLanguages[which]);
                    
                    // Recrear la actividad para aplicar el nuevo idioma
                    recreate();
                })
                .show();
    }

    private String getAppLanguageName(String code) {
        String[] appLanguages = getResources().getStringArray(R.array.app_languages);
        String[] languageCodes = {"es", "en", "de", "fr", "it", "pt", "ja", "ko", "zh"};
        
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(code)) {
                return appLanguages[i];
            }
        }
        return appLanguages[0]; // Default: primer idioma
    }
}
