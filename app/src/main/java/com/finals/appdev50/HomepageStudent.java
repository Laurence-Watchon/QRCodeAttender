package com.finals.appdev50;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class HomepageStudent extends AppCompatActivity {

    DatabaseReference database;
    String uid;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage_student);

        noInternet.showPendulumDialog(this, getLifecycle());
        loadseeclass();

        LinearLayout settings = findViewById(R.id.btnsetting);
        settings.setOnClickListener(v -> {
            replaceFragmentIfNotPresent(new Professor_settings(), "StudentSettings");
        });

        LinearLayout seeclass = findViewById(R.id.btnShowClass);
        seeclass.setOnClickListener(v -> {
            replaceFragmentIfNotPresent(new Student_show_class(), "StudentShowClass");
        });

        LinearLayout scan = findViewById(R.id.btnScan);
        scan.setOnClickListener(v -> {
            scancode();
        });
    }

    private void scancode() {

        showProgressDialog("Scanning code...");

        ScanOptions option = new ScanOptions();
        option.setPrompt("Volume up to flash on");
        option.setBeepEnabled(true);
        option.setOrientationLocked(true);
        option.setCaptureActivity(CaptureAct.class);
        barlauncher.launch(option);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false); // Prevent user from dismissing
        }
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    ActivityResultLauncher<ScanOptions> barlauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null){
            String scannedData = result.getContents();
            String[] elements = scannedData.split(","); // Split the result by spaces

            AlertDialog.Builder builder = new AlertDialog.Builder(HomepageStudent.this);

            if(elements.length == 6) {
                // Assign the elements to variables
                String currentuseruid = elements[0];
                String classcode = elements[1];
                String date = elements[2];
                String starttime = elements[3];
                String endtime = elements[4];
                String lastElement = elements[5];

                try {
                    // Try to convert the last element into an integer
                    int gracePeriodMinutes = Integer.parseInt(lastElement);

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        uid = user.getUid(); // Get the logged-in user's UID
                    }
                    database = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid);
                    database.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()){
                                String firstnameval = snapshot.child("firstname").getValue(String.class);
                                String lastnameval = snapshot.child("lastname").getValue(String.class);
                                String fullname = lastnameval + ", " + firstnameval;

                                DatabaseReference studentListRef = FirebaseDatabase.getInstance()
                                        .getReference("CurrentAttendance")
                                        .child(currentuseruid)
                                        .child(classcode)
                                        .child("Students");

                                DatabaseReference historyRef = FirebaseDatabase.getInstance()
                                        .getReference("History")
                                        .child(currentuseruid)
                                        .child(classcode)
                                        .child(date)
                                        .child("Students");

                                studentListRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        boolean studentExists = false;
                                        boolean alreadyPresent = false;

                                        if (dataSnapshot.hasChild(fullname)) {
                                            String attendanceStatus = dataSnapshot.child(fullname).getValue(String.class);
                                            alreadyPresent = "Present".equals(attendanceStatus) || "Late".equals(attendanceStatus) || "Absent".equals(attendanceStatus);
                                            studentExists = !alreadyPresent;
                                        }

                                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                                        try {
                                            Date classStartTime = sdf.parse(starttime);
                                            Date classEndTime = sdf.parse(endtime);
                                            Date currentTime = Calendar.getInstance().getTime();

                                            // Set classStartTime, classEndTime, and currentTime to the same date for accurate comparison
                                            Calendar classStartCal = Calendar.getInstance();
                                            classStartCal.setTime(classStartTime);
                                            Calendar classEndCal = Calendar.getInstance();
                                            classEndCal.setTime(classEndTime);
                                            Calendar currentCal = Calendar.getInstance();
                                            currentCal.setTime(currentTime);

                                            // Set date parts to the current day
                                            classStartCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR));
                                            classStartCal.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
                                            classStartCal.set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH));

                                            classEndCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR));
                                            classEndCal.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
                                            classEndCal.set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH));

                                            // Update the Date objects with the same day
                                            classStartTime = classStartCal.getTime();
                                            classEndTime = classEndCal.getTime();

                                            // Set grace period based on the updated classStartTime
                                            Calendar gracePeriodTime = Calendar.getInstance();
                                            gracePeriodTime.setTime(classStartTime);
                                            gracePeriodTime.add(Calendar.MINUTE, gracePeriodMinutes);

                                            // Logging in 12-hour format for verification
                                            SimpleDateFormat twelveHourFormat = new SimpleDateFormat("hh:mm a");
                                            Log.d("Current time", twelveHourFormat.format(currentTime));
                                            Log.d("Start time", twelveHourFormat.format(classStartTime));
                                            Log.d("End time", twelveHourFormat.format(classEndTime));

                                            if (currentCal.getTime().after(classEndTime)) {
                                                studentListRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                                            String studentName = studentSnapshot.getKey();
                                                            String attendanceStatus = studentSnapshot.getValue(String.class);

                                                            // If attendance hasn't been recorded yet (empty remarks)
                                                            if (attendanceStatus == null || attendanceStatus.isEmpty()) {
                                                                studentListRef.child(studentName).setValue("Absent");
                                                            }

                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Toast.makeText(HomepageStudent.this, "Failed to update attendance", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                builder.setTitle("Class Ended");
                                                builder.setMessage("Attendance has been automatically marked as Absent for students who didn't scan the QR code.");
                                            } else {
                                                // Continue with the usual logic for scanning QR and marking attendance
                                                if (alreadyPresent) {
                                                    builder.setTitle("Already Recorded");
                                                    builder.setMessage("Your attendance is already recorded.");
                                                } else if (studentExists) {
                                                    String remarks = "";

                                                    if (currentCal.getTime().before(classStartTime)) {
                                                        builder.setTitle("Too Early");
                                                        builder.setMessage("You are scanning too early. Attendance cannot be marked before the class start time.");
                                                    } else if (currentCal.getTime().after(classEndCal.getTime())) {
                                                        remarks = "Absent";
                                                        studentListRef.child(fullname).setValue(remarks);
                                                        historyRef.child(fullname).setValue(remarks);
                                                        builder.setTitle("Class Ended");
                                                        builder.setMessage("Sorry, the class has already ended.");
                                                    } else if (currentCal.getTime().after(gracePeriodTime.getTime())) {
                                                        remarks = "Late";
                                                        studentListRef.child(fullname).setValue(remarks);
                                                        historyRef.child(fullname).setValue(remarks);
                                                        builder.setTitle("Late Attendance Recorded");
                                                        builder.setMessage("You are marked as late.\nYour attendance to Classcode " + classcode +
                                                                " is recorded at " + getCurrentDate() + " " + getCurrentTime12HourFormat());
                                                    } else {
                                                        remarks = "Present";
                                                        studentListRef.child(fullname).setValue(remarks);
                                                        historyRef.child(fullname).setValue(remarks);
                                                        builder.setTitle("Attendance Recorded");
                                                        builder.setMessage("You are present.\nYour attendance to Classcode " + classcode +
                                                                " is recorded at " + getCurrentDate() + " " + getCurrentTime12HourFormat());
                                                    }

                                                    if(remarks.isEmpty()){
                                                        historyRef.child(fullname).setValue(remarks);
                                                    }
                                                } else {
                                                    builder.setTitle("Not Found");
                                                    builder.setMessage("Sorry, it seems like you don't belong to the class.");
                                                }
                                            }
                                        } catch (Exception e) {
                                            builder.setTitle("Error");
                                            builder.setMessage("Error parsing class times. Please try again.");
                                        }
                                        builder.setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(HomepageStudent.this, "Failed to check the students' record", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                } catch (NumberFormatException e) {
                    builder.setTitle("Error");
                    builder.setMessage("Error: The QR code data is invalid. Please check the QR code.");
                    builder.setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                }
            } else {
                // Show an error message if the number of elements is not 6
                builder.setTitle("Error");
                builder.setMessage("Error: The QR code you scanned is not valid. Please try again.");
                builder.setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss()).show();
            }
        }
        progressDialog.dismiss();
    });

    public String getCurrentDate() {
        // Create a SimpleDateFormat instance for the date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");

        // Get the current date
        Date date = new Date();

        // Format the current date and return it as a string
        return dateFormat.format(date);
    }

    public String getCurrentTime12HourFormat() {
        // Create a SimpleDateFormat instance for the 12-hour time format with AM/PM
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        // Get the current time
        Date time = new Date();

        // Format the current time and return it as a string
        return timeFormat.format(time);
    }

    private void replaceFragmentIfNotPresent(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.flfragment);

        if (currentFragment == null || !currentFragment.getClass().equals(fragment.getClass())) {
            // Replace the fragment if it's not already visible
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.flfragment, fragment, tag);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    private void loadseeclass() {
        replaceFragmentIfNotPresent(new Student_show_class(), "ShowClassFragment");
    }
}