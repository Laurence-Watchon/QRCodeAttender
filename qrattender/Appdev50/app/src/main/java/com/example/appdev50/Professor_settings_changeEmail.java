package com.example.appdev50;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class Professor_settings_changeEmail extends AppCompatActivity {

//    private FirebaseAuth mAuth;
//    private FirebaseUser user;
//    private TextView tvauthenticated;
//    private String oldemail, newemail, userpassword;
//    private Button updateemail;
//    private EditText newEmail, oldPassword;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private TextView tvauthenticated;
    private String oldemail, newemail, userpassword;
    private Button updateemail;
    private EditText newEmail, oldPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_settings_change_email);

        noInternet.showPendulumDialog(this, getLifecycle());
        newEmail = findViewById(R.id.etnewemail);
        oldPassword = findViewById(R.id.etpassword);
        tvauthenticated = findViewById(R.id.tvisauthenticated);
        updateemail = findViewById(R.id.btnupdateemail);

        updateemail.setEnabled(false);
        newEmail.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        oldemail = user != null ? user.getEmail() : "";
        TextView oldEmail = findViewById(R.id.etcurrentemail);
        oldEmail.setText(oldemail);

        if (user == null || TextUtils.isEmpty(oldemail)) {
            Toast.makeText(this, "Something went wrong, user details not available", Toast.LENGTH_SHORT).show();
        } else {
            reauthenticate(user);
        }
    }

    private void reauthenticate(FirebaseUser user) {
        Button verify = findViewById(R.id.btnauthenticate);
        verify.setOnClickListener(v -> {
            userpassword = oldPassword.getText().toString();

            if (TextUtils.isEmpty(userpassword)) {
                oldPassword.setError("This field is required");
                oldPassword.requestFocus();
            } else {
                AuthCredential credential = EmailAuthProvider.getCredential(oldemail, userpassword);

                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Professor_settings_changeEmail.this, "Password has been verified, you can update email now", Toast.LENGTH_SHORT).show();
                        tvauthenticated.setText("You are authenticated. You can update email now.");
                        newEmail.setEnabled(true);
                        oldPassword.setEnabled(false);
                        verify.setEnabled(false);
                        updateemail.setEnabled(true);

                        updateemail.setBackgroundTintList(ContextCompat.getColorStateList(Professor_settings_changeEmail.this, R.color.dark_green));
                        setupUpdateEmailClickListener();
                    } else {
                        handleException(task.getException());
                    }
                });
            }
        });
    }

    private void setupUpdateEmailClickListener() {
        updateemail.setOnClickListener(view -> {
            newemail = newEmail.getText().toString();

            // Validate new email
            if (TextUtils.isEmpty(newemail)) {
                newEmail.setError("Please provide a new email");
                newEmail.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(newemail).matches()) {
                newEmail.setError("Please provide a valid email");
                newEmail.requestFocus();
            } else if (newemail.equals(oldemail)) {
                newEmail.setError("Please provide a different email");
                newEmail.requestFocus();
            } else {
                sendVerificationToNewEmail();
            }
        });
    }

    private void sendVerificationToNewEmail() {
        FirebaseAuth.getInstance().sendSignInLinkToEmail(newemail, buildActionCodeSettings())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email sent to " + newemail, Toast.LENGTH_SHORT).show();
                        monitorNewEmailVerification();
                    } else {
                        handleException(task.getException());
                    }
                });
    }

    private ActionCodeSettings buildActionCodeSettings() {
        return ActionCodeSettings.newBuilder()
                .setUrl("https://example.com/verifyemail")  // Generic placeholder URL
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.example.appdev50", true, null)
                .build();
    }

    private void monitorNewEmailVerification() {
        new CountDownTimer(300000, 5000) {  // Check every 5 seconds for 5 minutes
            @Override
            public void onTick(long millisUntilFinished) {
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(newemail)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().getSignInMethods().size() > 0) {
                                setNewEmailAsPrimary();
                                cancel();  // Stop the timer
                            }
                        });
            }

            @Override
            public void onFinish() {
                Toast.makeText(Professor_settings_changeEmail.this, "Verification timed out. Try again.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void setNewEmailAsPrimary() {
        user.updateEmail(newemail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Email has been updated successfully.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, Professor_settings.class);
                startActivity(intent);
                finish();
            } else {
                handleException(task.getException());
            }
        });
    }

    private void handleException(Exception exception) {
        if (exception != null) {
            Toast.makeText(this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("UpdateEmailError", "Error updating email", exception);
        }
    }
}