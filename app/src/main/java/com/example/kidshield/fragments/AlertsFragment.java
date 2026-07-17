package com.example.kidshield.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.adapters.AlertAdapter;
import com.example.kidshield.models.AlertResponse;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.SessionManager;

import java.util.List;

public class AlertsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private AlertAdapter adapter;
    private int parentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentId = SessionManager.getInstance(requireContext()).getParentId();

        rvAlerts = view.findViewById(R.id.rv_alerts);
        adapter = new AlertAdapter();
        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlerts.setAdapter(adapter);

        view.findViewById(R.id.btn_mark_all_read).setOnClickListener(v -> markAllRead());

        loadAlerts();
    }

    private void loadAlerts() {
        if (parentId == -1) return;
        BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<com.example.kidshield.models.ChildProfile>>() {
            @Override
            public void onSuccess(List<com.example.kidshield.models.ChildProfile> result) {
                if (!isAdded() || result.isEmpty()) return;
                
                final java.util.List<AlertResponse> allAlerts = new java.util.ArrayList<>();
                final java.util.concurrent.atomic.AtomicInteger childrenFetched = new java.util.concurrent.atomic.AtomicInteger(0);
                
                for (com.example.kidshield.models.ChildProfile child : result) {
                    BackendManager.getAlerts(child.getId(), new BackendManager.ApiCallback<List<AlertResponse>>() {
                        @Override
                        public void onSuccess(List<AlertResponse> alerts) {
                            allAlerts.addAll(alerts);
                            checkCompletion();
                        }
                        
                        @Override 
                        public void onError(String error) {
                            checkCompletion();
                        }
                        
                        private void checkCompletion() {
                            if (childrenFetched.incrementAndGet() == result.size()) {
                                if (isAdded()) {
                                    // Sort by creation time (most recent first)
                                    java.util.Collections.sort(allAlerts, (a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()));
                                    adapter.setAlerts(allAlerts);
                                }
                            }
                        }
                    });
                }
            }
            @Override public void onError(String error) {}
        });
    }

    private void markAllRead() {
        if (parentId == -1) return;
        BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<com.example.kidshield.models.ChildProfile>>() {
            @Override
            public void onSuccess(List<com.example.kidshield.models.ChildProfile> result) {
                if (!isAdded() || result.isEmpty()) return;
                
                final java.util.concurrent.atomic.AtomicInteger childrenMarked = new java.util.concurrent.atomic.AtomicInteger(0);
                for (com.example.kidshield.models.ChildProfile child : result) {
                    BackendManager.markAllAlertsAsRead(child.getId(), new BackendManager.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String msg) {
                            checkCompletion();
                        }
                        @Override public void onError(String error) {
                            checkCompletion();
                        }
                        
                        private void checkCompletion() {
                            if (childrenMarked.incrementAndGet() == result.size()) {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "All alerts marked read", Toast.LENGTH_SHORT).show();
                                    loadAlerts();
                                }
                            }
                        }
                    });
                }
            }
            @Override public void onError(String error) {}
        });
    }
}
