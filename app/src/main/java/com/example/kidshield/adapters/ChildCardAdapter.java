package com.example.kidshield.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.models.ChildProfile;

import java.util.ArrayList;
import java.util.List;

public class ChildCardAdapter extends RecyclerView.Adapter<ChildCardAdapter.ViewHolder> {

    private List<ChildProfile> children = new ArrayList<>();
    private OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClicked(ChildProfile child);
    }

    public void setChildren(List<ChildProfile> children) {
        this.children.clear();
        this.children.addAll(children);
        notifyDataSetChanged();
    }

    public void setOnChildClickListener(OnChildClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChildProfile child = children.get(position);
        holder.tvName.setText(child.getName() != null ? child.getName() : "Unknown");
        String device = child.getDeviceName() != null ? child.getDeviceName()
                : (child.getDevice() != null ? child.getDevice() : "Unknown Device");
        holder.tvDevice.setText(device);

        // Online dot: green if online, grey if not
        int dotColor = child.isOnline()
                ? Color.parseColor("#10B981") : Color.parseColor("#D1D5DB");
        holder.onlineDot.getBackground().mutate();
        holder.onlineDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(dotColor));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChildClicked(child);
        });
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDevice;
        View onlineDot;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_child_name);
            tvDevice = itemView.findViewById(R.id.tv_child_device);
            onlineDot = itemView.findViewById(R.id.view_online_dot);
        }
    }
}
