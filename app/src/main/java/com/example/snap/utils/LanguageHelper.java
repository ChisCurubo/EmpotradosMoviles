package com.example.snap.utils;

import com.example.snap.R;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona idiomas y traducciones.
 * Centraliza toda la lógica relacionada con idiomas.
 */
public class LanguageHelper {

    // Mapa de posiciones a códigos de idioma
    private static final Map<Integer, String> POSITION_TO_CODE = new HashMap<>();
    private static final Map<String, Integer> CODE_TO_POSITION = new HashMap<>();
    private static final Map<String, String[]> QUICK_PHRASES = new HashMap<>();

    static {
        // Inicializar mapas de códigos
        POSITION_TO_CODE.put(0, "es");
        POSITION_TO_CODE.put(1, "en");
        POSITION_TO_CODE.put(2, "fr");
        POSITION_TO_CODE.put(3, "de");
        POSITION_TO_CODE.put(4, "it");
        POSITION_TO_CODE.put(5, "pt");

        CODE_TO_POSITION.put("es", 0);
        CODE_TO_POSITION.put("en", 1);
        CODE_TO_POSITION.put("fr", 2);
        CODE_TO_POSITION.put("de", 3);
        CODE_TO_POSITION.put("it", 4);
        CODE_TO_POSITION.put("pt", 5);

        // Frases rápidas por idioma
        QUICK_PHRASES.put("es", new String[] { "Hola", "¿Cómo estás?", "Gracias" });
        QUICK_PHRASES.put("en", new String[] { "Hello", "How are you?", "Thank you" });
        QUICK_PHRASES.put("fr", new String[] { "Bonjour", "Comment ça va?", "Merci" });
        QUICK_PHRASES.put("de", new String[] { "Hallo", "Wie geht es dir?", "Danke" });
        QUICK_PHRASES.put("it", new String[] { "Ciao", "Come stai?", "Grazie" });
        QUICK_PHRASES.put("pt", new String[] { "Olá", "Como você está?", "Obrigado" });
    }

    /**
     * Obtiene el código de idioma a partir de la posición del spinner
     */
    public static String getLanguageCode(int position) {
        return POSITION_TO_CODE.getOrDefault(position, "en");
    }

    /**
     * Obtiene la posición del spinner a partir del código de idioma
     */
    public static int getLanguagePosition(String code) {
        return CODE_TO_POSITION.getOrDefault(code, 1);
    }

    /**
     * Obtiene las frases rápidas para un idioma específico
     */
    public static String[] getQuickPhrases(String languageCode) {
        return QUICK_PHRASES.getOrDefault(languageCode, QUICK_PHRASES.get("en"));
    }

    /**
     * Obtiene las frases rápidas por posición de spinner
     */
    public static String[] getQuickPhrasesByPosition(int position) {
        String code = getLanguageCode(position);
        return getQuickPhrases(code);
    }

    /**
     * Obtiene el array de nombres de idiomas disponibles
     */
    public static String[] getAvailableLanguages(android.content.Context context) {
        return context.getResources().getStringArray(R.array.languages);
    }

    /**
     * Obtiene el nombre del idioma a partir del código
     */
    public static String getLanguageName(android.content.Context context, String code) {
        String[] languages = getAvailableLanguages(context);
        int position = getLanguagePosition(code);
        if (position >= 0 && position < languages.length) {
            return languages[position];
        }
        return "Unknown";
    }

    /**
     * Obtiene el código de locale para reconocimiento de voz (e.g. "es-ES")
     */
    public static String getSpeechLocale(String code) {
        switch (code) {
            case "es":
                return "es-ES";
            case "en":
                return "en-US";
            case "fr":
                return "fr-FR";
            case "de":
                return "de-DE";
            case "it":
                return "it-IT";
            case "pt":
                return "pt-PT";
            case "zh":
                return "zh-CN";
            case "ja":
                return "ja-JP";
            default:
                return "en-US";
        }
    }
}
