package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class QuickCaptureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_capture);

        LinearLayout btnBack = findViewById(R.id.btn_back);
        MaterialCardView cardSingleCapture = findViewById(R.id.card_single_capture);
        MaterialCardView cardBurstCapture = findViewById(R.id.card_burst_capture);
        MaterialCardView cardImportImage = findViewById(R.id.card_import_image);

        btnBack.setOnClickListener(v -> finish());

        cardSingleCapture.setOnClickListener(v -> {
            Intent intent = new Intent(this, ParentChildProfileActivity.class);
            startActivity(intent);
        });

        cardBurstCapture.setOnClickListener(v -> {
            Toast.makeText(this, "Burst Capture Selected", Toast.LENGTH_SHORT).show();
            // TODO: Implement burst capture logic
        });

        cardImportImage.setOnClickListener(v -> {
            Toast.makeText(this, "Import Image Selected", Toast.LENGTH_SHORT).show();
            // TODO: Implement gallery picker logic
        });
    }
}
