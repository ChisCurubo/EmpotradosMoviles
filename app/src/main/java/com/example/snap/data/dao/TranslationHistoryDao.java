package com.example.snap.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.snap.data.entities.TranslationHistory;
import java.util.List;

@Dao
public interface TranslationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TranslationHistory history);

    @Query("SELECT * FROM translation_history WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> getAllHistoryByUser(String userId);
    
    @Query("SELECT COUNT(*) FROM translation_history WHERE userId = :userId AND sourceText = :sourceText AND translatedText = :translatedText AND sourceLanguage = :sourceLang AND targetLanguage = :targetLang AND ((:currentTime - timestamp) < 2000)")
    int countRecentDuplicates(String userId, String sourceText, String translatedText, String sourceLang, String targetLang, long currentTime);

    @Query("DELETE FROM translation_history WHERE userId = :userId")
    void deleteHistoryByUser(String userId);

    @Delete
    void delete(TranslationHistory history);
}
