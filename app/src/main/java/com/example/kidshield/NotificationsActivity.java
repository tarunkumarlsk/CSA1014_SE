package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Bottom Navigation
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, ParentMainActivity.class));
            finishAffinity();
        });

        findViewById(R.id.nav_map).setOnClickListener(v -> {
            startActivity(new Intent(this, ParentMainActivity.class));
            finish();
        });

        findViewById(R.id.nav_controls).setOnClickListener(v -> {
            startActivity(new Intent(this, ParentMainActivity.class));
            finish();
        });

        findViewById(R.id.nav_alerts).setOnClickListener(v -> {
            startActivity(new Intent(this, ParentMainActivity.class));
            finish();
        });

        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            finish();
        });
    }
}
