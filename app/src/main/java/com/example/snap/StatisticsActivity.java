package com.example.snap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
 * Actividad de estadísticas refactorizada.
 * Utiliza BottomNavigationComponent para la navegación.
 */
public class StatisticsActivity extends BaseActivity {

    private TextView tvUserEmail, tvFavoriteLangs;
    private RecyclerView rvHistory, rvFavorites;
    private Button btnLogout, btnClearHistory, btnClearFavorites;
    private HistoryAdapter historyAdapter;
    private FavoritesAdapter favoritesAdapter;
    private BottomNavigationComponent bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initializeViews();
        setupComponents();
        
        if (!isUserLoggedIn()) {
            showLoginPrompt();
        } else {
            loadStatistics();
        }
    }

    private void initializeViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvFavoriteLangs = findViewById(R.id.tvFavoriteLangs);
        rvHistory = findViewById(R.id.rvHistory);
        rvFavorites = findViewById(R.id.rvFavorites);
        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnClearFavorites = findViewById(R.id.btnClearFavorites);

        if (isUserLoggedIn()) {
            tvUserEmail.setText(getCurrentUser());
            btnLogout.setVisibility(View.VISIBLE);
            btnClearHistory.setVisibility(View.VISIBLE);
            btnClearFavorites.setVisibility(View.VISIBLE);
        } else {
            tvUserEmail.setText("Usuario no identificado");
            btnLogout.setText("Iniciar Sesión");
            btnLogout.setVisibility(View.VISIBLE);
            btnClearHistory.setVisibility(View.GONE);
            btnClearFavorites.setVisibility(View.GONE);
        }
    }

    private void setupComponents() {
        // RecyclerViews
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
        
        // Configurar listener del historial
        historyAdapter.setOnHistoryActionListener(new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onHistoryItemClick(TranslationHistory history) {
                showMessage(history.getSourceText() + " → " + history.getTranslatedText());
            }

            @Override
            public void onHistoryItemDelete(TranslationHistory history) {
                showDeleteHistoryDialog(history);
            }
        });
        
        favoritesAdapter = new FavoritesAdapter(new ArrayList<>());
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(favoritesAdapter);
        
        favoritesAdapter.setOnFavoriteActionListener(new FavoritesAdapter.OnFavoriteActionListener() {
            @Override
            public void onFavoriteClick(Favorite favorite) {
                showMessage(favorite.getOriginalText() + " → " + favorite.getTranslatedText());
            }

            @Override
            public void onFavoriteDelete(Favorite favorite) {
                showDeleteFavoriteDialog(favorite);
            }
        });

        // Botón Logout
        btnLogout.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                performLogout();
            } else {
                navigationManager.navigateToLogin();
            }
        });
        
        // Botón Borrar todo el historial
        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());
        
        // Botón Borrar todos los favoritos
        btnClearFavorites.setOnClickListener(v -> showClearFavoritesDialog());

        // Configurar navegación
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("usuario");
            bottomNavigation.updateUserButtonState();
            
            // No es necesario añadir listeners extra, el componente ya navega 
            // y cierra la actividad actual automáticamente.
        }
    }

    private void loadStatistics() {
        String userId = getCurrentUser();
        
        viewModel.getHistoryByUserId(userId).observe(this, historyList -> {
            if (historyList != null && !historyList.isEmpty()) {
                List<TranslationHistory> last10 = historyList.size() > 10 
                    ? historyList.subList(0, 10) 
                    : historyList;
                historyAdapter.updateData(last10);
            } else {
                historyAdapter.updateData(new ArrayList<>());
            }
        });

        viewModel.getFavoritesByUser(userId).observe(this, favorites -> {
            displayFavorites(favorites);
        });

        viewModel.getHistoryByUserId(userId).observe(this, historyList -> {
            displayFavoriteLanguages(historyList);
        });
    }

    private void displayFavorites(List<Favorite> favorites) {
        if (favorites != null && !favorites.isEmpty()) {
            favoritesAdapter.updateData(favorites);
        } else {
            favoritesAdapter.updateData(new ArrayList<>());
        }
    }

    private void displayFavoriteLanguages(List<TranslationHistory> historyList) {
        if (historyList != null && !historyList.isEmpty()) {
            Map<String, Integer> langCount = new HashMap<>();
            
            for (TranslationHistory history : historyList) {
                String pair = history.getSourceLanguage() + " → " + history.getTargetLanguage();
                langCount.put(pair, langCount.getOrDefault(pair, 0) + 1);
            }
            
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
    
    private void showLoginPrompt() {
        tvFavoriteLangs.setText("Inicia sesión para ver tus idiomas favoritos");
        favoritesAdapter.updateData(new ArrayList<>());
        showMessage("Inicia sesión para ver tu historial y estadísticas");
    }
    
    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar historial")
                .setMessage("¿Desea borrar todo el historial?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    String userId = getCurrentUser();
                    if (userId != null) {
                        viewModel.clearAllHistory(userId);
                        showMessage("Historial borrado");
                        historyAdapter.updateData(new ArrayList<>());
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    
    private void showClearFavoritesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar favoritos")
                .setMessage("¿Desea borrar todos los favoritos?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    String userId = getCurrentUser();
                    if (userId != null) {
                        viewModel.clearAllFavorites(userId);
                        showMessage("Favoritos borrados");
                        favoritesAdapter.updateData(new ArrayList<>());
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    
    private void showDeleteHistoryDialog(TranslationHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar del historial")
                .setMessage("¿Desea borrar esta traducción del historial?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    viewModel.deleteHistory(history);
                    showMessage("Eliminado del historial");
                })
                .setNegativeButton("No", null)
                .show();
    }
    
    private void showDeleteFavoriteDialog(Favorite favorite) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar favorito")
                .setMessage("¿Desea borrar este favorito?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    viewModel.deleteFavorite(favorite);
                    showMessage("Favorito eliminado");
                })
                .setNegativeButton("No", null)
                .show();
    }
}