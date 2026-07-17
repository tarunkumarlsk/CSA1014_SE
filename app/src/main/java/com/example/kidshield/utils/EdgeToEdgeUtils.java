package com.example.kidshield.utils;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeUtils {
    
    /**
     * Applies top padding to the given view based on system bar insets.
     * This ensures the UI is not hidden behind the camera notch or status bar.
     */
    public static void applyTopPadding(View view) {
        if (view == null) return;
        
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });
    }

    /**
     * Applies top and bottom padding to the given view based on system bar insets.
     * Useful for activities that need to avoid overlapping with navigation bar too.
     */
    public static void applySystemBarPadding(View view) {
        if (view == null) return;
        
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });
    }
}
