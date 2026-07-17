package com.example.kidshield.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.R;
import com.example.kidshield.TimeLimitsActivity;
import com.example.kidshield.adapters.ChildCardAdapter;
import com.example.kidshield.models.ChildProfile;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.SessionManager;

import java.util.List;

public class DashboardFragment extends Fragment {

    private RecyclerView rvChildren;
    private ChildCardAdapter childAdapter;
    private TextView tvGreeting;
    private int parentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        parentId = SessionManager.getInstance(requireContext()).getParentId();
        tvGreeting = view.findViewById(R.id.tv_greeting);

        rvChildren = view.findViewById(R.id.rv_children);
        childAdapter = new ChildCardAdapter();
        rvChildren.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChildren.setAdapter(childAdapter);
        
        childAdapter.setOnChildClickListener(child -> {
            Intent intent = new Intent(requireContext(), com.example.kidshield.ParentChildProfileActivity.class);
            intent.putExtra("child_id", child.getId());
            intent.putExtra("child_name", child.getName());
            intent.putExtra("device_name", child.getDeviceName() != null ? child.getDeviceName() : child.getDevice());
            startActivity(intent);
        });

        // "Add child" navigates to AddNewDeviceActivity
        view.findViewById(R.id.btn_add_child).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.example.kidshield.AddNewDeviceActivity.class)));

        // Quick action cards
        view.findViewById(R.id.card_location).setOnClickListener(v ->
                switchFragment(new MapFragment()));
        view.findViewById(R.id.card_time_limit).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TimeLimitsActivity.class)));
        view.findViewById(R.id.card_safe_zones).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.example.kidshield.SafeZonesActivity.class)));
        view.findViewById(R.id.card_app_controls).setOnClickListener(v ->
                switchFragment(new AppControlFragment()));

        loadChildren();
    }

    private void switchFragment(Fragment f) {
        com.google.android.material.bottomnavigation.BottomNavigationView navView = 
                requireActivity().findViewById(R.id.parent_bottom_nav);
        
        if (navView != null) {
            if (f instanceof MapFragment) {
                navView.setSelectedItemId(R.id.nav_map);
            } else if (f instanceof AppControlFragment) {
                navView.setSelectedItemId(R.id.nav_controls);
            } else if (f instanceof AlertsFragment) {
                navView.setSelectedItemId(R.id.nav_alerts);
            } else if (f instanceof DashboardFragment) {
                navView.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    private void loadChildren() {
        if (parentId == -1) return;
        BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<ChildProfile>>() {
            @Override
            public void onSuccess(List<ChildProfile> result) {
                if (!isAdded()) return;
                String name = SessionManager.getInstance(requireContext()).getName();
                if (name == null || name.isEmpty()) name = "Parent";
                tvGreeting.setText("Hello, " + name + " 👋");
                childAdapter.setChildren(result);
            }
            @Override
            public void onError(String error) {
                if (isAdded()) tvGreeting.setText("Hello 👋");
            }
        });
    }
}
