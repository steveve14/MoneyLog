package com.moneylog.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.moneylog.R;
import com.moneylog.util.IconHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IconPickerAdapter extends RecyclerView.Adapter<IconPickerAdapter.ViewHolder> {

    private static final int COLUMNS = 5;
    private static final int COLLAPSED_ROWS = 3;
    private static final int COLLAPSED_COUNT = COLUMNS * COLLAPSED_ROWS; // 15

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName);
    }

    private final List<Map.Entry<String, Integer>> icons;
    private final OnIconSelectedListener listener;
    private String selectedIconName;
    private boolean isExpanded = false;

    public IconPickerAdapter(OnIconSelectedListener listener) {
        this.icons = new ArrayList<>(IconHelper.getAllIcons().entrySet());
        this.listener = listener;
    }

    public void expand() {
        if (!isExpanded) {
            int prevCount = getItemCount();
            isExpanded = true;
            notifyItemRangeInserted(prevCount, icons.size() - prevCount);
        }
    }

    public boolean isAllVisible() {
        return icons.size() <= COLLAPSED_COUNT;
    }

    public void setSelectedIcon(String iconName) {
        String old = this.selectedIconName;
        this.selectedIconName = iconName;
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            String name = icons.get(i).getKey();
            if (name.equals(old) || name.equals(iconName)) {
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_icon_picker, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Integer> entry = icons.get(position);
        holder.bind(entry.getKey(), entry.getValue());
    }

    @Override
    public int getItemCount() {
        return isExpanded ? icons.size() : Math.min(COLLAPSED_COUNT, icons.size());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout container;
        private final ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }

        void bind(String iconName, int drawableRes) {
            ivIcon.setImageResource(drawableRes);

            boolean selected = iconName.equals(selectedIconName);
            if (selected) {
                container.setBackgroundTintList(
                    ContextCompat.getColorStateList(itemView.getContext(),
                        R.color.md_theme_primary_container));
                ivIcon.setColorFilter(
                    ContextCompat.getColor(itemView.getContext(),
                        R.color.md_theme_on_primary_container));
            } else {
                container.setBackgroundTintList(
                    ContextCompat.getColorStateList(itemView.getContext(),
                        R.color.md_theme_surface_container));
                ivIcon.setColorFilter(
                    ContextCompat.getColor(itemView.getContext(),
                        R.color.md_theme_on_surface_variant));
            }

            itemView.setOnClickListener(v -> {
                setSelectedIcon(iconName);
                listener.onIconSelected(iconName);
            });
        }
    }
}
