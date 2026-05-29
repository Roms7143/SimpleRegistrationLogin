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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.activity.OnBackPressedCallback;

import java.util.Calendar;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting, tvUserName, tvBalance, tvIncome, tvExpenses;
    private MaterialCardView cvAddIncome, cvAddExpense, cvViewHistory;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private SharedPreferences prefs;

    // Listeners — kept as fields so we can remove them in onStop
    private ValueEventListener incomeListener;
    private ValueEventListener expenseListener;

    private double totalIncome   = 0.0;
    private double totalExpenses = 0.0;

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

        String uid = mAuth.getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        bindViews();
        loadUserData();
        setupBottomNav();
        setupQuickActions();
        listenToBalanceUpdates();
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

        String savedName = prefs.getString(KEY_NAME, null);
        if (savedName != null) {
            tvUserName.setText(savedName);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null
                && !user.getDisplayName().isEmpty()) {
            tvUserName.setText(user.getDisplayName());
            prefs.edit().putString(KEY_NAME, user.getDisplayName()).apply();
        }
    }

    // -----------------------------------------------------------------------
    // Firebase Realtime Balance Listener
    // -----------------------------------------------------------------------

    private void listenToBalanceUpdates() {
        // Listen to income changes
        incomeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                totalIncome = 0.0;
                for (DataSnapshot item : snapshot.getChildren()) {
                    Double amount = item.child("amount").getValue(Double.class);
                    if (amount != null) totalIncome += amount;
                }
                updateBalanceUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HomeActivity.this,
                        "Failed to load income: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Listen to expense changes
        expenseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                totalExpenses = 0.0;
                for (DataSnapshot item : snapshot.getChildren()) {
                    Double amount = item.child("amount").getValue(Double.class);
                    if (amount != null) totalExpenses += amount;
                }
                updateBalanceUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(HomeActivity.this,
                        "Failed to load expenses: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Attach listeners
        dbRef.child("income").addValueEventListener(incomeListener);
        dbRef.child("expenses").addValueEventListener(expenseListener);
    }

    /** Updates the balance, income, and expense TextViews */
    private void updateBalanceUI() {
        double balance = totalIncome - totalExpenses;

        tvBalance.setText(String.format(Locale.getDefault(), "₱%.2f", balance));
        tvIncome.setText(String.format(Locale.getDefault(), "₱%.2f", totalIncome));
        tvExpenses.setText(String.format(Locale.getDefault(), "₱%.2f", totalExpenses));

        // Turn balance red if negative
        if (balance < 0) {
            tvBalance.setTextColor(0xFF8B0000);
        } else {
            tvBalance.setTextColor(0xFF1A1C1B);
        }
    }

    // -----------------------------------------------------------------------
    // Remove listeners when screen is not visible to save memory
    // -----------------------------------------------------------------------

    @Override
    protected void onStop() {
        super.onStop();
        if (incomeListener != null)
            dbRef.child("income").removeEventListener(incomeListener);
        if (expenseListener != null)
            dbRef.child("expenses").removeEventListener(expenseListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-attach listeners when coming back to screen
        if (incomeListener != null)
            dbRef.child("income").addValueEventListener(incomeListener);
        if (expenseListener != null)
            dbRef.child("expenses").addValueEventListener(expenseListener);
    }

    // -----------------------------------------------------------------------
    // Bottom Nav & Quick Actions
    // -----------------------------------------------------------------------

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
        cvAddIncome.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(HomeActivity.this, AddIncomeActivity.class));
        });

        cvAddExpense.setOnClickListener(v -> {
            animateCard(v);
            startActivity(new Intent(HomeActivity.this, AddExpenseActivity.class));
        });

        cvViewHistory.setOnClickListener(v -> {
            animateCard(v);
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
}