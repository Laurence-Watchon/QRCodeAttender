package com.finals.appdev50;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Professor_settings_resetpassword extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSavePassword;
    private LinearLayout btnChangePasswordEmail;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ImageView hideshowpassold, hideshowpassnew, hideshowpassconfirm;
    private AlertDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_settings_resetpassword);

        noInternet.showPendulumDialog(this, getLifecycle());

        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        TextView tvMinCharacters = findViewById(R.id.tv_min_characters);
        TextView tvUppercase = findViewById(R.id.tv_uppercase);
        TextView tvNumber = findViewById(R.id.tv_number);
        TextView tvSpecialChar = findViewById(R.id.tv_special_char);
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String password = charSequence.toString();

                // Check if password meets conditions and update text color
                // At least 8 characters
                if (password.length() >= 8) {
                    tvMinCharacters.setTextColor(Color.GREEN);
                } else {
                    tvMinCharacters.setTextColor(Color.RED);
                }

                // At least one uppercase letter
                if (password.matches(".*[A-Z].*")) {
                    tvUppercase.setTextColor(Color.GREEN);
                } else {
                    tvUppercase.setTextColor(Color.RED);
                }

                // At least one number
                if (password.matches(".*\\d.*")) {
                    tvNumber.setTextColor(Color.GREEN);
                } else {
                    tvNumber.setTextColor(Color.RED);
                }

                // At least one special character
                if (password.matches(".*[!@#$%^&*(),.?\":{}|<>_].*")) {
                    tvSpecialChar.setTextColor(Color.GREEN);
                } else {
                    tvSpecialChar.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);

        hideshowpassold = findViewById(R.id.old_password_toggle);
        hideshowpassold.setOnClickListener(v -> {
            if(etOldPassword.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                etOldPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowpassold.setImageResource(R.drawable.hide);
            } else {
                etOldPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowpassold.setImageResource(R.drawable.visible);
            }
        });

        hideshowpassnew = findViewById(R.id.new_password_toggle);
        hideshowpassnew.setOnClickListener(v -> {
            if(etNewPassword.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowpassnew.setImageResource(R.drawable.hide);
            } else {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowpassnew.setImageResource(R.drawable.visible);
            }
        });

        hideshowpassconfirm = findViewById(R.id.confirm_password_toggle);
        hideshowpassconfirm.setOnClickListener(v -> {
            if(etConfirmNewPassword.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                etConfirmNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowpassconfirm.setImageResource(R.drawable.hide);
            } else {
                etConfirmNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowpassconfirm.setImageResource(R.drawable.visible);
            }
        });

        btnSavePassword = findViewById(R.id.btnSavePassword);
        btnChangePasswordEmail = findViewById(R.id.btnChangePasswordEmail);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        btnSavePassword.setOnClickListener(v -> changePassword());
        btnChangePasswordEmail.setOnClickListener(v -> showEmailDialog());

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmNewPassword.getText().toString();

        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("This field is required.");
            etOldPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("This field is required.");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmNewPassword.setError("This field is required.");
            etConfirmNewPassword.requestFocus();
            return;
        }

        if (!isValidPassword(newPassword)) {
            Toast.makeText(this, "Password too weak", Toast.LENGTH_SHORT).show();
            etNewPassword.setError("Enter strong password.");
            etNewPassword.requestFocus();
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmNewPassword.setError("Passwords do not match");
            etConfirmNewPassword.requestFocus();
            return;
        }

        if (newPassword.equals(oldPassword)) {
            etNewPassword.setError("New password cannot be the same as the old password");
            etNewPassword.requestFocus();
            return;
        }

        showProgressDialog("Saving new password...");

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(task1 -> {
                    hideProgressDialog();
                    if (task1.isSuccessful()) {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Password update failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                hideProgressDialog();
                etOldPassword.setError("Old password is incorrect");
            }
        });
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")     // At least one uppercase letter
                && password.matches(".*[0-9].*")     // At least one digit
                && password.matches(".*[!@#$%^&*(),_.?\":{}|<>].*"); // At least one special character
    }

    private void showEmailDialog() {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot, null);

        // Bind views from the custom layout
        EditText emailInput = dialogView.findViewById(R.id.etdialogEmail);
        Button cancelButton = dialogView.findViewById(R.id.btnCancel);
        Button resetButton = dialogView.findViewById(R.id.btnReset);

        // Create the AlertDialog
        AlertDialog emailDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false) // Prevent dialog dismissal by tapping outside
                .create();

        // Cancel button functionality
        cancelButton.setOnClickListener(v -> emailDialog.dismiss());

        // Reset button functionality
        resetButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            // Validate the email
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Please enter a valid email.");
                emailInput.requestFocus();
            } else if (!email.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                emailInput.setError("Email does not match your account.");
                emailInput.requestFocus();
            } else {
                // Send password reset email
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Reset email sent successfully.", Toast.LENGTH_SHORT).show();
                                emailDialog.dismiss(); // Close the dialog on success
                            } else {
                                Toast.makeText(this, "Failed to send reset email. Try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // Show the dialog
        emailDialog.show();
    }

    private void showProgressDialog(String message) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // Prevent dismissing the dialog by outside touches

        // Create a container layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        // Add a ProgressBar to the layout
        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, 0, 30, 0);
        progressBar.setLayoutParams(progressParams);
        layout.addView(progressBar);

        // Add a TextView for the message
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(Color.BLACK);
        layout.addView(textView);

        // Set the custom layout in the dialog
        builder.setView(layout);

        // Create and show the dialog
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void hideKeyboardAndClearFocus(View view) {
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Clear focus from the currently focused view
        view.clearFocus();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Get the currently focused view
        View v = getCurrentFocus();

        if (v instanceof EditText) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            float x = event.getRawX() + v.getLeft() - location[0];
            float y = event.getRawY() + v.getTop() - location[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom()) {
                hideKeyboardAndClearFocus(v); // Hide keyboard and clear focus
            }
        }
        return super.dispatchTouchEvent(event);
    }
}