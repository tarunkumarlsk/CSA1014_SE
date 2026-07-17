package com.example.kidshield.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.kidshield.repositories.ChildRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ChildRepository repository;

    public ViewModelFactory(ChildRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(repository);
        } else if (modelClass.isAssignableFrom(MapViewModel.class)) {
            return (T) new MapViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
