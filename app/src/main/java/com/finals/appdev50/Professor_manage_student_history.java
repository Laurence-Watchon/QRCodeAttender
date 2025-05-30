package com.finals.appdev50;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

import com.google.android.datatransport.backend.cct.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        classCode = sharedPreferences.getString("classCode", null);

        Button exportButton = findViewById(R.id.exportButton); // Add this button in your XML layout
        exportButton.setOnClickListener(v -> {
            showExportDialog(); // Show the dialog before exporting
        });

        loadDatesFromDatabase(currentUserUID, classCode);
    }

    private void showExportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Export Data")
                .setMessage("Do you want to export all history data to PDF?")
                .setPositiveButton("Export", (dialog, which) -> {
                    exportDataToPDF(); // Call the PDF export method
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportDataToPDF() {
        DatabaseReference qrDataRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(currentUserUID)
                .child(classCode);

        qrDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> historyData = new ArrayList<>();
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    String value = dateSnapshot.getValue() != null ? dateSnapshot.getValue().toString() : "No data";
                    historyData.add(date + ": " + value);
                }

                try {
                    generatePDF(historyData); // Generate PDF file
                } catch (Exception e) {
                    Toast.makeText(Professor_manage_student_history.this, "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ExportError", e.getMessage(), e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_student_history.this, "Failed to fetch data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePDF(List<String> historyData) throws Exception {
        String filePath = getExternalFilesDir(null).getAbsolutePath() + "/HistoryData.pdf"; // File location
        File file = new File(filePath);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Failed to create directories: " + file.getParentFile());
        }

        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add Title
        document.add(new Paragraph("History Data").setFontSize(20).setBold().setMarginBottom(20));

        // Add Each Record (Formatted as Date and Value)
        for (String record : historyData) {
            // Split the record into date and value
            String[] parts = record.split(": ", 2);
            String date = parts[0]; // The date part
            String value = parts.length > 1 ? parts[1] : "No data"; // The value part

            // Add Date and Value, each on a new line
            document.add(new Paragraph(date).setFontSize(14).setBold().setMarginBottom(5)); // Date in bold
            document.add(new Paragraph(value).setFontSize(12).setMarginBottom(15)); // Value with spacing
        }

        // Close the document
        document.close();

        Toast.makeText(this, "PDF saved at: " + filePath, Toast.LENGTH_LONG).show();

        // Open the PDF file
        openPDF(filePath);
    }


    private void openPDF(String filePath) {
        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(this, "com.finals.appdev50.provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found to open PDF.", Toast.LENGTH_SHORT).show();
            Log.e("OpenPDFError", e.getMessage(), e);
        }
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
                List<String> dates = new ArrayList<>();
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // The key is the date
                    if (date != null) {
                        dates.add(date);
                    }
                }
                Collections.sort(dates, Collections.reverseOrder());
                for (String date : dates) {
                    addDateView(date);
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
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                500, // Make the width shorter
                150);
        layoutParams.gravity = Gravity.CENTER;
        dateView.setLayoutParams(layoutParams);

        dateView.setBackground(ContextCompat.getDrawable(this, R.drawable.date_background));
        dateView.setPadding(12, 12, 12, 12);
        dateView.setGravity(Gravity.CENTER);
        dateView.setText(date);
        dateView.setTextColor(Color.BLACK);
        dateView.setTextSize(14);
        layoutParams.setMargins(0, 10, 0, 10);

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