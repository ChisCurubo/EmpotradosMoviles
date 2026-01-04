package com.example.snap.services;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkTranslationService {
    private Context context;
    private ExecutorService executorService;
    private TranslationApiService apiService;

    public NetworkTranslationService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.apiService = ApiClient.getApiService();
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);

        void onError(String error);
    }

    public void translateText(String text, String sourceLang, String targetLang,
            TranslationCallback callback) {

        executorService.execute(() -> {
            try {
                // Validar entrada
                if (text == null || text.trim().isEmpty()) {
                    callback.onError("Texto vacío");
                    return;
                }

                // Call<ResponseBody> call = apiService.translate("gtx", sourceLang, targetLang,
                // "t", text);
                Call<okhttp3.ResponseBody> call = apiService.translate("gtx", sourceLang, targetLang, "t", text);

                call.enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String jsonResponse = response.body().string();
                                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonResponse);
                                // Google Translate devuelve array de arrays: [[["Translated", "Original", ...],
                                // ...], ...]
                                if (jsonArray.length() > 0) {
                                    org.json.JSONArray sentences = jsonArray.getJSONArray(0);
                                    StringBuilder translatedBuilder = new StringBuilder();

                                    for (int i = 0; i < sentences.length(); i++) {
                                        org.json.JSONArray sentence = sentences.getJSONArray(i);
                                        if (sentence.length() > 0) {
                                            translatedBuilder.append(sentence.getString(0));
                                        }
                                    }

                                    String result = translatedBuilder.toString();
                                    callback.onSuccess(result);
                                } else {
                                    callback.onError("Respuesta vacía");
                                }
                            } catch (Exception e) {
                                callback.onError("Error parsing: " + e.getMessage());
                            }
                        } else {
                            callback.onError("Error API: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        callback.onError("Error de red: " + t.getMessage());
                    }
                });

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }
}