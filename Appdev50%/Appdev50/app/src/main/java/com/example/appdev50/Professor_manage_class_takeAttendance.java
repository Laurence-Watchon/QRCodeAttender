package com.example.appdev50;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Professor_manage_class_takeAttendance extends AppCompatActivity {

    private TextView date;
    private EditText starttime, endtime, graceperiod;
    private Button generateqrcode;
    private FirebaseAuth mAuth;
    private String currentUserUID, classCode, codedclassCode;
    private boolean isQRCodeGenerated = false;
    private Handler handler;
    private Runnable timeChecker;
    private SharedPreferences sharedPreferences;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_manage_class_take_attendance);
        noInternet.showPendulumDialog(this, getLifecycle());
        date = findViewById(R.id.tvDate);
        starttime = findViewById(R.id.etStartTime);
        endtime = findViewById(R.id.etEndTime);
        graceperiod = findViewById(R.id.etgraceperiod);
        generateqrcode = findViewById(R.id.btngenerateqrcode);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserUID = currentUser.getUid(); // Get the UID of the current user
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            // Handle the case where no user is logged in
        }

        sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        classCode = sharedPreferences.getString("classCode", null);
        if (classCode != null) {
            codedclassCode = classCode;
        } else {
            Toast.makeText(this, "Class Code not Found.", Toast.LENGTH_SHORT).show();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        date.setText(currentDate);

        // Set up the TimePicker for start time
        starttime.setOnClickListener(v -> showTimePickerDialog(starttime));

        // Set up the TimePicker for end time
        endtime.setOnClickListener(v -> showTimePickerDialog(endtime));

        generateqrcode.setOnClickListener(v -> {
            clearOldQRCodeData();
            generateQRCode();
        });
    }

    private void clearOldQRCodeData() {
        if (currentUserUID == null || classCode == null) {
            Toast.makeText(this, "User or class code not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference qrCodeClassRef = FirebaseDatabase.getInstance()
                .getReference("QRCodeShow")
                .child(currentUserUID)
                .child(classCode);

        // Remove the old QR code data
        qrCodeClassRef.removeValue()
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Old QR Code data cleared successfully"))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to clear old QR Code data", e));
    }


    private void generateQRCode(){
        String startTime = starttime.getText().toString();
        String endTime = endtime.getText().toString();
        String gracePeriodStr = graceperiod.getText().toString();

        if (startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Start and End times are required", Toast.LENGTH_SHORT).show();
            if (startTime.isEmpty()) starttime.setError("Required");
            if (endTime.isEmpty()) endtime.setError("Required");
            return;
        }

        if (!isStartTimeValid(startTime)) {
            Toast.makeText(this, "Start time cannot be earlier than current time", Toast.LENGTH_SHORT).show();
            starttime.setError("Invalid time");
            return;
        }

        if (!isEndTimeValid(startTime, endTime)) {
            Toast.makeText(this, "End time must be after Start time", Toast.LENGTH_SHORT).show();
            starttime.setError("Invalid time");
            endtime.setError("Invalid time");
            return;
        }

        if (!isDurationValid(startTime, endTime)) {
            Toast.makeText(this, "Class duration must be at least 1 hour", Toast.LENGTH_SHORT).show();
            endtime.setError("Duration must be at least 1 hour");
            return;
        }

        int gracePeriod = 0;
        if (!gracePeriodStr.isEmpty()) {
            gracePeriod = Integer.parseInt(gracePeriodStr);
            if (gracePeriod < 0 || gracePeriod > 60) {
                Toast.makeText(this, "Grace period must be between 0 and 60 minutes", Toast.LENGTH_SHORT).show();
                graceperiod.setError("Max 60 minutes");
                return;
            }
        }

        if (gracePeriodStr.isEmpty()) {
            Toast.makeText(this, "Grace period is required", Toast.LENGTH_SHORT).show();
            graceperiod.setError("Required");
            return;
        }

        long classDurationMinutes = calculateDurationInMinutes(startTime, endTime);
        if (gracePeriod > classDurationMinutes) {
            Toast.makeText(this, "Grace period is invalid. It cannot be longer than the class duration.", Toast.LENGTH_SHORT).show();
            graceperiod.setError("Grace period too long");
            return;
        }

        // Save QR code data and students to Firebase
        storeQRDataInFirebase(startTime, endTime, gracePeriod);

        // Prepare data to encode into the QR code
        String qrData = String.format("%s,%s,%s,%s,%s,%d",currentUserUID, codedclassCode, date.getText(), startTime, endTime, gracePeriod);

        // Generate QR code and show it
        generateQRCodeDialog(qrData);
        updateQRCodeStatusInFirebase();
        storeforHistory();
        CurrentAttendance();
        // Start a handler to periodically check and delete the QR code nodes if the end time is reached
    }

    private long calculateDurationInMinutes(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);
            if (startDate != null && endDate != null) {
                long differenceInMillis = endDate.getTime() - startDate.getTime();
                return differenceInMillis / (1000 * 60); // Convert milliseconds to minutes
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private boolean isStartTimeValid(String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Calendar startCalendar = Calendar.getInstance();
            Calendar currentCalendar = Calendar.getInstance();
            startCalendar.setTime(sdf.parse(startTime));

            // Only compare the time, set the same date for both calendars
            currentCalendar.set(Calendar.SECOND, 0);
            currentCalendar.set(Calendar.MILLISECOND, 0);
            startCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
            startCalendar.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
            startCalendar.set(Calendar.DAY_OF_MONTH, currentCalendar.get(Calendar.DAY_OF_MONTH));

            return startCalendar.after(currentCalendar);  // Start time should be after the current time
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // Utility method to validate end time is after start time
    private boolean isEndTimeValid(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Calendar startCalendar = Calendar.getInstance();
            Calendar endCalendar = Calendar.getInstance();

            startCalendar.setTime(sdf.parse(startTime));
            endCalendar.setTime(sdf.parse(endTime));

            return endCalendar.after(startCalendar);  // Check if end time is after start time
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isDurationValid(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Calendar startCalendar = Calendar.getInstance();
            Calendar endCalendar = Calendar.getInstance();
            startCalendar.setTime(sdf.parse(startTime));
            endCalendar.setTime(sdf.parse(endTime));

            long differenceInMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
            long differenceInMinutes = differenceInMillis / (1000 * 60);

            return differenceInMinutes >= 60;  // Duration must be at least 60 minutes
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void storeforHistory(){
        if (currentUserUID == null || classCode == null) {
            Toast.makeText(this, "User or class code not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve class details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);
        String currentDate = getCurrentDate();
        DatabaseReference historyref = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(currentUserUID)
                .child(classCode)
                .child(currentDate);

        DatabaseReference studentsref = historyref.child("Students");
        addStudentsToQRData(studentsref, currentUserUID, yearAndSection, subjectName, classCode);

        Log.d("Firebase", "Attempting to store history for class: " + classCode + " on date: " + currentDate);
    }

    private void CurrentAttendance(){
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);
        DatabaseReference currentattendance = FirebaseDatabase.getInstance()
                .getReference("CurrentAttendance")
                .child(currentUserUID)
                .child(classCode);
        DatabaseReference studentsreference = currentattendance.child("Students");
        addStudentstocurrentattendance(studentsreference, currentUserUID, yearAndSection, subjectName, classCode);
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void storeQRDataInFirebase(String startTime, String endTime, int gracePeriod){
        if (currentUserUID == null || classCode == null) {
            Toast.makeText(this, "User or class code not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference showqrref = FirebaseDatabase.getInstance()
                .getReference("QRCodeShow")
                .child(currentUserUID); // UserID as the first level

        String classcode = classCode;

        if(classCode != null){
            DatabaseReference qrCodeClasscode = showqrref.child(classcode);

            qrCodeClasscode.child("date").setValue(date.getText().toString());
            qrCodeClasscode.child("startTime").setValue(startTime);
            qrCodeClasscode.child("endTime").setValue(endTime);
            qrCodeClasscode.child("gracePeriod").setValue(gracePeriod);
        }
    }

    private void addStudentsToQRData(DatabaseReference studentsRef, String currentUserUid, String yearSection, String subjectName, String classCode) {
        // Reference to the "Students" node in Firebase
        DatabaseReference studentListRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUid)        // Navigate to the current user's UID
                .child(yearSection)           // Navigate to the year and section
                .child(subjectName)           // Navigate to the subject
                .child(classCode);            // Navigate to the class code

        studentListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if the class code exists in the database
                if (dataSnapshot.exists()) {
                    // Loop through all student records for the given class code
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        // Get the student's name (or full data depending on structure)
                        String studentName = studentSnapshot.getValue(String.class);

                        // If student name is not null, add it to the studentsRef (QR Data)
                        if (studentName != null) {
                            studentsRef.child(studentName).setValue("");
                        }
                    }
                } else {
                    // Log if no students found for the given class code
                    Log.d("Firebase", "No students found for class: " + classCode);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                Toast.makeText(Professor_manage_class_takeAttendance.this, "Failed to load students. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addStudentstocurrentattendance(DatabaseReference studentsRef, String currentUserUid, String yearSection, String subjectName, String classCode) {
        // Reference to the "Students" node in Firebase
        DatabaseReference studentListRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUid)        // Navigate to the current user's UID
                .child(yearSection)           // Navigate to the year and section
                .child(subjectName)           // Navigate to the subject
                .child(classCode);            // Navigate to the class code

        studentListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if the class code exists in the database
                if (dataSnapshot.exists()) {
                    // Loop through all student records for the given class code
                    for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                        // Get the student's name (or full data depending on structure)
                        String studentName = studentSnapshot.getValue(String.class);

                        // If student name is not null, add it to the studentsRef (QR Data)
                        if (studentName != null) {
                            studentsRef.child(studentName).setValue("");
                        }
                    }
                } else {
                    // Log if no students found for the given class code
                    Log.d("Firebase", "No students found for class: " + classCode);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                Toast.makeText(Professor_manage_class_takeAttendance.this, "Failed to load students. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQRCodeDialog(String qrData) {
        try {
            // Generate the QR code bitmap
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 400, 400);

            // Inflate custom dialog layout
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_qrcode, null);

            // Get references to the views
            ImageView qrImageView = dialogView.findViewById(R.id.ivQRCode);
            TextView qrDetails = dialogView.findViewById(R.id.tvQrDetails);
            TextView dateTimeDetails = dialogView.findViewById(R.id.tvDateTimeDetails);
            Button closeButton = dialogView.findViewById(R.id.btnClose);

            // Set the QR code image and details
            qrImageView.setImageBitmap(bitmap);
            qrDetails.setText(String.format("QR Code for %s", codedclassCode));
            dateTimeDetails.setText(String.format("%s: %s - %s", date.getText(), starttime.getText(), endtime.getText()));

            // Create an AlertDialog to show the custom layout
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Close button functionality
            closeButton.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                    isQRCodeGenerated = true; // Mark that a QR code has been generated

                    sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("classCode", classCode);
                    editor.putBoolean("isQRCodeGenerated", isQRCodeGenerated);
                    editor.apply();  // Save the changes

                    Intent intent = new Intent(Professor_manage_class_takeAttendance.this, HomepageProfessor.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);

                } catch (Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateQRCodeStatusInFirebase() {
        DatabaseReference qrStatusRef = FirebaseDatabase.getInstance()
                .getReference("QRStatus")
                .child(currentUserUID)
                .child(classCode);

        // Store a flag indicating QR Code was generated for this class
        qrStatusRef.setValue(true);
    }

    private void showTimePickerDialog(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    // Set the time in the EditText
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                            selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                            selectedMinute,
                            selectedHour < 12 ? "AM" : "PM");
                    editText.setText(formattedTime);
                }, hour, minute, false); // false for 12-hour format
        timePickerDialog.show();
    }
}