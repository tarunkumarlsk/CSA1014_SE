package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ChildProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_profile);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        // Bind dynamic data
        com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
        String childName = session.getName();
        
        android.widget.TextView tvName = findViewById(R.id.profile_name);
        android.widget.TextView tvInitials = findViewById(R.id.profile_initials);
        
        if (tvName != null) tvName.setText(childName);
        if (tvInitials != null) {
            String[] parts = childName.split(" ");
            String initials = "";
            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].substring(0, 1).toUpperCase();
            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].substring(0, 1).toUpperCase();
            if (initials.isEmpty()) initials = "CH";
            tvInitials.setText(initials);
        }

        // Bind device name
        String deviceName = session.getDeviceName();
        android.widget.TextView tvDeviceName = findViewById(R.id.tv_device_name);
        if (tvDeviceName != null) {
            tvDeviceName.setText(deviceName);
        }

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Bottom Navigation
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, ChildDashboardActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        findViewById(R.id.nav_apps).setOnClickListener(v -> {
            startActivity(new Intent(this, MyAppsActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            // Already on Profile
        });

        // Sign Out / Unlink
        findViewById(R.id.btn_child_sign_out).setOnClickListener(v -> {
            session.clearSession();

            startActivity(new Intent(this, RoleActivity.class));
            finishAffinity();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
