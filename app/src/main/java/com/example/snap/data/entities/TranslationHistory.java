package com.example.snap.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "translation_history")
public class TranslationHistory {
    @PrimaryKey(autoGenerate = true)
    private long id = 0;

    private String userId;
    private String sourceText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private long timestamp;
    private String inputMethod; // "TEXT", "CAMERA", "VOICE"

    @ColumnInfo(defaultValue = "0")
    private boolean isFavorite = false;

    private String category;

    // Constructores
    public TranslationHistory() {}

    public TranslationHistory(String userId, String sourceText, String translatedText,
                              String sourceLanguage, String targetLanguage, String inputMethod) {
        this.userId = userId;
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.timestamp = System.currentTimeMillis();
        this.inputMethod = inputMethod;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getInputMethod() { return inputMethod; }
    public void setInputMethod(String inputMethod) { this.inputMethod = inputMethod; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}