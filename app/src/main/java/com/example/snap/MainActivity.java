package com.example.snap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

        startActivity(new Intent(this, TextActivity.class));
        finish();

        
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
    private void showWelcomeMessage() {
        if (!isUserLoggedIn()) {
            showMessage("Modo Invitado: Inicia sesión para guardar");
        } else {
            showMessage(getString(R.string.sesion_activa, getCurrentUser()));
        }
    }
}
