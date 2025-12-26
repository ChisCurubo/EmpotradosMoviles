package com.example.snap.ui.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snap.data.entities.TranslationHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter reutilizable para mostrar el historial de traducciones.
 * Puede ser usado en cualquier Activity que necesite mostrar historial.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    
    private List<TranslationHistory> historyList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    private OnHistoryItemClickListener listener;
    
    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(TranslationHistory history);
    }
    
    public HistoryAdapter(List<TranslationHistory> historyList) {
        this.historyList = historyList != null ? historyList : new ArrayList<>();
    }
    
    /**
     * Actualiza los datos del adapter
     */
    public void updateData(List<TranslationHistory> newList) {
        this.historyList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * Establece el listener para clicks en items
     */
    public void setOnHistoryItemClickListener(OnHistoryItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setPadding(16, 16, 16, 16);
        textView.setTextSize(14);
        textView.setBackgroundColor(0xFFFFFFFF);
        
        // Agregar margen inferior
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        textView.setLayoutParams(params);
        
        return new ViewHolder(textView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranslationHistory history = historyList.get(position);
        String date = dateFormat.format(new Date(history.getTimestamp()));
        String text = date + "\n" +
                     history.getSourceText() + " → " + history.getTranslatedText() +
                     "\n(" + history.getSourceLanguage() + " → " + history.getTargetLanguage() + ")";
        holder.textView.setText(text);
        
        // Configurar click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(history);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        
        ViewHolder(TextView textView) {
            super(textView);
            this.textView = textView;
        }
    }
}
