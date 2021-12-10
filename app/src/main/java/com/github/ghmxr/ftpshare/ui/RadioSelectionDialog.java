package com.github.ghmxr.ftpshare.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ghmxr.ftpshare.R;

public class RadioSelectionDialog<V> extends Dialog {

    private final String[] selections;
    private final V[] values;
    private final ConfirmedCallback callback;
    private final String title;
    private V selected;

    public RadioSelectionDialog(@NonNull Context context, @Nullable String title, @NonNull String[] selections, V[] values, V selected
            , @NonNull ConfirmedCallback<V> callback) {
        super(context);
        this.selections = selections;
        this.values = values;
        this.selected = selected;
        this.callback = callback;
        this.title = title;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_recyclerview);
        TextView tv_title = findViewById(R.id.dialog_title);
        if (!TextUtils.isEmpty(title)) {
            tv_title.setText(title);
        } else {
            tv_title.setVisibility(View.GONE);
        }
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            //layoutParams.gravity= Gravity.BOTTOM;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            //window.setWindowAnimations(R.style.DialogAnimStyle);
        }

    }

    @Override
    public void show() {
        super.show();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new ListAdapter());
    }

    public interface ConfirmedCallback<V> {
        void onConfirmed(String selection, V value);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_button);
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder> {
        private ListAdapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_radio_button, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
            viewHolder.radioButton.setText(selections[viewHolder.getAdapterPosition()]);
            viewHolder.radioButton.setChecked(values[viewHolder.getAdapterPosition()].equals(selected));
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null)
                        callback.onConfirmed(selections[viewHolder.getAdapterPosition()], values[viewHolder.getAdapterPosition()]);
                    cancel();
                }
            });
        }

        @Override
        public int getItemCount() {
            return selections.length;
        }
    }
}
