package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.ScreenUsageResponse;
import com.example.kidshield.network.BackendManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppControlAdapter extends RecyclerView.Adapter<AppControlAdapter.ViewHolder> {

    private List<ScreenUsageResponse> apps = new ArrayList<>();
    private Set<String> blockedApps = new HashSet<>();
    private int childId = -1;

    public void setApps(List<ScreenUsageResponse> newApps) {
        this.apps.clear();
        this.apps.addAll(newApps);
        notifyDataSetChanged();
    }

    public void setBlockedApps(List<String> blocked) {
        this.blockedApps.clear();
        this.blockedApps.addAll(blocked);
        notifyDataSetChanged();
    }

    public void setChildId(int childId) {
        this.childId = childId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_control, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScreenUsageResponse app = apps.get(position);
        String appName = app.getAppName();
        holder.tvAppName.setText(appName);

        int m = app.getUsageTimeMinutes();
        holder.tvAppUsage.setText(m >= 60 ? (m / 60) + "h " + (m % 60) + "m today" : m + "m today");

        // UI state purely reflects the hashset
        boolean isBlocked = blockedApps.contains(appName);
        holder.switchApp.setOnCheckedChangeListener(null); 
        holder.switchApp.setChecked(!isBlocked);
        
        if (holder.tvStatusLabel != null) {
            holder.tvStatusLabel.setText(isBlocked ? "Blocked" : "Allowed");
            holder.tvStatusLabel.setTextColor(isBlocked ? android.graphics.Color.parseColor("#EF4444") : android.graphics.Color.parseColor("#10B981"));
        }
        holder.switchApp.setEnabled(true);

            holder.switchApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (childId == -1) return;
                holder.switchApp.setEnabled(false);
                int currentPos = holder.getAdapterPosition();

                if (!isChecked) {
                    BackendManager.blockApp(childId, appName, new BackendManager.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            blockedApps.add(appName);
                            notifyItemChanged(currentPos);
                            Toast.makeText(holder.itemView.getContext(),
                                    "\"" + appName + "\" is blocked", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onError(String error) {
                            notifyItemChanged(currentPos);
                            Toast.makeText(holder.itemView.getContext(), "Backend Block Failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    BackendManager.unblockApp(childId, appName, new BackendManager.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            blockedApps.remove(appName);
                            notifyItemChanged(currentPos);
                            Toast.makeText(holder.itemView.getContext(),
                                    "\"" + appName + "\" is unblocked", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onError(String error) {
                            notifyItemChanged(currentPos);
                            Toast.makeText(holder.itemView.getContext(), "Backend Unblock Failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

            // Make the whole card clickable
            holder.itemView.setOnClickListener(v -> holder.switchApp.setChecked(!holder.switchApp.isChecked()));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName;
        TextView tvAppUsage;
        TextView tvStatusLabel;
        SwitchMaterial switchApp;

        ViewHolder(View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvAppUsage = itemView.findViewById(R.id.tv_app_usage);
            tvStatusLabel = itemView.findViewById(R.id.tv_status_label);
            switchApp = itemView.findViewById(R.id.switch_app);
        }
    }
}
