package com.example.snap.camara;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Clase helper para realizar OCR (reconocimiento de texto) en imágenes
 */
public class OCR_Helper {

    private static final String TAG = "OCRHelper";
    private final TextRecognizer recognizer;

    /**
     * Interface para manejar los resultados del OCR
     */
    public interface OCRCallback {
        void onSuccess(String extractedText);
        void onFailure(Exception e);
    }

    /**
     * Constructor
     */
    public OCR_Helper() {
        // Inicializar el reconocedor de texto
        // Por defecto usa Latin script (español, inglés, francés, etc.)
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    /**
     * Procesa una imagen y extrae el texto
     *
     * @param bitmap Imagen a procesar
     * @param callback Callback con el resultado
     */
    public void extractTextFromBitmap(Bitmap bitmap, OCRCallback callback) {
        if (bitmap == null) {
            callback.onFailure(new IllegalArgumentException("Bitmap no puede ser null"));
            return;
        }

        try {
            // Crear InputImage desde el Bitmap
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Procesar la imagen
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();

                        if (extractedText.isEmpty()) {
                            Log.w(TAG, "No se detectó texto en la imagen");
                            callback.onSuccess("No se detectó texto en la imagen");
                        } else {
                            Log.d(TAG, "Texto extraído: " + extractedText);
                            callback.onSuccess(extractedText);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al extraer texto", e);
                        callback.onFailure(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar imagen", e);
            callback.onFailure(e);
        }
    }

    /**
     * Procesa una imagen y extrae información detallada por bloques
     *
     * @param bitmap Imagen a procesar
     * @param callback Callback con el resultado
     */
    public void extractDetailedText(Bitmap bitmap, DetailedOCRCallback callback) {
        if (bitmap == null) {
            callback.onFailure(new IllegalArgumentException("Bitmap no puede ser null"));
            return;
        }

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        if (visionText.getText().isEmpty()) {
                            callback.onSuccess(visionText, "No se detectó texto");
                        } else {
                            String fullText = buildDetailedText(visionText);
                            callback.onSuccess(visionText, fullText);
                        }
                    })
                    .addOnFailureListener(callback::onFailure);

        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * Construye un texto detallado con información de bloques y líneas
     */
    private String buildDetailedText(Text visionText) {
        StringBuilder result = new StringBuilder();

        result.append("📄 TEXTO COMPLETO:\n");
        result.append(visionText.getText()).append("\n\n");

        result.append("📊 ANÁLISIS POR BLOQUES:\n");
        result.append("═══════════════════════\n");

        int blockNumber = 1;
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            result.append("\n🔹 Bloque ").append(blockNumber++).append(":\n");
            result.append(block.getText()).append("\n");

            // Opcional: mostrar líneas dentro del bloque
            for (Text.Line line : block.getLines()) {
                result.append("  → ").append(line.getText()).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Interface para resultados detallados del OCR
     */
    public interface DetailedOCRCallback {
        void onSuccess(Text visionText, String formattedText);
        void onFailure(Exception e);
    }

    /**
     * Cierra el reconocedor y libera recursos
     */
    public void close() {
        if (recognizer != null) {
            recognizer.close();
        }
    }
}