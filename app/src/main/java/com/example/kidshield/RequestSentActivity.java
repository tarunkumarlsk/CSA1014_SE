package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class RequestSentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_sent);

        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            startActivity(new Intent(this, ChildDashboardActivity.class));
            finishAffinity();
        });
    }
}
