package com.moneylog.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneylog.R;
import com.moneylog.data.db.entity.CategoryEntity;
import com.moneylog.data.db.entity.TransactionEntity;
import com.moneylog.util.DateUtils;
import com.moneylog.util.FormatUtils;
import com.moneylog.util.IconHelper;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView Adapter for transaction list with date-grouped headers.
 * Items: DateHeader | TransactionItem
 */
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    public interface OnTransactionClickListener {
        void onEdit(TransactionEntity tx);
        void onDelete(TransactionEntity tx);
    }

    // ─── sealed item type ────────────────────────────────────

    public static class AdapterItem {
        final int viewType;
        AdapterItem(int viewType) { this.viewType = viewType; }
    }

    public static class HeaderItem extends AdapterItem {
        final String dateDisplay; // e.g. "4월 13일"
        HeaderItem(String dateDisplay) {
            super(TYPE_HEADER);
            this.dateDisplay = dateDisplay;
        }
    }

    public static class TxItem extends AdapterItem {
        final TransactionEntity tx;
        TxItem(TransactionEntity tx) {
            super(TYPE_ITEM);
            this.tx = tx;
        }
    }

    // ─────────────────────────────────────────────────────────

    private List<AdapterItem> items = new ArrayList<>();
    private Map<Long, CategoryEntity> categoryMap;
    private OnTransactionClickListener listener;
    private Context context;

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    /** Build the flat list with date headers from a sorted transaction list. */
    public void submitTransactions(List<TransactionEntity> txList,
                                   Map<Long, CategoryEntity> catMap,
                                   Context context) {
        this.categoryMap = catMap;
        this.context = context;
        List<AdapterItem> newItems = new ArrayList<>();
        String lastDate = null;
        for (TransactionEntity tx : txList) {
            if (!tx.date.equals(lastDate)) {
                newItems.add(new HeaderItem(DateUtils.toDisplayDate(tx.date, context)));
                lastDate = tx.date;
            }
            newItems.add(new TxItem(tx));
        }
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_transaction_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_transaction, parent, false);
            return new TxViewHolder(v, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdapterItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) item);
        } else if (holder instanceof TxViewHolder) {
            CategoryEntity cat = categoryMap != null
                ? categoryMap.get(((TxItem) item).tx.categoryId)
                : null;
            ((TxViewHolder) holder).bind(((TxItem) item).tx, cat);
        }
    }

    // ── ViewHolders ──────────────────────────────────────────

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;
        HeaderViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvMonthLabel);
        }
        void bind(HeaderItem item) {
            tvDate.setText(item.dateDisplay);
        }
    }

    static class TxViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvCategory;
        private final TextView tvMemo;
        private final TextView tvAmount;
        private final OnTransactionClickListener listener;
        private TransactionEntity boundTx;

        TxViewHolder(View v, OnTransactionClickListener listener) {
            super(v);
            this.listener = listener;
            ivIcon     = v.findViewById(R.id.ivCategoryIcon);
            tvCategory = v.findViewById(R.id.tvTransactionName);
            tvMemo     = v.findViewById(R.id.tvTransactionSubtitle);
            tvAmount   = v.findViewById(R.id.tvTransactionAmount);

            v.setOnClickListener(view -> {
                if (boundTx != null) listener.onEdit(boundTx);
            });
            v.setOnLongClickListener(view -> {
                if (boundTx != null) listener.onDelete(boundTx);
                return true;
            });
        }

        void bind(TransactionEntity tx, CategoryEntity cat) {
            this.boundTx = tx;
            tvCategory.setText(cat != null ? cat.name :
                    itemView.getContext().getString(R.string.category_fallback));
            int iconRes = IconHelper.getDrawableResId(cat != null ? cat.iconName : "category");
            ivIcon.setImageResource(iconRes);
            if (tx.memo != null && !tx.memo.isEmpty()) {
                tvMemo.setVisibility(View.VISIBLE);
                tvMemo.setText(tx.memo);
            } else {
                tvMemo.setVisibility(View.GONE);
            }
            boolean isIncome = "INCOME".equals(tx.type);
            String prefix = isIncome ? "+" : "-";
            tvAmount.setText(prefix + FormatUtils.formatAmountWithUnit(tx.amount, itemView.getContext()));
            tvAmount.setTextColor(itemView.getContext().getColor(
                isIncome ? R.color.income_color : R.color.expense_color
            ));
        }
    }
}
