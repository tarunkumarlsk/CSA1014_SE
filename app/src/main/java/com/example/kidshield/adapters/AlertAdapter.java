package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.AlertResponse;
import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<AlertResponse> alerts = new ArrayList<>();

    public void setAlerts(List<AlertResponse> alerts) {
        this.alerts = alerts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertResponse alert = alerts.get(position);
        holder.tvMessage.setText(alert.getMessage());
        holder.tvType.setText(alert.getAlertType());
        holder.tvTime.setText(alert.getCreatedAt()); // Ideally format this

        // Color coding based on type
        int color = 0xFF3B82F6; // Blue (Default/Geofence)
        if ("sos".equalsIgnoreCase(alert.getAlertType())) {
            color = 0xFFEF4444; // Red
        } else if ("screen_time".equalsIgnoreCase(alert.getAlertType())) {
            color = 0xFFF59E0B; // Orange
        }

        holder.indicator.setBackgroundColor(color);
        holder.tvType.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvMessage, tvTime;
        View indicator;
        ImageView ivIcon;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.alert_type);
            tvMessage = itemView.findViewById(R.id.alert_message);
            tvTime = itemView.findViewById(R.id.alert_time);
            indicator = itemView.findViewById(R.id.indicator);
            ivIcon = itemView.findViewById(R.id.alert_icon);
        }
    }
}
