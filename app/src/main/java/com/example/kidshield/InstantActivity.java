package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class InstantActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant);

        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            // Navigating to RoleActivity as requested
            startActivity(new Intent(this, RoleActivity.class));
        });

        findViewById(R.id.btn_skip).setOnClickListener(v -> {
            startActivity(new Intent(this, RoleActivity.class));
        });
    }
}
