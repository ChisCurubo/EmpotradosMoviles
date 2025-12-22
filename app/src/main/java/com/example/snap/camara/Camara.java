package com.example.snap.camara;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.snap.components.LanguageSelector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.example.snap.api.TranslateApiClient;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Camara extends AppCompatActivity {

    private static final String TAG = "CamaraActivity";

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    // Views
    private LanguageSelector languageSelector;


    private PreviewView cameraPreview;
    private ImageView imagePreview;
    private TextView tvTranslatedResult;

    private FloatingActionButton btnCapture;
    private FloatingActionButton btnGallery;
    private FloatingActionButton btnRefresh;

    private View btnTextMode;
    private View btnCameraMode;
    private View btnAudioMode;

    //CameraX y Logica
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // Controlar el ciclo de vida (Bandera global)
    private boolean isCameraActive = false;

    //Variables de optimización OCR
    private boolean isProcessing = false;
    private long lastAnalysisTime = 0;
    private static final long ANALYSIS_INTERVAL_MS = 700; // 0.7s (ideal para balancear respuesta/batería)

    private TextRecognizer textRecognizer;

    private String[] languages = {
            "Español", "Inglés", "Francés", "Alemán",
            "Italiano", "Portugués", "Chino", "Japonés"
    };

    private void showBigErrorMessage(String message) {
        runOnUiThread(() -> {
            tvTranslatedResult.setVisibility(View.VISIBLE);
            tvTranslatedResult.setText(message);
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        initializeViews();
        setupButtons();

        //Usamos un thread separado para análisis para no bloquear la UI
        cameraExecutor = Executors.newSingleThreadExecutor();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //Verificar permisos al iniciar, pero el arranque real lo maneja onResume
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void initializeViews() {
        languageSelector = findViewById(R.id.languageSelector);


        cameraPreview = findViewById(R.id.cameraPreview);
        imagePreview = findViewById(R.id.imagePreview);
        tvTranslatedResult = findViewById(R.id.tvTranslatedResult);

        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        btnRefresh = findViewById(R.id.btnRefresh);

        btnTextMode = findViewById(R.id.btnTextMode);
        btnCameraMode = findViewById(R.id.btnCameraMode);
        btnAudioMode = findViewById(R.id.btnAudioMode);
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
        btnCameraMode.setOnClickListener(v -> resetToCamera());

        btnTextMode.setOnClickListener(v -> Toast.makeText(this, "Modo Texto", Toast.LENGTH_SHORT).show());
        btnAudioMode.setOnClickListener(v -> Toast.makeText(this, "Modo Audio", Toast.LENGTH_SHORT).show());
    }

    //CICLO DE VIDA OPTIMIZADO

    @Override
    protected void onResume() {
        super.onResume();
        // Arrancar cámara SOLO si tenemos permiso y corresponde
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Solo iniciamos si no estamos mostrando una imagen estática (preview invisible)
            if (imagePreview.getVisibility() != View.VISIBLE) {
                startCamera();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausa real: liberar recursos de cámara inmediatamente
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Limpieza final
        stopCamera();

        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }

    /**
     * Método para detener y liberar la cámara completamente
     */
    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        isCameraActive = false;
        isProcessing = false;
    }

    //StartCamera

    private void startCamera() {
        // Evita reinicios innecesarios si ya está activa
        if (isCameraActive) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isCameraActive = true; //Marcamos como activa

                // Actualizar UI
                btnCapture.setVisibility(View.VISIBLE);
                btnGallery.setVisibility(View.VISIBLE);
                btnRefresh.setVisibility(View.GONE);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        imageCapture = new ImageCapture.Builder().build();

        //Configuración optimizada de análisis de imagen
        imageAnalysis = new ImageAnalysis.Builder()
                // Resolución más baja = menos CPU = más batería
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        //Analyzer
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            long currentTime = System.currentTimeMillis();

            //Logica de descarte de frames:
            //Si la cámara no está activa, si ya estamos procesando, o si no ha pasado el tiempo
            if (!isCameraActive ||
                    isProcessing ||
                    (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS)) {

                imageProxy.close(); //IMPORTANTE: Cerrar proxy inmediatamente
                return;
            }

            lastAnalysisTime = currentTime;
            isProcessing = true;
            processImageProxy(imageProxy);
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            isProcessing = false;
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    runOnUiThread(() -> {
                        StringBuilder fullText = new StringBuilder();
                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            fullText.append(block.getText()).append("\n");
                        }

                        //Actualizar UI
                        if (fullText.length() > 0) {
                            tvTranslatedResult.setVisibility(View.VISIBLE);
                            tvTranslatedResult.setText(fullText.toString().trim());
                        } else {
                            tvTranslatedResult.setVisibility(View.GONE);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error OCR", e))
                .addOnCompleteListener(task -> {
                    //SIEMPRE cerrar el proxy y liberar la bandera al terminar
                    imageProxy.close();
                    isProcessing = false;
                });
    }

    // CAPTURA ESTATICA

    private void capturePhoto() {
        if (imageCapture == null) return;

        btnCapture.setEnabled(false);

        File photoFile = new File(getExternalFilesDir(null), "photo.jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        btnCapture.setEnabled(true);
                        Uri savedUri = Uri.fromFile(photoFile);
                        displayStaticImage(savedUri);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        btnCapture.setEnabled(true);
                        Log.e(TAG, "Error captura: " + exc.getMessage());
                    }
                });
    }

    private void displayStaticImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            //Pausar OCR cuando hay imagen estática
            stopCamera(); // Esto llama a unbindAll() y pone isCameraActive = false

            cameraPreview.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageBitmap(bitmap);

            btnCapture.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            btnGallery.setVisibility(View.GONE);

            // Limpiar texto anterior mientras procesa nuevo
            tvTranslatedResult.setText("");

            runOCRkOnBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void runOCRkOnBitmap(Bitmap bitmap) {

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {

                    StringBuilder fullText = new StringBuilder();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        fullText.append(block.getText()).append("\n");
                    }

                    //No se detectó texto
                    if (fullText.length() == 0) {
                        runOnUiThread(() -> {
                            showBigErrorMessage("No se pudo leer bien la imagen");
                            Toast.makeText(
                                    Camara.this,
                                    "No se pudo leer bien la imagen",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                        return;
                    }

                    String originalText = fullText.toString().trim();

                    String sourceLang = languageSelector.getSourceLangCode();
                    String targetLang = languageSelector.getTargetLangCode();

                    // LLAMADA A TU API
                    TranslateApiClient.getInstance().translate(
                            originalText,
                            sourceLang,
                            targetLang,
                            new TranslateApiClient.Callback() {

                                @Override
                                public void onSuccess(String translatedText) {
                                    runOnUiThread(() -> {
                                        tvTranslatedResult.setVisibility(View.VISIBLE);
                                        tvTranslatedResult.setText(translatedText);
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        showBigErrorMessage("No se pudo leer bien la imagen");
                                        Toast.makeText(
                                                Camara.this,
                                                "Error en la traducción",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    });
                                }
                            }
                    );
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        showBigErrorMessage("No se pudo leer bien la imagen");
                        Toast.makeText(
                                Camara.this,
                                "Error al leer la imagen",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                });
    }


    private void resetToCamera() {
        imagePreview.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.VISIBLE);
        tvTranslatedResult.setVisibility(View.GONE);

        startCamera();
    }

    //PERMISOS Y UTILIDADES

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                displayStaticImage(selectedImage);
            }
        }
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
    }
}