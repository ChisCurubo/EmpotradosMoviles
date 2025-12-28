package com.example.snap.ui.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snap.R;
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
    private OnHistoryActionListener listener;
    
    public interface OnHistoryActionListener {
        void onHistoryItemClick(TranslationHistory history);
        void onHistoryItemDelete(TranslationHistory history);
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
     * Establece el listener para acciones en items
     */
    public void setOnHistoryActionListener(OnHistoryActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranslationHistory history = historyList.get(position);
        String date = dateFormat.format(new Date(history.getTimestamp()));
        
        holder.tvDate.setText(date);
        holder.tvSourceText.setText(history.getSourceText());
        holder.tvTranslatedText.setText(history.getTranslatedText());
        holder.tvLanguagePair.setText(history.getSourceLanguage() + " → " + history.getTargetLanguage());
        
        // Configurar click listener en el item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(history);
            }
        });
        
        // Configurar click listener en el botón eliminar
        holder.btnDeleteHistory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemDelete(history);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSourceText, tvTranslatedText, tvLanguagePair;
        ImageButton btnDeleteHistory;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSourceText = itemView.findViewById(R.id.tvSourceText);
            tvTranslatedText = itemView.findViewById(R.id.tvTranslatedText);
            tvLanguagePair = itemView.findViewById(R.id.tvLanguagePair);
            btnDeleteHistory = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}
