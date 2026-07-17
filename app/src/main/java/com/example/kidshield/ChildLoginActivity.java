package com.example.kidshield;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kidshield.models.CheckChildResponse;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ChildLoginActivity extends AppCompatActivity {

    EditText codeInput;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already linked AND setup complete, go straight to dashboard
        com.example.kidshield.utils.SessionManager session =
                com.example.kidshield.utils.SessionManager.getInstance(this);

        if (session.isChildLinked()) {
            if (session.isChildSetupDone()) {
                startActivity(new Intent(this, ChildMainActivity.class));
            } else {
                Intent intent = new Intent(this, ChildSetupActivity.class);
                intent.putExtra("CHILD_ID", session.getChildId());
                startActivity(intent);
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_child_login);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        codeInput = findViewById(R.id.editPairingCode);
        connectButton = findViewById(R.id.btnConnectDevice);

        // Long-press device icon to configure Server IP
        findViewById(R.id.icon_container).setOnLongClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Configure Server IP");
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("e.g. 192.168.1.100");
            com.example.kidshield.utils.SessionManager sm = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
            input.setText(sm.getServerIp());
            builder.setView(input);
            builder.setPositiveButton("Save", (dialog, which) -> {
                String newIp = input.getText().toString().trim();
                if (!newIp.isEmpty()) {
                    sm.saveServerIp(newIp);
                    com.example.kidshield.network.RetrofitClient.INSTANCE.updateBaseUrl(newIp);
                    Toast.makeText(this, "Server IP updated: " + newIp, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        });

        connectButton.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter pairing code", Toast.LENGTH_SHORT).show();
                return;
            }

            String deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

            // Build human-readable device name: e.g. "Samsung Galaxy S21" or "Google Pixel 7"
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            String deviceName = model.toLowerCase().startsWith(manufacturer.toLowerCase())
                    ? capitalize(model)
                    : capitalize(manufacturer) + " " + model;

            connectButton.setEnabled(false);
            connectButton.setText("Connecting...");

            // Use check endpoint: returns existing child if already set up, or creates new one
            BackendManager.checkChildByCode(code, deviceId, deviceName,
                    new BackendManager.ApiCallback<CheckChildResponse>() {
                        @Override
                        public void onSuccess(CheckChildResponse result) {
                            int childId = result.getChildId();
                            boolean isNew = result.isNew();
                            String childName = result.getChildName();
                            String returnedDeviceName = result.getDeviceName();

                            com.example.kidshield.utils.SessionManager sess =
                                    com.example.kidshield.utils.SessionManager.getInstance(ChildLoginActivity.this);

                            String finalDeviceName = (returnedDeviceName != null && !returnedDeviceName.isEmpty())
                                    ? returnedDeviceName : deviceName;

                            boolean setupDone = !isNew && childName != null && !childName.equals("Child Device");
                            sess.saveChildLinking(childId, isNew ? "Child Device" : childName, setupDone);
                            sess.saveDeviceName(finalDeviceName);

                            // ── Sync this device's apps before navigating ──
                            // This clears any stale app list from a previous device login.
                            connectButton.setText("Syncing apps...");
                            syncInstalledAppsToBackend(childId, () -> {
                                if (isNew) {
                                    Intent intent = new Intent(ChildLoginActivity.this, ChildSetupActivity.class);
                                    intent.putExtra("CHILD_ID", childId);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(ChildLoginActivity.this,
                                            "Welcome back, " + childName + "!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(ChildLoginActivity.this, ChildMainActivity.class);
                                    startActivity(intent);
                                }
                                finish();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            connectButton.setEnabled(true);
                            connectButton.setText("Connect");
                            if (error != null && (error.contains("failed to connect") || error.contains("Network error"))) {
                                String currentUrl = com.example.kidshield.network.RetrofitClient.getBASE_URL_DIAGNOSTIC();
                                android.app.AlertDialog.Builder diag = new android.app.AlertDialog.Builder(ChildLoginActivity.this);
                                diag.setTitle("Network Connection Failed");
                                diag.setMessage("Failed to connect to: " + currentUrl + "\n\nPlease ensure your server is running and on the same Wi-Fi.\n\nWould you like to reconfigure the Server IP?");
                                diag.setPositiveButton("Reconfigure", (d, w) -> {
                                    findViewById(R.id.icon_container).performLongClick();
                                });
                                diag.setNegativeButton("Wait", null);
                                diag.show();
                            } else {
                                Toast.makeText(ChildLoginActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    /** Collects non-system apps, pushes them via bulk-sync, then calls onDone on the main thread. */
    private void syncInstalledAppsToBackend(int childId, Runnable onDone) {
        new Thread(() -> {
            android.content.pm.PackageManager pm = getPackageManager();
            java.util.List<android.content.pm.ApplicationInfo> installed =
                    pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);

            java.util.List<String> appNames = new java.util.ArrayList<>();
            for (android.content.pm.ApplicationInfo info : installed) {
                // Skip system apps
                if ((info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    String label = pm.getApplicationLabel(info).toString();
                    if (label != null && !label.trim().isEmpty()) {
                        appNames.add(label.trim());
                    }
                }
            }

            android.util.Log.d("ChildLogin", "Syncing " + appNames.size() + " installed apps to backend");

            BackendManager.syncAppInventory(childId, appNames, new BackendManager.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(onDone);
                }
                @Override
                public void onError(String error) {
                    android.util.Log.e("ChildLogin", "App sync failed: " + error);
                    runOnUiThread(onDone); // Navigate anyway — don't block login
                }
            });
        }).start();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}