package com.example.snap.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.snap.data.entities.VocabularyItem;
import java.util.List;

@Dao
public interface VocabularyDao {

    @Insert
    void insert(VocabularyItem item);

    @Update
    void update(VocabularyItem item);

    @Delete
    void delete(VocabularyItem item);

    @Query("SELECT * FROM vocabulary_items WHERE userId = :userId ORDER BY addedDate DESC")
    LiveData<List<VocabularyItem>> getAllByUser(String userId);

    @Query("SELECT * FROM vocabulary_items WHERE userId = :userId AND languagePair = :languagePair ORDER BY addedDate DESC")
    LiveData<List<VocabularyItem>> getByLanguagePair(String userId, String languagePair);

    @Query("SELECT * FROM vocabulary_items WHERE userId = :userId AND masteryLevel < :minLevel ORDER BY reviewCount ASC, addedDate DESC")
    LiveData<List<VocabularyItem>> getNeedReview(String userId, int minLevel);

    @Query("SELECT * FROM vocabulary_items WHERE userId = :userId AND (originalText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%') ORDER BY addedDate DESC")
    LiveData<List<VocabularyItem>> search(String userId, String query);

    @Query("DELETE FROM vocabulary_items WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM vocabulary_items WHERE userId = :userId")
    void deleteAllByUser(String userId);
}