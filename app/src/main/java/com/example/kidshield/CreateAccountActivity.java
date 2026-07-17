package com.example.kidshield;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class CreateAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView btnLogin = findViewById(R.id.btn_login);
        android.widget.Button btnCreate = findViewById(R.id.btn_create);

        btnBack.setOnClickListener(v -> finish());
        btnLogin.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> {
            android.widget.EditText etName = findViewById(R.id.et_name);
            android.widget.EditText etEmail = findViewById(R.id.et_email);
            android.widget.EditText etPhone = findViewById(R.id.et_phone);
            android.widget.EditText etPassword = findViewById(R.id.et_password);
            android.widget.EditText etConfirm = findViewById(R.id.et_confirm_password);
            android.widget.CheckBox cbTerms = findViewById(R.id.cb_terms);

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Name, Email and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreate.setEnabled(false);
            btnCreate.setText("Creating...");

            com.example.kidshield.network.BackendManager.registerParent(email, password, name, phone,
                    new com.example.kidshield.network.BackendManager.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Toast.makeText(CreateAccountActivity.this, "Account created! You can now log in.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            btnCreate.setEnabled(true);
                            btnCreate.setText("Create Account");
                            Toast.makeText(CreateAccountActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
