package com.finals.appdev50;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ValidateEmail extends AppCompatActivity {
    private TextView emailInfo;
    private Button sendEmail, backToLogin;
    FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validate_email);

        noInternet.showPendulumDialog(this, getLifecycle());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailInfo = findViewById(R.id.tvEmailInfo);
        sendEmail = findViewById(R.id.btnSendEmail);
        backToLogin = findViewById(R.id.btnBackToLogin);

        if (user != null) {
            emailInfo.setText("Email not verified: " + user.getEmail());
        }

        sendEmail.setOnClickListener(v -> {
            if (user != null) {
                user.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ValidateEmail.this,
                                        "Verification email sent. Please check your inbox.",
                                        Toast.LENGTH_SHORT).show();

                                // Disable button and start countdown
                                sendEmail.setEnabled(false);
                                startCountdown();
                            } else {
                                Toast.makeText(ValidateEmail.this,
                                        "Failed to send verification email.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


        backToLogin.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent loginIntent = new Intent(ValidateEmail.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        });

        Button refreshButton = findViewById(R.id.btnRefresh);
        Button goToGmailButton = findViewById(R.id.btnGoToGmail);

        refreshButton.setOnClickListener(v -> {
            if (user != null) {
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (user.isEmailVerified()) {
                            String uid = user.getUid();

                            // Fetch user role from the database
                            mDatabase.child("Registered Users").child(uid).child("role")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                String userRole = snapshot.getValue(String.class);

                                                if (userRole != null) {
                                                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putString("userRole", userRole); // Save role in SharedPreferences
                                                    editor.apply();

                                                    // Redirect based on user role
                                                    if (userRole.equals("Student")) {
                                                        Intent intent1 = new Intent(ValidateEmail.this, HomepageStudent.class);
                                                        startActivity(intent1);
                                                        finish();
                                                    } else if (userRole.equals("Professor")) {
                                                        Intent intent2 = new Intent(ValidateEmail.this, HomepageProfessor.class);
                                                        startActivity(intent2);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(ValidateEmail.this, "Invalid user role in database.", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Toast.makeText(ValidateEmail.this, "User role not found in database.", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(ValidateEmail.this, "No role information for user in database.", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(ValidateEmail.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(ValidateEmail.this, "Email is still not verified.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ValidateEmail.this, "Failed to refresh user info.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        goToGmailButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensures Gmail opens as a new task
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No email application found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountdown() {
        new CountDownTimer(180000, 1000) { // 3 minutes (180 seconds)
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                sendEmail.setText("Wait " + seconds + "s to resend");
            }

            @Override
            public void onFinish() {
                sendEmail.setText("Resend Verification Email");
                sendEmail.setEnabled(true);
            }
        }.start();
    }
}