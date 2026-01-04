package com.example.snap.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TranslationApiService {
    @GET("translate_a/single")
    Call<ResponseBody> translate(
            @Query("client") String client,
            @Query("sl") String sourceLang,
            @Query("tl") String targetLang,
            @Query("dt") String dt,
            @Query("q") String text);
}