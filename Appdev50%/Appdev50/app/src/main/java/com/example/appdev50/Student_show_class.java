package com.example.appdev50;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Student_show_class extends Fragment {

    String uid;
    private DatabaseReference database;
    private LinearLayout classContainer;
    View view;
    private List<View> classViews = new ArrayList<>();
    private ProgressDialog progressDialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_student_show_class, container, false);
        classContainer = view.findViewById(R.id.classContainer);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading classes...");
        progressDialog.setCancelable(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid(); // Get the logged-in user's UID
        }
        database = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstnameval = snapshot.child("firstname").getValue(String.class);
                    String lastnameval = snapshot.child("lastname").getValue(String.class);

                    String fullname = lastnameval + ", " + firstnameval;

                    progressDialog.show();
                    fetchStudentClasses(fullname);
                } else {
                    Toast.makeText(getContext(), "Students does not exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
            }
        });
        return view;
    }


    private void fetchStudentClasses(String studentFullName) {
        // Reference to the 'Students' node in Firebase
        database = FirebaseDatabase.getInstance().getReference("Students");
        final boolean[] hasClasses = {false};

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot yearSectionSnapshot : teacherSnapshot.getChildren()) {
                        for (DataSnapshot subjectSnapshot : yearSectionSnapshot.getChildren()) {
                            for (DataSnapshot classCodeSnapshot : subjectSnapshot.getChildren()) {
                                for (DataSnapshot studentSnapshot : classCodeSnapshot.getChildren()) {
                                    String studentName = studentSnapshot.getValue(String.class);
                                    // Check if the student name matches the logged-in student
                                    if (studentName != null && studentName.equals(studentFullName)) {
                                        String classCode = classCodeSnapshot.getKey();
                                        String subjectName = subjectSnapshot.getKey();
                                        String profid = teacherSnapshot.getKey();

                                        hasClasses[0] = true;

                                        fetchProfessorName(profid, classCode, subjectName);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!hasClasses[0]) {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "No classes found for this student.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Failed to fetch data: " + databaseError.getMessage());
                progressDialog.dismiss();
            }
        });
    }

    private void fetchProfessorName(String profid, String classCode, String subjectName) {
        DatabaseReference profRef = FirebaseDatabase.getInstance().getReference("Registered Users").child(profid);
        profRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstname = snapshot.child("firstname").getValue(String.class);
                    String lastname = snapshot.child("lastname").getValue(String.class);
                    String profFullName = lastname + ", " + firstname;

                    // Add the class details to the container, including professor's name
                    addClassToContainer(classCode, subjectName, profFullName,  profid);
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to fetch professor's name: " + error.getMessage());
                progressDialog.dismiss();
            }
        });
    }


    // Dynamically add class information to the container
    private void addClassToContainer(String classCode, String subjectName, String profFullName, String profid) {
        // Creating a LinearLayout as the container with a background
        LinearLayout classLayout = new LinearLayout(getContext());
        classLayout.setOrientation(LinearLayout.VERTICAL);
        classLayout.setBackgroundResource(R.drawable.border); // Set the drawable background
        classLayout.setPadding(16, 16, 16, 16);
        classLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) classLayout.getLayoutParams()).setMargins(10, 16, 10, 16);

        // Creating TextView for Class Code
        TextView tvClassCode = new TextView(getContext());
        tvClassCode.setText(classCode);
        tvClassCode.setId(View.generateViewId());
        tvClassCode.setTextSize(16);
        tvClassCode.setTypeface(null, Typeface.BOLD);
        tvClassCode.setTextColor(Color.BLACK); // Set text color to white
        tvClassCode.setPadding(8, 8, 0, 4);

        // Creating TextView for Subject Name
        TextView tvSubjectName = new TextView(getContext());
        tvSubjectName.setText(subjectName);
        tvSubjectName.setId(View.generateViewId());
        tvSubjectName.setTextSize(14);
        tvSubjectName.setTextColor(Color.BLACK); // Set text color to white
        tvSubjectName.setPadding(8, 0, 0, 4);

        // Creating TextView for Professor's Full Name
        TextView tvProfName = new TextView(getContext());
        tvProfName.setText(profFullName);
        tvProfName.setId(View.generateViewId());
        tvProfName.setTextSize(14);
        tvProfName.setTextColor(Color.BLACK); // Set text color to white
        tvSubjectName.setPadding(8, 0, 0, 8);

        // Add the TextViews to the container
        classLayout.addView(tvClassCode);
        classLayout.addView(tvSubjectName);
        classLayout.addView(tvProfName);

        // OnClickListener to save profid and classCode to SharedPreferences and navigate to next activity
        classLayout.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("ClassDetails", getContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("profid", profid);
            editor.putString("subjectName", subjectName);
            editor.putString("classCode", classCode);
            editor.apply();

            Intent intent = new Intent(getContext(), Student_show_class_click.class);
            startActivity(intent);
        });

        classContainer.addView(classLayout);
        classViews.add(classLayout);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Close the app when the back button is pressed
                exitApp();
            }
        });
    }

    private void exitApp() {
        // Finish the activity and close the app
        getActivity().finishAffinity();
    }
}