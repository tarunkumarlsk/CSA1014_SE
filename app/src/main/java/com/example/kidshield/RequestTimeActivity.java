package com.example.kidshield;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

public class RequestTimeActivity extends AppCompatActivity {

    private MaterialCardView selectedTimeCard;
    private android.widget.TextView selectedTimeText;
    private int selectedMinutes = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_time);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        MaterialCardView btn15m = findViewById(R.id.btn_15m);
        MaterialCardView btn30m = findViewById(R.id.btn_30m);
        MaterialCardView btn1h = findViewById(R.id.btn_1h);

        btn15m.setOnClickListener(v -> selectTime(btn15m, findViewById(R.id.tv_15m), 15));
        btn30m.setOnClickListener(v -> selectTime(btn30m, findViewById(R.id.tv_30m), 30));
        btn1h.setOnClickListener(v -> selectTime(btn1h, findViewById(R.id.tv_1h), 60));

        findViewById(R.id.btn_send).setOnClickListener(v -> {
            v.setEnabled(false);
            android.widget.EditText etReason = findViewById(R.id.et_reason);
            String reason = etReason != null ? etReason.getText().toString().trim() : "";
            
            String msg = "Requested: " + selectedMinutes + "m\n" + (reason.isEmpty() ? "No reason provided." : "Reason: " + reason);

            int childId = com.example.kidshield.utils.SessionManager.getInstance(this).getChildId();
            if (childId == -1) {
                v.setEnabled(true);
                android.widget.Toast.makeText(this, "Device not properly linked. Please re-setup.", android.widget.Toast.LENGTH_LONG).show();
                return;
            }

            com.example.kidshield.network.BackendManager.sendAlert(childId, "time_request", msg,
                    new com.example.kidshield.network.BackendManager.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            startActivity(new Intent(RequestTimeActivity.this, RequestSentActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            v.setEnabled(true);
                            android.widget.Toast.makeText(RequestTimeActivity.this, "Failed to send request: " + error, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Set initial selection
        selectTime(btn30m, findViewById(R.id.tv_30m), 30);
    }

    private void selectTime(MaterialCardView card, android.widget.TextView text, int minutes) {
        selectedMinutes = minutes;
        if (selectedTimeCard != null) {
            selectedTimeCard.setCardBackgroundColor(Color.parseColor("#F9FAFB"));
            selectedTimeText.setTextColor(Color.parseColor("#4B5563"));
        }

        card.setCardBackgroundColor(Color.parseColor("#10B981"));
        text.setTextColor(Color.WHITE);

        selectedTimeCard = card;
        selectedTimeText = text;
    }
}
