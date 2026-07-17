package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.SafeZone;

import java.util.ArrayList;
import java.util.List;

public class SafeZoneAdapter extends RecyclerView.Adapter<SafeZoneAdapter.ViewHolder> {

    private List<SafeZone> zones = new ArrayList<>();
    private OnZoneDeleteListener listener;

    public interface OnZoneDeleteListener {
        void onDelete(SafeZone zone, int position);
    }

    public void setZones(List<SafeZone> zones) {
        this.zones.clear();
        this.zones.addAll(zones);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        zones.remove(position);
        notifyItemRemoved(position);
    }

    public void setOnDeleteListener(OnZoneDeleteListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safe_zone, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SafeZone zone = zones.get(position);
        holder.tvName.setText(zone.getName());
        int r = (int) zone.getRadius();
        holder.tvRadius.setText("Radius: " + r + " m");
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(zone, position);
        });
    }

    @Override
    public int getItemCount() { return zones.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRadius;
        ImageButton btnDelete;
        ViewHolder(View v) {
            super(v);
            tvName   = v.findViewById(R.id.tv_zone_name);
            tvRadius = v.findViewById(R.id.tv_zone_radius);
            btnDelete = v.findViewById(R.id.btn_delete_zone);
        }
    }
}
