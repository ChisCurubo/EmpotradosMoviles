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

import com.example.snap.R;
import com.example.snap.SnapLinguaApplication;
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

public class Camara extends BaseActivity {

    private static final String TAG = "CamaraActivity";

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    private static final long ANALYSIS_DELAY = 800L;

    // --- Views ---

    private PreviewView cameraPreview;
    private ImageView imagePreview;
    private GraphicOverlay graphicOverlay;
    private TextView tvTranslatedResult;

    private FloatingActionButton btnCapture;
    private FloatingActionButton btnGallery;
    private FloatingActionButton btnRefresh;

    private LanguageSelector languageSelector;

    // Componente reutilizable
    private BottomNavigationComponent bottomNavigation;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    private boolean isProcessing = false;
    private long lastAnalysisTime = 0;

    private TextRecognizer textRecognizer;

    // Selector
    private String currentSourceCode = "es";
    private String currentTargetCode = "en";
    private int currentSourceIndex = 0;
    private int currentTargetIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        initializeViews();
        setupLanguageSelector();
        setupButtons();
        setupNavigation(); // Configuración limpia

        cameraExecutor = Executors.newSingleThreadExecutor();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

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

    // --- MÁGICA Y LIMPIA ---
    private void setupNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.attachToScreen("camara");
        }
    }

    private void setupLanguageSelector() {
        // Cargar preferencias por defecto
        String currentUser = getCurrentUser();
        if (currentUser == null)
            currentUser = "guest";

        android.content.SharedPreferences prefs = getSharedPreferences(
                com.example.snap.SettingsActivity.PREFS_NAME + "_" + currentUser, MODE_PRIVATE);

        String defaultSource = prefs.getString(com.example.snap.SettingsActivity.KEY_DEFAULT_SOURCE_LANG, "es");
        String defaultTarget = prefs.getString(com.example.snap.SettingsActivity.KEY_DEFAULT_TARGET_LANG, "en");

        // Establecer idiomas por defecto
        languageSelector.setLanguages(defaultSource, defaultTarget);

        languageSelector.setOnLanguageChangeListener((srcCode, tgtCode, srcIndex, tgtIndex) -> {
            currentSourceCode = srcCode;
            currentTargetCode = tgtCode;
            currentSourceIndex = srcIndex;
            currentTargetIndex = tgtIndex;

            Log.d("LANG_SELECTOR", "Nuevo idioma: " + srcCode + " -> " + tgtCode);
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
        if (graphicOverlay != null)
            graphicOverlay.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null)
            cameraExecutor.shutdown();
        if (textRecognizer != null)
            textRecognizer.close();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                btnCapture.setVisibility(View.VISIBLE);
                btnGallery.setVisibility(View.VISIBLE);
                btnRefresh.setVisibility(View.GONE);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null)
            return;
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        imageCapture = new ImageCapture.Builder().build();
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (!isProcessing)
                processImageProxy(imageProxy);
            else
                imageProxy.close();
        });
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < ANALYSIS_DELAY) {
            imageProxy.close();
            return;
        }
        lastAnalysisTime = currentTime;

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    runOnUiThread(() -> {
                        graphicOverlay.clear();
                        boolean needRotation = imageProxy.getImageInfo().getRotationDegrees() == 90
                                || imageProxy.getImageInfo().getRotationDegrees() == 270;
                        int width = needRotation ? imageProxy.getHeight() : imageProxy.getWidth();
                        int height = needRotation ? imageProxy.getWidth() : imageProxy.getHeight();
                        graphicOverlay.setImageSourceInfo(width, height, false);
                        StringBuilder fullText = new StringBuilder();
                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            String translated = block.getText();
                            graphicOverlay.add(block, translated);
                            fullText.append(translated).append("\n");
                        }
                        if (fullText.length() > 0) {
                            tvTranslatedResult.setVisibility(View.VISIBLE);
                            tvTranslatedResult.setText(fullText.toString().trim());
                        } else {
                            tvTranslatedResult.setVisibility(View.GONE);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error OCR: " + e.getMessage()))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void capturePhoto() {
        if (imageCapture == null)
            return;
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
            cameraProvider.unbindAll();
            cameraPreview.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageBitmap(bitmap);
            btnCapture.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            btnGallery.setVisibility(View.GONE);
            runOCRkOnBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runOCRkOnBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    graphicOverlay.clear();
                    graphicOverlay.setImageSourceInfo(bitmap.getWidth(), bitmap.getHeight(), false);
                    StringBuilder fullText = new StringBuilder();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String txt = block.getText();
                        graphicOverlay.add(block, txt);
                        fullText.append(txt).append("\n");
                    }
                    if (fullText.length() > 0) {
                        tvTranslatedResult.setVisibility(View.VISIBLE);
                        tvTranslatedResult.setText(fullText.toString());
                    } else {
                        Toast.makeText(SnapLinguaApplication.getLanguageContext(this), R.string.no_texto_encontrado, Toast.LENGTH_SHORT).show();
                        tvTranslatedResult.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SnapLinguaApplication.getLanguageContext(this), R.string.error_ocr, Toast.LENGTH_SHORT).show());
    }

    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado — camara activa");
        } else {
            showMessage(getString(R.string.hola_usuario, getCurrentUser()));
        }
    }

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
