package com.example.snap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.ui.components.LanguageSelector;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class VoiceFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceFragment";

    // Componentes centralizados
    private LanguageSelector languageSelector;
    private BottomNavigationComponent bottomNavigation;

    // Views
    private ImageButton recordButton, speakButton;
    private TextView inputText, translatedText;

    // TTS
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    // Traducciones
    private final Map<String, Translator> translatorCache = new HashMap<>();

    // Idiomas actuales
    private String currentSourceCode = "es";
    private String currentTargetCode = "en";

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
                    Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()), R.string.permiso_microfono_denegado, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupLanguageSelector();
        setupButtons();
        setupNavigation();

        tts = new TextToSpeech(requireContext(), this);
    }

//Inicializa todas las vistas


    private void initializeViews(View view) {
        languageSelector = view.findViewById(R.id.languageSelector);
        bottomNavigation = view.findViewById(R.id.bottomNavigation);

        recordButton = view.findViewById(R.id.record_button);
        speakButton = view.findViewById(R.id.speak_button);
        inputText = view.findViewById(R.id.input_text);
        translatedText = view.findViewById(R.id.translated_text);
    }

//Configura el LanguageSelector centralizado


    private void setupLanguageSelector() {
        languageSelector.setOnLanguageChangeListener((srcCode, tgtCode, srcIndex, tgtIndex) -> {
            currentSourceCode = srcCode;
            currentTargetCode = tgtCode;

            Log.d(TAG, "Idiomas cambiados: " + srcCode + " -> " + tgtCode);

            // Re-traducir si ya hay texto
            String text = inputText.getText().toString();
            if (!text.isEmpty() && !text.equals("El texto reconocido aparecerá aquí...")) {
                translateText(text);
            }
        });
    }

//Configura los botones


    private void setupButtons() {
        recordButton.setOnClickListener(v -> checkPermissionAndStartVoice());
        speakButton.setOnClickListener(v -> speakTranslatedText());
    }

//Configura la navegación centralizada


    private void setupNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("audio");
            bottomNavigation.updateUserButtonState();
        }
    }

//Verifica permisos y inicia reconocimiento de voz


    private void checkPermissionAndStartVoice() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startVoiceRecognition();
        }
    }

//Inicia el reconocimiento de voz


    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getSpeechLocale(currentSourceCode));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...");

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//Traduce el texto reconocido


    private void translateText(String text) {
        if (currentSourceCode.equals(currentTargetCode)) {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()),
                    R.string.idiomas_iguales,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        inputText.setText(text);
        translatedText.setText("Traduciendo...");

        String key = currentSourceCode + "-" + currentTargetCode;
        Translator translator = translatorCache.get(key);

        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(getMLKitLanguage(currentSourceCode))
                    .setTargetLanguage(getMLKitLanguage(currentTargetCode))
                    .build();

            translator = Translation.getClient(options);
            translatorCache.put(key, translator);
        }

        Translator finalTranslator = translator;
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnSuccessListener(v -> finalTranslator.translate(text)
                        .addOnSuccessListener(result -> {
                            translatedText.setText(result);
                            showCustomToast("Traducción completada", android.R.drawable.ic_input_add);
                        })
                        .addOnFailureListener(e -> {
                            translatedText.setText("Error al traducir");
                            showCustomToast("Error al traducir", android.R.drawable.ic_delete);
                        }))
                .addOnFailureListener(e -> {
                    translatedText.setText("Error de modelo");
                    showCustomToast("Error de modelo", android.R.drawable.ic_dialog_alert);
                });
    }

//Reproduce el texto traducido con TTS


    private void speakTranslatedText() {
        if (!isTtsReady) {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()),
                    R.string.motor_voz_no_disponible,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String text = translatedText.getText().toString();
        if (text.isEmpty() || text.equals("La traducción aparecerá aquí...") ||
                text.equals("Traduciendo...")) {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()),
                    R.string.no_texto_reproducir,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Locale locale = new Locale(currentTargetCode);

        if (tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(SnapLinguaApplication.getLanguageContext(requireContext()),
                    R.string.idioma_no_disponible_tts,
                    Toast.LENGTH_SHORT).show();
        }
    }

//Convierte código ISO a formato ML Kit


    private String getMLKitLanguage(String isoCode) {
        switch (isoCode) {
            case "es": return TranslateLanguage.SPANISH;
            case "en": return TranslateLanguage.ENGLISH;
            case "fr": return TranslateLanguage.FRENCH;
            case "de": return TranslateLanguage.GERMAN;
            case "it": return TranslateLanguage.ITALIAN;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "zh": return TranslateLanguage.CHINESE;
            case "ja": return TranslateLanguage.JAPANESE;
            default: return TranslateLanguage.ENGLISH;
        }
    }
    //Obtiene el locale para reconocimiento de voz


    private String getSpeechLocale(String isoCode) {
        switch (isoCode) {
            case "es": return "es-ES";
            case "en": return "en-US";
            case "fr": return "fr-FR";
            case "de": return "de-DE";
            case "it": return "it-IT";
            case "pt": return "pt-PT";
            case "zh": return "zh-CN";
            case "ja": return "ja-JP";
            default: return "en-US";
        }
    }

    @Override
    public void onInit(int status) {
        isTtsReady = status == TextToSpeech.SUCCESS;
        if (isTtsReady) {
            Log.d(TAG, "TTS inicializado correctamente");
        } else {
            Log.e(TAG, "Error al inicializar TTS");
        }
    }
    //Muestra un toast personalizado


    private void showCustomToast(String message, int iconResId) {
        View layout = getLayoutInflater().inflate(R.layout.custom_toast, null);

        android.widget.ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        icon.setImageResource(iconResId);
        text.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("audio");
            bottomNavigation.updateUserButtonState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Limpiar traductores
        for (Translator t : translatorCache.values()) {
            t.close();
        }
        translatorCache.clear();

        // Cerrar TTS
        if (tts != null) {
            tts.shutdown();
        }
    }
}
