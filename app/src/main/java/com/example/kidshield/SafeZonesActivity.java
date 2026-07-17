package com.example.kidshield;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kidshield.utils.EdgeToEdgeUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kidshield.adapters.SafeZoneAdapter;
import com.example.kidshield.adapters.SearchResultAdapter;
import com.example.kidshield.models.SafeZone;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.network.NominatimClient;
import com.example.kidshield.utils.SessionManager;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class SafeZonesActivity extends AppCompatActivity {

    private SafeZoneAdapter zoneAdapter;
    private SearchResultAdapter searchAdapter;
    private TextView tvZoneCount;
    private View panelAdd;
    private TextInputEditText etZoneName, etZoneAddress;
    private TextView tvDistanceInfo;
    private Slider sliderRadius;
    private TextView tvRadiusLabel;

    private boolean addMode = false;
    private FusedLocationProviderClient fusedLocationClient;
    private int parentId, childId;
    private double pendingLat, pendingLng;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_zones);
        EdgeToEdgeUtils.applyTopPadding(findViewById(android.R.id.content));

        parentId = SessionManager.getInstance(this).getParentId();
        childId  = SessionManager.getInstance(this).getChildId();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        setupViews();
        loadChildAndZones();
    }

    private void setupMap() {
        // Map has been removed to simplify UI and prevent missing content issues.
    }

    private void setupViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Add zone button: toggles add mode
        findViewById(R.id.btn_add_zone).setOnClickListener(v -> {
            addMode = !addMode;
            if (addMode) {
                panelAdd.setVisibility(View.VISIBLE);
                // Just reset fields
                tvDistanceInfo.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                tvDistanceInfo.setText("Not Located Yet. Type an address and hit the search icon.");
                pendingLat = 0; pendingLng = 0;
            } else {
                hidePanelAdd();
            }
        });

        panelAdd      = findViewById(R.id.panel_add_zone);
        tvZoneCount   = findViewById(R.id.tv_zone_count);
        tvRadiusLabel = findViewById(R.id.tv_radius_label);
        etZoneName    = findViewById(R.id.et_zone_name);
        etZoneAddress = findViewById(R.id.et_zone_address);
        tvDistanceInfo= findViewById(R.id.tv_distance_info);
        sliderRadius  = findViewById(R.id.slider_radius);

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            tvRadiusLabel.setText("Radius: " + (int) value + " m");
        });

        // Toggle radius UI based on zone name
        etZoneName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                boolean isHome = "Home".equalsIgnoreCase(name);
                int visibility = isHome ? View.GONE : View.VISIBLE;
                sliderRadius.setVisibility(visibility);
                tvRadiusLabel.setVisibility(visibility);
            }
        });

        // Search address button inside panel
        findViewById(R.id.btn_search_address).setOnClickListener(v -> {
            String q = etZoneAddress.getText() != null ? etZoneAddress.getText().toString().trim() : "";
            if (q.length() >= 3) {
                hideKeyboard();
                Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
                NominatimClient.search(SafeZonesActivity.this, q, results -> {
                    if (results != null && !results.isEmpty()) {
                        com.example.kidshield.models.NominatimResult first = results.get(0);
                        pendingLat = first.lat;
                        pendingLng = first.lon;
                        
                        // Calculate distance from Home
                        double homeLat = SessionManager.getInstance(SafeZonesActivity.this).getHomeLat();
                        double homeLng = SessionManager.getInstance(SafeZonesActivity.this).getHomeLng();
                        if (homeLat != 0.0 || homeLng != 0.0) {
                            float[] res = new float[1];
                            android.location.Location.distanceBetween(homeLat, homeLng, pendingLat, pendingLng, res);
                            double distInMeters = res[0];
                            double distKm = distInMeters / 1000.0;
                            
                            tvDistanceInfo.setTextColor(android.graphics.Color.parseColor("#10B981"));
                            tvDistanceInfo.setText(String.format(java.util.Locale.US, "Address Locked! Distance from home: %.1f km", distKm));

                            float autoRadius = (float) (Math.round(distInMeters / 50.0) * 50.0);
                            if (autoRadius < sliderRadius.getValueFrom()) autoRadius = sliderRadius.getValueFrom();
                            if (autoRadius > sliderRadius.getValueTo()) {
                                sliderRadius.setValueTo(autoRadius + 1000.0f);
                            }
                            sliderRadius.setValue(autoRadius);
                        } else {
                            tvDistanceInfo.setTextColor(android.graphics.Color.parseColor("#10B981"));
                            tvDistanceInfo.setText("Address Locked! Ready to save.");
                        }
                    } else {
                        Toast.makeText(SafeZonesActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Enter an address", Toast.LENGTH_SHORT).show();
            }
        });

        View btnGps = findViewById(R.id.btn_gps_address);
        if (btnGps != null) {
            btnGps.setOnClickListener(v -> acquireCurrentLocation());
        }

        // Safe zone list
        RecyclerView rvZones = findViewById(R.id.rv_safe_zones);
        zoneAdapter = new SafeZoneAdapter();
        rvZones.setLayoutManager(new LinearLayoutManager(this));
        rvZones.setAdapter(zoneAdapter);
        zoneAdapter.setOnDeleteListener((zone, pos) -> deleteZone(zone, pos));

        // Confirm / cancel panel
        findViewById(R.id.btn_confirm_zone).setOnClickListener(v -> confirmAddZone());
        findViewById(R.id.btn_cancel_zone).setOnClickListener(v -> hidePanelAdd());
    }

    private void loadChildAndZones() {
        // Load children to find first child's safe zones
        if (parentId == -1) return;
        BackendManager.getChildren(parentId, new BackendManager.ApiCallback<List<com.example.kidshield.models.ChildProfile>>() {
            @Override
            public void onSuccess(List<com.example.kidshield.models.ChildProfile> result) {
                if (result.isEmpty()) return;
                int cid = result.get(0).getId();
                childId = cid;
                loadZones(cid);
            }
            @Override public void onError(String e) {}
        });
    }

    private void loadZones(int cid) {
        BackendManager.getSafeZones(cid, new BackendManager.ApiCallback<List<SafeZone>>() {
            @Override
            public void onSuccess(List<SafeZone> zones) {
                boolean hasHome = false;
                for (SafeZone sz : zones) {
                    if ("Home".equalsIgnoreCase(sz.getName())) {
                        hasHome = true;
                        break;
                    }
                }
                
                double homeLat = SessionManager.getInstance(SafeZonesActivity.this).getHomeLat();
                double homeLng = SessionManager.getInstance(SafeZonesActivity.this).getHomeLng();
                
                if (!hasHome && (homeLat != 0.0 || homeLng != 0.0)) {
                    BackendManager.createSafeZone(cid, "Home", homeLat, homeLng, 200, new BackendManager.ApiCallback<SafeZone>() {
                        @Override
                        public void onSuccess(SafeZone result) {
                            java.util.List<SafeZone> updatedZones = new java.util.ArrayList<>(zones);
                            updatedZones.add(0, result);
                            updateZonesList(updatedZones);
                        }
                        @Override public void onError(String e) { updateZonesList(zones); }
                    });
                } else {
                    updateZonesList(zones);
                }
            }
            @Override public void onError(String e) {}
        });
    }

    private void updateZonesList(List<SafeZone> zones) {
        zoneAdapter.setZones(zones);
        tvZoneCount.setText(zones.size() + " zone" + (zones.size() == 1 ? "" : "s"));
        drawZonesOnMap(zones);
    }

    private void drawZonesOnMap(List<SafeZone> zones) {
        // Map removed. Display handled exclusively by RecyclerView in bottom sheet.
        if (zoneAdapter.getItemCount() == 0) {
            tvZoneCount.setText("You have no safe zones.");
        }
    }

    private void confirmAddZone() {
        String name = etZoneName.getText() != null ? etZoneName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a zone name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        float radius;
        if ("Home".equalsIgnoreCase(name)) {
            radius = 200; // Default radius for Home as it is not required to be set by user
        } else {
            radius = sliderRadius.getValue();
        }

        if (childId == -1) {
            Toast.makeText(this, "No child linked yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendingLat == 0 && pendingLng == 0) {
            Toast.makeText(this, "Please search for an address before saving", Toast.LENGTH_LONG).show();
            return;
        }

        BackendManager.createSafeZone(childId, name, pendingLat, pendingLng, radius,
                new BackendManager.ApiCallback<SafeZone>() {
                    @Override
                    public void onSuccess(SafeZone result) {
                        Toast.makeText(SafeZonesActivity.this, "Zone saved ✓", Toast.LENGTH_SHORT).show();
                        hidePanelAdd();
                        addMode = false;
                        etZoneName.setText("");
                        if (etZoneAddress != null) etZoneAddress.setText("");
                        loadZones(childId);
                    }
                    @Override
                    public void onError(String e) {
                        Toast.makeText(SafeZonesActivity.this, "Failed: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteZone(SafeZone zone, int pos) {
        BackendManager.deleteSafeZone(zone.getId(), new BackendManager.ApiCallback<String>() {
            @Override
            public void onSuccess(String r) {
                zoneAdapter.removeAt(pos);
                int n = zoneAdapter.getItemCount();
                tvZoneCount.setText(n + " zone" + (n == 1 ? "" : "s"));
                loadZones(childId);
                Toast.makeText(SafeZonesActivity.this, "Zone deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String e) {
                Toast.makeText(SafeZonesActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hidePanelAdd() {
        panelAdd.setVisibility(View.GONE);
    }

    private void acquireCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Toast.makeText(this, "🛰️ GPS: Acquiring location...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    pendingLat = location.getLatitude();
                    pendingLng = location.getLongitude();
                    
                    tvDistanceInfo.setTextColor(android.graphics.Color.parseColor("#10B981"));
                    tvDistanceInfo.setText("GPS Located! Ready to save.");

                    double homeLat = SessionManager.getInstance(SafeZonesActivity.this).getHomeLat();
                    double homeLng = SessionManager.getInstance(SafeZonesActivity.this).getHomeLng();
                    if (homeLat != 0.0 || homeLng != 0.0) {
                        float[] res = new float[1];
                        android.location.Location.distanceBetween(homeLat, homeLng, pendingLat, pendingLng, res);
                        double distInMeters = res[0];
                        float autoRadius = (float) (Math.round(distInMeters / 50.0) * 50.0);
                        if (autoRadius < sliderRadius.getValueFrom()) autoRadius = sliderRadius.getValueFrom();
                        if (autoRadius > sliderRadius.getValueTo()) {
                            sliderRadius.setValueTo(autoRadius + 1000.0f);
                        }
                        sliderRadius.setValue(autoRadius);
                    }

                    NominatimClient.reverse(SafeZonesActivity.this, location.getLatitude(), location.getLongitude(), results -> {
                        if (results != null && !results.isEmpty()) {
                            if (etZoneAddress != null) {
                                etZoneAddress.setText(results.get(0).displayName);
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Could not get GPS fix. Make sure Location is on.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }
}
