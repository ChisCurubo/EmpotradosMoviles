package com.example.snap;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private Spinner spinnerInput, spinnerOutput;
    private EditText etInput;
    private TextView tvOutput;
    private ImageView btnClear, btnSwap, btnVolume, btnStar;
    private View btnPaste, btnCopy;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        spinnerInput = findViewById(R.id.spinnerInput);
        spinnerOutput = findViewById(R.id.spinnerOutput);
        etInput = findViewById(R.id.etInput);
        tvOutput = findViewById(R.id.tvOutput);
        btnClear = findViewById(R.id.btnClear);
        btnSwap = findViewById(R.id.btnSwap);
        btnVolume = findViewById(R.id.btnVolume);
        btnStar = findViewById(R.id.btnStar);
        btnPaste = findViewById(R.id.btnPaste);
        btnCopy = findViewById(R.id.btnCopy);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Set default selection for spinners
        spinnerInput.setSelection(0); // Español
        spinnerOutput.setSelection(1); // Inglés

        // Clear button logic
        btnClear.setOnClickListener(v -> {
            etInput.setText("");
            tvOutput.setText("");
        });

        // Swap languages logic
        btnSwap.setOnClickListener(v -> {
            int inputPos = spinnerInput.getSelectedItemPosition();
            int outputPos = spinnerOutput.getSelectedItemPosition();
            spinnerInput.setSelection(outputPos);
            spinnerOutput.setSelection(inputPos);
            
            // Swap text as well
            String inputText = etInput.getText().toString();
            String outputText = tvOutput.getText().toString();
            etInput.setText(outputText);
            tvOutput.setText(inputText);
        });

        // Mock translation logic
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // This is a placeholder for real translation logic
                    tvOutput.setText(s.toString());
                } else {
                    tvOutput.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Paste logic
        btnPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                etInput.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
            }
        });

        // Copy logic
        btnCopy.setOnClickListener(v -> {
            String text = tvOutput.getText().toString();
            if (!text.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Texto copiado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_texto) {
                return true;
            } else if (itemId == R.id.nav_camara) {
                Toast.makeText(this, "Cámara seleccionada", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_audio) {
                Toast.makeText(this, "Audio seleccionado", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}