package com.example.snap.camara;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.snap.R;
import com.example.snap.presentation.viewmodel.TranslationViewModel;
import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.ui.components.LanguageSelector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Clase principal que maneja la cámara, el OCR (lectura de texto) y muestra la traducción.
 */
public class Camara extends BaseActivity {

    private static final String TAG = "CamaraActivity";

    // Códigos para saber qué permiso pedimos
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    // TIEMPO DE ESPERA (1.2 segundos) entre cada escaneo de la cámara.
    // Esto es vital para no saturar la API ni calentar el teléfono.
    private static final long ANALYSIS_DELAY = 1200L;

    // --- Elementos visuales (la pantalla) ---
    private PreviewView cameraPreview;      // Donde se ve la cámara
    private ImageView imagePreview;         // Donde se ve la foto estática (galería)
    private GraphicOverlay graphicOverlay;  // Capa transparente para dibujar los cuadritos
    private TextView tvTranslatedResult;    // Texto abajo con la traducción completa

    // Botones
    private FloatingActionButton btnCapture;
    private FloatingActionButton btnGallery;
    private FloatingActionButton btnRefresh;

    // Componentes propios
    private LanguageSelector languageSelector;
    private BottomNavigationComponent bottomNavigation;

    // --- Herramientas de la Cámara (CameraX) ---
    private ImageCapture imageCapture;       // Para tomar foto
    private ImageAnalysis imageAnalysis;     // Para analizar el video en vivo
    private ExecutorService cameraExecutor;  // Hilo de fondo para no trabar la UI
    private ProcessCameraProvider cameraProvider;

    // --- Variables de control ---
    private boolean isProcessing = false;
    private long lastAnalysisTime = 0; // Guarda la hora del último escaneo

    // --- OCR y Traducción ---
    private TextRecognizer textRecognizer;   // El lector de texto de Google
    private TranslationViewModel viewModel;  // Conexión con la API
    private OCR_Helper ocrHelper;            // Nuestro ayudante que decide si usar ML Kit o API

    // --- Idiomas seleccionados ---
    private String currentSourceCode = "es";
    private String currentTargetCode = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        // 1. Vinculamos los elementos del diseño (XML) con el código
        initializeViews();
        setupLanguageSelector();
        setupButtons();
        setupNavigation();

        // 2. Iniciamos el hilo secundario para la cámara
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 3. Iniciamos el lector de texto
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // 4. Preparamos el ViewModel y el Helper de traducción
        viewModel = new ViewModelProvider(this).get(TranslationViewModel.class);
        ocrHelper = new OCR_Helper(viewModel);

        // 5. Pedimos permiso de cámara si no lo tenemos
        checkAndRequestPermissions();

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
        cameraPreview = findViewById(R.id.cameraPreview);
        imagePreview = findViewById(R.id.imagePreview);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        tvTranslatedResult = findViewById(R.id.tvTranslatedResult);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        btnRefresh = findViewById(R.id.btnRefresh);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.attachToScreen("camara");
        }
    }

    private void setupLanguageSelector() {
        // Primero intentamos cargar los idiomas de la sesión actual
        String[] currentLanguages = languageSelector.loadCurrentLanguages();
        
        if (currentLanguages != null) {
            // Si existen idiomas guardados en la sesión, los usamos
            languageSelector.setLanguages(currentLanguages[0], currentLanguages[1]);
        } else {
            // Si no existen, cargamos los idiomas por defecto del usuario
            String currentUser = getCurrentUser();
            if (currentUser == null) currentUser = "guest";

            android.content.SharedPreferences prefs = getSharedPreferences(
                    com.example.snap.SettingsActivity.PREFS_NAME + "_" + currentUser, MODE_PRIVATE);

            // Para el invitado (guest), usar inglés → español
            String defaultSource = prefs.getString(com.example.snap.SettingsActivity.KEY_DEFAULT_SOURCE_LANG, 
                    currentUser.equals("guest") ? "en" : "es");
            String defaultTarget = prefs.getString(com.example.snap.SettingsActivity.KEY_DEFAULT_TARGET_LANG, 
                    currentUser.equals("guest") ? "es" : "en");

            languageSelector.setLanguages(defaultSource, defaultTarget);
        }

        // Escuchamos cuando el usuario cambia los idiomas
        languageSelector.setOnLanguageChangeListener((srcCode, tgtCode, srcIndex, tgtIndex) -> {
            currentSourceCode = srcCode;
            currentTargetCode = tgtCode;
            
            // Guardar la selección actual para que persista al cambiar de pantalla
            languageSelector.saveCurrentLanguages();
        });
    }

    private void setupButtons() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });
        btnRefresh.setOnClickListener(v -> resetToCamera());
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnCapture.setEnabled(true);
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("camara");
            bottomNavigation.updateUserButtonState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isProcessing = false;
        if (graphicOverlay != null) graphicOverlay.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpieza de memoria al cerrar la pantalla
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (textRecognizer != null) textRecognizer.close();
        if (ocrHelper != null) ocrHelper.close(); // Cerramos los traductores
    }

    // Configura e inicia la cámara
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(); // Aquí conectamos la lógica

                // Mostrar botones correctos
                btnCapture.setVisibility(View.VISIBLE);
                btnGallery.setVisibility(View.VISIBLE);
                btnRefresh.setVisibility(View.GONE);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Une la vista previa, la captura y el análisis al ciclo de vida de la app
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        imageCapture = new ImageCapture.Builder().build();

        // Configuración del análisis de imagen (para el OCR)
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Si va lento, descarta frames viejos
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            // Llamamos a nuestra función de procesamiento
            processImageProxy(imageProxy);
        });

        try {
            cameraProvider.unbindAll(); // Limpiar configuraciones viejas
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
        }
    }

    // ========================================================================
    // AQUÍ ESTÁ LA MAGIA: Procesamiento en vivo
    // ========================================================================

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();

        // 1. FRENO DE MANO: Si pasó menos tiempo del configurado (1.2s), no hacemos nada.
        // Esto evita que la app se vuelva loca enviando peticiones a la API.
        if (currentTime - lastAnalysisTime < ANALYSIS_DELAY) {
            imageProxy.close(); // Importante cerrar para liberar la cámara
            return;
        }
        lastAnalysisTime = currentTime;

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Preparamos la imagen para ML Kit
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        // 2. Buscamos texto en la imagen (OCR)
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // Volvemos al hilo principal para dibujar en pantalla
                    runOnUiThread(() -> {
                        graphicOverlay.clear(); // Borrar dibujos anteriores

                        // Calculamos si la imagen está rotada para dibujar bien los cuadros
                        boolean needRotation = imageProxy.getImageInfo().getRotationDegrees() == 90
                                || imageProxy.getImageInfo().getRotationDegrees() == 270;
                        int width = needRotation ? imageProxy.getHeight() : imageProxy.getWidth();
                        int height = needRotation ? imageProxy.getWidth() : imageProxy.getHeight();
                        graphicOverlay.setImageSourceInfo(width, height, false);
                    });

                    StringBuilder fullText = new StringBuilder();

                    // 3. Recorremos cada bloque de texto encontrado
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String originalText = block.getText();

                        // 4. Mandamos a traducir (OCR_Helper decide si usa API o ML Kit)
                        ocrHelper.translateText(
                                originalText,
                                currentSourceCode,
                                currentTargetCode,
                                getCurrentUser(),
                                new OCR_Helper.TranslationCallback() {
                                    @Override
                                    public void onSuccess(String translated) {
                                        runOnUiThread(() -> {
                                            // Si funciona: Dibujamos el texto traducido
                                            if (graphicOverlay != null) {
                                                graphicOverlay.add(block, translated);
                                                fullText.append(translated).append("\n");
                                                tvTranslatedResult.setText(fullText.toString());
                                                tvTranslatedResult.setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // Si falla: Dibujamos el texto original (para no dejar vacío)
                                        runOnUiThread(() -> {
                                            if (graphicOverlay != null) {
                                                graphicOverlay.add(block, originalText);
                                                fullText.append(originalText).append("\n");
                                                tvTranslatedResult.setText(fullText.toString());
                                                tvTranslatedResult.setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }
                                }
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error OCR: " + e.getMessage()))
                .addOnCompleteListener(task -> {
                    // 5. MUY IMPORTANTE: Cerramos la imagen para que la cámara pueda enviar la siguiente.
                    imageProxy.close();
                });
    }

    // Procesamiento para fotos de la galería o capturas (imagen estática)
    private void runOCRkOnBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    graphicOverlay.clear();
                    graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);
                    StringBuilder fullText = new StringBuilder();

                    if (visionText.getTextBlocks().isEmpty()) {
                        Toast.makeText(Camara.this, "No se encontró texto", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String originalText = block.getText();

                        // Llamada al traductor
                        ocrHelper.translateText(
                                originalText,
                                currentSourceCode,
                                currentTargetCode,
                                getCurrentUser(),
                                new OCR_Helper.TranslationCallback() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        runOnUiThread(() -> {
                                            graphicOverlay.add(block, translatedText);
                                            fullText.append(translatedText).append("\n");

                                            if (fullText.length() > 0) {
                                                tvTranslatedResult.setVisibility(View.VISIBLE);
                                                tvTranslatedResult.setText(fullText.toString().trim());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // Si falla, mostramos original
                                        runOnUiThread(() -> {
                                            graphicOverlay.add(block, originalText);
                                            fullText.append(originalText).append("\n");

                                            if (fullText.length() > 0) {
                                                tvTranslatedResult.setVisibility(View.VISIBLE);
                                                tvTranslatedResult.setText(fullText.toString().trim());
                                            }
                                        });
                                    }
                                }
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error OCR", Toast.LENGTH_SHORT).show()
                );
    }

    // ========================================================================
    // Funciones auxiliares (Permisos, Galería, etc)
    // ========================================================================

    private void capturePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(null), "photo.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        displayStaticImage(Uri.fromFile(photoFile));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Error captura: " + exc.getMessage());
                    }
                });
    }

    private void displayStaticImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            cameraProvider.unbindAll(); // Detenemos la cámara en vivo

            cameraPreview.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageBitmap(bitmap);

            btnCapture.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE); // Mostramos botón para volver
            btnGallery.setVisibility(View.GONE);

            runOCRkOnBitmap(bitmap); // Analizamos la foto
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado — camara activa");
        } else {
            showMessage(getString(R.string.hola_usuario, getCurrentUser()));
        }
    }

    // Vuelve al modo cámara en vivo
    private void resetToCamera() {
        cameraPreview.setVisibility(View.VISIBLE);
        imagePreview.setVisibility(View.GONE);
        tvTranslatedResult.setVisibility(View.GONE);
        graphicOverlay.clear();
        btnCapture.setVisibility(View.VISIBLE);
        btnGallery.setVisibility(View.VISIBLE);
        btnRefresh.setVisibility(View.GONE);
        startCamera();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            displayStaticImage(data.getData());
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_MEDIA_IMAGES },
                    STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
    }
}