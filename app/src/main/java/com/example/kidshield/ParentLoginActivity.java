package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ParentLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnForgotPassword = findViewById(R.id.btn_forgot_password);
        TextView btnCreateAccount = findViewById(R.id.btn_create_account);
        android.widget.Button btnSignIn = findViewById(R.id.btn_sign_in);

        btnBack.setOnClickListener(v -> finish());

        // Long press on Logo to change Server IP (Support for Hotspot/Testing)
        findViewById(R.id.logo_container).setOnLongClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Configure Server IP");
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("e.g. 192.168.1.100");
            com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
            input.setText(session.getServerIp());
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newIp = input.getText().toString().trim();
                if (!newIp.isEmpty()) {
                    session.saveServerIp(newIp);
                    com.example.kidshield.network.RetrofitClient.INSTANCE.updateBaseUrl(newIp);
                    Toast.makeText(this, "Server IP updated: " + newIp, Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        });

        btnSignIn.setOnClickListener(v -> {
            android.widget.EditText etEmail = findViewById(R.id.et_email);
            android.widget.EditText etPassword = findViewById(R.id.et_password);

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSignIn.setEnabled(false);
            btnSignIn.setText("Signing in...");

            com.example.kidshield.network.BackendManager.loginParent(email, password,
                    new com.example.kidshield.network.BackendManager.ApiCallback<com.example.kidshield.models.LoginResponse>() {
                        @Override
                        public void onSuccess(com.example.kidshield.models.LoginResponse result) {
                            Toast.makeText(ParentLoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                            // Persist session via SessionManager (keeps user logged in across restarts)
                            com.example.kidshield.utils.SessionManager session =
                                    com.example.kidshield.utils.SessionManager.getInstance(ParentLoginActivity.this);
                            session.saveParentSession(result.getUserId(), result.getName());
                            if (result.getEmail() != null) session.saveParentEmail(result.getEmail());
                            if (result.getPhoneNumber() != null) session.saveParentPhone(result.getPhoneNumber());

                            // Check for children to decide next screen
                            com.example.kidshield.network.BackendManager.getChildren(result.getUserId(),
                                    new com.example.kidshield.network.BackendManager.ApiCallback<java.util.List<com.example.kidshield.models.ChildProfile>>() {
                                        @Override
                                        public void onSuccess(java.util.List<com.example.kidshield.models.ChildProfile> kids) {
                                            if (kids.isEmpty()) {
                                                // New parent with no kids setup
                                                startActivity(new Intent(ParentLoginActivity.this, AddNewDeviceActivity.class));
                                            } else {
                                                com.example.kidshield.models.ChildProfile firstChild = kids.get(0);
                                                session.setSelectedChild(firstChild.getId(), firstChild.getName());
                                                startActivity(new Intent(ParentLoginActivity.this, ParentMainActivity.class));
                                            }
                                            finishAffinity();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            // Fallback to dashboard
                                            startActivity(new Intent(ParentLoginActivity.this, ParentMainActivity.class));
                                            finishAffinity();
                                        }
                                    });
                        }

                        @Override
                        public void onError(String error) {
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In");
                            if (error != null && (error.contains("failed to connect") || error.contains("Network error"))) {
                                String currentUrl = com.example.kidshield.network.RetrofitClient.getBASE_URL_DIAGNOSTIC();
                                android.app.AlertDialog.Builder diag = new android.app.AlertDialog.Builder(ParentLoginActivity.this);
                                diag.setTitle("Network Connection Failed");
                                diag.setMessage("Failed to connect to: " + currentUrl + "\n\nPlease ensure your server is running and on the same Wi-Fi.\n\nWould you like to reconfigure the Server IP?");
                                diag.setPositiveButton("Reconfigure", (d, w) -> {
                                    // Trigger the manual IP setup
                                    findViewById(R.id.logo_container).performLongClick();
                                });
                                diag.setNegativeButton("Wait", null);
                                diag.show();
                            } else {
                                Toast.makeText(ParentLoginActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        btnForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        btnCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateAccountActivity.class));
        });
    }
}
