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

    private SwitchMaterial switchAutoTts;
    private SwitchMaterial switchSaveHistory;
    private ImageView btnBack;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            // Fallback or finish if critical
            userId = "guest";
        }

        initializeViews();
        loadPreferences();
        setupListeners();
    }

    private void initializeViews() {
        switchAutoTts = findViewById(R.id.switchAutoTts);
        switchSaveHistory = findViewById(R.id.switchSaveHistory);
        btnBack = findViewById(R.id.btnBack);
    }

    private String getPrefsName() {
        return PREFS_NAME + "_" + userId;
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);
        boolean autoTts = prefs.getBoolean(KEY_AUTO_TTS, true); // Default true
        boolean saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, true); // Default true

        switchAutoTts.setChecked(autoTts);
        switchSaveHistory.setChecked(saveHistory);
    }

    private void setupListeners() {
        SharedPreferences prefs = getSharedPreferences(getPrefsName(), MODE_PRIVATE);

        switchAutoTts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_AUTO_TTS, isChecked).apply();
        });

        switchSaveHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SAVE_HISTORY, isChecked).apply();
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
