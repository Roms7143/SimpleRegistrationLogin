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
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnGoogle;
    private CheckBox cbTerms;
    private TextView tvLoginRedirect;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "SpendWisePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();
        styleLoginText();
        setClickListeners();
    }

    private void bindViews() {
        tilFullName        = findViewById(R.id.tilFullName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilPhone           = findViewById(R.id.tilPhone);
        tilPassword        = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etPhone            = findViewById(R.id.etPhone);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        btnRegister        = findViewById(R.id.btnRegister);
        btnGoogle          = findViewById(R.id.btnGoogle);
        cbTerms            = findViewById(R.id.cbTerms);
        tvLoginRedirect    = findViewById(R.id.tvLoginRedirect);
    }

    private void setClickListeners() {
        btnRegister.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            handleRegister();
        });

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google sign-up coming soon", Toast.LENGTH_SHORT).show()
        );

        tvLoginRedirect.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    // -----------------------------------------------------------------------
    // Firebase Registration
    // -----------------------------------------------------------------------

    private void handleRegister() {
        clearErrors();

        String fullName = getTextFrom(etFullName);
        String email    = getTextFrom(etEmail);
        String phone    = getTextFrom(etPhone);
        String password = getTextFrom(etPassword);
        String confirm  = getTextFrom(etConfirmPassword);

        if (!validateInputs(fullName, email, phone, password, confirm)) return;

        setLoadingState(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    // Save display name to Firebase profile
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();

                    authResult.getUser().updateProfile(profileUpdate)
                            .addOnSuccessListener(unused -> {

                                // Save user info locally via SharedPreferences
                                prefs.edit()
                                        .putString("saved_email", email)
                                        .putString("display_name", fullName)
                                        .putString("phone", phone)
                                        .apply();

                                // Send verification email
                                authResult.getUser().sendEmailVerification()
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(this,
                                                    "Account created! Check your email to verify.",
                                                    Toast.LENGTH_LONG).show();
                                            goToLogin();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this,
                                                    "Account created! (Verification email failed)",
                                                    Toast.LENGTH_LONG).show();
                                            goToLogin();
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    handleFirebaseError(e.getMessage());
                });
    }

    private void handleFirebaseError(String message) {
        if (message == null) {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.contains("email address is already in use") ||
                message.contains("email-already-in-use")) {
            tilEmail.setError("An account already exists with this email");
            shakeView(tilEmail);
        } else if (message.contains("badly formatted") || message.contains("invalid-email")) {
            tilEmail.setError("Invalid email format");
            shakeView(tilEmail);
        } else if (message.contains("password") || message.contains("weak-password")) {
            tilPassword.setError("Password is too weak");
            shakeView(tilPassword);
        } else {
            Toast.makeText(this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
        }
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    // -----------------------------------------------------------------------
    // UI Helpers
    // -----------------------------------------------------------------------

    private void setLoadingState(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Creating account..." : "CREATE ACCOUNT");
        btnRegister.setAlpha(isLoading ? 0.7f : 1.0f);
    }

    private void shakeView(View view) {
        view.animate()
                .translationX(-12f).setDuration(60)
                .withEndAction(() -> view.animate().translationX(12f).setDuration(60)
                        .withEndAction(() -> view.animate().translationX(-8f).setDuration(50)
                                .withEndAction(() -> view.animate().translationX(0f).setDuration(50).start())
                                .start()).start()).start();
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private boolean validateInputs(String fullName, String email, String phone,
                                   String password, String confirm) {
        boolean isValid = true;

        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            shakeView(tilFullName);
            isValid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            shakeView(tilEmail);
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            shakeView(tilEmail);
            isValid = false;
        }
        if (phone.isEmpty()) {
            tilPhone.setError("Phone number is required");
            shakeView(tilPhone);
            isValid = false;
        } else if (phone.length() < 10) {
            tilPhone.setError("Enter a valid phone number");
            shakeView(tilPhone);
            isValid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            shakeView(tilPassword);
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            shakeView(tilPassword);
            isValid = false;
        }
        if (confirm.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            shakeView(tilConfirmPassword);
            isValid = false;
        } else if (!confirm.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            shakeView(tilConfirmPassword);
            isValid = false;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms & Privacy Policy",
                    Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void styleLoginText() {
        String full      = "Already have an account? Log In";
        String highlight = "Log In";
        int start        = full.indexOf(highlight);
        int end          = start + highlight.length();
        SpannableString spannable = new SpannableString(full);
        spannable.setSpan(new ForegroundColorSpan(0xFF00665C), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLoginRedirect.setText(spannable);
    }
}