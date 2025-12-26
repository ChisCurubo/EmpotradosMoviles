package com.example.snap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snap.data.entities.Favorite;
import com.example.snap.data.entities.TranslationHistory;
import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.ui.components.FavoritesAdapter;
import com.example.snap.ui.components.HistoryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Actividad de estadísticas refactorizada usando componentes reutilizables.
 * Muestra el historial y favoritos del usuario de forma organizada.
 */
public class StatisticsActivity extends BaseActivity {

    private TextView tvUserEmail, tvFavoriteLangs;
    private RecyclerView rvHistory, rvFavorites;
    private Button btnLogout;
    private HistoryAdapter historyAdapter;
    private FavoritesAdapter favoritesAdapter;
    private BottomNavigationComponent bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Inicializar vistas
        initializeViews();
        
        // Configurar componentes
        setupComponents();
        
        // Si no hay usuario, mostrar opciones de login, pero permitir navegar
        if (!isUserLoggedIn()) {
            showLoginPrompt();
        } else {
            // Cargar datos solo si hay sesión
            loadStatistics();
        }
    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initializeViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvFavoriteLangs = findViewById(R.id.tvFavoriteLangs);
        rvHistory = findViewById(R.id.rvHistory);
        rvFavorites = findViewById(R.id.rvFavorites);
        btnLogout = findViewById(R.id.btnLogout);

        // Configurar UI según estado de sesión
        if (isUserLoggedIn()) {
            tvUserEmail.setText(getCurrentUser());
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            tvUserEmail.setText("Usuario no identificado");
            btnLogout.setText("Iniciar Sesión");
            btnLogout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Configura los componentes de la pantalla
     */
    private void setupComponents() {
        // Configurar RecyclerView para historial
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
        
        // Configurar RecyclerView para favoritos
        favoritesAdapter = new FavoritesAdapter(new ArrayList<>());
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(favoritesAdapter);
        
        // Configurar listener para eliminar favoritos
        favoritesAdapter.setOnFavoriteActionListener(new FavoritesAdapter.OnFavoriteActionListener() {
            @Override
            public void onFavoriteClick(Favorite favorite) {
                showMessage(favorite.getOriginalText() + " → " + favorite.getTranslatedText());
            }

            @Override
            public void onFavoriteDelete(Favorite favorite) {
                viewModel.deleteFavorite(favorite);
                showMessage("Favorito eliminado");
            }
        });

        // Configurar botón de logout/login
        btnLogout.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                performLogout();
            } else {
                navigationManager.navigateToLogin();
            }
        });

        // Configurar navegación
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setNavigationListener(new BottomNavigationComponent.NavigationListener() {
                @Override
                public void onTextoClicked() {
                    navigationManager.navigateToMain();
                    finish();
                }

                @Override
                public void onCamaraClicked() {
                    navigationManager.navigateToMain();
                    finish();
                }

                @Override
                public void onAudioClicked() {
                    showMessage("Modo Audio (Próximamente)");
                }

                @Override
                public void onUsuarioClicked() {
                    showMessage("Ya estás en tu perfil");
                }
            });
            bottomNavigation.setActiveScreen("usuario");
            bottomNavigation.updateUserButtonState();
        }
    }

    /**
     * Carga las estadísticas del usuario
     */
    private void loadStatistics() {
        String userId = getCurrentUser();
        
        // Cargar historial (últimas 10 traducciones)
        viewModel.getHistoryByUserId(userId).observe(this, historyList -> {
            if (historyList != null && !historyList.isEmpty()) {
                // Limitar a las últimas 10
                List<TranslationHistory> last10 = historyList.size() > 10 
                    ? historyList.subList(0, 10) 
                    : historyList;
                historyAdapter.updateData(last10);
            } else {
                historyAdapter.updateData(new ArrayList<>());
            }
        });

        // Cargar favoritos
        viewModel.getFavoritesByUser(userId).observe(this, favorites -> {
            displayFavorites(favorites);
        });

        // Cargar idiomas favoritos (más usados en historial)
        viewModel.getHistoryByUserId(userId).observe(this, historyList -> {
            displayFavoriteLanguages(historyList);
        });
    }

    /**
     * Muestra los favoritos del usuario
     */
    private void displayFavorites(List<Favorite> favorites) {
        if (favorites != null && !favorites.isEmpty()) {
            favoritesAdapter.updateData(favorites);
        } else {
            favoritesAdapter.updateData(new ArrayList<>());
        }
    }

    /**
     * Muestra los idiomas más utilizados
     */
    private void displayFavoriteLanguages(List<TranslationHistory> historyList) {
        if (historyList != null && !historyList.isEmpty()) {
            Map<String, Integer> langCount = new HashMap<>();
            
            for (TranslationHistory history : historyList) {
                String pair = history.getSourceLanguage() + " → " + history.getTargetLanguage();
                langCount.put(pair, langCount.getOrDefault(pair, 0) + 1);
            }
            
            // Encontrar los 3 más usados
            List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(langCount.entrySet());
            sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, sortedList.size()); i++) {
                Map.Entry<String, Integer> entry = sortedList.get(i);
                sb.append(entry.getKey())
                  .append(" (")
                  .append(entry.getValue())
                  .append(" veces)\n");
            }
            
            tvFavoriteLangs.setText(sb.toString().isEmpty() 
                ? "Aún no tienes traducciones" 
                : sb.toString().trim());
        } else {
            tvFavoriteLangs.setText("Aún no tienes traducciones");
        }
    }
    
    /**
     * Muestra un mensaje cuando no hay sesión activa
     */
    private void showLoginPrompt() {
        tvFavoriteLangs.setText("Inicia sesión para ver tus idiomas favoritos");
        favoritesAdapter.updateData(new ArrayList<>());
        showMessage("Inicia sesión para ver tu historial y estadísticas");
    }
}
