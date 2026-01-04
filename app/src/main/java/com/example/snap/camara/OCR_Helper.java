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

        // --- CAMBIO IMPORTANTE: PREFERIR SIEMPRE LA API SI HAY INTERNET ---
        // El usuario reportó problemas esperando la descarga de ML Kit.
        // Además, ML Kit no guarda historial automáticamente en el ViewModel.
        // Solución: Intentar API primero (ViewModel guarda historial y es inmediato online).
        // Solo usar ML Kit si falla la API o queremos modo offline explícito (futuro).
        
        translateWithAPI(text, sourceCode, targetCode, userId, callback);
    }

    // ------------------------------------------------------------------------
    // LÓGICA ML KIT (AHORA ES SECUNDARIA / FALLBACK)
    // ------------------------------------------------------------------------
    private void translateWithMLKit(
            String text,
            String sourceCode,
            String targetCode,
            String userId,
            TranslationCallback callback
    ) {
        // ... (código existente de ML Kit, se mantiene como respaldo si se desea reactivar)
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

        finalTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    finalTranslator.translate(text)
                            .addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Fallo traducción ML Kit");
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Modelo no descargado o error ML Kit.");
                    callback.onFailure(e);
                });
    }

    // ------------------------------------------------------------------------
    // LÓGICA API (VIEWMODEL) - AHORA PRINCIPAL
    // ------------------------------------------------------------------------
    private void translateWithAPI(
            String text,
            String sourceCode,
            String targetCode,
            String userId,
            TranslationCallback callback
    ) {
        // 1. Llamar al ViewModel para iniciar la petición
        // El ViewModel internamente llama a NetworkTranslationService Y LUEGO guarda en historial (Room)
        viewModel.translateText(text, sourceCode, targetCode, userId);

        // 2. Observar la respuesta (LiveData)
        final Observer<String> observer = new Observer<String>() {
            @Override
            public void onChanged(String translated) {
                // Filtrar estado "Traduciendo..." para no retornar prematuramente
                if ("Traduciendo...".equals(translated)) {
                    return;
                }

                // IMPORTANTE: Remover el observer para evitar duplicados y fugas
                viewModel.getCurrentTranslation().removeObserver(this);

                if (translated == null || translated.trim().isEmpty()) {
                    // Si falla API, podríamos intentar ML Kit aquí como fallback offline
                     if (MLKIT_SUPPORTED.containsKey(sourceCode) && MLKIT_SUPPORTED.containsKey(targetCode)) {
                        translateWithMLKit(text, sourceCode, targetCode, userId, callback);
                     } else {
                        callback.onFailure(new Exception("La API devolvió una traducción vacía y no hay soporte ML Kit"));
                     }
                } else if (translated.startsWith("Error")) {
                     // Lo mismo, si da error de red, probar ML Kit
                     if (MLKIT_SUPPORTED.containsKey(sourceCode) && MLKIT_SUPPORTED.containsKey(targetCode)) {
                        translateWithMLKit(text, sourceCode, targetCode, userId, callback);
                     } else {
                        callback.onFailure(new Exception(translated));
                     }
                } else {
                    callback.onSuccess(translated);
                }
            }
        };

        // Observar de forma permanente (con cuidado de removerlo en onChanged)
        try {
            // Asegurarse de estar en el hilo principal
            viewModel.getCurrentTranslation().observeForever(observer);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}
