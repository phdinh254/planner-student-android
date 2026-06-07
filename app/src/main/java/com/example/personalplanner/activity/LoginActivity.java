package com.example.personalplanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.database.DatabaseHelper;
import com.example.personalplanner.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnLogin;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            openMainScreen();
            return;
        }

        setContentView(R.layout.activity_login);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView txtGoToRegister = findViewById(R.id.txtGoToRegister);
        databaseHelper = new DatabaseHelper(this);

        btnLogin.setOnClickListener(v -> loginUser());
        txtGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (username.isEmpty()) {
            edtUsername.setError(getString(R.string.error_username_required));
            edtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError(getString(R.string.error_password_required));
            edtPassword.requestFocus();
            return;
        }

        setLoading(true);
        executorService.execute(() -> {
            int userId = databaseHelper.loginUser(username, password);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                if (userId == -1) {
                    edtPassword.setError(getString(R.string.error_login_invalid));
                    edtPassword.requestFocus();
                    return;
                }
                sessionManager.saveLoginSession(userId, username);
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                openMainScreen();
            });
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.loading : R.string.login);
        edtUsername.setEnabled(!loading);
        edtPassword.setEnabled(!loading);
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
