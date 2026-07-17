package com.example.kidshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;

import com.example.kidshield.models.ChildProfile;
import com.example.kidshield.network.BackendManager;

import java.util.List;

public class AddNewDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_device);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        android.widget.Button btnLinked = findViewById(R.id.btn_linked);
        btnLinked.setOnClickListener(v -> {
            // Fetch the first child to save child_id to prefs before going to dashboard
            com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
            int parentId = session.getParentId();

            if (parentId == -1) {
                Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLinked.setEnabled(false);
            btnLinked.setText("Checking...");

            BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<ChildProfile>>() {
                @Override
                public void onSuccess(List<ChildProfile> result) {
                    if (!result.isEmpty()) {
                        // Save the newest child's ID to session for easy access
                        ChildProfile newlyLinkedChild = result.get(result.size() - 1);
                        session.setSelectedChild(newlyLinkedChild.getId(), newlyLinkedChild.getName());
                    }
                    startActivity(new Intent(AddNewDeviceActivity.this, ParentMainActivity.class));
                    finish();
                }

                @Override
                public void onError(String error) {
                    // On error, just go to dashboard anyway (it will handle empty state)
                    startActivity(new Intent(AddNewDeviceActivity.this, ParentMainActivity.class));
                    finish();
                }
            });
        });

        // Generate the code dynamically from backend
        generateCodeAndDisplay();
    }

    private void generateCodeAndDisplay() {
        com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
        int parentId = session.getParentId();

        if (parentId == -1) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        BackendManager.generatePairingCode(parentId,
                new BackendManager.ApiCallback<com.example.kidshield.models.PairingResponse>() {
                    @Override
                    public void onSuccess(com.example.kidshield.models.PairingResponse result) {
                        String code = result.getCode();
                        if (code != null && code.length() == 4) {
                            android.widget.LinearLayout codeContainer = findViewById(R.id.code_container);
                            if (codeContainer != null) {
                                for (int i = 0; i < 4; i++) {
                                    android.view.View child = codeContainer.getChildAt(i);
                                    if (child instanceof com.google.android.material.card.MaterialCardView) {
                                        android.widget.TextView tv = (android.widget.TextView) ((com.google.android.material.card.MaterialCardView) child)
                                                .getChildAt(0);
                                        if (tv != null) {
                                            tv.setText(String.valueOf(code.charAt(i)));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AddNewDeviceActivity.this, "Failed to get pairing code: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
