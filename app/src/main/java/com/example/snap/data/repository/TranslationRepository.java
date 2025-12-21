package com.example.snap.data.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.snap.data.database.AppDatabase;
import com.example.snap.data.dao.TranslationHistoryDao;
import com.example.snap.data.entities.TranslationHistory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslationRepository {

    private TranslationHistoryDao translationHistoryDao;
    private LiveData<List<TranslationHistory>> allTranslations;
    private ExecutorService executorService;

    public TranslationRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        translationHistoryDao = database.translationHistoryDao();
        allTranslations = translationHistoryDao.getAll();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<TranslationHistory>> getAllTranslations() {
        return allTranslations;
    }

    public void insert(TranslationHistory history) {
        executorService.execute(() -> {
            translationHistoryDao.insert(history);
        });
    }

    public void delete(TranslationHistory history) {
        executorService.execute(() -> {
            translationHistoryDao.delete(history);
        });
    }

    public void deleteById(long id) {
        executorService.execute(() -> {
            translationHistoryDao.deleteById(id);
        });
    }

    public void deleteAll() {
        executorService.execute(() -> {
            translationHistoryDao.deleteAll();
        });
    }
}