package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;
import com.google.android.material.card.MaterialCardView;

public class RoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        MaterialCardView cardParent = findViewById(R.id.card_parent);
        MaterialCardView cardChild = findViewById(R.id.card_child);

        // Navigate to Parent Login screen
        cardParent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleActivity.this, ParentLoginActivity.class);
            startActivity(intent);
        });

        // Navigate to Child Login screen
        cardChild.setOnClickListener(v -> {
            Intent intent = new Intent(RoleActivity.this, ChildLoginActivity.class);
            startActivity(intent);
        });

        // Hidden Server IP Config (Long press on main title)
        findViewById(R.id.welcome_title).setOnLongClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Configure Server IP");
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("e.g. 192.168.1.100");
            com.example.kidshield.utils.SessionManager sm = com.example.kidshield.utils.SessionManager.getInstance(this);
            input.setText(sm.getServerIp());
            builder.setView(input);
            builder.setPositiveButton("Save", (dialog, which) -> {
                String newIp = input.getText().toString().trim();
                if (!newIp.isEmpty()) {
                    sm.saveServerIp(newIp);
                    com.example.kidshield.network.RetrofitClient.updateBaseUrl(newIp);
                    android.widget.Toast.makeText(this, "Server IP updated: " + newIp, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
            return true;
        });
    }
}
