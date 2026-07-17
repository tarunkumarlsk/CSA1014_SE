package com.example.kidshield.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kidshield.R;
import com.example.kidshield.models.TimelineEvent;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    private List<TimelineEvent> events = new java.util.ArrayList<>();
    private static final String TAG = "TimelineAdapter";

    public TimelineAdapter() {
    }

    public TimelineAdapter(List<TimelineEvent> events) {
        this.events = events;
    }

    public void setEvents(List<TimelineEvent> events) {
        this.events.clear();
        this.events.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        TimelineEvent event = events.get(position);
        holder.tvEventName.setText(event.getEventName());
        holder.tvLocationName.setText(event.getLocationName());
        holder.tvTime.setText(formatTimestamp(event.getTimestamp()));
        
        // Event type dot color
        if ("EXIT".equalsIgnoreCase(event.getEventType())) {
            holder.dot.setBackgroundResource(R.drawable.rounded_logo_bg);
            holder.dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF9CA3AF)); // Gray for exit
        } else {
            holder.dot.setBackgroundResource(R.drawable.rounded_logo_bg);
            holder.dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981)); // Green for enter
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private String formatTimestamp(String isoTime) {
        try {
            // Simple format for now, e.g. 2026-03-24T20:38:08 -> 8:38 PM
            // In a real app use SimpleDateFormat
            return isoTime.split("T")[1].substring(0, 5); 
        } catch (Exception e) {
            return isoTime;
        }
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvLocationName, tvTime;
        View dot;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tv_event_name);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            dot = itemView.findViewById(R.id.view_dot);
        }
    }
}
