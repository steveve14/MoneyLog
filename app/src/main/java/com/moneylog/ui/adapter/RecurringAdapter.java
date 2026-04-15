package com.moneylog.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.moneylog.R;
import com.moneylog.data.db.entity.RecurringEntity;
import com.moneylog.util.FormatUtils;

import java.util.Map;

public class RecurringAdapter extends ListAdapter<RecurringEntity, RecurringAdapter.ViewHolder> {

    public interface OnRecurringActionListener {
        void onToggleActive(long id, boolean active);
        void onLongPress(RecurringEntity item);
        String formatSchedule(RecurringEntity item);
    }

    private final OnRecurringActionListener listener;
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
                        && a.intervalType.equals(b.intervalType)
                        && a.dayOfMonth == b.dayOfMonth
                        && (a.memo == null ? b.memo == null : a.memo.equals(b.memo));
            }
        };

    public RecurringAdapter(OnRecurringActionListener listener) {
        super(DIFF);
        this.listener = listener;
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

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategory;
        private final TextView tvMemo;
        private final TextView tvSchedule;
        private final TextView tvAmount;
        private final MaterialSwitch switchActive;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvRecCategory);
            tvMemo = itemView.findViewById(R.id.tvRecMemo);
            tvSchedule = itemView.findViewById(R.id.tvRecSchedule);
            tvAmount = itemView.findViewById(R.id.tvRecAmount);
            switchActive = itemView.findViewById(R.id.switchRecActive);
        }

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

            itemView.setOnLongClickListener(v -> {
                listener.onLongPress(rec);
                return true;
            });
        }
    }
}
