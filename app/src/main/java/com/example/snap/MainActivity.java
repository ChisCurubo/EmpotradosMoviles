package com.example.snap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    static final String KEY_CONTADOR = "CONTADOR";
    private View main_view;
    private TextView textoContador;
    private Button botonSumar;
    private Button botonResultados;

    private Integer contador;

    private void initContador() {
        contador = (Integer) SingletonMap.getInstance().get(MainActivity.KEY_CONTADOR);
        if (contador == null) {
            contador = 0;
            SingletonMap.getInstance().put(MainActivity.KEY_CONTADOR, contador);
        }
    }

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

        textoContador = findViewById(R.id.txtContador);
        botonSumar = findViewById(R.id.btnSumar);
        botonResultados = findViewById(R.id.btnResultados);
        main_view = findViewById(R.id.main);

        initContador();

        botonSumar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contador++;
                SingletonMap.getInstance().put(MainActivity.KEY_CONTADOR, contador);

                textoContador.setText(String.valueOf(contador));

                if (contador > 10) {
                    Snackbar.make(main_view, "El contador es mayor que 10, quieres deshacer el cambio?", Snackbar.LENGTH_LONG)
                            .setAction("DESHACER", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    contador = 10;
                                    SingletonMap.getInstance().put(MainActivity.KEY_CONTADOR, contador);
                                    textoContador.setText(String.valueOf(contador));
                                    Toast.makeText(MainActivity.this, "Accion deshecha", Toast.LENGTH_LONG).show();
                                }
                            })
                            .setActionTextColor(Color.RED).show();

                }
            }
        });

        botonResultados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ResultadosActivity.class);

                // intent.putExtra("CONTADOR", contador);

                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initContador();
        textoContador.setText(String.valueOf(contador));
    }
}