package com.example.kidshield;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;
import com.example.kidshield.models.ChildProfile;
import com.example.kidshield.network.BackendManager;
import java.util.Calendar;
import java.util.Locale;
import android.app.AppOpsManager;
import android.content.Context;
import android.provider.Settings;

public class ChildSetupActivity extends AppCompatActivity {

    private EditText etName, etDob, etBloodGroup;
    private Button btnCompleteSetup;
    private int childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_setup);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        childId = getIntent().getIntExtra("CHILD_ID", -1);
        if (childId == -1) {
            // Fallback: try reading from session
            childId = com.example.kidshield.utils.SessionManager.getInstance(this).getChildId();
        }
        if (childId == -1) {
            Toast.makeText(this, "Error: Invalid Child ID. Please re-link the device.", Toast.LENGTH_LONG).show();
            startActivity(new android.content.Intent(this, ChildLoginActivity.class));
            finish();
            return;
        }


        etName = findViewById(R.id.et_name);
        etDob = findViewById(R.id.et_dob);
        etBloodGroup = findViewById(R.id.et_blood_group);
        btnCompleteSetup = findViewById(R.id.btn_complete_setup);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etDob.setOnClickListener(v -> showDatePicker());

        btnCompleteSetup.setOnClickListener(v -> {
            if (!hasUsageStatsPermission(this)) {
                Toast.makeText(this, "Please grant Usage Access permission. Return here after granting.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow 'Display over other apps'. Return here after granting.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
            saveDetails();
        });
    }

    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
            android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    etDob.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveDetails() {
        String name = etName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String bloodGroup = etBloodGroup.getText().toString().trim();

        if (name.isEmpty() || dob.isEmpty() || bloodGroup.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCompleteSetup.setEnabled(false);
        btnCompleteSetup.setText("Saving...");

        BackendManager.updateChildDetails(childId, name, dob, bloodGroup, new BackendManager.ApiCallback<ChildProfile>() {
            @Override
            public void onSuccess(ChildProfile result) {
                Toast.makeText(ChildSetupActivity.this, "Setup complete!", Toast.LENGTH_SHORT).show();
                
                // Update setup completion status in session
                com.example.kidshield.utils.SessionManager session = 
                                com.example.kidshield.utils.SessionManager.getInstance(ChildSetupActivity.this);
                session.saveChildLinking(childId, name, true);

                // Go to Dashboard
                Intent intent = new Intent(ChildSetupActivity.this, ChildMainActivity.class);
                intent.putExtra("CHILD_ID", childId);
                intent.putExtra("CHILD_NAME", name);
                startActivity(intent);
                finishAffinity();
            }

            @Override
            public void onError(String error) {
                btnCompleteSetup.setEnabled(true);
                btnCompleteSetup.setText("Complete Setup");
                Toast.makeText(ChildSetupActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
