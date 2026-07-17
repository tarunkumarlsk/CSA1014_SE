package com.example.kidshield.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.kidshield.R;
import com.example.kidshield.RoleActivity;
import com.example.kidshield.utils.SessionManager;

public class ChildProfileFragment extends Fragment {

    private TextView tvName, tvDevice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tv_child_name);
        tvDevice = view.findViewById(R.id.tv_device_info);

        com.example.kidshield.utils.SessionManager session = 
                com.example.kidshield.utils.SessionManager.getInstance(requireContext());
        String name = session.getName();
        String device = session.getDeviceName();

        tvName.setText(name);
        tvDevice.setText(device);

        // Load AI Safety Score
        TextView tvScore = view.findViewById(R.id.tv_ai_score);
        TextView tvStatus = view.findViewById(R.id.tv_ai_status);
        View dot = view.findViewById(R.id.view_ai_status_dot);

        com.example.kidshield.ai.AISafetyEngine engine = new com.example.kidshield.ai.AISafetyEngine(requireContext());
        
        // Populate features with some realistic or dummy usage data for the current child
        com.example.kidshield.ai.AISafetyEngine.Features features = new com.example.kidshield.ai.AISafetyEngine.Features();
        features.screenTimeRatio = 1.05f; // Slightly over limit
        features.lateNightUsageHrs = 0.5f;
        features.socialMediaFrac = 0.3f;
        features.educationalFrac = 0.4f;
        features.geofenceExits = 1;
        features.sosCount7d = 0;
        features.blockedBypassCount = 0;
        features.daysLimitExceeded = 2;

        com.example.kidshield.ai.AISafetyEngine.SafetyResult result = engine.predict(features);
        
        tvScore.setText(String.valueOf(result.score));
        tvScore.setTextColor(android.graphics.Color.parseColor(result.color));
        tvStatus.setText("Status: " + result.label);
        
        dot.getBackground().mutate();
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(result.color)));

        engine.close();

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            SessionManager.getInstance(requireContext()).clearSession();
            Intent intent = new Intent(requireContext(), RoleActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
