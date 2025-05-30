package com.finals.appdev50;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password, emailbox;
    private ProgressBar progressbar;
    FirebaseAuth mAuth;
    private Button login;
    private TextView tvregister, forgotpassword;
    private DatabaseReference mDatabase;
    private ImageView hideshowpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        noInternet.showPendulumDialog(this, getLifecycle());

        mAuth= FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        progressbar = findViewById(R.id.pbProgressBar);
        tvregister = findViewById(R.id.tvRegister);
        tvregister.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        hideshowpass = findViewById(R.id.showpassword);
        hideshowpass.setImageResource(R.drawable.hide);
        hideshowpass.setOnClickListener(v -> {
            if(password.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowpass.setImageResource(R.drawable.hide);
            } else {
                password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowpass.setImageResource(R.drawable.visible);
            }
        });

        forgotpassword = findViewById(R.id.forgotPassword);
        forgotpassword.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            View dialogview = getLayoutInflater().inflate(R.layout.dialog_forgot, null);
            emailbox = dialogview.findViewById(R.id.etdialogEmail);

            builder.setView(dialogview);
            AlertDialog dialog = builder.create();

            dialogview.findViewById(R.id.btnReset).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        String useremail = emailbox.getText().toString();

                        if (TextUtils.isEmpty(useremail) || !Patterns.EMAIL_ADDRESS.matcher(useremail).matches()) {
                            Toast.makeText(LoginActivity.this, "Enter your Valid Email Address", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mAuth.sendPasswordResetEmail(useremail).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "Check your Email inbox or Spam", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Unable to send email, Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (Exception ex){
                        Toast.makeText(LoginActivity.this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            dialogview.findViewById(R.id.btnCancel).setOnClickListener(v1 -> {
                dialog.dismiss();  // Dismiss the dialog when cancel is clicked
            });

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            dialog.show();
        });

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        login = findViewById(R.id.btnlogin);
        login.setOnClickListener(v -> {
            String emailtext, passwordtext;
            emailtext = email.getText().toString().trim();
            passwordtext = password.getText().toString().trim();

            progressbar.setVisibility(View.VISIBLE);

            if(TextUtils.isEmpty(emailtext)){
                progressbar.setVisibility(View.GONE);
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show();
                email.setError("Please enter valid email");
                email.requestFocus();
                return;
            }

            if(TextUtils.isEmpty(passwordtext)){
                progressbar.setVisibility(View.GONE);
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
                password.setError("Please fill password");
                password.requestFocus();
                return;
            }
            progressbar.setVisibility(View.VISIBLE);

            try{
                mAuth.signInWithEmailAndPassword(emailtext, passwordtext)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressbar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if(user != null){
                                        if (user.isEmailVerified()) {
                                            String uid = user.getUid();

                                            mDatabase.child("Registered Users").child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        String userRole = snapshot.getValue(String.class);
                                                        Log.d("UserRole", "Fetched role: " + userRole);
                                                        if (userRole != null && userRole.equalsIgnoreCase("Student")) {
                                                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                                            editor.putString("userRole", userRole);  // Save role in SharedPreferences
                                                            editor.apply();

                                                            Intent intent1 = new Intent(getApplicationContext(), HomepageStudent.class);
                                                            startActivity(intent1);
                                                            finish();
                                                        } else {
                                                            mAuth.signOut();
                                                            Toast.makeText(LoginActivity.this, "Please make sure you're logging in as a Student", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        Toast.makeText(LoginActivity.this, "User type not found in the database", Toast.LENGTH_SHORT).show();
                                                        mAuth.signOut(); // Sign out if role is not found
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Toast.makeText(LoginActivity.this,
                                                            "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            // Redirect to ValidateEmail activity if email is not verified
                                            Intent validateEmailIntent = new Intent(LoginActivity.this, ValidateEmail.class);
                                            startActivity(validateEmailIntent);
                                            finish();
                                        }

                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } catch (Exception e) {
                progressbar.setVisibility(View.GONE);  // Hide progress bar in case of an error
                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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