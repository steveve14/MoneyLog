package com.moneylog.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneylog.R;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.databinding.ItemCategoryBinding;
import com.moneylog.util.IconHelper;

import java.util.List;

public class CategoryAdapter extends ListAdapter<CategoryEntity, CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onEdit(CategoryEntity category);
    }

    private final OnCategoryClickListener listener;

    private static final DiffUtil.ItemCallback<CategoryEntity> DIFF =
        new DiffUtil.ItemCallback<CategoryEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull CategoryEntity a, @NonNull CategoryEntity b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull CategoryEntity a, @NonNull CategoryEntity b) {
                return a.name.equals(b.name) && a.iconName.equals(b.iconName) && a.type.equals(b.type);
            }
        };

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        ViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategoryEntity category) {
            binding.tvCategoryName.setText(category.name);
            binding.ivCategoryIcon.setImageResource(IconHelper.getDrawableResId(category.iconName));
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(category));
            binding.getRoot().setOnClickListener(v -> listener.onEdit(category));
        }
    }
}
