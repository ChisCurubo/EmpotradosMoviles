package com.example.snap.ui.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snap.R;
import com.example.snap.data.entities.Favorite;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter reutilizable para mostrar favoritos con opción de eliminar.
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
    
    private List<Favorite> favoritesList;
    private OnFavoriteActionListener listener;
    
    public interface OnFavoriteActionListener {
        void onFavoriteClick(Favorite favorite);
        void onFavoriteDelete(Favorite favorite);
    }
    
    public FavoritesAdapter(List<Favorite> favoritesList) {
        this.favoritesList = favoritesList != null ? favoritesList : new ArrayList<>();
    }
    
    /**
     * Actualiza los datos del adapter
     */
    public void updateData(List<Favorite> newList) {
        this.favoritesList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * Establece el listener para acciones en items
     */
    public void setOnFavoriteActionListener(OnFavoriteActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Favorite favorite = favoritesList.get(position);
        
        holder.tvOriginal.setText(favorite.getOriginalText());
        holder.tvTranslated.setText(favorite.getTranslatedText());
        holder.tvLanguages.setText(favorite.getSourceLang() + " → " + favorite.getTargetLang());
        
        // Click en el item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(favorite);
            }
        });
        
        // Click en el botón eliminar
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteDelete(favorite);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return favoritesList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOriginal, tvTranslated, tvLanguages;
        ImageButton btnDelete;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginal = itemView.findViewById(R.id.tvOriginal);
            tvTranslated = itemView.findViewById(R.id.tvTranslated);
            tvLanguages = itemView.findViewById(R.id.tvLanguages);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
