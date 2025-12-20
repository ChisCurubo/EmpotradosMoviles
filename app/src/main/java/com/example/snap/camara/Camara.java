package com.example.snap.camara;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.snap.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Camara extends AppCompatActivity {

    private static final String TAG = "CamaraActivity";

    // Códigos de solicitud
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    // Views
    private Spinner spinnerSourceLanguage;
    private Spinner spinnerTargetLanguage;
    private ImageButton btnSwapLanguages;
    private ImageButton btnSettings;
    private PreviewView cameraPreview;
    private ImageView imagePreview;
    private ImageView iconGallery;
    private FloatingActionButton btnCapture;
    private FloatingActionButton btnGallery;
    private ImageButton btnRefresh;
    private View btnTextMode;
    private View btnCameraMode;
    private View btnAudioMode;

    // CameraX
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private boolean isCameraActive = false;


    // OCR
    private OCR_Helper ocrHelper;
    private ProgressDialog progressDialog;

    // Idiomas disponibles
    private String[] languages = {
            "Español", "Italiano", "Inglés", "Francés",
            "Alemán", "Portugués", "Chino", "Japonés", "Árabe", "Ruso"
    };
    private int sourceLanguagePosition = 0;
    private int targetLanguagePosition = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        // Inicializar componentes
        initializeViews();
        setupLanguageSpinners();
        setupButtons();

        // Inicializar executor para CameraX
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Inicializar OCR Helper
        ocrHelper = new OCR_Helper();

        // Verificar y solicitar permisos al iniciar
        checkAndRequestPermissions();
    }

    /**
     * Verifica y solicita todos los permisos necesarios
     */
    private void checkAndRequestPermissions() {
        if (checkCameraPermission()) {
            // Si tenemos permiso de cámara, iniciarla automáticamente
            startCamera();
        } else {
            // Solicitar permiso de cámara
            requestCameraPermission();
        }
    }

    /**
     * Inicializa todas las vistas
     */
    private void initializeViews() {
        // Spinners de idiomas
        spinnerSourceLanguage = findViewById(R.id.spinnerSourceLanguage);
        spinnerTargetLanguage = findViewById(R.id.spinnerTargetLanguage);

        // Botones
        btnSwapLanguages = findViewById(R.id.btnSwapLanguages);
        btnSettings = findViewById(R.id.btnSettings);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Vistas de imagen y cámara
        cameraPreview = findViewById(R.id.cameraPreview);
        imagePreview = findViewById(R.id.imagePreview);
        iconGallery = findViewById(R.id.iconGallery);

        // Botones inferiores
        btnTextMode = findViewById(R.id.btnTextMode);
        btnCameraMode = findViewById(R.id.btnCameraMode);
        btnAudioMode = findViewById(R.id.btnAudioMode);

        // Hacer visible el botón de galería desde el inicio
        btnGallery.setVisibility(View.VISIBLE);

        // Configurar color del ícono de galería para mejor contraste
        btnGallery.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF5548E5));
    }

    /**
     * Configura los spinners de selección de idioma
     */
    private void setupLanguageSpinners() {
        // Crear adaptador para los spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Configurar spinner de idioma origen
        spinnerSourceLanguage.setAdapter(adapter);
        spinnerSourceLanguage.setSelection(sourceLanguagePosition);
        spinnerSourceLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sourceLanguagePosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configurar spinner de idioma destino
        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setSelection(targetLanguagePosition);
        spinnerTargetLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetLanguagePosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Configura todos los botones de la interfaz
     */
    private void setupButtons() {
        // Botón de intercambio de idiomas
        btnSwapLanguages.setOnClickListener(v -> swapLanguages());

        // Botón de configuración
        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        );

        // Botón de modo cámara
        btnCameraMode.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                startCamera();
            } else {
                requestCameraPermission();
            }
        });

        // Botón de modo texto
        btnTextMode.setOnClickListener(v ->
                Toast.makeText(this, "Modo texto próximamente", Toast.LENGTH_SHORT).show()
        );

        // Botón de modo audio
        btnAudioMode.setOnClickListener(v ->
                Toast.makeText(this, "Modo audio próximamente", Toast.LENGTH_SHORT).show()
        );

        // Botón de galería (FAB)
        btnGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        // Botón de captura de foto
        btnCapture.setOnClickListener(v -> capturePhoto());

        // Botón de refrescar/reintentar
        btnRefresh.setOnClickListener(v -> resetToCamera());
    }

    /**
     * Intercambia los idiomas seleccionados
     */
    private void swapLanguages() {
        int temp = sourceLanguagePosition;
        sourceLanguagePosition = targetLanguagePosition;
        targetLanguagePosition = temp;

        spinnerSourceLanguage.setSelection(sourceLanguagePosition);
        spinnerTargetLanguage.setSelection(targetLanguagePosition);

        Toast.makeText(this, "Idiomas intercambiados", Toast.LENGTH_SHORT).show();
    }

    /**
     * Verifica si se tiene permiso de cámara
     */
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Solicita permiso de cámara
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE
        );
    }

    /**
     * Verifica si se tiene permiso de almacenamiento
     */
    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Solicita permiso de almacenamiento
     */
    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    STORAGE_PERMISSION_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    /**
     * Inicia la cámara usando CameraX
     */
    private void startCamera() {
        Log.d(TAG, "Iniciando cámara...");

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al obtener ProcessCameraProvider", e);
                Toast.makeText(this,
                        "Error al iniciar la cámara: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Vincula la vista previa de la cámara
     */
    private void bindCameraPreview(ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "Vinculando cámara...");

        // Configurar vista previa
        Preview preview = new Preview.Builder().build();

        // Seleccionar cámara trasera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Configurar captura de imagen
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Conectar la vista previa
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        try {
            // Desvincular casos de uso anteriores
            cameraProvider.unbindAll();

            // Vincular casos de uso a la cámara
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

            // Mostrar vista de cámara
            isCameraActive = true;
            cameraPreview.setVisibility(View.VISIBLE);
            imagePreview.setVisibility(View.GONE);
            iconGallery.setVisibility(View.GONE);
            btnCapture.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);
            btnGallery.setVisibility(View.VISIBLE); // ✅ Mantener visible

            Log.d(TAG, "Cámara vinculada correctamente");
            Toast.makeText(this, "Cámara lista", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error al vincular cámara", e);
            Toast.makeText(this,
                    "Error al vincular cámara: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Captura una foto con la cámara
     */
    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Cámara no está lista", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear archivo para guardar la foto
        File photoFile = new File(
                getExternalFilesDir(null),
                "IMG_" + System.currentTimeMillis() + ".jpg"
        );

        // Configurar opciones de salida
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Capturar la imagen
        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        runOnUiThread(() -> {
                            Uri savedUri = Uri.fromFile(photoFile);
                            showCapturedImage(savedUri);
                            Toast.makeText(Camara.this,
                                    "Foto capturada correctamente",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        runOnUiThread(() ->
                                Toast.makeText(Camara.this,
                                        "Error al capturar: " + exception.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                }
        );
    }

    /**
     * Abre la galería para seleccionar una imagen
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    /**
     * Muestra la imagen capturada o seleccionada
     */
    private void showCapturedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    getContentResolver(),
                    imageUri
            );

            imagePreview.setImageBitmap(bitmap);

            // Ocultar cámara y mostrar imagen
            isCameraActive = false;
            cameraPreview.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            iconGallery.setVisibility(View.GONE);
            btnCapture.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            btnGallery.setVisibility(View.VISIBLE);

            Toast.makeText(this,
                    "Imagen cargada. ¡Lista para traducir!",
                    Toast.LENGTH_SHORT).show();
            //Procesar texto de la imagen
            processImageWithOCR(bitmap);
        } catch (IOException e) {
            Toast.makeText(this,
                    "Error al cargar imagen: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Procesa la imagen con OCR y extrae el texto
     */
    private void processImageWithOCR(Bitmap bitmap) {
        // Mostrar diálogo de progreso
        showProgressDialog("Extrayendo texto...");

        // Procesar con OCR
        ocrHelper.extractTextFromBitmap(bitmap, new OCR_Helper.OCRCallback() {
            @Override
            public void onSuccess(String extractedText) {
                runOnUiThread(() -> {
                    hideProgressDialog();

                    // Mostrar el texto extraído
                    showExtractedTextDialog(extractedText);

                    Log.d(TAG, "Texto extraído: " + extractedText);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(Camara.this,
                            "Error al extraer texto: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    /**
     * Muestra el texto extraído en un diálogo
     */
    private void showExtractedTextDialog(String text) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📄 Texto Extraído")
                .setMessage(text)
                .setPositiveButton("Traducir", (dialog, which) -> {
                    // TODO: Aquí irá la funcionalidad de traducción
                    Toast.makeText(this,
                            "Traducción próximamente",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Copiar", (dialog, which) -> {
                    // Copiar al portapapeles
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip =
                            android.content.ClipData.newPlainText("Texto extraído", text);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Texto copiado", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Cerrar", null)
                .show();
    }


    /**
     * Muestra el diálogo de progreso
     */
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    /**
     *  Oculta el diálogo de progreso
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Reinicia la vista de cámara
     */
    private void resetToCamera() {
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Maneja el resultado de seleccionar imagen de galería
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            if (data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                showCapturedImage(selectedImageUri);
            }
        }
    }

    /**
     * Maneja el resultado de solicitudes de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de cámara concedido");
                startCamera();
            } else {
                Log.w(TAG, "Permiso de cámara denegado");
                Toast.makeText(this,
                        "Permiso de cámara denegado. La app necesita acceso a la cámara.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de almacenamiento concedido");
                openGallery();
            } else {
                Log.w(TAG, "Permiso de almacenamiento denegado");
                Toast.makeText(this,
                        "Permiso de almacenamiento denegado",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Limpia recursos al destruir la actividad
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (ocrHelper != null) {
            ocrHelper.close();
        }
        hideProgressDialog();
    }

    /**
     * Pausa la cámara al pausar la actividad
     */
    @Override
    protected void onPause() {
        super.onPause();
        // No ocultar la cámara aquí para que se mantenga visible
    }

    /**
     * Reanuda la cámara al reanudar la actividad
     */
    @Override
    protected void onResume() {
        super.onResume();
        // La cámara se mantendrá visible si ya estaba activa
    }
}