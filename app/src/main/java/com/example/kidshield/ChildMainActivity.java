package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.kidshield.fragments.MyAppsFragment;
import com.example.kidshield.fragments.ChildProfileFragment;
import com.example.kidshield.fragments.ChildHomeFragment;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ChildMainActivity extends AppCompatActivity {
    
    // We don't have direct access to childId here easily, but we can get it from SessionManager
    private int childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        childId = com.example.kidshield.utils.SessionManager.getInstance(this).getChildId();

        // Request standard runtime permissions first
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
        }
        setContentView(R.layout.activity_child_main);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        BottomNavigationView navView = findViewById(R.id.child_bottom_nav);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home_child) {
                selectedFragment = new ChildHomeFragment(); 
            } else if (itemId == R.id.nav_apps_child) {
                selectedFragment = new MyAppsFragment();
            } else if (itemId == R.id.nav_profile_child) {
                selectedFragment = new ChildProfileFragment(); 
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.child_fragment_container, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            navView.setSelectedItemId(R.id.nav_home_child);
        }

        findViewById(R.id.btn_sos).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SOSActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (childId != -1) {
            checkAndEnforcePermissions();
        }
    }

    private void checkAndEnforcePermissions() {
        if (!hasUsageStatsPermission(this)) {
            Toast.makeText(this, "Usage Access is strictly required to protect this device.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }

        if (!android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please allow 'Display over other apps' to enable App Blocking.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        // Both critical permissions are granted — safe to start all protection services!
        startBackgroundServices();
    }

    private boolean hasUsageStatsPermission(android.content.Context context) {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(android.content.Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, 
            android.os.Process.myUid(), context.getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    private void startBackgroundServices() {
        try {
            startService(new Intent(this, com.example.kidshield.services.LocationService.class));
        } catch (Exception e) {}
        
        try {
            startService(new Intent(this, com.example.kidshield.services.ScreenTimeService.class));
        } catch (Exception e) {}

        try {
            startService(new Intent(this, com.example.kidshield.services.AppBlockerService.class));
        } catch (Exception e) {}
    }
}
