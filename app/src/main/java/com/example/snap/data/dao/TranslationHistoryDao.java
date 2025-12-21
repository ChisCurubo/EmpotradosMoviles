package com.example.snap.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.snap.data.entities.TranslationHistory;
import java.util.List;

@Dao
public interface TranslationHistoryDao {

    @Insert
    void insert(TranslationHistory history);

    @Update
    void update(TranslationHistory history);

    @Delete
    void delete(TranslationHistory history);

    @Query("DELETE FROM translation_history WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM translation_history")
    void deleteAll();

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> getAll();

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> getFavorites();

    @Query("SELECT * FROM translation_history WHERE sourceLanguage = :lang OR targetLanguage = :lang ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> getByLanguage(String lang);

    @Query("SELECT * FROM translation_history WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> search(String query);
}