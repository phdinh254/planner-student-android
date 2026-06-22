package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.PlanCategoryListActivity;
import com.example.personalplanner.activity.LoginActivity;
import com.example.personalplanner.data.local.DatabaseHelper;
import com.example.personalplanner.data.model.User;
import com.example.personalplanner.utils.SessionManager;
import com.example.personalplanner.utils.ThemeManager;
import com.google.android.material.chip.ChipGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private TextView txtProfileUsername;
    private TextView txtProfileEmail;
    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_simplified, container, false);
        txtProfileUsername = view.findViewById(R.id.txtProfileUsername);
        txtProfileEmail = view.findViewById(R.id.txtProfileEmail);
        View btnManageCategories = view.findViewById(R.id.btnManageCategories);
        View btnLogout = view.findViewById(R.id.btnLogout);
        sessionManager = new SessionManager(requireContext());
        databaseHelper = new DatabaseHelper(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        setupThemeChips(view);
        btnManageCategories.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PlanCategoryListActivity.class)));
        btnLogout.setOnClickListener(v -> confirmLogout());
        loadProfile();
        return view;
    }

    private void setupThemeChips(View view) {
        ChipGroup chipGroupTheme = view.findViewById(R.id.chipGroupTheme);
        int mode = ThemeManager.getSavedMode(requireContext());
        if (mode == ThemeManager.MODE_LIGHT) {
            chipGroupTheme.check(R.id.chipThemeLight);
        } else if (mode == ThemeManager.MODE_DARK) {
            chipGroupTheme.check(R.id.chipThemeDark);
        } else {
            chipGroupTheme.check(R.id.chipThemeSystem);
        }
        chipGroupTheme.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = checkedIds.isEmpty() ? R.id.chipThemeSystem : checkedIds.get(0);
            if (checkedId == R.id.chipThemeLight) {
                ThemeManager.saveMode(requireContext(), ThemeManager.MODE_LIGHT);
            } else if (checkedId == R.id.chipThemeDark) {
                ThemeManager.saveMode(requireContext(), ThemeManager.MODE_DARK);
            } else {
                ThemeManager.saveMode(requireContext(), ThemeManager.MODE_SYSTEM);
            }
        });
    }

    private void loadProfile() {
        executorService.execute(() -> {
            User user = databaseHelper.getUser(sessionManager.getUserId());
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                txtProfileUsername.setText(user == null
                        ? sessionManager.getUsername() : user.getUsername());
                txtProfileEmail.setText(user == null
                        ? getString(R.string.profile_email_unavailable) : user.getEmail());
            });
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.confirm_logout_message)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        if (executorService != null) executorService.shutdownNow();
        super.onDestroyView();
    }
}
