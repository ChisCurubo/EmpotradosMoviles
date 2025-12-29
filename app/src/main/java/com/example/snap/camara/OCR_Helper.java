package com.example.snap.camara;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;

import com.example.snap.presentation.viewmodel.TranslationViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR_Helper: Híbrido (ML Kit + API)
 * 1. Intenta usar ML Kit para resultados instantáneos (offline).
 * 2. Si ML Kit falla o el modelo no está listo, usa la API (ViewModel) como respaldo.
 */
public class OCR_Helper {

    private static final String TAG = "OCR_Helper";
    private final TranslationViewModel viewModel;

    // Caché para no recrear el cliente de traducción en cada frame
    private final Map<String, Translator> translatorCache = new HashMap<>();

    // Mapa de idiomas soportados por ML Kit en tu app
    private static final Map<String, String> MLKIT_SUPPORTED = new HashMap<>();
    static {
        MLKIT_SUPPORTED.put("es", TranslateLanguage.SPANISH);
        MLKIT_SUPPORTED.put("en", TranslateLanguage.ENGLISH);
        MLKIT_SUPPORTED.put("it", TranslateLanguage.ITALIAN);
        MLKIT_SUPPORTED.put("pt", TranslateLanguage.PORTUGUESE);
        MLKIT_SUPPORTED.put("de", TranslateLanguage.GERMAN);
        MLKIT_SUPPORTED.put("fr", TranslateLanguage.FRENCH);
        // Agrega más si los necesitas
    }

    public OCR_Helper(TranslationViewModel vm) {
        this.viewModel = vm;
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(Exception e);
    }

    /**
     * Cierra los traductores para liberar memoria cuando se destruye la actividad
     */
    public void close() {
        for (Translator t : translatorCache.values()) {
            t.close();
        }
        translatorCache.clear();
    }

    public void translateText(
            String text,
            String sourceCode,
            String targetCode,
            String userId,
            TranslationCallback callback
    ) {
        // Validaciones básicas
        if (text == null || text.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Texto vacío"));
            return;
        }

        // Si el idioma es el mismo, no traducir
        if (sourceCode.equals(targetCode)) {
            callback.onSuccess(text);
            return;
        }

        // ESTRATEGIA:
        // 1. Verificamos si ambos idiomas están en ML Kit.
        // 2. Si están, intentamos ML Kit.
        // 3. Si no están (o ML Kit falla dentro del método), usamos la API.

        if (MLKIT_SUPPORTED.containsKey(sourceCode) && MLKIT_SUPPORTED.containsKey(targetCode)) {
            translateWithMLKit(text, sourceCode, targetCode, userId, callback);
        } else {
            // Idiomas no soportados por ML Kit (ej: Japonés, Ruso, etc.), ir directo a API
            translateWithAPI(text, sourceCode, targetCode, userId, callback);
        }
    }

    // ------------------------------------------------------------------------
    // LÓGICA ML KIT
    // ------------------------------------------------------------------------
    private void translateWithMLKit(
            String text,
            String sourceCode,
            String targetCode,
            String userId,
            TranslationCallback callback
    ) {
        String key = sourceCode + "-" + targetCode;
        Translator translator = translatorCache.get(key);

        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(MLKIT_SUPPORTED.get(sourceCode))
                    .setTargetLanguage(MLKIT_SUPPORTED.get(targetCode))
                    .build();
            translator = Translation.getClient(options);
            translatorCache.put(key, translator);
        }

        Translator finalTranslator = translator;

        // Condiciones de descarga (solo wifi para no gastar datos, opcional)
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        // Intentamos preparar el modelo
        finalTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    // Modelo listo, traducimos
                    finalTranslator.translate(text)
                            .addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Fallo traducción ML Kit, probando API...");
                                // FALLBACK: Si falla la traducción interna, usar API
                                translateWithAPI(text, sourceCode, targetCode, userId, callback);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Modelo no descargado o error ML Kit. Usando API de respaldo.");
                    // FALLBACK: Si falla la descarga del modelo, usar API inmediatamente
                    translateWithAPI(text, sourceCode, targetCode, userId, callback);
                });
    }

    // ------------------------------------------------------------------------
    // LÓGICA API (VIEWMODEL)
    // ------------------------------------------------------------------------
    private void translateWithAPI(
            String text,
            String sourceCode,
            String targetCode,
            String userId,
            TranslationCallback callback
    ) {
        // 1. Llamar al ViewModel para iniciar la petición
        viewModel.translateText(text, sourceCode, targetCode, userId);

        // 2. Observar la respuesta (LiveData)
        final Observer<String> observer = new Observer<String>() {
            @Override
            public void onChanged(String translated) {
                // IMPORTANTE: Remover el observer para evitar duplicados
                viewModel.getCurrentTranslation().removeObserver(this);

                if (translated == null || translated.trim().isEmpty()) {
                    callback.onFailure(new Exception("La API devolvió una traducción vacía"));
                } else if (translated.startsWith("Error")) {
                    callback.onFailure(new Exception(translated));
                } else {
                    callback.onSuccess(translated);
                }
            }
        };

        // Observar de forma permanente
        try {
            // Asegurarse de estar en el hilo principal
            viewModel.getCurrentTranslation().observeForever(observer);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}