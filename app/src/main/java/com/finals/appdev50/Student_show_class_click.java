package com.finals.appdev50;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Student_show_class_click extends AppCompatActivity {

    private String profid, classCode, subjectName, fullname;
    private LinearLayout attendanceContainer;
    private Button done;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_show_class_click);

        noInternet.showPendulumDialog(this, getLifecycle());
        TextView tvClassSubject = findViewById(R.id.tvClassSubject);
        attendanceContainer = findViewById(R.id.attendanceContainer);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        done = findViewById(R.id.btnDone);
        done.setOnClickListener(v -> {
            finish();
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading attendance...");
        progressDialog.setCancelable(false); // Prevent dismissing by clicking outside
        progressDialog.show();

        // Get data from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        profid = sharedPreferences.getString("profid", "");
        classCode = sharedPreferences.getString("classCode", "");
        subjectName = sharedPreferences.getString("subjectName", "");

        tvClassSubject.setText(classCode + " - " + subjectName);

        // Get current user ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchFullName(currentUserId);
    }

    private void fetchFullName(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Registered Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstname").getValue(String.class);
                    String lastName = snapshot.child("lastname").getValue(String.class);
                    fullname = lastName + ", " + firstName;

                    // Fetch attendance history after getting the full name
                    fetchAttendanceHistory();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
            }
        });
    }

    private void fetchAttendanceHistory() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("History").child(profid).child(classCode);
        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    DataSnapshot studentSnapshot = dateSnapshot.child("Students").child(fullname);
                    if (studentSnapshot.exists()) {
                        String date = dateSnapshot.getKey();
                        String remarks = studentSnapshot.getValue(String.class);

                        // Add attendance entry to the layout if fullname is found
                        addAttendanceEntry(date, remarks);
                    }
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
            }
        });
    }

    private void addAttendanceEntry(String date, String remarks) {
        // Create a horizontal LinearLayout for each entry
        LinearLayout entryLayout = new LinearLayout(this);
        entryLayout.setOrientation(LinearLayout.HORIZONTAL);
        entryLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        entryLayout.setPadding(16, 8, 16, 8); // Add padding for better appearance

        // Date TextView with weight 1
        TextView dateView = new TextView(this);
        dateView.setText(date);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); // Weight 1
        dateView.setLayoutParams(dateParams);
        dateView.setGravity(Gravity.CENTER);
        dateView.setTextSize(16); // Set text size
        dateView.setTextColor(Color.BLACK); // Set text color

        // Remarks TextView with weight 1
        TextView remarksView = new TextView(this);
        LinearLayout.LayoutParams remarksParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); // Weight 1
        remarksView.setLayoutParams(remarksParams);
        remarksView.setGravity(Gravity.CENTER);
        remarksView.setTextSize(16); // Set text size

        // Set the text and color based on the remarks
        if ("Present".equalsIgnoreCase(remarks)) {
            remarksView.setText("Present");
            remarksView.setTextColor(Color.GREEN);
        } else if ("Late".equalsIgnoreCase(remarks)) {
            remarksView.setText("Late");
            remarksView.setTextColor(Color.parseColor("#FFA500")); // Orange
        } else if ("Absent".equalsIgnoreCase(remarks)) {
            remarksView.setText("Absent");
            remarksView.setTextColor(Color.RED);
        } else {
            remarksView.setText("");
            remarksView.setTextColor(Color.BLACK); // Default color
        }

        // Add the views to the entry layout
        entryLayout.addView(dateView);
        entryLayout.addView(remarksView);

        // Add the entry layout to the attendance container at the top
        attendanceContainer.addView(entryLayout, 0); // Insert at the top (index 0)
    }
}