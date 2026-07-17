package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SmartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart);

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            startActivity(new Intent(this, InstantActivity.class));
        });

        findViewById(R.id.btn_skip).setOnClickListener(v -> {
            // Navigating to RoleActivity on skip
            startActivity(new Intent(this, RoleActivity.class));
            finishAffinity();
        });
    }
}
