package com.example.kidshield.ai;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * KidShield AI Safety Score Engine
 * ----------------------------------
 * Runs the on-device TFLite model to compute a safety score for a child.
 *
 * Usage:
 *   AISafetyEngine engine = new AISafetyEngine(context);
 *   AISafetyEngine.SafetyResult result = engine.predict(features);
 *   // result.label  → "Safe" / "Warning" / "Danger"
 *   // result.score  → 0-100 (confidence mapped to score)
 *   // result.color  → "#10B981" / "#F59E0B" / "#EF4444"
 */
public class AISafetyEngine {

    private static final String TAG = "AISafetyEngine";
    private static final String MODEL_FILE   = "kidshield_safety_model.tflite";
    private static final String SCALER_FILE  = "kidshield_scaler_params.json";

    private Interpreter tflite;
    private float[] scalerMean;
    private float[] scalerScale;
    private String[] labels = {"Safe", "Warning", "Danger"};
    private static final int NUM_FEATURES = 8;

    public static class Features {
        /** actual screen time / daily limit (e.g. 1.2 = 20% over) */
        public float screenTimeRatio;
        /** hours used between 22:00 and 06:00 */
        public float lateNightUsageHrs;
        /** fraction of screen time on social media apps (0.0 – 1.0) */
        public float socialMediaFrac;
        /** fraction of screen time on educational apps (0.0 – 1.0) */
        public float educationalFrac;
        /** number of geofence EXIT events today */
        public int geofenceExits;
        /** SOS button presses in last 7 days */
        public int sosCount7d;
        /** times child opened a blocked app this week */
        public int blockedBypassCount;
        /** how many of last 7 days the child exceeded the screen time limit */
        public int daysLimitExceeded;
    }

    public static class SafetyResult {
        public String label;    // "Safe", "Warning", "Danger"
        public int    score;    // 0-100 (100 = perfectly safe)
        public String color;    // hex color for UI
        public float  confidence; // raw probability of predicted class

        @Override
        public String toString() {
            return label + " (" + score + "/100)";
        }
    }

    public AISafetyEngine(Context context) {
        try {
            // Load TFLite model from assets
            MappedByteBuffer modelBuffer = loadModelFile(context);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            tflite = new Interpreter(modelBuffer, options);
            Log.d(TAG, "TFLite model loaded successfully");

            // Load scaler params from assets JSON
            loadScalerParams(context);
            Log.d(TAG, "Scaler params loaded");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AI engine: " + e.getMessage());
        }
    }

    /**
     * Run inference on a feature vector and return SafetyResult.
     */
    public SafetyResult predict(Features f) {
        if (tflite == null || scalerMean == null) {
            // Model not loaded — return safe default
            SafetyResult fallback = new SafetyResult();
            fallback.label = "Safe";
            fallback.score = 75;
            fallback.color = "#10B981";
            return fallback;
        }

        // Build raw feature array
        float[] rawFeatures = {
            f.screenTimeRatio,
            f.lateNightUsageHrs,
            f.socialMediaFrac,
            f.educationalFrac,
            (float) f.geofenceExits,
            (float) f.sosCount7d,
            (float) f.blockedBypassCount,
            (float) f.daysLimitExceeded
        };

        // Apply StandardScaler normalization: (x - mean) / scale
        float[] normalized = new float[NUM_FEATURES];
        for (int i = 0; i < NUM_FEATURES; i++) {
            normalized[i] = (rawFeatures[i] - scalerMean[i]) / scalerScale[i];
        }

        // Prepare input buffer (float32, 1×8)
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * NUM_FEATURES);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (float val : normalized) {
            inputBuffer.putFloat(val);
        }

        // Prepare output buffer (float32, 1×3 — 3 class probabilities)
        float[][] outputProbs = new float[1][3];

        // Run inference
        tflite.run(inputBuffer, outputProbs);

        float[] probs = outputProbs[0];

        // Find predicted class
        int predictedClass = 0;
        float maxProb = probs[0];
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > maxProb) {
                maxProb = probs[i];
                predictedClass = i;
            }
        }

        // Map to result:
        // Class 0 = Safe    → score near 100 (high safe prob = high score)
        // Class 1 = Warning → score near 50
        // Class 2 = Danger  → score near 0
        SafetyResult result = new SafetyResult();
        result.label      = labels[predictedClass];
        result.confidence = maxProb;

        switch (predictedClass) {
            case 0: // Safe
                result.score = (int)(50 + (probs[0] * 50));   // 50–100
                result.color = "#10B981";
                break;
            case 1: // Warning
                result.score = (int)(25 + (probs[1] * 25));   // 25–50
                result.color = "#F59E0B";
                break;
            case 2: // Danger
                result.score = (int)(probs[0] * 25);           // 0–25
                result.color = "#EF4444";
                break;
            default:
                result.score  = 50;
                result.color  = "#6B7280";
        }

        Log.d(TAG, "Prediction: " + result + " (Safe="+probs[0]+", Warn="+probs[1]+", Danger="+probs[2]+")");
        return result;
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private MappedByteBuffer loadModelFile(Context context) throws Exception {
        // Try to load from assets
        InputStream is = context.getAssets().open(MODEL_FILE);
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        is.close();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.rewind();
        // Wrap in a mapped buffer by writing to a temp file
        java.io.File tmpFile = java.io.File.createTempFile("kidshield_model", ".tflite", context.getCacheDir());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
            fos.write(bytes);
        }
        FileInputStream fis = new FileInputStream(tmpFile);
        FileChannel fc = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    }

    private void loadScalerParams(Context context) throws Exception {
        InputStream is = context.getAssets().open(SCALER_FILE);
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        is.close();
        String json = new String(bytes);

        JSONObject obj = new JSONObject(json);
        JSONArray meanArr  = obj.getJSONArray("mean");
        JSONArray scaleArr = obj.getJSONArray("scale");

        scalerMean  = new float[meanArr.length()];
        scalerScale = new float[scaleArr.length()];
        for (int i = 0; i < meanArr.length(); i++) {
            scalerMean[i]  = (float) meanArr.getDouble(i);
            scalerScale[i] = (float) scaleArr.getDouble(i);
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
