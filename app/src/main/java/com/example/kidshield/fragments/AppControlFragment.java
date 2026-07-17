package com.example.kidshield.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.adapters.AppControlAdapter;
import com.example.kidshield.models.ChildProfile;
import com.example.kidshield.models.ScreenUsageResponse;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class AppControlFragment extends Fragment {

    private RecyclerView rvApps;
    private AppControlAdapter adapter;
    private Spinner spinnerChild;
    private List<ChildProfile> children = new ArrayList<>();
    private int parentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentId = SessionManager.getInstance(requireContext()).getParentId();

        rvApps = view.findViewById(R.id.rv_app_controls);
        adapter = new AppControlAdapter();
        rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvApps.setAdapter(adapter);

        spinnerChild = view.findViewById(R.id.spinner_child_select);
        loadChildren();
    }

    private void loadChildren() {
        if (parentId == -1) return;
        BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<ChildProfile>>() {
            @Override
            public void onSuccess(List<ChildProfile> result) {
                if (!isAdded()) return;
                children = result;
                List<String> names = new ArrayList<>();
                for (ChildProfile c : result) names.add(c.getName());
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, names);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerChild.setAdapter(spinnerAdapter);
                spinnerChild.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                        loadApps(children.get(pos).getId());
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
                if (!result.isEmpty()) loadApps(result.get(0).getId());
            }
            @Override public void onError(String error) {}
        });
    }

    private void loadApps(int childId) {
        adapter.setChildId(childId);

        // Load usage list
        BackendManager.getScreenUsage(childId, new BackendManager.ApiCallback<List<ScreenUsageResponse>>() {
            @Override
            public void onSuccess(List<ScreenUsageResponse> result) {
                if (!isAdded()) return;
                adapter.setApps(result);
            }
            @Override public void onError(String error) {}
        });

        // Load blocked apps and sync toggle states
        BackendManager.getBlockedApps(childId, new BackendManager.ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> blocked) {
                if (!isAdded()) return;
                adapter.setBlockedApps(blocked);
            }
            @Override public void onError(String error) {}
        });
    }
}
