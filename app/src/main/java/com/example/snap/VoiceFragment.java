package com.example.snap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

    private Spinner spinnerFrom, spinnerTo;
    private ImageButton recordButton, swapButton, speakButton;
    private TextView inputText, translatedText;

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private final Map<String, Translator> translatorCache = new HashMap<>();

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
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
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
        initializeUI(view);
        setupSpinners();
        setupButtons();

        tts = new TextToSpeech(requireContext(), this);
    }

    private void initializeUI(View view) {
        spinnerFrom = view.findViewById(R.id.spinner_from);
        spinnerTo = view.findViewById(R.id.spinner_to);
        recordButton = view.findViewById(R.id.record_button);
        swapButton = view.findViewById(R.id.swap_button);
        speakButton = view.findViewById(R.id.speak_button);
        inputText = view.findViewById(R.id.input_text);
        translatedText = view.findViewById(R.id.translated_text);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        spinnerFrom.setSelection(0);
        spinnerTo.setSelection(1);

        android.widget.AdapterView.OnItemSelectedListener listener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String text = inputText.getText().toString();
                if (!text.isEmpty()) {
                    translateText(text);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        };

        spinnerFrom.setOnItemSelectedListener(listener);
        spinnerTo.setOnItemSelectedListener(listener);
    }

    private void setupButtons() {
        recordButton.setOnClickListener(v -> checkPermissionAndStartVoice());
        swapButton.setOnClickListener(v -> swapLanguages());
        speakButton.setOnClickListener(v -> speakTranslatedText());
    }

    private void checkPermissionAndStartVoice() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                getSpeechLocale(spinnerFrom.getSelectedItemPosition()));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void translateText(String text) {
        if (spinnerFrom.getSelectedItemPosition() == spinnerTo.getSelectedItemPosition()) {
            Toast.makeText(requireContext(), "Source and target languages are the same.", Toast.LENGTH_SHORT).show();
            return;
        }

        inputText.setText(text);

        String source = getLanguageCode(spinnerFrom.getSelectedItemPosition());
        String target = getLanguageCode(spinnerTo.getSelectedItemPosition());
        String key = source + "-" + target;

        Translator translator = translatorCache.get(key);

        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
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
                        .addOnFailureListener(e -> showCustomToast("Error al traducir", android.R.drawable.ic_delete)))
                .addOnFailureListener(
                        e -> showCustomToast("Error de modelo", android.R.drawable.ic_dialog_alert));
    }

    private void speakTranslatedText() {
        if (!isTtsReady)
            return;

        String text = translatedText.getText().toString();
        if (text.isEmpty())
            return;

        Locale locale = Locale.forLanguageTag(
                getLanguageTag(spinnerTo.getSelectedItemPosition()));

        if (tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void swapLanguages() {
        int from = spinnerFrom.getSelectedItemPosition();
        spinnerFrom.setSelection(spinnerTo.getSelectedItemPosition());
        spinnerTo.setSelection(from);
    }

    private String getLanguageCode(int position) {
        switch (position) {
            case 0:
                return TranslateLanguage.SPANISH;
            case 1:
                return TranslateLanguage.ENGLISH;
            case 2:
                return TranslateLanguage.FRENCH;
            case 3:
                return TranslateLanguage.GERMAN;
            case 4:
                return TranslateLanguage.ITALIAN;
            case 5:
                return TranslateLanguage.PORTUGUESE;
            default:
                return TranslateLanguage.ENGLISH;
        }
    }

    private String getLanguageTag(int position) {
        switch (position) {
            case 0:
                return "es";
            case 1:
                return "en";
            case 2:
                return "fr";
            case 3:
                return "de";
            case 4:
                return "it";
            case 5:
                return "pt";
            default:
                return "en";
        }
    }

    private String getSpeechLocale(int position) {
        switch (position) {
            case 0:
                return "es-ES"; // Spanish (Spain)
            case 1:
                return "en-US"; // English (US)
            case 2:
                return "fr-FR"; // French (France)
            case 3:
                return "de-DE"; // German (Germany)
            case 4:
                return "it-IT"; // Italian (Italy)
            case 5:
                return "pt-PT"; // Portuguese (Portugal)
            default:
                return "en-US";
        }
    }

    @Override
    public void onInit(int status) {
        isTtsReady = status == TextToSpeech.SUCCESS;
    }

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
    public void onDestroy() {
        super.onDestroy();
        for (Translator t : translatorCache.values()) {
            t.close();
        }
        if (tts != null) {
            tts.shutdown();
        }
    }
}
