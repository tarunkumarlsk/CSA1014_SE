package com.example.kidshield.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.kidshield.R;
import com.example.kidshield.RoleActivity;
import com.example.kidshield.models.ParentProfile;
import com.example.kidshield.models.UpdateParentProfileRequest;
import com.example.kidshield.network.BackendManager;
import com.example.kidshield.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class ParentProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone;
    private TextInputEditText etName, etEmail, etPhone;
    private View cardView, cardEdit;
    private int parentId;
    private ParentProfile cachedProfile;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentId = SessionManager.getInstance(requireContext()).getParentId();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // View mode
        tvName  = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        cardView = view.findViewById(R.id.card_profile_view);
        cardEdit = view.findViewById(R.id.card_profile_edit);

        // Edit mode
        etName  = view.findViewById(R.id.et_edit_name);
        etEmail = view.findViewById(R.id.et_edit_email);
        etPhone = view.findViewById(R.id.et_edit_phone);
        
        TextInputLayout tilAddress = view.findViewById(R.id.til_edit_address);
        if (tilAddress != null) {
            tilAddress.setEndIconOnClickListener(v -> acquireCurrentLocation());
        }

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> showEditMode(true));
        view.findViewById(R.id.btn_cancel_edit).setOnClickListener(v -> showEditMode(false));
        view.findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            SessionManager.getInstance(requireContext()).clearSession();
            Intent intent = new Intent(requireContext(), RoleActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Pre-fill from local cache immediately, then refresh from backend
        prefillFromCache();
        loadProfileFromBackend();
    }

    private void prefillFromCache() {
        SessionManager s = SessionManager.getInstance(requireContext());
        String name  = s.getName();
        String email = s.getEmail();
        String phone = s.getPhone();
        if (name  != null && !name.isEmpty())  tvName.setText(name);
        if (email != null && !email.isEmpty()) tvEmail.setText(email);
        if (phone != null && !phone.isEmpty()) tvPhone.setText(phone);
        else tvPhone.setText("Not set");
    }

    private void loadProfileFromBackend() {
        if (parentId == -1) return;
        BackendManager.getParentProfile(parentId, new BackendManager.ApiCallback<ParentProfile>() {
            @Override
            public void onSuccess(ParentProfile result) {
                if (!isAdded()) return;
                cachedProfile = result;
                tvName.setText(isEmpty(result.getName())   ? "—" : result.getName());
                tvEmail.setText(isEmpty(result.getEmail()) ? "—" : result.getEmail());
                tvPhone.setText(isEmpty(result.getPhone()) ? "Not set" : result.getPhone());
                
                TextView tvAddress = getView().findViewById(R.id.tv_profile_address);
                if (tvAddress != null) {
                    tvAddress.setText(isEmpty(result.getHomeAddress()) ? "Not set" : result.getHomeAddress());
                }

                // Also update local cache
                SessionManager s = SessionManager.getInstance(requireContext());
                if (!isEmpty(result.getName()))  s.saveParentSession(parentId, result.getName());
                if (!isEmpty(result.getEmail())) s.saveParentEmail(result.getEmail());
                if (!isEmpty(result.getPhone())) s.saveParentPhone(result.getPhone());
                if (result.getHomeLatitude() != null && result.getHomeLongitude() != null) {
                    s.saveHomeLocation(result.getHomeLatitude(), result.getHomeLongitude());
                }
            }
            @Override public void onError(String e) { /* show cached values */ }
        });
    }

    private void showEditMode(boolean edit) {
        cardView.setVisibility(edit ? View.GONE : View.VISIBLE);
        cardEdit.setVisibility(edit ? View.VISIBLE : View.GONE);
        if (edit && cachedProfile != null) {
            etName.setText(cachedProfile.getName());
            etEmail.setText(cachedProfile.getEmail());
            etPhone.setText(isEmpty(cachedProfile.getPhone()) ? "" : cachedProfile.getPhone());
            TextInputEditText etAddress = getView().findViewById(R.id.et_edit_address);
            if (etAddress != null) {
                etAddress.setText(isEmpty(cachedProfile.getHomeAddress()) ? "" : cachedProfile.getHomeAddress());
            }
        }
    }

    private void saveProfile() {
        String name    = etName.getText() != null ? etName.getText().toString().trim()  : "";
        String email   = etEmail.getText() != null ? etEmail.getText().toString().trim()  : "";
        String phone   = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        TextInputEditText etAddress = getView().findViewById(R.id.et_edit_address);
        String address = etAddress != null && etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (parentId == -1) return;

        // Disable button during network call
        getView().findViewById(R.id.btn_save_profile).setEnabled(false);

        if (!address.isEmpty()) {
            Toast.makeText(requireContext(), "Resolving address...", Toast.LENGTH_SHORT).show();
            com.example.kidshield.network.NominatimClient.search(requireContext(), address, results -> {
                if (results != null && !results.isEmpty()) {
                    double lat = results.get(0).lat;
                    double lng = results.get(0).lon;
                    sendProfileUpdate(name, email, phone, address, lat, lng);
                } else {
                    Toast.makeText(requireContext(), "Could not resolve address. Using default location.", Toast.LENGTH_SHORT).show();
                    sendProfileUpdate(name, email, phone, address, 0.0, 0.0);
                }
            });
        } else {
            sendProfileUpdate(name, email, phone, null, null, null);
        }
    }

    private void acquireCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Toast.makeText(requireContext(), "🛰️ GPS: Acquiring location...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    com.example.kidshield.network.NominatimClient.reverse(requireContext(), location.getLatitude(), location.getLongitude(), results -> {
                        if (results != null && !results.isEmpty()) {
                            TextInputEditText et = getView().findViewById(R.id.et_edit_address);
                            if (et != null) {
                                et.setText(results.get(0).displayName);
                                Toast.makeText(requireContext(), "Location acquired!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), "Could not get GPS fix. Try moving near a window.", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void sendProfileUpdate(String name, String email, String phone, String address, Double lat, Double lng) {
        BackendManager.updateParentProfile(parentId, name, email, phone, address, lat, lng,
                new BackendManager.ApiCallback<ParentProfile>() {
            @Override
            public void onSuccess(ParentProfile result) {
                if (!isAdded()) return;
                cachedProfile = result;
                tvName.setText(result.getName());
                tvEmail.setText(result.getEmail());
                tvPhone.setText(isEmpty(result.getPhone()) ? "Not set" : result.getPhone());
                TextView tvAddress = getView().findViewById(R.id.tv_profile_address);
                if (tvAddress != null) {
                    tvAddress.setText(isEmpty(result.getHomeAddress()) ? "Not set" : result.getHomeAddress());
                }

                SessionManager s = SessionManager.getInstance(requireContext());
                s.saveParentSession(parentId, result.getName());
                s.saveParentEmail(result.getEmail());
                if (!isEmpty(result.getPhone())) s.saveParentPhone(result.getPhone());
                if (result.getHomeLatitude() != null && result.getHomeLongitude() != null) {
                    s.saveHomeLocation(result.getHomeLatitude(), result.getHomeLongitude());
                }
                
                showEditMode(false);
                getView().findViewById(R.id.btn_save_profile).setEnabled(true);
                Toast.makeText(requireContext(), "Profile updated ✓", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String err) {
                if (isAdded()) {
                    getView().findViewById(R.id.btn_save_profile).setEnabled(true);
                    Toast.makeText(requireContext(), "Update failed: " + err, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
