package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kidshield.utils.SessionManager;
import com.example.kidshield.network.RetrofitClient;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.models.ChildProfile;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View root = findViewById(R.id.splash_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        SessionManager session = SessionManager.getInstance(this);
        
        // Initialize network using saved IP and local environment context
        RetrofitClient.initFromPrefs(this, session.getServerIp());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!session.isLoggedIn()) {
                // Not logged in -> start onboarding/role selection
                startActivity(new Intent(SplashActivity.this, RealTimeActivity.class));
                finish();
                return;
            }

            String role = session.getRole();

            if ("child".equals(role)) {
                // --- CHILD SESSION ---
                int childId = session.getChildId();
                if (childId == -1) {
                    session.clearSession();
                    startActivity(new Intent(SplashActivity.this, RoleActivity.class));
                } else if (!session.isChildSetupDone()) {
                    startActivity(new Intent(SplashActivity.this, ChildSetupActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, ChildMainActivity.class));
                }
                finish();

            } else if ("parent".equals(role)) {
                // --- PARENT SESSION ---
                int parentId = session.getParentId();
                if (parentId == -1) {
                    session.clearSession();
                    startActivity(new Intent(SplashActivity.this, RoleActivity.class));
                    finish();
                    return;
                }

                // Refresh selected child / child list
                BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<ChildProfile>>() {
                    @Override
                    public void onSuccess(List<ChildProfile> result) {
                        if (result.isEmpty()) {
                            startActivity(new Intent(SplashActivity.this, AddNewDeviceActivity.class));
                        } else {
                            // If no child selected yet, pick first one
                            if (session.getSelectedChildId() == -1) {
                                session.setSelectedChild(result.get(0).getId(), result.get(0).getName());
                            }
                            startActivity(new Intent(SplashActivity.this, ParentMainActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        // Network error, fall back to what we had
                        startActivity(new Intent(SplashActivity.this, ParentMainActivity.class));
                        finish();
                    }
                });
            } else {
                // Unknown role
                session.clearSession();
                startActivity(new Intent(SplashActivity.this, RoleActivity.class));
                finish();
            }
        }, 2000);
    }
}
