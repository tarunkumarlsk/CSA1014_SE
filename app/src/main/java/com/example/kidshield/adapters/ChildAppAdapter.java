package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.ScreenUsageResponse;

import java.util.ArrayList;
import java.util.List;

public class ChildAppAdapter extends RecyclerView.Adapter<ChildAppAdapter.ViewHolder> {

    private List<ScreenUsageResponse> apps = new ArrayList<>();
    private java.util.Map<String, Boolean> blockedApps = new java.util.HashMap<>();

    public void setBlockedApps(List<String> blocked) {
        blockedApps.clear();
        if (blocked != null) {
            for (String app : blocked) {
                blockedApps.put(app, true);
            }
        }
        notifyDataSetChanged();
    }

    public void setApps(List<ScreenUsageResponse> newApps) {
        this.apps.clear();
        this.apps.addAll(newApps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScreenUsageResponse app = apps.get(position);
        holder.tvAppName.setText(app.getAppName());
        
        // Dynamic determination if blocked or not. For now we assume allowed.
        // Dynamic determination if blocked or not.
        boolean isAllowed = !Boolean.TRUE.equals(blockedApps.get(app.getAppName()));
        if (isAllowed) {
            holder.tvAppStatus.setText("Allowed");
            holder.tvAppStatus.setTextColor(android.graphics.Color.parseColor("#10B981")); // Green
            holder.tvAppStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#ECFDF5")));
        } else {
            holder.tvAppStatus.setText("Blocked");
            holder.tvAppStatus.setTextColor(android.graphics.Color.parseColor("#EF4444")); // Red for blocked
            holder.tvAppStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF2F2")));
        }
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName;
        TextView tvAppStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvAppStatus = itemView.findViewById(R.id.tv_app_status);
        }
    }
}
