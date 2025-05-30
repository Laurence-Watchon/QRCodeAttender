package com.finals.appdev50;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Professor_manage_student_history_click extends AppCompatActivity {

    private String currentUserUID, classCode, date;
    private LinearLayout studentContainer;
    private Button done;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_manage_students_history_click);

        noInternet.showPendulumDialog(this, getLifecycle());

        TextView tvDate = findViewById(R.id.tvDate);
        studentContainer = findViewById(R.id.studentContainer);

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
                int presentCount = 0;
                int lateCount = 0;
                int absentCount = 0;
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentName = studentSnapshot.getKey();
                    String remark = studentSnapshot.getValue(String.class);

                    if ("Present".equalsIgnoreCase(remark)) {
                        presentCount++;
                    } else if ("Late".equalsIgnoreCase(remark)) {
                        lateCount++;
                    } else if ("Absent".equalsIgnoreCase(remark)) {
                        absentCount++;
                    }

                    addStudentView(studentName, remark);
                }
                setupPieChart(presentCount, lateCount, absentCount);
                addAnalyticsView(presentCount, lateCount, absentCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_student_history_click.this, "Failed to load student data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPieChart(int present, int late, int absent) {
        PieChart pieChart = findViewById(R.id.pieChart);

        // Create entries for the chart
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (present > 0) {
            entries.add(new PieEntry(present, "Present"));
            colors.add(Color.GREEN); // Green for Present
        } else {
            // Add a dummy entry for Present with 0 value to keep the color
            entries.add(new PieEntry(0, ""));
            colors.add(Color.GREEN);
        }

        if (late > 0) {
            entries.add(new PieEntry(late, "Late"));
            colors.add(Color.YELLOW); // Yellow for Late
        } else {
            // Add a dummy entry for Late with 0 value to keep the color
            entries.add(new PieEntry(0, ""));
            colors.add(Color.YELLOW);
        }

        if (absent > 0) {
            entries.add(new PieEntry(absent, "Absent"));
            colors.add(Color.RED); // Red for Absent
        }

        // Set up the dataset
        PieDataSet dataSet = new PieDataSet(entries, "Attendance");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        // Set up the data
        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Customize the chart appearance
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setEntryLabelTextSize(14f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // Refresh the chart
        pieChart.invalidate();
    }

    private void addAnalyticsView(int presentCount, int lateCount, int absentCount) {
        // Find your PieChart from the layout
        PieChart pieChart = findViewById(R.id.pieChart);

        // Create entries for the PieChart
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(presentCount, "Present"));
        entries.add(new PieEntry(lateCount, "Late"));
        entries.add(new PieEntry(absentCount, "Absent"));

        // Set up the dataset and PieChart
        PieDataSet dataSet = new PieDataSet(entries, "Attendance Statistics");
        dataSet.setColors(new int[]{Color.GREEN, Color.YELLOW, Color.RED});
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setCenterText("Attendance");
        pieChart.setCenterTextSize(18f);
        pieChart.invalidate(); // Refresh the PieChart
    }


    private void addStudentView(String name, String remark) {
        // Create a horizontal LinearLayout for each student entry
        LinearLayout entryLayout = new LinearLayout(this);
        entryLayout.setOrientation(LinearLayout.HORIZONTAL);
        entryLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        entryLayout.setPadding(16, 8, 16, 8); // Add padding for better appearance

        // Name TextView with weight 3
        TextView nameView = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f); // Weight 3
        nameView.setLayoutParams(nameParams);
        nameView.setText(name);
        nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); // Align text to the start
        nameView.setTextSize(16); // Set text size
        nameView.setTextColor(Color.BLACK); // Set text color

        // Remark TextView with weight 1
        TextView remarkView = new TextView(this);
        LinearLayout.LayoutParams remarkParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); // Weight 1
        remarkView.setLayoutParams(remarkParams);
        remarkView.setGravity(Gravity.CENTER); // Center the text
        remarkView.setTextSize(16); // Set text size

        // Set the text and color based on the remark
        if ("Present".equalsIgnoreCase(remark)) {
            remarkView.setText("Present");
            remarkView.setTextColor(Color.GREEN);
        } else if ("Late".equalsIgnoreCase(remark)) {
            remarkView.setText("Late");
            remarkView.setTextColor(Color.parseColor("#FFA500")); // Orange
        } else if ("Absent".equalsIgnoreCase(remark)) {
            remarkView.setText("Absent");
            remarkView.setTextColor(Color.RED);
        } else {
            remarkView.setText(remark); // Default to the given text
            remarkView.setTextColor(Color.BLACK); // Default color
        }

        // Add the views to the entry layout
        entryLayout.addView(nameView);
        entryLayout.addView(remarkView);

        // Add the entry layout to the student container
        studentContainer.addView(entryLayout, 0); // Insert at the top (index 0)
    }

}