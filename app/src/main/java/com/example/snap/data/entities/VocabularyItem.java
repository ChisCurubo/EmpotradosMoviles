package com.example.snap.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "vocabulary_items")
public class VocabularyItem {
    @PrimaryKey(autoGenerate = true)
    private long id = 0;

    private String userId;
    private String originalText;
    private String translatedText;
    private String languagePair;
    private long addedDate;

    @ColumnInfo(defaultValue = "0")
    private int reviewCount = 0;

    @ColumnInfo(defaultValue = "0")
    private int masteryLevel = 0; // 0-100

    private String tags;

    // Constructores
    public VocabularyItem() {}

    public VocabularyItem(String userId, String originalText, String translatedText,
                          String languagePair) {
        this.userId = userId;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.languagePair = languagePair;
        this.addedDate = System.currentTimeMillis();
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public String getLanguagePair() { return languagePair; }
    public void setLanguagePair(String languagePair) { this.languagePair = languagePair; }

    public long getAddedDate() { return addedDate; }
    public void setAddedDate(long addedDate) { this.addedDate = addedDate; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}