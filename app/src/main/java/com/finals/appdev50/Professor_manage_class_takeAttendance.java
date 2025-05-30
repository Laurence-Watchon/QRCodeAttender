package com.finals.appdev50;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Professor_manage_class_takeAttendance extends AppCompatActivity {

    private TextView date;
    private EditText starttime, endtime, graceperiod;
    private Button generateqrcode, save;
    private FirebaseAuth mAuth;
    private String currentUserUID, classCode, codedclassCode;
    private boolean isQRCodeGenerated = false;
    private Handler handler;
    private Runnable timeChecker;
    private SharedPreferences sharedPreferences;
    private AlertDialog progressDialog;
    Spinner spinnerSchedule;
    ArrayAdapter<String> spinnerAdapter;
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

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

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

        save = findViewById(R.id.btnSave);
        save.setOnClickListener(v -> {
            saveScheduleToDatabase();
        });

        spinnerSchedule = findViewById(R.id.spinnerSchedule);
        List<String> scheduleList = new ArrayList<>();
        scheduleList.add("Select Schedule"); // Default option
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, scheduleList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSchedule.setAdapter(spinnerAdapter);

        fetchSchedulesFromDatabase();

        spinnerSchedule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedSchedule = (String) parentView.getItemAtPosition(position);

                if (selectedSchedule.equals("Select Schedule")) {
                    // Clear the fields for Start Time, End Time, and Grace Period
                    EditText startTimeField = findViewById(R.id.etStartTime);
                    EditText endTimeField = findViewById(R.id.etEndTime);
                    EditText gracePeriodField = findViewById(R.id.etgraceperiod);

                    startTimeField.setText("");
                    endTimeField.setText("");
                    gracePeriodField.setText("");
                } else {
                    // Parse the selected schedule (StartTime - EndTime, GracePeriod minutes)
                    String[] parts = selectedSchedule.split(", ");
                    String[] times = parts[0].split(" - ");
                    String startTime = times[0]; // Extract start time
                    String endTime = times[1];   // Extract end time
                    String gracePeriodStr = parts[1].split(" ")[0]; // Extract grace period (number before "minutes")

                    // Set the extracted values into the respective fields
                    EditText startTimeField = findViewById(R.id.etStartTime);
                    EditText endTimeField = findViewById(R.id.etEndTime);
                    EditText gracePeriodField = findViewById(R.id.etgraceperiod);

                    startTimeField.setText(startTime);
                    endTimeField.setText(endTime);
                    gracePeriodField.setText(gracePeriodStr);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // No action needed here
            }
        });

        generateqrcode.setOnClickListener(v -> {
            clearOldQRCodeData();
            generateQRCode();
        });
    }

    private void fetchSchedulesFromDatabase() {
        DatabaseReference scheduleRef = FirebaseDatabase.getInstance()
                .getReference("Schedule")
                .child(currentUserUID)
                .child(classCode);

        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                spinnerAdapter.clear(); // Clear existing items
                spinnerAdapter.add("Select Schedule"); // Add default option

                for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
                    String startTime = scheduleSnapshot.child("startTime").getValue(String.class);
                    String endTime = scheduleSnapshot.child("endTime").getValue(String.class);
                    Integer gracePeriod = scheduleSnapshot.child("gracePeriod").getValue(Integer.class);

                    if (startTime != null && endTime != null) {
                        // Add schedule in "StartTime - EndTime" format
                        String scheduleText = startTime + " - " + endTime + ", " + gracePeriod + " minutes";
                        spinnerAdapter.add(scheduleText);
                    }
                }

                spinnerAdapter.notifyDataSetChanged(); // Update spinner
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to fetch schedules: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveScheduleToDatabase() {
        String startTime = starttime.getText().toString();
        String endTime = endtime.getText().toString();
        String gracePeriodStr = graceperiod.getText().toString();

        if (startTime.isEmpty() || endTime.isEmpty() || gracePeriodStr.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            if (startTime.isEmpty()) starttime.setError("Required");
            if (endTime.isEmpty()) endtime.setError("Required");
            if (gracePeriodStr.isEmpty()) graceperiod.setError("Required");
            return;
        }

        int gracePeriod;
        try {
            gracePeriod = Integer.parseInt(gracePeriodStr);
            if (gracePeriod < 0 || gracePeriod > 60) {
                graceperiod.setError("Grace period must be between 0 and 60 minutes");
                return;
            }
        } catch (NumberFormatException e) {
            graceperiod.setError("Invalid number");
            return;
        }

        showProgressDialog("Saving Schedule");

        DatabaseReference scheduleRef = FirebaseDatabase.getInstance()
                .getReference("Schedule")
                .child(currentUserUID)
                .child(classCode);

        // Query to check for duplicate schedules
        scheduleRef.orderByChild("startTime").equalTo(startTime).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isDuplicate = false;

                for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
                    String dbEndTime = scheduleSnapshot.child("endTime").getValue(String.class);
                    Integer dbGracePeriod = scheduleSnapshot.child("gracePeriod").getValue(Integer.class);

                    if (endTime.equals(dbEndTime) && gracePeriod == dbGracePeriod) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(getApplicationContext(), "Schedule already exists!", Toast.LENGTH_SHORT).show();
                    hideProgressDialog();
                } else {
                    // Generate a unique key for the new schedule
                    String scheduleId = scheduleRef.push().getKey();
                    if (scheduleId == null) {
                        Toast.makeText(getApplicationContext(), "Failed to generate unique schedule ID.", Toast.LENGTH_SHORT).show();
                        hideProgressDialog();
                        return;
                    }

                    // Prepare data to store
                    Map<String, Object> scheduleData = new HashMap<>();
                    scheduleData.put("startTime", startTime);
                    scheduleData.put("endTime", endTime);
                    scheduleData.put("gracePeriod", gracePeriod);

                    scheduleRef.child(scheduleId).setValue(scheduleData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getApplicationContext(), "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
                                hideProgressDialog(); // Hide progress dialog on success
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getApplicationContext(), "Failed to save schedule.", Toast.LENGTH_SHORT).show();
                                hideProgressDialog(); // Hide progress dialog on failure
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to check for duplicates: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgressDialog(String message) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // Prevent dismissing the dialog by outside touches

        // Create a container layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        // Add a ProgressBar to the layout
        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, 0, 30, 0);
        progressBar.setLayoutParams(progressParams);
        layout.addView(progressBar);

        // Add a TextView for the message
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(Color.BLACK);
        layout.addView(textView);

        // Set the custom layout in the dialog
        builder.setView(layout);

        // Create and show the dialog
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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