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
 * Actividad de login refactorizada.
 * Navegación delegada al componente BottomNavigationComponent.
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

        initializeViews();
        initializeComponents();
        setupListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void initializeComponents() {
        userRepository = new UserRepository(getApplication());
        executorService = Executors.newSingleThreadExecutor();

        // Configurar navegación (SÚPER LIMPIO)
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setActiveScreen("usuario");
            
            // Opcional: Si quieres cerrar LoginActivity al salir, el componente ya tiene 
            // un finishCurrentActivity() interno, así que no necesitas overridear nada 
            // a menos que quieras comportamiento extra.
            // Para Login, confiamos en la lógica por defecto del componente.
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptLogin() {
        String email = getEmailText();
        String password = getPasswordText();

        ValidationHelper.ValidationResult validation = 
            ValidationHelper.validateLoginFields(email, password);
        
        if (!validation.isValid()) {
            showMessage(validation.getMessage());
            return;
        }

        setLoginButtonLoading(true);

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

    private void attemptRegister() {
        String email = getEmailText();
        String password = getPasswordText();

        ValidationHelper.ValidationResult validation = 
            ValidationHelper.validateLoginFields(email, password);
        
        if (!validation.isValid()) {
            showMessage(validation.getMessage());
            return;
        }

        setRegisterButtonLoading(true);

        executorService.execute(() -> {
            try {
                User existingUser = userRepository.getUserByEmailSync(email);
                if (existingUser != null) {
                    runOnUiThread(() -> {
                        setRegisterButtonLoading(false);
                        showLongMessage("Este correo ya está registrado. Usa 'Iniciar Sesión'");
                    });
                } else {
                    User newUser = new User(email, email.split("@")[0], password);
                    userRepository.registerSync(newUser);
                    runOnUiThread(() -> {
                        handleSuccessfulLogin(email);
                        showMessage(getString(R.string.cuenta_creada));
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

    private void handleSuccessfulLogin(String email) {
        sessionManager.saveSession(email);
        showMessage(getString(R.string.bienvenido, email));
        navigationManager.navigateToMain(true);
    }

    private String getEmailText() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    private String getPasswordText() {
        return etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
    }

    private void setLoginButtonLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Verificando..." : "Iniciar Sesión");
    }

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