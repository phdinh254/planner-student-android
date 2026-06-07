package com.example.personalplanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.personalplanner.R;
import com.example.personalplanner.activity.LoginActivity;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.model.User;
import com.example.personalplanner.utils.SessionManager;

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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        txtProfileUsername = view.findViewById(R.id.txtProfileUsername);
        txtProfileEmail = view.findViewById(R.id.txtProfileEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        sessionManager = new SessionManager(requireContext());
        databaseHelper = new DatabaseHelper(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        btnLogout.setOnClickListener(v -> confirmLogout());
        loadProfile();
        return view;
    }

    private void loadProfile() {
        executorService.execute(() -> {
            User user = databaseHelper.getUser(sessionManager.getUserId());
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                if (user == null) {
                    txtProfileUsername.setText(sessionManager.getUsername());
                    txtProfileEmail.setText(R.string.profile_email_unavailable);
                    return;
                }
                txtProfileUsername.setText(user.getUsername());
                txtProfileEmail.setText(user.getEmail());
            });
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.confirm_logout_message)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.onDestroyView();
    }
}
