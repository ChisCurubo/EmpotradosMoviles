package com.example.snap.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranslateApiClient {
    /**
     * Interfaz para manejar los resultados de la traducción
     */
    public interface Callback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    private static final String TAG = "TranslateApiClient";

    private static final String TRANSLATE_URL =
            "https://TU_ENDPOINT_CLOUD/translate";

    private static TranslateApiClient instance;

    private TranslateApiClient() {}

    /**
     * Patron Singleton
     * @return
     */
    public static synchronized TranslateApiClient getInstance() {
        if (instance == null) {
            instance = new TranslateApiClient();
        }
        return instance;
    }
    // MÉTODO PÚBLICO

    /**
     * Realiza la traducción
     * @param text
     * @param sourceLang
     * @param targetLang
     * @param callback
     */
    public void translate(
            String text,
            String sourceLang,
            String targetLang,
            Callback callback
    ) {

        if (text == null || text.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(TRANSLATE_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // JSON BODY
                JSONObject body = new JSONObject();
                body.put("q", text);
                body.put("source", sourceLang);
                body.put("target", targetLang);

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = connection.getResponseCode();
                InputStream is = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                //Ajusta según la respuesta de tu API
                JSONObject jsonResponse = new JSONObject(response.toString());
                String translatedText = jsonResponse.getString("translatedText");

                callback.onSuccess(translatedText);

            } catch (Exception e) {
                Log.e(TAG, "Error API", e);
                callback.onError(e.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}