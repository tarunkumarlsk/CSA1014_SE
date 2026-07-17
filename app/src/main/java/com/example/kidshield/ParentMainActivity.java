package com.example.kidshield;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.kidshield.fragments.DashboardFragment;
import com.example.kidshield.fragments.MapFragment;
import com.example.kidshield.fragments.AppControlFragment;
import com.example.kidshield.fragments.AlertsFragment;
import com.example.kidshield.fragments.ParentProfileFragment;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ParentMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);
        EdgeToEdgeUtils.applyTopPadding(findViewById(R.id.parent_fragment_container));
        BottomNavigationView navView = findViewById(R.id.parent_bottom_nav);

        // Start Background Alert Monitoring
        try {
            android.content.Intent serviceIntent = new android.content.Intent(this, com.example.kidshield.services.ParentAlertService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            android.util.Log.e("ParentMain", "Could not start ParentAlertService: " + e.getMessage());
        }

        // Request Permissions for Notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_controls) {
                selectedFragment = new AppControlFragment();
            } else if (itemId == R.id.nav_alerts) {
                selectedFragment = new AlertsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new ParentProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.parent_fragment_container, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            navView.setSelectedItemId(R.id.nav_home);
        }
    }
}
