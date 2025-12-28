package com.example.snap.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.snap.data.database.AppDatabase;
import com.example.snap.data.dao.TranslationHistoryDao;
import com.example.snap.data.entities.TranslationHistory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslationHistoryRepository {
    private TranslationHistoryDao dao;
    private ExecutorService executorService;

    public TranslationHistoryRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        dao = database.translationHistoryDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<TranslationHistory>> getHistoryByUserId(String userId) {
        return dao.getAllHistoryByUser(userId);
    }

    public void insert(TranslationHistory history) {
        executorService.execute(() -> {
            // Verificar si ya existe un registro muy similar en los últimos 2 segundos
            int duplicates = dao.countRecentDuplicates(
                history.getUserId(),
                history.getSourceText(),
                history.getTranslatedText(),
                history.getSourceLanguage(),
                history.getTargetLanguage(),
                System.currentTimeMillis()
            );
            
            // Solo insertar si no hay duplicados recientes
            if (duplicates == 0) {
                dao.insert(history);
            }
        });
    }

    public void delete(TranslationHistory history) {
        executorService.execute(() -> dao.delete(history));
    }

    public void clearHistory(String userId) {
        executorService.execute(() -> dao.deleteHistoryByUser(userId));
    }
    
    // Borrar todo el historial de un usuario
    public void clearAllHistory(String userId) {
        executorService.execute(() -> dao.deleteHistoryByUser(userId));
    }
}