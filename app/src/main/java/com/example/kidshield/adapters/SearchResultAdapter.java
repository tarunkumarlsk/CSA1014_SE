package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.NominatimResult;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<NominatimResult> results = new ArrayList<>();
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onClick(NominatimResult result);
    }

    public void setResults(List<NominatimResult> results) {
        this.results.clear();
        this.results.addAll(results);
        notifyDataSetChanged();
    }

    public void setOnResultClickListener(OnResultClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NominatimResult r = results.get(position);
        holder.tvName.setText(r.displayName);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });
    }

    @Override
    public int getItemCount() { return results.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_result_name);
        }
    }
}
