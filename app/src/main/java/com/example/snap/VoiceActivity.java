package com.example.snap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.ui.components.LanguageSelector;
import com.example.snap.utils.LanguageHelper;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceActivity";

    // Componentes
    private LanguageSelector languageSelector;
    private BottomNavigationComponent bottomNavigation;

    // View Components
    private ImageButton btnRecord;
    private ImageButton btnPlay;
    private TextView tvInput;
    private TextView tvResult;

    // TTS
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    // Estado
    private String sourceLang = "es";
    private String targetLang = "en";

    // Launchers
    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        translateText(matches.get(0));
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVoiceRecognition();
                } else {
                    showMessage("Permiso de micrófono denegado");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        initializeViews();
        setupLanguageSelector();
        setupListeners();
        setupBottomNavigation();
        setupObservers();

        tts = new TextToSpeech(this, this);
        showWelcomeMessage();
    }

    @Override
    protected void onSessionUpdated() {
        if (bottomNavigation != null) {
            bottomNavigation.updateUserButtonState();
        }
    }

    private void initializeViews() {
        languageSelector = findViewById(R.id.languageSelector);
        btnRecord = findViewById(R.id.record_button); // updated ID to match XML
        btnPlay = findViewById(R.id.speak_button); // updated ID to match XML
        tvInput = findViewById(R.id.input_text);
        tvResult = findViewById(R.id.translated_text);
    }

    private void setupLanguageSelector() {
        languageSelector.setOnLanguageChangeListener((srcCode, tgtCode, srcIndex, tgtIndex) -> {
            sourceLang = srcCode;
            targetLang = tgtCode;

            Log.d(TAG, "Idiomas cambiados: " + srcCode + " -> " + tgtCode);

            // Re-traducir si ya hay texto
            String text = tvInput.getText().toString();
            if (!text.isEmpty() && !text.equals("El texto reconocido aparecerá aquí...")) {
                translateText(text);
            }
        });
    }

    private void setupListeners() {
        btnRecord.setOnClickListener(v -> checkPermissionAndStartVoice());
        btnPlay.setOnClickListener(v -> speakTranslatedText());
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.attachToScreen("audio");
        }
    }

    private void setupObservers() {
        // Observamos el resultado de la traducción del ViewModel heredado de
        // BaseActivity
        viewModel.getCurrentTranslation().observe(this, translatedText -> {
            if (translatedText != null) {
                tvResult.setText(translatedText);

                // Si no es mensaje de error ni "Traduciendo...", lo leemos automáticamente
                // PERO solo si la preferencia está activada
                if (!translatedText.startsWith("Error") && !translatedText.equals("Traduciendo...")) {

                    String currentUser = getCurrentUser();
                    if (currentUser == null)
                        currentUser = "guest";

                    android.content.SharedPreferences prefs = getSharedPreferences(
                            com.example.snap.SettingsActivity.PREFS_NAME + "_" + currentUser, MODE_PRIVATE);
                    boolean shouldSpeak = prefs.getBoolean(com.example.snap.SettingsActivity.KEY_AUTO_TTS, true);

                    if (shouldSpeak) {
                        speakTranslatedText();
                    }

                    showCustomToast("Traducción completada", android.R.drawable.ic_input_add);
                } else if (translatedText.startsWith("Error")) {
                    showCustomToast("Error al traducir", android.R.drawable.ic_delete);
                }
            }
        });
    }

    private void checkPermissionAndStartVoice() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Usamos LanguageHelper para obtener el locale (reutilización)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageHelper.getSpeechLocale(sourceLang));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...");

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            showMessage("Error al iniciar voz: " + e.getMessage());
        }
    }

    private void translateText(String text) {
        if (sourceLang.equals(targetLang)) {
            showMessage("Los idiomas introducidos son iguales");
            return;
        }

        tvInput.setText(text);
        tvResult.setText("Traduciendo...");

        // Usamos el ViewModel compartido para traducir (coherencia con TextActivity)
        viewModel.translateText(text, sourceLang, targetLang, getCurrentUser());
    }

    private void speakTranslatedText() {
        if (!isTtsReady) {
            showMessage("Motor de voz no disponible");
            return;
        }

        String text = tvResult.getText().toString();
        if (text.isEmpty() || text.equals("La traducción aparecerá aquí...") ||
                text.equals("Traduciendo...") || text.startsWith("Error")) {
            return;
        }

        Locale locale = new Locale(targetLang);
        int result = tts.setLanguage(locale);

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            showMessage("Idioma no soportado por TTS");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        isTtsReady = status == TextToSpeech.SUCCESS;
        if (isTtsReady) {
            // Configuración inicial TTS si necesaria
        } else {
            Log.e(TAG, "Error al inicializar TTS");
        }
    }

    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado — voz activa");
        } else {
            showMessage("Hola " + getCurrentUser());
        }
    }

    private void showCustomToast(String message, int iconResId) {
        View layout = getLayoutInflater().inflate(R.layout.custom_toast, null);
        if (layout == null) {
            showMessage(message);
            return;
        }

        android.widget.ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        if (icon != null)
            icon.setImageResource(iconResId);
        if (text != null)
            text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
