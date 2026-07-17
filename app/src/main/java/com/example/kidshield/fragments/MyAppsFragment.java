package com.example.kidshield.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.adapters.ChildAppAdapter;
import com.example.kidshield.models.ScreenTimeLimitResponse;
import com.example.kidshield.models.ScreenUsageResponse;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.SessionManager;

import java.util.List;

public class MyAppsFragment extends Fragment {

    private RecyclerView rvMyApps;
    private ChildAppAdapter adapter;
    private int childId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        childId = SessionManager.getInstance(requireContext()).getChildId();

        rvMyApps = view.findViewById(R.id.rv_my_apps);
        adapter = new ChildAppAdapter();
        rvMyApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyApps.setAdapter(adapter);

        loadApps();
    }

    private void loadApps() {
        if (childId == -1) return;

        // Load explicitly blocked apps
        BackendManager.getBlockedApps(childId, new BackendManager.ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> blocked) {
                if (!isAdded()) return;
                adapter.setBlockedApps(blocked);

                // Load screen usage data
                BackendManager.getScreenUsage(childId, new BackendManager.ApiCallback<List<ScreenUsageResponse>>() {
                    @Override
                    public void onSuccess(List<ScreenUsageResponse> usage) {
                        if (!isAdded()) return;
                        adapter.setApps(usage);
                    }
                    @Override public void onError(String error) {}
                });
            }
            @Override public void onError(String error) {
                // Still load usage even if blocking check fails
                if (!isAdded()) return;
                BackendManager.getScreenUsage(childId, new BackendManager.ApiCallback<List<ScreenUsageResponse>>() {
                    @Override
                    public void onSuccess(List<ScreenUsageResponse> usage) {
                        if (!isAdded()) return;
                        adapter.setApps(usage);
                    }
                    @Override public void onError(String e) {}
                });
            }
        });
    }
}
