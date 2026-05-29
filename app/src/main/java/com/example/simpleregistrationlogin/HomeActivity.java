package com.example.simpleregistrationlogin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvBalance, tvIncome, tvExpenses;
    private MaterialCardView cvAddIncome, cvAddExpense, cvViewHistory;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    private static final String PREFS_NAME    = "SpendWisePrefs";
    private static final String KEY_NAME      = "display_name";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_REMEMBER  = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();
        loadUserData();
        setupBottomNav();
        setupQuickActions();
    }

    private void bindViews() {
        tvGreeting    = findViewById(R.id.tvGreeting);
        tvUserName    = findViewById(R.id.tvUserName);
        tvBalance     = findViewById(R.id.tvBalance);
        tvIncome      = findViewById(R.id.tvIncome);
        tvExpenses    = findViewById(R.id.tvExpenses);
        cvAddIncome   = findViewById(R.id.cvAddIncome);
        cvAddExpense  = findViewById(R.id.cvAddExpense);
        cvViewHistory = findViewById(R.id.cvViewHistory);
        bottomNav     = findViewById(R.id.bottomNav);
    }

    private void loadUserData() {
        tvGreeting.setText(getGreeting());

        // Load from SharedPreferences first (instant)
        String savedName = prefs.getString(KEY_NAME, null);
        if (savedName != null) {
            tvUserName.setText(savedName);
        }

        // Verify with Firebase and update if different
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty()) {
            tvUserName.setText(user.getDisplayName());
            prefs.edit().putString(KEY_NAME, user.getDisplayName()).apply();
        }
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_expenses) {
                Toast.makeText(this, "Expenses coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                showProfileOptions();
                return true;
            }
            return false;
        });
    }

    private void setupQuickActions() {
        // Add Income
        cvAddIncome.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(HomeActivity.this, AddIncomeActivity.class));
        });

        // Add Expense
        cvAddExpense.setOnClickListener(v -> {
            animateCard(v);
            // TODO: Navigate to AddExpenseActivity
            Toast.makeText(this, "Add Expense coming soon", Toast.LENGTH_SHORT).show();
        });

        // View History
        cvViewHistory.setOnClickListener(v -> {
            animateCard(v);
            // TODO: Navigate to HistoryActivity
            Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void animateCard(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();
    }

    private void showProfileOptions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Profile")
                .setMessage("Logged in as: " + prefs.getString(KEY_NAME, "User"))
                .setPositiveButton("Log Out", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogout() {
        mAuth.signOut();
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .putBoolean(KEY_REMEMBER, false)
                .remove(KEY_NAME)
                .apply();

        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning,";
        else if (hour < 17) return "Good afternoon,";
        else return "Good evening,";
    }

    @Override
    public void onBackPressed() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Do you want to exit SpendWise?")
                .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                .setNegativeButton("Cancel", null)
                .show();
    }
}