package com.example.appdev50;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
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

public class Professor_manage_student_history_click extends AppCompatActivity {

    private String currentUserUID, classCode, date;
    private LinearLayout studentContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_manage_students_history_click);

        noInternet.showPendulumDialog(this, getLifecycle());

        TextView tvDate = findViewById(R.id.tvDate);
        studentContainer = findViewById(R.id.studentContainer);

        // Retrieve the class code and date from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        classCode = sharedPreferences.getString("classCode", null);
        date = sharedPreferences.getString("selectedDate", null); // Assuming you've stored the date as "selectedDate"
        tvDate.setText("History for " + date);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserUID = user.getUid();
        }

        loadStudentData();
    }

    private void loadStudentData() {
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(currentUserUID)
                .child(classCode)
                .child(date)
                .child("Students");

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentContainer.removeAllViews(); // Clear previous data
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentName = studentSnapshot.getKey();
                    String remark = studentSnapshot.getValue(String.class);
                    addStudentView(studentName, remark);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_student_history_click.this, "Failed to load student data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addStudentView(String name, String remark) {
        LinearLayout studentRow = new LinearLayout(this);
        studentRow.setOrientation(LinearLayout.HORIZONTAL);
        studentRow.setPadding(0, 8, 0, 8);

        // Name TextView
        TextView nameView = new TextView(this);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
        nameView.setText(name);
        nameView.setTextSize(16);
        studentRow.addView(nameView);

        // Remark TextView
        TextView remarkView = new TextView(this);
        remarkView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        remarkView.setText(remark);
        remarkView.setTextSize(16);
        remarkView.setGravity(Gravity.CENTER);

        // Set background color based on remark
        switch (remark) {
            case "Present":
                remarkView.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                remarkView.setTextColor(Color.GREEN);
                break;
            case "Late":
                remarkView.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                remarkView.setTextColor(Color.YELLOW);
                break;
            case "Absent":
                remarkView.setBackgroundColor(Color.parseColor("#F44336")); // Red
                remarkView.setTextColor(Color.RED);
                break;
        }
        studentRow.addView(remarkView);

        // Add student row to the container
        studentContainer.addView(studentRow);
    }
}