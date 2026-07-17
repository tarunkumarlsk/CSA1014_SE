package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kidshield.ai.AISafetyEngine;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.models.ScreenTimeLimitResponse;
import com.example.kidshield.models.ScreenUsageResponse;
import com.example.kidshield.models.AlertResponse;
import com.example.kidshield.models.TimelineEvent;
import com.example.kidshield.adapters.TimelineAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.example.kidshield.utils.EdgeToEdgeUtils;

public class ParentChildProfileActivity extends AppCompatActivity {

    private int childId;
    private String childName;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_child_profile);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        childId = getIntent().getIntExtra("child_id", -1);
        childName = getIntent().getStringExtra("child_name");
        deviceName = getIntent().getStringExtra("device_name");

        if (childName != null) {
            ((TextView) findViewById(R.id.profile_name)).setText(childName);
            TextView tvInitials = findViewById(R.id.profile_initials);
            String[] parts = childName.split(" ");
            String init = "";
            if (parts.length > 0 && !parts[0].isEmpty()) init += parts[0].charAt(0);
            if (parts.length > 1 && !parts[1].isEmpty()) init += parts[1].charAt(0);
            tvInitials.setText(init.toUpperCase());
        }
        if (deviceName != null) {
            ((TextView) findViewById(R.id.profile_device_model)).setText(deviceName);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // AI Safety Score Logic (Dynamic calculation)
        calculateAndDisplayAIScore();
        setupTimeline();
        findViewById(R.id.btn_generate_relink_code).setOnClickListener(v -> generateRelinkCode());
        findViewById(R.id.btn_unlink_device).setOnClickListener(v -> unlinkChild());
    }

    private void setupTimeline() {
        if (childId == -1) return;
        RecyclerView rv = findViewById(R.id.rv_child_timeline);
        if (rv == null) return;

        TimelineAdapter adapter = new TimelineAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        BackendManager.getTimelineEvents(childId, new BackendManager.ApiCallback<List<TimelineEvent>>() {
            @Override
            public void onSuccess(List<TimelineEvent> result) {
                adapter.setEvents(result);
            }
            @Override
            public void onError(String error) {
                // Silently fail or show empty state
            }
        });
    }

    private void calculateAndDisplayAIScore() {
        if (childId == -1) return;

        final AISafetyEngine.Features features = new AISafetyEngine.Features();
        
        // 1. Fetch Limits for ratio
        BackendManager.getScreenTimeLimits(childId, new BackendManager.ApiCallback<java.util.List<com.example.kidshield.models.ScreenTimeLimitResponse>>() {
            @Override
            public void onSuccess(java.util.List<com.example.kidshield.models.ScreenTimeLimitResponse> limits) {
                int overallLimit = 120; // fallback
                for (com.example.kidshield.models.ScreenTimeLimitResponse l : limits) {
                    if (l.getAppName() == null || l.getAppName().isEmpty()) overallLimit = l.getLimitMinutes();
                }

                // 2. Fetch Usage
                final int finalLimit = overallLimit;
                BackendManager.getScreenUsage(childId, new BackendManager.ApiCallback<java.util.List<com.example.kidshield.models.ScreenUsageResponse>>() {
                    @Override
                    public void onSuccess(java.util.List<com.example.kidshield.models.ScreenUsageResponse> usage) {
                        int totalUsage = 0;
                        int socialUsage = 0;
                        int eduUsage = 0;
                        for (com.example.kidshield.models.ScreenUsageResponse u : usage) {
                            int mins = u.getUsageTimeMinutes();
                            totalUsage += mins;
                            String pkg = u.getAppName().toLowerCase();
                            if (isSocialApp(pkg)) socialUsage += mins;
                            else if (isEduApp(pkg)) eduUsage += mins;
                        }
                        
                        features.screenTimeRatio = (float) totalUsage / Math.max(1, finalLimit);
                        features.socialMediaFrac = (float) socialUsage / Math.max(1, totalUsage);
                        features.educationalFrac = (float) eduUsage / Math.max(1, totalUsage);
                        
                        // Update UI with usage data
                        int finalTotalUsage = totalUsage;
                        runOnUiThread(() -> {
                            TextView tvTotalUsage = findViewById(R.id.tv_profile_total_usage);
                            TextView tvUsagePercent = findViewById(R.id.tv_profile_usage_percent);
                            TextView tvTimeLimit = findViewById(R.id.tv_profile_time_limit);
                            com.google.android.material.progressindicator.LinearProgressIndicator pbUsage = findViewById(R.id.pb_profile_usage);

                            if (tvTotalUsage != null) {
                                int h = finalTotalUsage / 60;
                                int m = finalTotalUsage % 60;
                                tvTotalUsage.setText(h + "h " + m + "m used today");
                            }
                            if (tvUsagePercent != null) {
                                int percent = (int)(features.screenTimeRatio * 100);
                                tvUsagePercent.setText(Math.min(percent, 100) + "%");
                            }
                            if (tvTimeLimit != null) {
                                int lh = finalLimit / 60;
                                int lm = finalLimit % 60;
                                tvTimeLimit.setText("Limit: " + (lh > 0 ? lh + "h " : "") + (lm > 0 ? lm + "m" : (lh == 0 ? "0m" : "")));
                            }
                            if (pbUsage != null) {
                                pbUsage.setProgress(Math.min((int)(features.screenTimeRatio * 100), 100));
                            }
                        });
                        
                        // Rough mock for late night based on high usage ratio
                        features.lateNightUsageHrs = features.screenTimeRatio > 1.2f ? 0.5f : 0.0f;

                        // 3. Fetch Alerts
                        BackendManager.getAlerts(childId, new BackendManager.ApiCallback<java.util.List<com.example.kidshield.models.AlertResponse>>() {
                            @Override
                            public void onSuccess(java.util.List<com.example.kidshield.models.AlertResponse> alerts) {
                                int sos = 0, geo = 0, bypass = 0;
                                for (com.example.kidshield.models.AlertResponse a : alerts) {
                                    if ("sos".equalsIgnoreCase(a.getAlertType())) sos++;
                                    else if ("geofence".equalsIgnoreCase(a.getAlertType())) geo++;
                                    else if ("screen_time".equalsIgnoreCase(a.getAlertType()) || "app_misuse".equalsIgnoreCase(a.getAlertType())) bypass++;
                                }
                                features.sosCount7d = sos;
                                features.geofenceExits = geo;
                                features.blockedBypassCount = bypass;
                                features.daysLimitExceeded = (features.screenTimeRatio > 1.0f) ? 1 : 0;

                                runInference(features);
                            }
                            @Override
                            public void onError(String error) { runInference(features); }
                        });
                    }
                    @Override
                    public void onError(String error) { runInference(features); }
                });
            }
            @Override
            public void onError(String error) { runInference(features); }
        });
    }

    private void runInference(AISafetyEngine.Features features) {
        AISafetyEngine engine = new AISafetyEngine(this);
        AISafetyEngine.SafetyResult res = engine.predict(features);
        engine.close();

        runOnUiThread(() -> {
            TextView tvAiScore = findViewById(R.id.tv_ai_score_value);
            TextView tvAiStatus = findViewById(R.id.tv_ai_status);
            if (tvAiScore != null) {
                tvAiScore.setText(String.valueOf(res.score));
                tvAiScore.setTextColor(android.graphics.Color.parseColor(res.color));
            }
            if (tvAiStatus != null) {
                tvAiStatus.setText(res.label);
            }
        });
    }

    private void generateRelinkCode() {
        com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(this);
        int parentId = session.getParentId();
        
        com.google.android.material.button.MaterialButton btn = findViewById(R.id.btn_generate_relink_code);
        btn.setEnabled(false);
        btn.setText("Wait...");
        
        BackendManager.generatePairingCode(parentId, childId, new BackendManager.ApiCallback<com.example.kidshield.models.PairingResponse>() {
            @Override
            public void onSuccess(com.example.kidshield.models.PairingResponse result) {
                btn.setEnabled(true);
                btn.setText("Generate");
                showCode(result.getCode());
            }

            @Override
            public void onError(String error) {
                btn.setEnabled(true);
                btn.setText("Generate");
                Toast.makeText(ParentChildProfileActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCode(String code) {
        findViewById(R.id.relink_code_container).setVisibility(android.view.View.VISIBLE);
        if (code != null && code.length() >= 4) {
            ((TextView) findViewById(R.id.relink_code_d1)).setText(String.valueOf(code.charAt(0)));
            ((TextView) findViewById(R.id.relink_code_d2)).setText(String.valueOf(code.charAt(1)));
            ((TextView) findViewById(R.id.relink_code_d3)).setText(String.valueOf(code.charAt(2)));
            ((TextView) findViewById(R.id.relink_code_d4)).setText(String.valueOf(code.charAt(3)));
        }
    }

    private void unlinkChild() {
        if (childId == -1) return;
        BackendManager.unlinkChild(childId, new BackendManager.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(ParentChildProfileActivity.this, "Child unlinked successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ParentChildProfileActivity.this, "Failed to unlink child: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isSocialApp(String pkg) {
        return pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("tiktok") || 
               pkg.contains("snapchat") || pkg.contains("youtube") || pkg.contains("twitter") || 
               pkg.contains("whatsapp") || pkg.contains("messenger") || pkg.contains("reddit");
    }

    private boolean isEduApp(String pkg) {
        return pkg.contains("duolingo") || pkg.contains("classroom") || pkg.contains("khan") || 
               pkg.contains("scholar") || pkg.contains("learning") || pkg.contains("dictionary") || 
               pkg.contains("wikipedia") || pkg.contains("math") || pkg.contains("science");
    }
}
