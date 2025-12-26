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
import android.widget.Spinner;
import android.widget.TextView;

import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.utils.LanguageHelper;

/**
 * Actividad principal refactorizada usando componentes reutilizables.
 * Toda la lógica compleja se ha dividido en componentes especializados.
 */
public class MainActivity extends BaseActivity {

    // Vistas
    private Spinner spinnerInput, spinnerOutput;
    private EditText etInput;
    private TextView tvOutput;
    private ImageView btnClear, btnSwap, btnVolume, btnStar, btnCopy;
    private Button chip1, chip2, chip3;
    private ProgressBar progressBar;
    private BottomNavigationComponent bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        // Inicializar vistas
        initializeViews();
        
        // Configurar spinners
        setupSpinners();
        
        // Configurar listeners
        setupListeners();
        
        // Configurar observers
        setupObservers();
        
        // Configurar navegación
        setupBottomNavigation();
        
        // Mostrar mensaje de bienvenida
        showWelcomeMessage();
    }

    @Override
    protected void onSessionUpdated() {
        // Actualizar UI cuando la sesión cambia
        if (bottomNavigation != null) {
            bottomNavigation.updateUserButtonState();
        }
    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initializeViews() {
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

    /**
     * Configura los spinners de idiomas usando LanguageHelper
     */
    private void setupSpinners() {
        String[] languages = LanguageHelper.getAvailableLanguages();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerInput.setAdapter(adapter);
        spinnerOutput.setAdapter(adapter);

        spinnerInput.setSelection(0); // Español
        spinnerOutput.setSelection(1); // Inglés
    }

    /**
     * Configura los listeners de las vistas
     */
    private void setupListeners() {
        // Listener para actualizar chips según idioma
        spinnerInput.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateQuickTranslationChips();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Botón Limpiar
        btnClear.setOnClickListener(v -> {
            etInput.setText("");
            tvOutput.setText("La traducción aparecerá aquí");
            hideProgress();
        });

        // Botón Intercambiar idiomas
        btnSwap.setOnClickListener(v -> {
            int inputPos = spinnerInput.getSelectedItemPosition();
            int outputPos = spinnerOutput.getSelectedItemPosition();
            spinnerInput.setSelection(outputPos);
            spinnerOutput.setSelection(inputPos);

            String inputText = etInput.getText().toString();
            if (!inputText.isEmpty()) {
                performTranslation();
            }
        });

        // Botón Copiar al portapapeles
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

        // Botón Star (Guardar como favorito)
        btnStar.setOnClickListener(v -> saveFavorite());

        // Botón Audio
        if (btnVolume != null) {
            btnVolume.setOnClickListener(v -> showMessage("Función de audio próximamente"));
        }

        // Enter en el teclado
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            performTranslation();
            return true;
        });

        // Listeners para los Chips
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

    /**
     * Configura la navegación inferior
     */
    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setNavigationListener(new BottomNavigationComponent.NavigationListener() {
                @Override
                public void onTextoClicked() {
                    btnClear.performClick();
                    showMessage("Modo Texto");
                }

                @Override
                public void onCamaraClicked() {
                    btnClear.performClick();
                    showMessage("Modo Cámara");
                }

                @Override
                public void onAudioClicked() {
                    showMessage("Modo Audio (Próximamente)");
                }

                @Override
                public void onUsuarioClicked() {
                    // Navegar a estadísticas siempre, sin requerir login
                    navigationManager.navigateToStatistics();
                }
            });
            bottomNavigation.setActiveScreen("texto");
            bottomNavigation.updateUserButtonState();
        }
    }

    /**
     * Configura los observadores del ViewModel
     */
    private void setupObservers() {
        viewModel.getCurrentTranslation().observe(this, translatedText -> {
            hideProgress();
            if (translatedText != null) {
                tvOutput.setText(translatedText);
                // Animación de entrada
                tvOutput.setAlpha(0);
                tvOutput.animate().alpha(1).setDuration(500).start();
            }
        });
    }

    /**
     * Realiza una traducción
     */
    private void performTranslation() {
        String text = etInput.getText().toString().trim();

        if (text.isEmpty()) {
            showMessage("Escribe algo para traducir");
            return;
        }

        showProgress();
        tvOutput.setText("Traduciendo...");

        String sourceLang = LanguageHelper.getLanguageCode(spinnerInput.getSelectedItemPosition());
        String targetLang = LanguageHelper.getLanguageCode(spinnerOutput.getSelectedItemPosition());

        viewModel.translateText(text, sourceLang, targetLang, getCurrentUser());
    }

    /**
     * Guarda una traducción como favorita
     */
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
        
        String sourceLang = LanguageHelper.getLanguageCode(spinnerInput.getSelectedItemPosition());
        String targetLang = LanguageHelper.getLanguageCode(spinnerOutput.getSelectedItemPosition());
        
        viewModel.saveFavorite(getCurrentUser(), inputText, outputText, sourceLang, targetLang);
        showMessage("Agregado a favoritos ⭐");
    }

    /**
     * Actualiza los chips de traducción rápida usando LanguageHelper
     */
    private void updateQuickTranslationChips() {
        String[] phrases = LanguageHelper.getQuickPhrasesByPosition(
            spinnerInput.getSelectedItemPosition()
        );
        chip1.setText(phrases[0]);
        chip2.setText(phrases[1]);
        chip3.setText(phrases[2]);
    }

    /**
     * Verifica si el texto es una traducción válida
     */
    private boolean isValidTranslation(String text) {
        return text != null && 
               !text.isEmpty() && 
               !text.equals("La traducción aparecerá aquí") &&
               !text.equals("Traduciendo...");
    }

    /**
     * Muestra el indicador de progreso
     */
    private void showProgress() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSwap.setEnabled(false);
    }

    /**
     * Oculta el indicador de progreso
     */
    private void hideProgress() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnSwap.setEnabled(true);
    }

    /**
     * Muestra mensaje de bienvenida según el estado de la sesión
     */
    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado: Inicia sesión para guardar");
        } else {
            showMessage("Sesión activa: " + getCurrentUser());
        }
    }
}
