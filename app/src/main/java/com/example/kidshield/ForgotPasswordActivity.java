package com.example.kidshield;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnLogin = findViewById(R.id.btn_login);
        android.widget.Button btnSend = findViewById(R.id.btn_send);

        btnBack.setOnClickListener(v -> finish());
        btnLogin.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
