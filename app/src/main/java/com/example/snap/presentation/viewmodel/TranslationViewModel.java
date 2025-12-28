package com.example.snap.presentation.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.snap.data.entities.Favorite;
import com.example.snap.data.entities.TranslationHistory;
import com.example.snap.data.entities.User;
import com.example.snap.data.repository.FavoriteRepository;
import com.example.snap.data.repository.TranslationHistoryRepository;
import com.example.snap.data.repository.UserRepository;
import com.example.snap.services.NetworkTranslationService;

import java.util.List;

public class TranslationViewModel extends AndroidViewModel {

    private TranslationHistoryRepository historyRepository;
    private FavoriteRepository favoriteRepository;
    private UserRepository userRepository;

    private NetworkTranslationService networkService;
    private MutableLiveData<String> currentTranslation;

    public TranslationViewModel(Application application) {
        super(application);
        historyRepository = new TranslationHistoryRepository(application);
        favoriteRepository = new FavoriteRepository(application);
        userRepository = new UserRepository(application);

        networkService = new NetworkTranslationService(application);
        currentTranslation = new MutableLiveData<>();
    }


    public void insertUser(String userId, String name, String email, Runnable onSuccess, Runnable onError) {

        userRepository.getUserByEmail(email).observeForever(new Observer<User>() {
            @Override
            public void onChanged(User existingUser) {
                // Remover observer inmediatamente para evitar múltiples llamadas
                userRepository.getUserByEmail(email).removeObserver(this);
                
                if (existingUser == null) {
                    User newUser = new User(userId, name, email);
                    // Usar el register con callback para ejecutar onSuccess solo cuando se haya guardado
                    userRepository.register(newUser, () -> {
                        android.util.Log.d("TranslationViewModel", "Usuario guardado en BD, ejecutando onSuccess");
                        if (onSuccess != null) onSuccess.run();
                    });
                } else {
                    if (onError != null) onError.run();
                }
            }
        });
    }


    public LiveData<User> getUserData(String userId) {
        return userRepository.getUserByEmail(userId);
    }

    public LiveData<List<TranslationHistory>> getHistoryByUserId(String userId) {
        return historyRepository.getHistoryByUserId(userId);
    }

    public void clearHistory(String userId) {
        historyRepository.clearHistory(userId);
    }

    public LiveData<String> getCurrentTranslation() {
        return currentTranslation;
    }

    public void translateText(String text, String sourceLang, String targetLang, String userId) {
        showLoading();
        networkService.translateText(text, sourceLang, targetLang, new NetworkTranslationService.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                currentTranslation.postValue(translatedText);

                // Solo intentamos guardar si hay un usuario logueado
                if (userId != null) {
                    saveToHistory(userId, text, translatedText, sourceLang, targetLang, "TEXT");
                }
            }
            @Override
            public void onError(String error) {
                currentTranslation.postValue("Error: " + error);
            }
        });
    }

    private void saveToHistory(String userId, String sourceText, String translatedText,
                               String sourceLang, String targetLang, String inputMethod) {

        if (userId == null) {
            return;
        }

        TranslationHistory history = new TranslationHistory(
                userId,
                sourceText,
                translatedText,
                sourceLang,
                targetLang,
                inputMethod
        );

        historyRepository.insert(history);
    }

    public void addToFavorites(String userId, String original, String translated, String sLang, String tLang, boolean isExp) {
        // También es recomendable validar aquí si el userId es nulo según tu lógica de negocio
        if (userId != null) {
            Favorite fav = new Favorite(userId, original, translated, sLang, tLang, isExp);
            favoriteRepository.insert(fav);
        }
    }
    
    public void saveFavorite(String userId, String original, String translated, String sLang, String tLang) {
        if (userId != null) {
            Favorite fav = new Favorite(userId, original, translated, sLang, tLang, false);
            favoriteRepository.insert(fav);
        }
    }
    
    public LiveData<List<Favorite>> getFavoritesByUser(String userId) {
        return favoriteRepository.getAllFavoritesByUser(userId);
    }

    public LiveData<List<String>> getFavoriteLanguages(String userId) {
        return favoriteRepository.getFavoriteLanguagesByUser(userId);
    }
    
    public void deleteFavorite(Favorite favorite) {
        if (favorite != null) {
            favoriteRepository.delete(favorite);
        }
    }
    
    public void clearAllHistory(String userId) {
        if (userId != null) {
            historyRepository.clearAllHistory(userId);
        }
    }
    
    public void clearAllFavorites(String userId) {
        if (userId != null) {
            favoriteRepository.deleteAllByUser(userId);
        }
    }

    private void showLoading() {
        currentTranslation.setValue("Traduciendo...");
    }
}
