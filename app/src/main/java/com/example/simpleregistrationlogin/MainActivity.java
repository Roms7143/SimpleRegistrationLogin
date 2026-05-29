package com.example.simpleregistrationlogin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private TextView tvForgotPassword, tvSignUp;
    private CheckBox cbRememberMe;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    private static final String PREFS_NAME    = "SpendWisePrefs";
    private static final String KEY_EMAIL     = "saved_email";
    private static final String KEY_REMEMBER  = "remember_me";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_NAME      = "display_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();
        styleSignUpText();
        setClickListeners();
        restoreSession();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER, false);
        if (currentUser != null && rememberMe) {
            goToHome();
        }
    }

    private void bindViews() {
        tilEmail         = findViewById(R.id.tilEmail);
        tilPassword      = findViewById(R.id.tilPassword);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoogle        = findViewById(R.id.btnGoogle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp         = findViewById(R.id.tvSignUp);
        cbRememberMe     = findViewById(R.id.cbRememberMe);
    }

    private void setClickListeners() {
        btnLogin.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            handleLogin();
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google sign-in coming soon", Toast.LENGTH_SHORT).show()
        );

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    // -----------------------------------------------------------------------
    // Firebase Login
    // -----------------------------------------------------------------------

    private void handleLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email    = getTextFrom(etEmail);
        String password = getTextFrom(etPassword);

        if (!validateInputs(email, password)) {
            shakeView(tilEmail.getError() != null ? tilEmail : tilPassword);
            return;
        }

        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, authResult -> {
                    FirebaseUser user = authResult.getUser();

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(KEY_EMAIL, email);
                    editor.putBoolean(KEY_LOGGED_IN, true);
                    editor.putBoolean(KEY_REMEMBER, cbRememberMe.isChecked());

                    if (user != null && user.getDisplayName() != null) {
                        editor.putString(KEY_NAME, user.getDisplayName());
                    }
                    editor.apply();

                    String name = prefs.getString(KEY_NAME, "there");
                    Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    goToHome();
                })
                .addOnFailureListener(this, e -> {
                    setLoadingState(false);
                    handleFirebaseError(e.getMessage());
                });
    }

    private void handleFirebaseError(String message) {
        if (message == null) {
            Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.contains("no user record") || message.contains("user-not-found")) {
            tilEmail.setError("No account found with this email");
            shakeView(tilEmail);
        } else if (message.contains("password is invalid") || message.contains("wrong-password")) {
            tilPassword.setError("Incorrect password");
            shakeView(tilPassword);
        } else if (message.contains("blocked") || message.contains("too-many-requests")) {
            Toast.makeText(this, "Too many attempts. Try again later.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Login failed: " + message, Toast.LENGTH_LONG).show();
        }
    }

    // -----------------------------------------------------------------------
    // Forgot Password
    // -----------------------------------------------------------------------

    private void handleForgotPassword() {
        String email = getTextFrom(etEmail);
        if (email.isEmpty()) {
            tilEmail.setError("Enter your email first");
            shakeView(tilEmail);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            shakeView(tilEmail);
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show()
                );
    }

    // -----------------------------------------------------------------------
    // Session Restore
    // -----------------------------------------------------------------------

    private void restoreSession() {
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER, false);
        String savedEmail  = prefs.getString(KEY_EMAIL, null);

        if (rememberMe && savedEmail != null) {
            etEmail.setText(savedEmail);
            if (cbRememberMe != null) cbRememberMe.setChecked(true);
        }
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    private void goToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // -----------------------------------------------------------------------
    // UI Helpers
    // -----------------------------------------------------------------------

    private void setLoadingState(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Logging in..." : "LOG IN");
        btnLogin.setAlpha(isLoading ? 0.7f : 1.0f);
    }

    private void shakeView(View view) {
        view.animate()
                .translationX(-12f).setDuration(60)
                .withEndAction(() -> view.animate().translationX(12f).setDuration(60)
                        .withEndAction(() -> view.animate().translationX(-8f).setDuration(50)
                                .withEndAction(() -> view.animate().translationX(0f).setDuration(50).start())
                                .start()).start()).start();
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }
        return isValid;
    }

    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void styleSignUpText() {
        String full      = "Don't have an account? Sign Up";
        String highlight = "Sign Up";
        int start        = full.indexOf(highlight);
        int end          = start + highlight.length();
        SpannableString spannable = new SpannableString(full);
        spannable.setSpan(new ForegroundColorSpan(0xFF00665C), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSignUp.setText(spannable);
    }
}