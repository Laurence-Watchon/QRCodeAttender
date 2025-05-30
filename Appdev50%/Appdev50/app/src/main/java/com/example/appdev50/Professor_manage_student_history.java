package com.example.appdev50;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Professor_manage_student_history extends AppCompatActivity {

    String currentUserUID, classCode;
    SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private LinearLayout datesContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_manage_student_history);

        noInternet.showPendulumDialog(this, getLifecycle());

        datesContainer = findViewById(R.id.dateContainer);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserUID = currentUser.getUid(); // Get the UID of the current user
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        classCode = sharedPreferences.getString("classCode", null);

        loadDatesFromDatabase(currentUserUID, classCode);
    }

    private void loadDatesFromDatabase(String currentUserUID, String classCode) {
        DatabaseReference qrDataRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(currentUserUID)
                .child(classCode);

        qrDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                datesContainer.removeAllViews();
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    // Each dateSnapshot corresponds to a date
                    String date = dateSnapshot.getKey(); // The key is the date
                    addDateView(date);  // Add the date to the layout
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_student_history.this, "Failed to load dates.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addDateView(String date) {
        TextView dateView = new TextView(this);
        dateView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dateView.setBackground(ContextCompat.getDrawable(this, R.drawable.date_background));
        dateView.setPadding(12, 12, 12, 12);
        dateView.setGravity(Gravity.CENTER);
        dateView.setText(date);
        dateView.setTextColor(Color.BLACK);
        dateView.setTextSize(18);
        ((LinearLayout.LayoutParams) dateView.getLayoutParams()).setMargins(0, 10, 0, 10);

        dateView.setOnClickListener(v -> {
            // Store the selected date in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selectedDate", date);
            editor.apply();

            // Navigate to Professor_manage_student_history_click activity
            Intent intent = new Intent(Professor_manage_student_history.this, Professor_manage_student_history_click.class);
            startActivity(intent);
        });
        // Add to the container
        datesContainer.addView(dateView);
    }
}