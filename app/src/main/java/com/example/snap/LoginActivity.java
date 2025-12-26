package com.example.snap;

import android.os.Bundle;
import android.widget.Button;

import com.example.snap.data.entities.User;
import com.example.snap.data.repository.UserRepository;
import com.example.snap.ui.base.BaseActivity;
import com.example.snap.ui.components.BottomNavigationComponent;
import com.example.snap.utils.ValidationHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Actividad de login refactorizada usando componentes reutilizables.
 * Utiliza ValidationHelper para validaciones y SessionManager para gestión de sesión.
 */
public class LoginActivity extends BaseActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private UserRepository userRepository;
    private ExecutorService executorService;
    private BottomNavigationComponent bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar componentes
        initializeViews();
        initializeComponents();
        
        // Configurar listeners
        setupListeners();
    }

    /**
     * Inicializa las vistas de la actividad
     */
    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
    }

    /**
     * Inicializa los componentes necesarios
     */
    private void initializeComponents() {
        userRepository = new UserRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

        // Configurar navegación - permitir salir sin iniciar sesión
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
                    showMessage("Ya estás en la pantalla de inicio de sesión");
                }
            });
            bottomNavigation.setActiveScreen("usuario");
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    /**
     * Intenta hacer login con las credenciales ingresadas
     */
    private void attemptLogin() {
        String email = getEmailText();
        String password = getPasswordText();

        // Validar campos usando ValidationHelper
        ValidationHelper.ValidationResult validation = 
            ValidationHelper.validateLoginFields(email, password);
        
        if (!validation.isValid()) {
            showMessage(validation.getMessage());
            return;
        }

        setLoginButtonLoading(true);

        // Verificar credenciales en segundo plano
        executorService.execute(() -> {
            User user = userRepository.login(email, password);
            
            runOnUiThread(() -> {
                setLoginButtonLoading(false);

                if (user != null) {
                    handleSuccessfulLogin(email);
                } else {
                    showLongMessage("Credenciales incorrectas. ¿Deseas registrarte?");
                }
            });
        });
    }

    /**
     * Intenta registrar un nuevo usuario
     */
    private void attemptRegister() {
        String email = getEmailText();
        String password = getPasswordText();

        // Validar campos
        ValidationHelper.ValidationResult validation = 
            ValidationHelper.validateLoginFields(email, password);
        
        if (!validation.isValid()) {
            showMessage(validation.getMessage());
            return;
        }

        setRegisterButtonLoading(true);

        // Verificar si el usuario ya existe y registrar si no existe
        executorService.execute(() -> {
            try {
                // Verificar en la BD de forma síncrona
                User existingUser = userRepository.getUserByEmailSync(email);
                
                if (existingUser != null) {
                    // El usuario ya existe
                    runOnUiThread(() -> {
                        setRegisterButtonLoading(false);
                        showLongMessage("Este correo ya está registrado. Usa 'Iniciar Sesión'");
                    });
                } else {
                    // El usuario no existe, registrarlo
                    User newUser = new User(email, email.split("@")[0], password);
                    userRepository.registerSync(newUser);
                    
                    runOnUiThread(() -> {
                        handleSuccessfulLogin(email);
                        showMessage("Cuenta creada exitosamente");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setRegisterButtonLoading(false);
                    showLongMessage("Error al registrar: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Maneja el login exitoso
     */
    private void handleSuccessfulLogin(String email) {
        sessionManager.saveSession(email);
        showMessage("Bienvenido, " + email);
        navigationManager.navigateToMain(true);
    }

    /**
     * Obtiene el texto del campo email
     */
    private String getEmailText() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    /**
     * Obtiene el texto del campo password
     */
    private String getPasswordText() {
        return etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
    }

    /**
     * Establece el estado de carga del botón de login
     */
    private void setLoginButtonLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Verificando..." : "Iniciar Sesión");
    }

    /**
     * Establece el estado de carga del botón de registro
     */
    private void setRegisterButtonLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Registrando..." : "Registrarse");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
