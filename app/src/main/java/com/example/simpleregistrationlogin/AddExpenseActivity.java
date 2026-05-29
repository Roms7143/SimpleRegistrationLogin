package com.example.simpleregistrationlogin;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    private TextInputLayout tilAmount, tilCategory, tilDate, tilNotes;
    private TextInputEditText etAmount, etDate, etNotes;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSaveExpense;
    private TextView tvBack, tvAmountDisplay;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "SpendWisePrefs";
    private final Calendar calendar = Calendar.getInstance();

    // Expense categories
    private final String[] CATEGORIES = {
            "Food & Dining", "Transportation", "Shopping",
            "Entertainment", "Bills & Utilities", "Health",
            "Education", "Rent", "Groceries", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_expense);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Firebase Realtime Database reference
        String uid = mAuth.getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("expenses");

        bindViews();
        setupCategoryDropdown();
        setupDatePicker();
        setupAmountPreview();
        setClickListeners();

        // Set today's date by default
        updateDateField();
    }

    private void bindViews() {
        tilAmount       = findViewById(R.id.tilAmount);
        tilCategory     = findViewById(R.id.tilCategory);
        tilDate         = findViewById(R.id.tilDate);
        tilNotes        = findViewById(R.id.tilNotes);
        etAmount        = findViewById(R.id.etAmount);
        etDate          = findViewById(R.id.etDate);
        etNotes         = findViewById(R.id.etNotes);
        actvCategory    = findViewById(R.id.actvCategory);
        btnSaveExpense  = findViewById(R.id.btnSaveExpense);
        tvBack          = findViewById(R.id.tvBack);
        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        actvCategory.setAdapter(adapter);
        actvCategory.setText(CATEGORIES[0], false); // Default: Food & Dining
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> showDatePicker());
        tilDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            updateDateField();
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    /** Updates the amount display in the header as user types */
    private void setupAmountPreview() {
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    tvAmountDisplay.setText("₱0.00");
                } else {
                    try {
                        double amount = Double.parseDouble(val);
                        tvAmountDisplay.setText(String.format(Locale.getDefault(), "₱%.2f", amount));
                    } catch (NumberFormatException e) {
                        tvAmountDisplay.setText("₱0.00");
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setClickListeners() {
        tvBack.setOnClickListener(v -> finish());

        btnSaveExpense.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            handleSaveExpense();
        });
    }

    // -----------------------------------------------------------------------
    // Save to Firebase Realtime Database
    // -----------------------------------------------------------------------

    private void handleSaveExpense() {
        tilAmount.setError(null);
        tilCategory.setError(null);

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String category  = actvCategory.getText().toString().trim();
        String date      = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String notes     = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        // Validate
        if (amountStr.isEmpty()) {
            tilAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                tilAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            tilAmount.setError("Enter a valid amount");
            return;
        }

        if (category.isEmpty()) {
            tilCategory.setError("Please select a category");
            return;
        }

        // Disable button during save
        btnSaveExpense.setEnabled(false);
        btnSaveExpense.setText("Saving...");
        btnSaveExpense.setAlpha(0.7f);

        // Build expense record
        String expenseId = dbRef.push().getKey();
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("id", expenseId);
        expenseData.put("amount", amount);
        expenseData.put("category", category);
        expenseData.put("date", date);
        expenseData.put("notes", notes);
        expenseData.put("timestamp", System.currentTimeMillis());

        // Save to Firebase
        dbRef.child(expenseId).setValue(expenseData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            String.format(Locale.getDefault(), "₱%.2f expense saved!", amount),
                            Toast.LENGTH_SHORT).show();
                    finish(); // Go back to home
                })
                .addOnFailureListener(e -> {
                    btnSaveExpense.setEnabled(true);
                    btnSaveExpense.setText("SAVE EXPENSE");
                    btnSaveExpense.setAlpha(1.0f);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}