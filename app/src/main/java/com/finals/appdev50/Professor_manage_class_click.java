package com.finals.appdev50;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Professor_manage_class_click extends AppCompatActivity {

    private TextView tvYearAndBlock, tvClassDetails, history;
    private LinearLayout studentListContainer;
    private Button takeattendance;
    private FirebaseAuth mAuth;
    private String currentUserUID, classCode, date, starttime, endtime;
    private int graceperiod;
    private boolean isQRCodeGenerated = false;
    private AlertDialog qrCodeDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_manage_class_click);
        noInternet.showPendulumDialog(this, getLifecycle());
        tvYearAndBlock = findViewById(R.id.tvYearAndBlock);
        tvClassDetails = findViewById(R.id.tvClassDetails);
        studentListContainer = findViewById(R.id.studentListContainer);
        takeattendance = findViewById(R.id.btnTakeAttendance);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        ImageView arrowDown = findViewById(R.id.arrowDown);
        LinearLayout addStudent = findViewById(R.id.addStudentContainer);
        LinearLayout addstudentform = findViewById(R.id.addStudentForm);
        EditText studname = findViewById(R.id.etStudentName);
        addStudent.setOnClickListener(v -> {
            if (addstudentform.getVisibility() == View.GONE) {
                addstudentform.setVisibility(View.VISIBLE); // Show the form
                arrowDown.setRotation(180); // Rotate arrow upwards
            } else {
                addstudentform.setVisibility(View.GONE); // Hide the form
                studname.setText("");
                arrowDown.setRotation(0); // Reset arrow to original position
            }
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserUID = currentUser.getUid(); // Get the UID of the current user
        }

        Button add = findViewById(R.id.addButton);
        add.setOnClickListener(v -> {
            String rawname = studname.getText().toString().trim();
            if (!rawname.isEmpty()) {
                String formattedName = formatStudentName(rawname);
                if (formattedName != null){
                    checkIfNameExists(formattedName, (exists) -> {
                        if (!exists) {
                            try {
                                addStudentToDatabase(formattedName, studname);
                            } catch (Exception e){
                                Toast.makeText(Professor_manage_class_click.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Professor_manage_class_click.this, "Name already exists!", Toast.LENGTH_SHORT).show();
                            studname.setText("");
                        }
                    });
                } else {
                    studname.setError("Invalid format! Please use 'Surname, Firstname'.");
                    studname.requestFocus();
                }
            } else {
                Toast.makeText(this, "Please enter a valid name.", Toast.LENGTH_SHORT).show();
                studname.setError("Please input valid Name");
                studname.requestFocus();
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        classCode = sharedPreferences.getString("classCode", null);
        String subjectName = sharedPreferences.getString("subjectName", null);
        String yearSection = sharedPreferences.getString("yearSection", null);

        if (classCode != null && subjectName != null && yearSection != null) {
            tvYearAndBlock.setText(yearSection);
            tvClassDetails.setText(String.format("%s - %s", classCode, subjectName));
        }

        loadStudentsFromDatabase();  // Load students dynamically from Firebase
        checkQRCodeStatus();         // Check the QR code status and update the button text

        if (classCode == null || date == null || starttime == null || endtime == null) {
            fetchQRCodeDataFromFirebase();
        }

        takeattendance.setOnClickListener(v -> {
            try {
                if (takeattendance.getText().toString().equals("Show QR Code")) {
                    // Show the previously generated QR code (you can retrieve the data from Firebase and display it)
                    showQRCode();
                } else {
                    if(classCode != null) {
                        String encodedClassCode = classCode;
                        SharedPreferences sharedPreferences1 = getSharedPreferences("ClassDetails", MODE_PRIVATE);
                        String classCode = sharedPreferences1.getString("classCode", null); // Default value is null if not found
                        boolean isQRCodeGenerated = sharedPreferences1.getBoolean("isQRCodeGenerated", false); // Default value is false

                        // Now you can use these values as needed
                        if (classCode != null) {
                            // Use classCode
                        } else {
                            Toast.makeText(this, "Class Code not Found.", Toast.LENGTH_SHORT).show();
                        }

                        Intent intent = new Intent(Professor_manage_class_click.this, Professor_manage_class_takeAttendance.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Class code is missing!", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception ex){
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        history = findViewById(R.id.btnHistory);
        history.setOnClickListener(v -> {
            // Create an intent to start the Professor_manage_student_history activity
            Intent intent = new Intent(this, Professor_manage_student_history.class);
            startActivity(intent);
        });

        checkQRCodeStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkQRCodeStatus();
    }

    private void loadStudentsFromDatabase() {
        if (currentUserUID == null || classCode == null) return;

        // Show a loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading students...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Retrieve class details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);

        // If yearAndSection or subjectName is null, show an error
        if (yearAndSection == null || subjectName == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Class details are missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUID)        // UID of the current user (professor)
                .child(yearAndSection)        // Year and Section
                .child(subjectName)           // Subject Name
                .child(classCode);            // Class Code

        studentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Dismiss the loading dialog
                progressDialog.dismiss();

                // Clear the container first
                studentListContainer.removeAllViews();

                if (snapshot.exists()) {
                    int index = 1;
                    for (DataSnapshot Classsnapshot : snapshot.getChildren()) {
                        String studentKey = Classsnapshot.getKey();
                        String studentName = Classsnapshot.getValue(String.class);

                        // Create a new layout for each student
                        LinearLayout studentLayout = new LinearLayout(Professor_manage_class_click.this);
                        studentLayout.setOrientation(LinearLayout.HORIZONTAL);
                        studentLayout.setPadding(16, 16, 16, 16); // Padding for spacing
                        studentLayout.setBackgroundResource(R.drawable.border); // Apply border drawable
                        studentLayout.setGravity(Gravity.CENTER_VERTICAL); // Center content vertically

                        // Add user avatar (ImageView)
                        ImageView userAvatar = new ImageView(Professor_manage_class_click.this);
                        userAvatar.setImageResource(R.drawable.user); // Use the user drawable image
                        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(60, 60); // Smaller size
                        avatarParams.rightMargin = 16; // Margin to separate from text
                        userAvatar.setLayoutParams(avatarParams);

                        // Add student name (TextView)
                        TextView studentView = new TextView(Professor_manage_class_click.this);
                        studentView.setText(index + ". " + studentName);
                        studentView.setTextSize(16); // Normal text size
                        studentView.setGravity(Gravity.CENTER_VERTICAL);
                        studentView.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); // Fill available width

                        // Add menu button (ImageView)
                        ImageView menuButton = new ImageView(Professor_manage_class_click.this);
                        menuButton.setImageResource(R.drawable.menu); // Menu icon
                        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(60, 60); // Smaller size
                        menuButton.setLayoutParams(menuParams);

                        // Placeholder for dynamic icons (edit, delete, close)
                        LinearLayout actionIcons = new LinearLayout(Professor_manage_class_click.this);
                        actionIcons.setOrientation(LinearLayout.HORIZONTAL);
                        actionIcons.setVisibility(View.GONE); // Initially hidden

                        // Add edit icon
                        ImageView editIcon = new ImageView(Professor_manage_class_click.this);
                        editIcon.setImageResource(R.drawable.edit);
                        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
                        iconParams.setMargins(8, 0, 8, 0);
                        editIcon.setLayoutParams(iconParams);
                        actionIcons.addView(editIcon);
                        editIcon.setOnClickListener(v -> {
                            EditText editText = new EditText(Professor_manage_class_click.this);
                            editText.setText(studentName);

                            if (studentName.isEmpty()) {
                                editText.setHint("Surname, Firstname");
                            }

                            AlertDialog dialog = new AlertDialog.Builder(Professor_manage_class_click.this)
                                    .setTitle("Edit Student Name")
                                    .setView(editText)
                                    .setPositiveButton("Save", null) // Set null for custom handling
                                    .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                                    .create();

                            dialog.setOnShowListener(dialogInterface -> {
                                Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                saveButton.setOnClickListener(view -> {
                                    String rawName = editText.getText().toString().trim();

                                    if (!rawName.isEmpty()) {
                                        String formattedName = formatStudentName(rawName);
                                        if (formattedName != null) {
                                            checkIfNameExists(formattedName, exists -> {
                                                if (!exists) {
                                                    updateStudentName(studentKey, formattedName);
                                                    dialog.dismiss();
                                                } else {
                                                    editText.setError("This name already exists.");
                                                    editText.requestFocus();
                                                }
                                            });
                                        } else {
                                            editText.setError("Invalid format! Please use 'Surname, Firstname'.");
                                            editText.requestFocus();
                                        }
                                    } else {
                                        editText.setError("Please input a valid name.");
                                        editText.requestFocus();
                                    }
                                });
                            });

                            dialog.show();
                        });


                        // Add delete icon
                        ImageView deleteIcon = new ImageView(Professor_manage_class_click.this);
                        deleteIcon.setImageResource(R.drawable.delete);
                        deleteIcon.setLayoutParams(iconParams);
                        actionIcons.addView(deleteIcon);

                        deleteIcon.setOnClickListener(v -> {
                            // Confirm deletion
                            new AlertDialog.Builder(Professor_manage_class_click.this)
                                    .setTitle("Delete Student")
                                    .setMessage("Are you sure you want to delete this student?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        // Pass the studentKey to the deleteStudent method
                                        deleteStudent(studentKey);  // We use studentKey here, not studentName
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        });

                        // Add close icon
                        ImageView closeIcon = new ImageView(Professor_manage_class_click.this);
                        closeIcon.setImageResource(R.drawable.close);
                        closeIcon.setLayoutParams(iconParams);
                        actionIcons.addView(closeIcon);

                        // Handle menu button click
                        menuButton.setOnClickListener(view -> {
                            menuButton.setVisibility(View.GONE); // Hide menu button
                            actionIcons.setVisibility(View.VISIBLE); // Show edit, delete, close
                        });

                        // Handle close button click
                        closeIcon.setOnClickListener(view -> {
                            actionIcons.setVisibility(View.GONE); // Hide action icons
                            menuButton.setVisibility(View.VISIBLE); // Show menu button
                        });

                        // Add the views (avatar, name, menu) to the LinearLayout
                        studentLayout.addView(userAvatar);
                        studentLayout.addView(studentView);
                        studentLayout.addView(menuButton);
                        studentLayout.addView(actionIcons);

                        // Set top margin for the student layout
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        layoutParams.topMargin = 20; // Smaller margin for better spacing
                        layoutParams.bottomMargin = 8;
                        layoutParams.rightMargin = (int) getResources().getDisplayMetrics().density * 20;
                        layoutParams.leftMargin = (int) getResources().getDisplayMetrics().density * 20;
                        layoutParams.height = (int) getResources().getDisplayMetrics().density * 40;
                        studentLayout.setLayoutParams(layoutParams);

                        // Check attendance status and update the layout's appearance
                        DatabaseReference attendanceRef = FirebaseDatabase.getInstance()
                                .getReference("CurrentAttendance")
                                .child(currentUserUID)
                                .child(classCode)
                                .child("Students")
                                .child(studentName);

                        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                                if (attendanceSnapshot.exists()) {
                                    String attendanceStatus = attendanceSnapshot.getValue(String.class);

                                    // Set the background color based on attendance status
                                    switch (attendanceStatus) {
                                        case "Present":
                                            studentLayout.setBackgroundResource(R.drawable.present_border);
                                            break;
                                        case "Late":
                                            studentLayout.setBackgroundResource(R.drawable.late_border);
                                            break;
                                        case "Absent":
                                            studentLayout.setBackgroundResource(R.drawable.absent_border);
                                            break;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(Professor_manage_class_click.this, "Failed to check attendance status.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Add the freshly created student layout to the container
                        studentListContainer.addView(studentLayout);

                        index++;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Dismiss the loading dialog in case of error
                progressDialog.dismiss();
            }
        });
    }

    private void updateStudentName(String studentKey, String newName) {

        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);

        if (yearAndSection == null || subjectName == null) {
            Toast.makeText(this, "Failed to delete student: Missing class details.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Reference to the student's name in Firebase
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUID)        // UID of the current user (professor)
                .child(yearAndSection)        // Year and Section
                .child(subjectName)           // Subject Name
                .child(classCode)             // Class Code
                .child(studentKey);           // Unique student key

        studentRef.setValue(newName) // Update the student's name
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Professor_manage_class_click.this, "Student name updated successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Professor_manage_class_click.this, "Failed to update name.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteStudent(String studentKey) {
        if (currentUserUID == null || classCode == null) {
            Toast.makeText(this, "Failed to delete student: Missing class or user information.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve class details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);

        if (yearAndSection == null || subjectName == null) {
            Toast.makeText(this, "Failed to delete student: Missing class details.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to the student node in Firebase
        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUID)        // UID of the current user (professor)
                .child(yearAndSection)        // Year and Section
                .child(subjectName)           // Subject Name
                .child(classCode)             // Class Code
                .child(studentKey);           // Unique student key

        // Deleting the student
        studentRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Student successfully deleted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete student.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatStudentName(String rawName) {
        String[] nameParts = rawName.split(",");  // Split by comma

        if (nameParts.length == 2) {
            String surname = nameParts[0].trim().toLowerCase();
            String firstname = nameParts[1].trim().toLowerCase();

            if (!surname.isEmpty() && !firstname.isEmpty()) {
                // Capitalize the first letter of each name part
                String formattedSurname = capitalizeEachWord(surname);
                String formattedFirstname = capitalizeEachWord(firstname);

                // Return the formatted name
                return formattedSurname + ", " + formattedFirstname;
            }
        }

        // Return null if the format is incorrect
        return null;
    }

    private String capitalizeEachWord(String namePart) {
        String[] words = namePart.split(" ");  // Split by spaces
        StringBuilder capitalizedWords = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                capitalizedWords.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1)).append(" ");
            }
        }

        return capitalizedWords.toString().trim();  // Return the capitalized string, trimming the extra space
    }

    private void checkIfNameExists(String fullname, final Professor_manage_class_click.OnNameExistsCallback callback) {
        if (currentUserUID == null || classCode == null) return;

        // Retrieve class details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);

        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUID)
                .child(yearAndSection)
                .child(subjectName)
                .child(classCode);
        studentRef.orderByValue().equalTo(fullname).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCallback(snapshot.exists());  // Call the callback with the result
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_class_click.this, "Error checking names: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addStudentToDatabase(String fullname, EditText input) {
        if (classCode == null || currentUserUID == null) return;

        // Retrieve class details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ClassDetails", MODE_PRIVATE);
        String yearAndSection = sharedPreferences.getString("yearSection", null);
        String subjectName = sharedPreferences.getString("subjectName", null);

        DatabaseReference studentRef = FirebaseDatabase.getInstance()
                .getReference("Students")
                .child(currentUserUID)        // UID of the current user
                .child(yearAndSection)        // Year and Section
                .child(subjectName)           // Subject Name
                .child(classCode);            // Class Code

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> studentList = new ArrayList<>();
                // Fetch current student names and add to the list
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentName = studentSnapshot.getValue(String.class);
                    if (studentName != null) {
                        studentList.add(studentName);
                    }
                }

                // Add the new student to the list
                studentList.add(fullname);

                // Sort the list alphabetically
                Collections.sort(studentList);

                // Clear the Firebase node to update sorted data
                studentRef.setValue(null);

                // Re-insert students in alphabetical order with a number prefix
                for (String studentName : studentList) {
                    studentRef.push().setValue(studentName);  // Store only the name, without prefix
                }

                Toast.makeText(Professor_manage_class_click.this, "Student added successfully.", Toast.LENGTH_SHORT).show();
                input.setText("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_class_click.this, "Failed to add student.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private interface OnNameExistsCallback {
        void onCallback(boolean exists);
    }

    private void checkQRCodeStatus() {
        if (currentUserUID == null || classCode == null) return;

        DatabaseReference qrStatusRef = FirebaseDatabase.getInstance()
                .getReference("QRStatus")
                .child(currentUserUID)
                .child(classCode);

        qrStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean qrGenerated = snapshot.getValue(Boolean.class);
                    isQRCodeGenerated = qrGenerated != null && qrGenerated; // Update local state
                    updateTakeAttendanceButtonText();
                } else {
                    isQRCodeGenerated = false; // No QR generated
                    updateTakeAttendanceButtonText();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_class_click.this, "Failed to check QR code status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchQRCodeDataFromFirebase() {
        if (currentUserUID == null || classCode == null) return;

        DatabaseReference qrDataRef = FirebaseDatabase.getInstance()
                .getReference("QRCodeShow")
                .child(currentUserUID)
                .child(classCode);

        qrDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    date = snapshot.child("date").getValue(String.class);
                    starttime = snapshot.child("startTime").getValue(String.class);
                    endtime = snapshot.child("endTime").getValue(String.class);
                    graceperiod = snapshot.child("gracePeriod").getValue(Integer.class);

                    Log.d("QRCodeDebug", "Fetched End Time from Firebase: " + endtime);
                    checkQRCodeStatus();
                    scheduleQRCodeDeletion();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_class_click.this, "Failed to fetch QR data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateTakeAttendanceButtonText() {
        // Update button text based on the QR code state
        if (isQRCodeGenerated) {
            takeattendance.setText("Show QR Code");
        } else {
            takeattendance.setText("Take Attendance");
        }
    }

    private void showQRCode() {
        if (currentUserUID == null || classCode == null){
            Toast.makeText(this, "User ID or Class code is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Prepare the data that will be encoded in the QR code
            String qrData = String.format("%s,%s,%s,%s,%s,%d",
                    currentUserUID, classCode, date, starttime, endtime, graceperiod);

            // Show QR code based on this data
            showQRCodeDialog(qrData);
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showQRCodeDialog(String qrCodeData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR Code");

        // Set up the image view for the QR code
        ImageView qrCodeImageView = new ImageView(this);
        qrCodeImageView.setPadding(16, 16, 16, 16);

        // Generate the QR code bitmap
        Bitmap qrCodeBitmap = generateQRCode(qrCodeData);
        if (qrCodeBitmap != null) {
            qrCodeImageView.setImageBitmap(qrCodeBitmap);
        } else {
            Toast.makeText(this, "Error generating QR code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a LinearLayout to hold the QR code and buttons
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(qrCodeImageView);

        // Create the "Delete QR Code" button
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete QR Code");
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Add the button to the layout
        layout.addView(deleteButton);
        builder.setView(layout);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        qrCodeDialog = builder.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Do you want to destroy this QR code? This action cannot be reversed.");

        builder.setPositiveButton("Yes", (dialog, which) -> deleteQRCode());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void deleteQRCode() {
        if (currentUserUID == null || classCode == null) return;

        // Reference to the nodes to delete
        DatabaseReference qrStatusRef = FirebaseDatabase.getInstance()
                .getReference("QRStatus")
                .child(currentUserUID)
                .child(classCode);
        DatabaseReference qrCodeShowRef = FirebaseDatabase.getInstance()
                .getReference("QRCodeShow")
                .child(currentUserUID)
                .child(classCode);
        DatabaseReference currentattendanceref = FirebaseDatabase.getInstance()
                .getReference("CurrentAttendance")
                .child(currentUserUID)
                .child(classCode);

        // Delete the nodes
        qrStatusRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                qrCodeShowRef.removeValue().addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        currentattendanceref.removeValue().addOnCompleteListener(finalstask -> {
                            if(finalstask.isSuccessful()){
                                loadStudentsFromDatabase();
                                if (qrCodeDialog != null && qrCodeDialog.isShowing()) {
                                    qrCodeDialog.dismiss();
                                }
                                // Optionally update the UI or QR code status
                                checkQRCodeStatus();
                                markAbsentInHistory();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Failed to delete QR code from QRCodeShow.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Failed to delete QR code from QRStatus.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAbsentInHistory() {
        // Format the current date as MM-dd-yyyy
        String currentDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());

        // Reference to the history for the current date
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(currentUserUID)
                .child(classCode)
                .child(currentDate)
                .child("Students");

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String fullname = studentSnapshot.getKey();
                    String remark = studentSnapshot.getValue(String.class);

                    // Check if the student has no remark (null or empty) and set it to "Absent"
                    if (remark == null || remark.isEmpty()) {
                        historyRef.child(fullname).setValue("Absent");
                    }
                }
                Toast.makeText(Professor_manage_class_click.this, "All students with no remark are marked as Absent.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Professor_manage_class_click.this, "Failed to update history remarks.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap generateQRCode(String data) {
        try {
            // Define the size of the QR code
            int size = 500;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size);

            // Create a Bitmap and set each pixel based on the BitMatrix
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void scheduleQRCodeDeletion() {
        if (endtime == null || currentUserUID == null || classCode == null) {
            Toast.makeText(this, "End time or required details are missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse the end time in 12-hour format
            SimpleDateFormat endTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // 12-hour format
            Date endTimeDate = endTimeFormat.parse(endtime);

            // Get the current time in 12-hour format
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // 12-hour format
            Date currentTime = Calendar.getInstance().getTime();
            String currentTimeFormatted = currentTimeFormat.format(currentTime);

            // Log the current and end times for debugging
            Log.d("QRCodeDebug", "Current Time (12-hour): " + currentTimeFormatted);
            Log.d("QRCodeDebug", "End Time: " + endtime);

            if (endTimeDate != null) {
                // Parse current time into a Date object for comparison
                Date currentTimeDate = endTimeFormat.parse(currentTimeFormatted);

                // Calculate the delay in milliseconds
                long delay = endTimeDate.getTime() - currentTimeDate.getTime();

                // Log the delay for debugging
                Log.d("QRCodeDebug", "Calculated Delay (ms): " + delay);

                if (delay > 0) {
                    // Schedule the task after the delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        deleteQRCode(); // Call your existing deleteQRCode() method
                    }, delay);
                } else {
                    Log.d("QRCodeDebug", "End time has already passed.");
                    deleteQRCode();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("QRCodeDebug", "Error parsing time: " + e.getMessage());
            Toast.makeText(this, "Error parsing time.", Toast.LENGTH_SHORT).show();
        }
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


    public void hideKeyboardAndClearFocus(View view) {
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Clear focus from the currently focused view
        view.clearFocus();
    }
}