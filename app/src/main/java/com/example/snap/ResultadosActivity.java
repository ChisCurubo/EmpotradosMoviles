package com.example.snap;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultadosActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_resultados);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView textoResultados = findViewById(R.id.txtResultados);

        initContador();

        String textoFinal = getString(R.string.TextResult) + String.valueOf(contador);
        textoResultados.setText(textoFinal);

        if (contador > 10) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ResultadosActivity.this);
            builder.setTitle("Contador alto");
            builder.setMessage("El contador es muy alto, quieres resetearlo a 0?");
            builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    contador = 0;
                    SingletonMap.getInstance().put(MainActivity.KEY_CONTADOR, contador);
                    String textoFinal = getString(R.string.TextResult) + String.valueOf(contador);
                    textoResultados.setText(textoFinal);
                }
            });
            builder.setNegativeButton("No", null);
            builder.setNeutralButton("Mas tarde", null);

            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}