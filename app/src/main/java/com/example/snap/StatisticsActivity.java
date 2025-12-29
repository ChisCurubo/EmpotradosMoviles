package com.example.snap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

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

        // Botón Ajustes
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
                intent.putExtra("USER_ID", getCurrentUser());
                startActivity(intent);
            });
        }

        // Botón Borrar todo el historial
        btnClearHistory.setOnClickListener(v ->

        showClearHistoryDialog());

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

                // También actualizar los idiomas favoritos con el mismo historial
                displayFavoriteLanguages(historyList);
            } else {
                historyAdapter.updateData(new ArrayList<>());
                tvFavoriteLangs.setText("Aún no tienes traducciones");
            }
        });

        viewModel.getFavoritesByUser(userId).observe(this, favorites -> {
            displayFavorites(favorites);
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
        showMessage(getString(R.string.inicia_sesion_historial));
    }

    private void showClearHistoryDialog() {
        String userId = getCurrentUser();
        if (userId == null)
            return;

        // Capturar el historial actual antes de mostrar el diálogo
        final List<TranslationHistory> currentHistory = new ArrayList<>();

        // Usar observeForever para capturar los datos una sola vez
        androidx.lifecycle.Observer<List<TranslationHistory>> observer = new androidx.lifecycle.Observer<List<TranslationHistory>>() {
            @Override
            public void onChanged(List<TranslationHistory> historyList) {
                if (historyList != null) {
                    currentHistory.addAll(historyList);
                }
                // Remover el observer inmediatamente después de capturar los datos
                viewModel.getHistoryByUserId(userId).removeObserver(this);

                // Mostrar el diálogo después de capturar los datos
                showClearHistoryDialogWithData(userId, currentHistory);
            }
        };

        viewModel.getHistoryByUserId(userId).observeForever(observer);
    }

    private void showClearHistoryDialogWithData(String userId, List<TranslationHistory> currentHistory) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrar_historial_titulo)
                .setMessage(R.string.borrar_historial_mensaje)
                .setPositiveButton(R.string.si, (dialog, which) -> {
                    viewModel.clearAllHistory(userId);
                    historyAdapter.updateData(new ArrayList<>());

                    // Mostrar Snackbar con opción de deshacer
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.historial_borrado, Snackbar.LENGTH_LONG)
                            .setAction(R.string.deshacer, v -> {
                                // Restaurar todos los elementos
                                for (TranslationHistory history : currentHistory) {
                                    viewModel.restoreHistory(history);
                                }
                            })
                            .show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showClearFavoritesDialog() {
        String userId = getCurrentUser();
        if (userId == null)
            return;

        // Capturar los favoritos actuales antes de mostrar el diálogo
        final List<Favorite> currentFavorites = new ArrayList<>();

        // Usar observeForever para capturar los datos una sola vez
        androidx.lifecycle.Observer<List<Favorite>> observer = new androidx.lifecycle.Observer<List<Favorite>>() {
            @Override
            public void onChanged(List<Favorite> favorites) {
                if (favorites != null) {
                    currentFavorites.addAll(favorites);
                }
                // Remover el observer inmediatamente después de capturar los datos
                viewModel.getFavoritesByUser(userId).removeObserver(this);

                // Mostrar el diálogo después de capturar los datos
                showClearFavoritesDialogWithData(userId, currentFavorites);
            }
        };

        viewModel.getFavoritesByUser(userId).observeForever(observer);
    }

    private void showClearFavoritesDialogWithData(String userId, List<Favorite> currentFavorites) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrar_favoritos_titulo)
                .setMessage(R.string.borrar_favoritos_mensaje)
                .setPositiveButton(R.string.si, (dialog, which) -> {
                    viewModel.clearAllFavorites(userId);
                    favoritesAdapter.updateData(new ArrayList<>());

                    // Mostrar Snackbar con opción de deshacer
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.favoritos_borrados, Snackbar.LENGTH_LONG)
                            .setAction(R.string.deshacer, v -> {
                                // Restaurar todos los elementos
                                for (Favorite favorite : currentFavorites) {
                                    viewModel.restoreFavorite(favorite);
                                }
                            })
                            .show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showDeleteHistoryDialog(TranslationHistory history) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrar_del_historial_titulo)
                .setMessage(R.string.borrar_del_historial_mensaje)
                .setPositiveButton(R.string.si, (dialog, which) -> {
                    viewModel.deleteHistory(history);

                    // Mostrar Snackbar con opción de deshacer
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.traduccion_borrada, Snackbar.LENGTH_LONG)
                            .setAction(R.string.deshacer, v -> {
                                viewModel.restoreHistory(history);
                            })
                            .show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showDeleteFavoriteDialog(Favorite favorite) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.borrar_favorito_titulo)
                .setMessage(R.string.borrar_favorito_mensaje)
                .setPositiveButton(R.string.si, (dialog, which) -> {
                    viewModel.deleteFavorite(favorite);

                    // Mostrar Snackbar con opción de deshacer
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.favorito_borrado, Snackbar.LENGTH_LONG)
                            .setAction(R.string.deshacer, v -> {
                                viewModel.restoreFavorite(favorite);
                            })
                            .show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}