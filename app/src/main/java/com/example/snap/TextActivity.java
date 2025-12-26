package com.example.snap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.snap.ui.components.LanguageSelector;

import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.utils.LanguageHelper;

/**
 * Actividad principal de traducción de texto.
 * Permite traducir texto entre diferentes idiomas.
 */
public class TextActivity extends BaseActivity {

    // Vistas
    private EditText etInput;
    private TextView tvOutput;
    private LanguageSelector languageSelector;
    private ImageView btnClear, btnVolume, btnStar, btnCopy;
    private Button chip1, chip2, chip3;
    private ProgressBar progressBar;
    private BottomNavigationComponent bottomNavigation;

    // Selector
    private String sourceLang = "es";
    private String targetLang = "en";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        initializeViews();
        setupLanguageSelector();
        setupListeners();
        setupObservers();

        // Configurar navegación
        setupBottomNavigation();

        showWelcomeMessage();
    }

    @Override
    protected void onSessionUpdated() {
        if (bottomNavigation != null) {
            bottomNavigation.updateUserButtonState();
        }
    }

    private void initializeViews() {
        languageSelector = findViewById(R.id.languageSelector);

        etInput = findViewById(R.id.etInput);
        tvOutput = findViewById(R.id.tvOutput);
        btnClear = findViewById(R.id.btnClear);
        btnVolume = findViewById(R.id.btnVolume);
        btnStar = findViewById(R.id.btnStar);
        btnCopy = findViewById(R.id.btnCopy);
        chip1 = findViewById(R.id.chip1);
        chip2 = findViewById(R.id.chip2);
        chip3 = findViewById(R.id.chip3);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupLanguageSelector() {
        languageSelector.setOnLanguageChangeListener((srcCode, tgtCode, srcIndex, tgtIndex) -> {
            sourceLang = srcCode;
            targetLang = tgtCode;
            updateQuickTranslationChips();
        });
    }

    private void setupListeners() {


        btnClear.setOnClickListener(v -> {
            etInput.setText("");
            tvOutput.setText("La traducción aparecerá aquí");
            hideProgress();
        });



        btnCopy.setOnClickListener(v -> {
            String textToCopy = tvOutput.getText().toString();
            if (isValidTranslation(textToCopy)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Traducción", textToCopy);
                clipboard.setPrimaryClip(clip);
                showMessage("Texto copiado");
            } else {
                showMessage("No hay nada para copiar");
            }
        });

        btnStar.setOnClickListener(v -> saveFavorite());

        if (btnVolume != null) {
            btnVolume.setOnClickListener(v -> showMessage("Función de audio próximamente"));
        }

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            performTranslation();
            return true;
        });

        View.OnClickListener chipListener = v -> {
            Button b = (Button) v;
            etInput.setText(b.getText().toString());
            performTranslation();
        };
        chip1.setOnClickListener(chipListener);
        chip2.setOnClickListener(chipListener);
        chip3.setOnClickListener(chipListener);

        updateQuickTranslationChips();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            // Solo establecemos la pantalla activa, el componente maneja la navegación
            bottomNavigation.setActiveScreen("texto");

            // Overrides opcionales para comportamiento específico
            bottomNavigation.setNavigationListener(new BottomNavigationComponent.NavigationListener() {
                @Override
                public void onTextoClicked() {
                    // Si ya estamos en texto, limpiamos el campo
                    btnClear.performClick();
                    showMessage("Modo Texto reiniciado");
                }

                @Override
                public void onCamaraClicked() {
                    bottomNavigation.getNavigationManager().navigateToCamera();
                    // finish(); // No cerramos TextActivity si queremos mantenerla como base, o sí si queremos pure tabs.
                    // Si no ponemos finish() aquí pero el componente tiene finishCurrentActivity(),
                    // el setNavigationListener evita que se ejecute el switch por defecto del componente.
                    // Si queremos que TextActivity se cierre, debemos llamarlo explícitamente o no poner el listener y dejar el default.

                    // Como estamos overrideando, llamamos finish si queremos comportamiento tab
                    // finish();
                }

                @Override
                public void onAudioClicked() {
                    showMessage("Modo Audio (Próximamente)");
                }

                @Override
                public void onUsuarioClicked() {
                    bottomNavigation.getNavigationManager().navigateToStatistics();
                    // finish();
                }
            });

            bottomNavigation.updateUserButtonState();
        }
    }
/*
    // --- AHORA SÍ: Limpio y sin lógica repetida ---
    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("texto");
            bottomNavigation.updateUserButtonState();
        }
    }*/

    private void setupObservers() {
        viewModel.getCurrentTranslation().observe(this, translatedText -> {
            hideProgress();
            if (translatedText != null) {
                tvOutput.setText(translatedText);
                tvOutput.setAlpha(0);
                tvOutput.animate().alpha(1).setDuration(500).start();
            }
        });
    }

    private void performTranslation() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) {
            showMessage("Escribe algo para traducir");
            return;
        }

        showProgress();
        tvOutput.setText("Traduciendo...");

        viewModel.translateText(text, sourceLang, targetLang, getCurrentUser());
    }

    private void saveFavorite() {
        String inputText = etInput.getText().toString().trim();
        String outputText = tvOutput.getText().toString().trim();

        if (!isUserLoggedIn()) {
            showMessage("Inicia sesión para guardar favoritos");
            return;
        }

        if (inputText.isEmpty() || !isValidTranslation(outputText)) {
            showMessage("No hay traducción para guardar");
            return;
        }

        viewModel.saveFavorite(getCurrentUser(), inputText, outputText, sourceLang, targetLang);
        showMessage("Agregado a favoritos ⭐");
    }

    private void updateQuickTranslationChips() {
        String[] phrases = LanguageHelper.getQuickPhrases(sourceLang);
        chip1.setText(phrases[0]);
        chip2.setText(phrases[1]);
        chip3.setText(phrases[2]);
    }


    private boolean isValidTranslation(String text) {
        return text != null &&
               !text.isEmpty() &&
               !text.equals("La traducción aparecerá aquí") &&
               !text.equals("Traduciendo...");
    }

    private void showProgress() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado: Inicia sesión para guardar");
        } else {
            showMessage("Sesión activa: " + getCurrentUser());
        }
    }
}