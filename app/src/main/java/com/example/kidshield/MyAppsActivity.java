package com.example.kidshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;

import com.example.kidshield.models.ScreenUsageResponse;
import com.example.kidshield.network.BackendManager;

import java.util.List;

public class MyAppsActivity extends AppCompatActivity {

    private int childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_apps);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        childId = com.example.kidshield.utils.SessionManager.getInstance(this).getChildId();

        fetchApps();

        // Bottom Navigation
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildDashboardActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        View navApps = findViewById(R.id.nav_apps);
        if (navApps != null) navApps.setOnClickListener(v -> { /* Already here */ });

        View navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildProfileActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private com.example.kidshield.adapters.ChildAppAdapter adapter;

    private void fetchApps() {
        if (childId == -1) return;
        
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rv_child_apps);
        if (adapter == null && rv != null) {
            rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
            adapter = new com.example.kidshield.adapters.ChildAppAdapter();
            rv.setAdapter(adapter);
        }

        BackendManager.getBlockedApps(childId, new BackendManager.ApiCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> blocked) {
                if (adapter != null) adapter.setBlockedApps(blocked);
                
                // Fetch usage after limits
                BackendManager.getScreenUsage(childId, new BackendManager.ApiCallback<List<ScreenUsageResponse>>() {
                    @Override
                    public void onSuccess(List<ScreenUsageResponse> result) {
                        if (result != null && !result.isEmpty()) {
                            if (adapter != null) {
                                adapter.setApps(result);
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.d("MyApps", "No usage data: " + error);
                    }
                });
            }
            @Override
            public void onError(String error) {
                android.util.Log.e("MyApps", "Failed to get blocked apps: " + error);
            }
        });
    }
}
