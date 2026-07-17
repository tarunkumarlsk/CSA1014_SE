package com.example.kidshield;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.Slider;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.EdgeToEdgeUtils;
import com.example.kidshield.models.ScreenTimeLimitResponse;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;

public class TimeLimitsActivity extends AppCompatActivity {

    private int childId;
    private int currentLimitMinutes = 240; // Default 4 hours if not fetched
    private String bedtimeStart = "21:00", bedtimeEnd = "07:00";
    private String schoolStart = "08:00", schoolEnd = "15:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_limits);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        com.example.kidshield.utils.SessionManager session = 
            com.example.kidshield.utils.SessionManager.getInstance(this);
        // Prefer CHILD_ID passed via intent, fallback to session default
        childId = getIntent().getIntExtra("CHILD_ID", session.getSelectedChildId());
        if (childId == -1) childId = session.getChildId();

        Slider timeSlider = findViewById(R.id.time_slider);
        TextView tvTimeValue = findViewById(R.id.tv_time_value);
        com.google.android.material.switchmaterial.SwitchMaterial switchBedtime = findViewById(R.id.switch_bedtime);
        com.google.android.material.switchmaterial.SwitchMaterial switchSchool = findViewById(R.id.switch_school);

        if (timeSlider != null && tvTimeValue != null) {
            timeSlider.addOnChangeListener((slider, value, fromUser) -> {
                int hours = (int) value;
                currentLimitMinutes = hours * 60;
                tvTimeValue.setText(hours + "h 00m");
            });
        }

        TextView tvBedtimeRange = findViewById(R.id.tv_bedtime_range);
        TextView tvSchoolRange = findViewById(R.id.tv_school_range);

        if (switchBedtime != null) {
            switchBedtime.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveLimitsDirectly();
            });
        }

        if (switchSchool != null) {
            switchSchool.setOnCheckedChangeListener((buttonView, isChecked) -> {
                saveLimitsDirectly();
            });
        }

        // Make the whole row clickable
        android.view.View cardBedtime = findViewById(R.id.card_bedtime);
        if (cardBedtime != null && switchBedtime != null) {
            cardBedtime.setOnClickListener(v -> switchBedtime.setChecked(!switchBedtime.isChecked()));
        }

        android.view.View cardSchool = findViewById(R.id.card_school);
        if (cardSchool != null && switchSchool != null) {
            cardSchool.setOnClickListener(v -> switchSchool.setChecked(!switchSchool.isChecked()));
        }

        if (tvBedtimeRange != null) {
            tvBedtimeRange.setOnClickListener(v -> showTimeRangePicker("Bedtime", bedtimeStart, bedtimeEnd, (start, end) -> {
                bedtimeStart = start;
                bedtimeEnd = end;
                tvBedtimeRange.setText(formatRange(start, end));
                saveLimitsDirectly();
            }));
        }

        if (tvSchoolRange != null) {
            tvSchoolRange.setOnClickListener(v -> showTimeRangePicker("School Hours", schoolStart, schoolEnd, (start, end) -> {
                schoolStart = start;
                schoolEnd = end;
                tvSchoolRange.setText(formatRange(start, end));
                saveLimitsDirectly();
            }));
        }

        findViewById(R.id.btn_apply_limits).setOnClickListener(v -> {
            saveLimitsDirectly(true);
        });

        // Fetch current limit dynamically
        fetchCurrentLimit(timeSlider, tvTimeValue);
    }

    private void saveLimitsDirectly() {
        saveLimitsDirectly(false);
    }

    private void saveLimitsDirectly(boolean closeOnSuccess) {
        if (childId == -1) return;

        com.google.android.material.switchmaterial.SwitchMaterial switchBedtime = findViewById(R.id.switch_bedtime);
        com.google.android.material.switchmaterial.SwitchMaterial switchSchool = findViewById(R.id.switch_school);
        
        boolean bedtimeEnabled = switchBedtime != null && switchBedtime.isChecked();
        boolean schoolEnabled = switchSchool != null && switchSchool.isChecked();

        if (switchBedtime != null) switchBedtime.setEnabled(false);
        if (switchSchool != null) switchSchool.setEnabled(false);

        BackendManager.setScreenTimeLimit(
            childId, null, currentLimitMinutes, 
            bedtimeEnabled, schoolEnabled, 
            bedtimeStart, bedtimeEnd, schoolStart, schoolEnd,
            new BackendManager.ApiCallback<ScreenTimeLimitResponse>() {
                @Override
                public void onSuccess(ScreenTimeLimitResponse result) {
                    if (switchBedtime != null) switchBedtime.setEnabled(true);
                    if (switchSchool != null) switchSchool.setEnabled(true);
                    
                    if (closeOnSuccess) {
                        Toast.makeText(TimeLimitsActivity.this, "Limits applied", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Silent or subtle success indicator
                        android.util.Log.d("TimeLimits", "Setting synced");
                    }
                }

                @Override
                public void onError(String error) {
                    if (switchBedtime != null) switchBedtime.setEnabled(true);
                    if (switchSchool != null) switchSchool.setEnabled(true);
                    
                    if (closeOnSuccess) {
                        Toast.makeText(TimeLimitsActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private void fetchCurrentLimit(Slider timeSlider, TextView tvTimeValue) {
        com.google.android.material.switchmaterial.SwitchMaterial switchBedtime = findViewById(R.id.switch_bedtime);
        com.google.android.material.switchmaterial.SwitchMaterial switchSchool = findViewById(R.id.switch_school);

        BackendManager.getScreenTimeLimits(childId, new BackendManager.ApiCallback<java.util.List<ScreenTimeLimitResponse>>() {
            @Override
            public void onSuccess(java.util.List<ScreenTimeLimitResponse> result) {
                if (result != null && !result.isEmpty()) {
                    for (ScreenTimeLimitResponse limit : result) {
                        if (limit.getAppName() == null || limit.getAppName().isEmpty()) {
                            // This is the overall limit
                            int mins = limit.getLimitMinutes();
                            currentLimitMinutes = mins;
                            if (timeSlider != null) {
                                float hoursValue = mins / 60f;
                                if (hoursValue < 1.0f) hoursValue = 1.0f;
                                if (hoursValue > 8.0f) hoursValue = 8.0f;
                                try {
                                    timeSlider.setValue(hoursValue);
                                } catch (Exception e) {
                                    android.util.Log.e("TimeLimits", "Slider error: " + e.getMessage());
                                }
                            }
                            if (tvTimeValue != null) {
                                tvTimeValue.setText(mins >= 60 ? (mins / 60) + "h " + (mins % 60) + "m" : mins + "m");
                            }
                            
                            if (switchBedtime != null) {
                                switchBedtime.setOnCheckedChangeListener(null);
                                switchBedtime.setChecked(limit.getBedtimeEnabled());
                                switchBedtime.setOnCheckedChangeListener((buttonView, isChecked) -> saveLimitsDirectly());
                            }
                            if (switchSchool != null) {
                                switchSchool.setOnCheckedChangeListener(null);
                                switchSchool.setChecked(limit.getSchoolHoursEnabled());
                                switchSchool.setOnCheckedChangeListener((buttonView, isChecked) -> saveLimitsDirectly());
                            }
                            
                            if (limit.getBedtimeStart() != null) bedtimeStart = limit.getBedtimeStart();
                            if (limit.getBedtimeEnd() != null) bedtimeEnd = limit.getBedtimeEnd();
                            if (limit.getSchoolStart() != null) schoolStart = limit.getSchoolStart();
                            if (limit.getSchoolEnd() != null) schoolEnd = limit.getSchoolEnd();

                            TextView tvBedtimeRange = findViewById(R.id.tv_bedtime_range);
                            TextView tvSchoolRange = findViewById(R.id.tv_school_range);
                            if (tvBedtimeRange != null) tvBedtimeRange.setText(formatRange(bedtimeStart, bedtimeEnd));
                            if (tvSchoolRange != null) tvSchoolRange.setText(formatRange(schoolStart, schoolEnd));
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("TimeLimitsActivity", "Failed to fetch limit: " + error);
            }
        });
    }

    private void showTimeRangePicker(String title, String currentStart, String currentEnd, TimeRangeCallback callback) {
        String[] s = currentStart.split(":");
        int startH = Integer.parseInt(s[0]);
        int startM = Integer.parseInt(s[1]);

        new TimePickerDialog(this, (view, h1, m1) -> {
            String newStart = String.format(Locale.getDefault(), "%02d:%02d", h1, m1);
            String[] e = currentEnd.split(":");
            int endH = Integer.parseInt(e[0]);
            int endM = Integer.parseInt(e[1]);
            
            new TimePickerDialog(this, (view2, h2, m2) -> {
                String newEnd = String.format(Locale.getDefault(), "%02d:%02d", h2, m2);
                callback.onSelected(newStart, newEnd);
            }, endH, endM, false).show();
            
            Toast.makeText(this, "Set end time for " + title, Toast.LENGTH_SHORT).show();
        }, startH, startM, false).show();
        
        Toast.makeText(this, "Set start time for " + title, Toast.LENGTH_SHORT).show();
    }

    private String formatRange(String start, String end) {
        return convertToAmPm(start) + " - " + convertToAmPm(end);
    }

    private String convertToAmPm(String time24) {
        try {
            String[] parts = time24.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String suffix = (h >= 12) ? "PM" : "AM";
            int hAmPm = (h > 12) ? h - 12 : (h == 0 ? 12 : h);
            return String.format(Locale.getDefault(), "%02d:%02d %s", hAmPm, m, suffix);
        } catch (Exception e) {
            return time24;
        }
    }

    interface TimeRangeCallback {
        void onSelected(String start, String end);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
