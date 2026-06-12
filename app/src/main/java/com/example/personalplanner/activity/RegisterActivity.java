package com.example.personalplanner.activity;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.personalplanner.R;
import com.example.personalplanner.data.local.DatabaseHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnRegister;
    private DatabaseHelper databaseHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView txtGoToLogin = findViewById(R.id.txtGoToLogin);
        databaseHelper = new DatabaseHelper(this);

        btnRegister.setOnClickListener(v -> registerUser());
        txtGoToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (username.length() < 3) {
            edtUsername.setError(getString(R.string.error_username_length));
            edtUsername.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError(getString(R.string.error_email_invalid));
            edtEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError(getString(R.string.error_password_length));
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.error_password_mismatch));
            edtConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);
        executorService.execute(() -> {
            boolean usernameExists = databaseHelper.checkUsernameExists(username);
            boolean emailExists = !usernameExists && databaseHelper.checkEmailExists(email);
            boolean registered = !usernameExists
                    && !emailExists
                    && databaseHelper.registerUser(username, email, password);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                if (usernameExists) {
                    edtUsername.setError(getString(R.string.error_username_exists));
                    edtUsername.requestFocus();
                } else if (emailExists) {
                    edtEmail.setError(getString(R.string.error_email_exists));
                    edtEmail.requestFocus();
                } else if (registered) {
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? R.string.loading : R.string.register);
        edtUsername.setEnabled(!loading);
        edtEmail.setEnabled(!loading);
        edtPassword.setEnabled(!loading);
        edtConfirmPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
