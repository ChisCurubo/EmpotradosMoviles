package com.example.snap.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.snap.data.database.AppDatabase;
import com.example.snap.data.dao.FavoriteDao;
import com.example.snap.data.entities.Favorite;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteRepository {

    private FavoriteDao favoriteDao;
    private ExecutorService executorService;

    public FavoriteRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        favoriteDao = database.favoriteDao();
        // Usamos un pool de hilos para operaciones de base de datos
        executorService = Executors.newSingleThreadExecutor();
    }

    // Insertar un favorito en segundo plano
    public void insert(Favorite favorite) {
        executorService.execute(() -> {
            favoriteDao.insert(favorite);
        });
    }

    // Borrar un favorito en segundo plano
    public void delete(Favorite favorite) {
        executorService.execute(() -> {
            favoriteDao.delete(favorite);
        });
    }

    // Obtener favoritos filtrados por usuario
    public LiveData<List<Favorite>> getAllFavoritesByUser(String userId) {
        return favoriteDao.getAllFavoritesByUser(userId);
    }

    // Obtener estadísticas de idiomas por usuario
    public LiveData<List<String>> getFavoriteLanguagesByUser(String userId) {
        return favoriteDao.getFavoriteLanguagesByUser(userId);
    }
    
    // Borrar todos los favoritos de un usuario
    public void deleteAllByUser(String userId) {
        executorService.execute(() -> {
            favoriteDao.deleteByUser(userId);
        });
    }
}
