package com.example.snap.camara;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<TextBlockGraphic> graphics = new ArrayList<>();

    // Configuración de la cámara/imagen para escalar coordenadas
    private int imageWidth;
    private int imageHeight;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Limpia todos los dibujos del overlay
     */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /**
     * Agrega un bloque de texto para dibujar
     */
    public void add(Text.TextBlock textBlock, String translatedText) {
        synchronized (lock) {
            graphics.add(new TextBlockGraphic(textBlock, translatedText));
        }
        postInvalidate();
    }

    /**
     * Establece las dimensiones de la imagen fuente para calcular la escala
     */
    public void setImageSourceInfo(int width, int height, boolean isFlipped) {
        synchronized (lock) {
            this.imageWidth = width;
            this.imageHeight = height;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            // Calcular factor de escala basado en el tamaño actual de la vista
            if (imageWidth != 0 && imageHeight != 0) {
                widthScaleFactor = (float) getWidth() / imageWidth;
                heightScaleFactor = (float) getHeight() / imageHeight;
            }

            /*for (TextBlockGraphic graphic : graphics) {
                graphic.draw(canvas);
            }*/
        }
    }

    /**
     * Clase interna para definir cómo dibujar cada bloque de texto
     */
    private class TextBlockGraphic {
        private final Text.TextBlock textBlock;
        private final String translatedText;
        private final Paint rectPaint;
        private final Paint textPaint;
        private final Paint textBackgroundPaint;

        TextBlockGraphic(Text.TextBlock textBlock, String translatedText) {
            this.textBlock = textBlock;
            this.translatedText = translatedText;

            //estilo del borde del recuadro
            rectPaint = new Paint();
            rectPaint.setColor(Color.CYAN);
            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setStrokeWidth(4.0f);

            //estilo del texto
            textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(40.0f);
            textPaint.setFakeBoldText(true);

            // Estilo del fondo del texto (para que se lea bien)
            textBackgroundPaint = new Paint();
            textBackgroundPaint.setColor(Color.WHITE);
            textBackgroundPaint.setStyle(Paint.Style.FILL);
            textBackgroundPaint.setAlpha(200);
        }

        void draw(Canvas canvas) {
            if (textBlock.getBoundingBox() == null) return;

            // Obtener rectángulo original y escalarlo
            Rect rect = textBlock.getBoundingBox();
            RectF scaledRect = new RectF(
                    rect.left * widthScaleFactor,
                    rect.top * heightScaleFactor,
                    rect.right * widthScaleFactor,
                    rect.bottom * heightScaleFactor
            );

            //dibujar el recuadro alrededor del texto detectado
            canvas.drawRect(scaledRect, rectPaint);

            //dibujar fondo para el texto traducido
            float x = scaledRect.left;
            float y = scaledRect.bottom; // Dibujar debajo del recuadro

            //si el texto a mostrar es el traducido o el original
            String textToShow = translatedText != null ? translatedText : textBlock.getText();

            //ajustar fondo del texto
            Rect textBounds = new Rect();
            textPaint.getTextBounds(textToShow, 0, textToShow.length(), textBounds);
            // Dar un poco de margen al fondo
            RectF backgroundRect = new RectF(
                    x,
                    y,
                    x + textBounds.width() + 20,
                    y + textBounds.height() + 20
            );

            canvas.drawRect(backgroundRect, textBackgroundPaint);

            //dibujar el texto
            canvas.drawText(textToShow, x + 10, y + textBounds.height() + 5, textPaint);
        }
    }
}