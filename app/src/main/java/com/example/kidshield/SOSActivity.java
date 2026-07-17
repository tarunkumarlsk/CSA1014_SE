package com.example.kidshield;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SOSActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        // Get childId from session (child's own device)
        com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
        int childId = session.getChildId();
        // Also accept from intent (in case triggered from parent dashboard)
        int intentChildId = getIntent().getIntExtra("CHILD_ID", -1);
        if (intentChildId != -1) childId = intentChildId;

        if (childId == -1) {
            Toast.makeText(this, "No device linked. Cannot send SOS.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final int finalChildId = childId;

        // Immediately trigger SOS when this screen opens
        com.example.kidshield.network.BackendManager.triggerSOS(finalChildId,
                new com.example.kidshield.network.BackendManager.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(SOSActivity.this, "🚨 SOS Alert Sent to parent!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SOSActivity.this, "Failed to send SOS: " + error, Toast.LENGTH_SHORT).show();
                    }
                });

        // Stop Alert button goes back
        android.view.View btnStop = findViewById(R.id.btn_stop_alert);
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> finish());
        }
    }
}
