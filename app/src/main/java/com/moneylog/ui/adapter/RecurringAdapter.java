package com.moneylog.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.moneylog.R;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.util.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecurringAdapter extends ListAdapter<RecurringEntity, RecurringAdapter.ViewHolder> {

    public interface OnRecurringActionListener {
        void onToggleActive(long id, boolean active);
        void onEdit(RecurringEntity item);
        String formatSchedule(RecurringEntity item);
    }

    public interface OnDragStartListener {
        void onDragStart(RecyclerView.ViewHolder holder);
    }

    private final OnRecurringActionListener listener;
    private OnDragStartListener dragStartListener;
    private Map<Long, String> categoryNameMap;
    private String fallbackName = "";

    private static final DiffUtil.ItemCallback<RecurringEntity> DIFF =
        new DiffUtil.ItemCallback<RecurringEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull RecurringEntity a, @NonNull RecurringEntity b) {
                return a.id == b.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull RecurringEntity a, @NonNull RecurringEntity b) {
                return a.amount == b.amount
                        && a.categoryId == b.categoryId
                        && a.isActive == b.isActive
                        && a.sortOrder == b.sortOrder
                        && a.intervalType.equals(b.intervalType)
                        && a.dayOfMonth == b.dayOfMonth
                        && (a.memo == null ? b.memo == null : a.memo.equals(b.memo));
            }
        };

    public RecurringAdapter(OnRecurringActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setDragStartListener(OnDragStartListener dragStartListener) {
        this.dragStartListener = dragStartListener;
    }

    public void setCategoryNameMap(Map<Long, String> map, String fallback) {
        this.categoryNameMap = map;
        this.fallbackName = fallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recurring, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public boolean moveItem(int from, int to) {
        List<RecurringEntity> list = new ArrayList<>(getCurrentList());
        Collections.swap(list, from, to);
        submitList(list);
        return true;
    }

    public List<RecurringEntity> getReorderedList() {
        return new ArrayList<>(getCurrentList());
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivDragHandle;
        private final TextView tvCategory;
        private final TextView tvMemo;
        private final TextView tvSchedule;
        private final TextView tvAmount;
        private final MaterialSwitch switchActive;
        private final ImageButton btnEdit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.ivDragHandle);
            tvCategory = itemView.findViewById(R.id.tvRecCategory);
            tvMemo = itemView.findViewById(R.id.tvRecMemo);
            tvSchedule = itemView.findViewById(R.id.tvRecSchedule);
            tvAmount = itemView.findViewById(R.id.tvRecAmount);
            switchActive = itemView.findViewById(R.id.switchRecActive);
            btnEdit = itemView.findViewById(R.id.btnRecEdit);
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(RecurringEntity rec) {
            String catName = categoryNameMap != null ? categoryNameMap.get(rec.categoryId) : null;
            tvCategory.setText(catName != null ? catName : fallbackName);

            if (rec.memo != null && !rec.memo.isEmpty()) {
                tvMemo.setText(rec.memo);
                tvMemo.setVisibility(View.VISIBLE);
            } else {
                tvMemo.setVisibility(View.GONE);
            }

            tvSchedule.setText(listener.formatSchedule(rec));

            boolean isIncome = "INCOME".equals(rec.type);
            String prefix = isIncome ? "+" : "-";
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(rec.amount, itemView.getContext()));
            tvAmount.setTextColor(itemView.getContext().getColor(
                    isIncome ? R.color.income_color : R.color.expense_color));

            float alpha = rec.isActive ? 1.0f : 0.4f;
            tvCategory.setAlpha(alpha);
            tvMemo.setAlpha(alpha);
            tvSchedule.setAlpha(alpha);
            tvAmount.setAlpha(alpha);

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(rec.isActive);
            switchActive.setOnCheckedChangeListener((btn, checked) ->
                    listener.onToggleActive(rec.id, checked));

            btnEdit.setOnClickListener(v -> listener.onEdit(rec));
            itemView.setOnClickListener(v -> listener.onEdit(rec));

            ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragStartListener != null) {
                    dragStartListener.onDragStart(ViewHolder.this);
                }
                return false;
            });
        }
    }
}
