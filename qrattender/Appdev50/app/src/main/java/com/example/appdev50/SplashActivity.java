package com.example.appdev50;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        noInternet.showPendulumDialog(this, getLifecycle());
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                checkUserRole(); // Proceed to role-based homepage
            } else {
                // If email is not verified, redirect to ValidateEmail activity
                Intent intent = new Intent(SplashActivity.this, ValidateEmail.class);
                startActivity(intent);
                finish();
            }
        } else {
            // If no user is logged in, go to the login page
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void checkUserRole() {
        // Fetch user role from SharedPreferences
        String userRole = sharedPreferences.getString("userRole", null);

        if (userRole != null) {
            // Redirect user based on their role
            if (userRole.equalsIgnoreCase("Student")) {
                Intent intent1 = new Intent(SplashActivity.this, HomepageStudent.class);
                startActivity(intent1);
                finish();
            } else if (userRole.equalsIgnoreCase("Professor")) {
                Intent intent = new Intent(SplashActivity.this, HomepageProfessor.class);
                startActivity(intent);
                finish();
            } else {
                // Handle invalid role if needed (optional)
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        } else {
            // If the role is not found in SharedPreferences, go to the login page
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}