package com.finals.appdev50;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomepageProfessor extends AppCompatActivity {

    private DatabaseReference database;
    private FirebaseAuth auth;
    private LinearLayout classContainer;
    private static final int MENU_PROFILE = 1;
    private static final int MENU_PRIVACY = 2;
    private static final int MENU_TERMS = 3;
    private static final int MENU_RESET_PASSWORD = 4;
    private static final int MENU_LOGOUT = 5;
    private List<View> classViews = new ArrayList<>();
    EditText classcode, subjectname, yearsection;
    Button createadd;
    ProgressBar progress;
    private AlertDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage_professor);

        noInternet.showPendulumDialog(this, getLifecycle());
        try {
            auth = FirebaseAuth.getInstance();
            database = FirebaseDatabase.getInstance().getReference("Classes");
            classContainer = findViewById(R.id.classContainer);

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                loadClasses(userId);
            } else {
                Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            }

            LinearLayout addClass = findViewById(R.id.addClass);
            LinearLayout addClassForm = findViewById(R.id.addClassForm);
            classcode = findViewById(R.id.classCode);
            subjectname = findViewById(R.id.subjectName);
            yearsection = findViewById(R.id.yearAndSection);
            ImageView arrowDown = findViewById(R.id.arrowDown);

            addClass.setOnClickListener(v -> {
                if (addClassForm.getVisibility() == View.GONE) {
                    addClassForm.setVisibility(View.VISIBLE); // Show the form
                    arrowDown.setRotation(180); // Rotate arrow upwards
                } else {
                    addClassForm.setVisibility(View.GONE); // Hide the form
                    classcode.setText("");
                    subjectname.setText("");
                    yearsection.setText("");
                    arrowDown.setRotation(0); // Reset arrow to original position
                }
            });

            progress = findViewById(R.id.progressBar);

                createadd = findViewById(R.id.addButton);
                createadd.setOnClickListener(v -> {
                    try{
                        String classCode = classcode.getText().toString();
                        String subjectName = subjectname.getText().toString();
                        String yearSection = yearsection.getText().toString();

                        // Define a regex to check for the invalid characters
                        String restrictedChars = "[#$\\[\\]/]";  // characters #, $, [, ], /

                        if(TextUtils.isEmpty(yearSection)){
                            Toast.makeText(this, "Enter Year and Section", Toast.LENGTH_SHORT).show();
                            yearsection.setError("Please fill Year and Section");
                            yearsection.requestFocus();
                            return;
                        } else if (yearSection.matches(".*" + restrictedChars + ".*")) {
                            yearsection.setError("Year and Section contains invalid characters.");
                            yearsection.requestFocus();
                            return;
                        }

                        if (TextUtils.isEmpty(classCode)) {
                            Toast.makeText(this, "Enter Class Code", Toast.LENGTH_SHORT).show();
                            classcode.setError("Please fill Class Code");
                            classcode.requestFocus();
                            return;
                        } else if (classCode.matches(".*" + restrictedChars + ".*")) {
                            classcode.setError("Class Code contains invalid characters.");
                            classcode.requestFocus();
                            return;
                        }

                        if (TextUtils.isEmpty(subjectName)) {
                            Toast.makeText(this, "Enter Subject Name", Toast.LENGTH_SHORT).show();
                            subjectname.setError("Please fill Subject Name");
                            subjectname.requestFocus();
                            return;
                        } else if (subjectName.matches(".*" + restrictedChars + ".*")) {
                            subjectname.setError("Subject Name contains invalid characters.");
                            subjectname.requestFocus();
                            return;
                        }

                        progress.setVisibility(View.VISIBLE);
                        showProgressDialog("Adding Class...");

                        //FirebaseUser currentUser = auth.getCurrentUser();
                        if (currentUser != null){
                            String userId = currentUser.getUid();

                            database.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean isDuplicate = false;
                                    String classCodeLower = classCode.toLowerCase();
                                    String subjectNameLower = subjectName.toLowerCase();

                                    for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                                        String existingClassCode = classSnapshot.child("classCode").getValue(String.class);
                                        String existingSubjectName = classSnapshot.child("className").getValue(String.class);

                                        if (existingClassCode != null && existingClassCode.toLowerCase().equals(classCodeLower)) {
                                            classcode.setError("Classcode already exist");
                                            classcode.requestFocus();
                                            isDuplicate = true;
                                            break;
                                        }

                                        if (existingSubjectName != null && existingSubjectName.toLowerCase().equals(subjectNameLower)) {
                                            subjectname.setError("Classcode already exist");
                                            subjectname.requestFocus();
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    if (!isDuplicate) {
                                        // Proceed with adding the class if no duplicates are found
                                        addClassToFirebase(classCode, subjectName, yearSection, userId);
                                        hideProgressDialog();
                                    } else {
                                        progress.setVisibility(View.GONE);
                                        hideProgressDialog();
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    hideProgressDialog();
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(HomepageProfessor.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "An error occurred: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        ex.printStackTrace(); // Log the exception
                    }
                });
                ImageView profileIcon = findViewById(R.id.profileIcon);

                profileIcon.setOnClickListener(v -> {
                    Log.d("PopupMenu", "Profile icon clicked");
                    // Create PopupMenu
                    PopupMenu popupMenu = new PopupMenu(this, profileIcon);

                    // Add menu items programmatically with constant IDs
                    popupMenu.getMenu().add(0, MENU_PROFILE, 0, "Profile");
                    popupMenu.getMenu().add(0, MENU_PRIVACY, 1, "Privacy Policy");
                    popupMenu.getMenu().add(0, MENU_TERMS, 2, "Terms and Conditions");
                    popupMenu.getMenu().add(0, MENU_RESET_PASSWORD, 3, "Reset Password");
                    popupMenu.getMenu().add(0, MENU_LOGOUT, 4, "Log Out");

                    // Set click listeners for menu items
                    popupMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case MENU_PROFILE: // Profile
                                Intent profileIntent = new Intent(this, Professor_settings_profile.class);
                                startActivity(profileIntent);
                                return true;

                            case MENU_PRIVACY:
                                Intent privacypolicy = new Intent(this, privacyPolicy.class);
                                startActivity(privacypolicy);
                                return true;

                            case MENU_TERMS:
                                Intent termsandcon = new Intent(this, TermsandConditions.class);
                                startActivity(termsandcon);
                                return true;

                            case MENU_RESET_PASSWORD: // Reset Password
                                Intent resetPasswordIntent = new Intent(this, Professor_settings_resetpassword.class);
                                startActivity(resetPasswordIntent);
                                return true;

                            case MENU_LOGOUT: // Log Out
                                new AlertDialog.Builder(this)
                                        .setTitle("Log Out")
                                        .setMessage("Are you sure you want to log out?")
                                        .setPositiveButton("Yes", (dialog, which) -> {
                                            FirebaseAuth.getInstance().signOut();
                                            Intent logoutIntent = new Intent(this, MainActivity.class);
                                            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(logoutIntent);
                                        })
                                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                                        .create()
                                        .show();
                                return true;

                            default:
                                return false;
                        }
                    });

                    // Show the menu
                    popupMenu.show();
                });

                EditText etSearch = findViewById(R.id.searchInput);
            for (int i = 0; i < classContainer.getChildCount(); i++) {
                classViews.add(classContainer.getChildAt(i));
            }
                etSearch.clearFocus();
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterAndHighlightClasses(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) { }
                });

                etSearch.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        // Reset all highlights if the search bar goes invisible
                        resetClassContainers();
                        etSearch.setText("");
                    }
                });

                View rootView = findViewById(android.R.id.content);
                rootView.setOnClickListener(v -> {
                    hideKeyboardAndClearFocus(v);
                });

            } catch (Exception e) {
                Log.e("ProfessorManageClass", "Error in onCreateView: " + e.getMessage());
                Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void loadClasses(String userId) {
            try {
                showProgressDialog("Loading Classes");
                // Query Firebase to get all classes associated with the user ID
                database.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            classContainer.removeAllViews(); // Clear previous entries

                            if (snapshot.exists()) {
                                // Iterate through the classes
                                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                                    String classId = classSnapshot.getKey();
                                    String classCode = classSnapshot.child("classCode").getValue(String.class);
                                    String subjectName = classSnapshot.child("className").getValue(String.class);
                                    String yearSection = classSnapshot.child("yearSection").getValue(String.class);

                                    // Dynamically create TextViews to display class details
                                    if (classCode != null && subjectName != null && yearSection != null) {
                                        addClassView(classCode, subjectName, yearSection);
                                    }
                                }
                            }
                            hideProgressDialog();
                        } catch (Exception e) {
                            Log.e("loadClasses", "Error processing data: " + e.getMessage());
                            hideProgressDialog();
                            Toast.makeText(HomepageProfessor.this, "An error occurred while loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideProgressDialog();
                        Toast.makeText(HomepageProfessor.this, "Failed to load classes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("loadClasses", "Error in Firebase query: " + e.getMessage());
                hideProgressDialog();
                Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void addClassView(String classCode, String subjectName, String yearSection) {
            try {
                // Create a parent layout for the card
                LinearLayout cardLayout = new LinearLayout(this);
                cardLayout.setOrientation(LinearLayout.HORIZONTAL);
                cardLayout.setPadding(16, 16, 16, 16);
                cardLayout.setBackgroundResource(R.drawable.border); // Add a rounded background drawable

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(0, 0, 0, 32); // Increased bottom margin to 32dp (change as needed)
                cardLayout.setLayoutParams(layoutParams);
                cardLayout.setGravity(Gravity.CENTER_VERTICAL);

                // Create and add the icon (student_classes)
                ImageView iconView = new ImageView(this);
                iconView.setImageResource(R.drawable.student_classes);
                iconView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                iconView.setPadding(8, 8, 8, 8);
                cardLayout.addView(iconView);

                LinearLayout textLayout = new LinearLayout(this);
                textLayout.setOrientation(LinearLayout.VERTICAL);
                textLayout.setPadding(30, 0, 0, 0); // Add padding between icon and text
                textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1 // Weight to take up remaining space
                ));

                // Create and add the class code TextView (bold and larger text)
                TextView yearSectionText = new TextView(this);
                yearSectionText.setText(yearSection); // Set class code
                yearSectionText.setTextSize(16); // Larger text
                yearSectionText.setTypeface(null, Typeface.BOLD); // Bold text
                yearSectionText.setTextColor(Color.BLACK); // Set text color to white
                textLayout.addView(yearSectionText);

                TextView classCodeText = new TextView(this); // Add yearSectionText
                classCodeText.setText(classCode); // Set year/section
                classCodeText.setTextSize(12); // Same size as subject name
                classCodeText.setTextColor(Color.BLACK); // Set text color to light gray
                textLayout.addView(classCodeText);

                // Create and add the subject name TextView (smaller text)
                TextView subjectNameText = new TextView(this);
                subjectNameText.setText(subjectName); // Set subject name
                subjectNameText.setTextSize(12); // Smaller text
                subjectNameText.setTextColor(Color.BLACK); // Set text color to light gray
                textLayout.addView(subjectNameText);

                // Add the text layout to the card
                cardLayout.addView(textLayout);

                LinearLayout menuLayout = new LinearLayout(this);
                menuLayout.setOrientation(LinearLayout.HORIZONTAL);
                menuLayout.setGravity(Gravity.END);

                final ImageView menuIcon = new ImageView(this);
                menuIcon.setImageResource(R.drawable.menu); // Three-dot menu icon
                menuIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100)); // Set size for the menu icon
                menuIcon.setPadding(8, 8, 8, 8);

    // Create the closeIcon ImageView (initially hidden)
                final ImageView closeIcon = new ImageView(this);
                closeIcon.setImageResource(R.drawable.close); // Close icon
                closeIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100)); // Set size for the close icon
                closeIcon.setPadding(8, 8, 8, 8);
                closeIcon.setVisibility(View.GONE); // Initially hidden

    // Create the Edit and Delete ImageViews, initially hidden
                final ImageView editIcon = new ImageView(this);
                editIcon.setImageResource(R.drawable.edit); // Edit icon
                editIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                editIcon.setPadding(8, 8, 8, 8);
                editIcon.setVisibility(View.GONE); // Hide the Edit icon initially

                final ImageView deleteIcon = new ImageView(this);
                deleteIcon.setImageResource(R.drawable.delete); // Delete icon
                deleteIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                deleteIcon.setPadding(8, 8, 8, 8);
                deleteIcon.setVisibility(View.GONE); // Hide the Delete icon initially

                editIcon.setTag(classCode); // Set the class UID
                deleteIcon.setTag(classCode);
                cardLayout.setTag(new View[]{editIcon, deleteIcon, closeIcon, menuIcon});
                menuLayout.addView(editIcon);  // Add Edit icon to the layout
                menuLayout.addView(deleteIcon);  // Add Delete icon to the layout
                menuLayout.addView(menuIcon);
                menuLayout.addView(closeIcon);

                cardLayout.addView(menuLayout);

    // Set OnClickListener for the menuIcon
                menuIcon.setOnClickListener(v -> {
                    // Hide the menuIcon, show the closeIcon, and show Edit and Delete icons
                    menuIcon.setVisibility(View.GONE); // Hide the menuIcon
                    closeIcon.setVisibility(View.VISIBLE); // Show the closeIcon
                    editIcon.setVisibility(View.VISIBLE); // Show the Edit icon
                    deleteIcon.setVisibility(View.VISIBLE); // Show the Delete icon
                });

    // Set OnClickListener for the closeIcon
                closeIcon.setOnClickListener(v -> {
                    // Hide the closeIcon, show the menuIcon, and hide Edit and Delete icons
                    closeIcon.setVisibility(View.GONE); // Hide the closeIcon
                    menuIcon.setVisibility(View.VISIBLE); // Show the menuIcon
                    editIcon.setVisibility(View.GONE); // Hide the Edit icon
                    deleteIcon.setVisibility(View.GONE); // Hide the Delete icon
                });

                final String finalClassCode = classCode;
                final String finalSubjectName = subjectName;
                final String finalYearSection = yearSection;

                editIcon.setOnClickListener(v -> {
                    showProgressDialog("Loading");
                    // Retrieve classCode from tag (assuming classCode is set here, not the classUid)
                    String classuid1 = (String) v.getTag();
                    if (classuid1 == null) {
                        hideProgressDialog();
                        Toast.makeText(this, "Error: Class Code is null!", Toast.LENGTH_SHORT).show();
                        Log.e("EditAction", "editIcon Tag is null! Ensure setTag(classCode) is properly called.");
                        return;
                    }

                    Log.d("EditAction", "Class Code to edit: " + classuid1);

                    // Query Firebase to get the class UID based on classCode
                    DatabaseReference classesRef = FirebaseDatabase.getInstance().getReference("Classes");
                    classesRef.orderByChild("classCode").equalTo(classuid1).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // We found the class, get the classUID
                                for (DataSnapshot classSnapshot : dataSnapshot.getChildren()) {
                                    String classUid = classSnapshot.getKey(); // Get the classUID
                                    Log.d("EditAction", "Class UID to edit: " + classUid);

                                    // Inflate and display the edit dialog
                                    LayoutInflater inflater = LayoutInflater.from(HomepageProfessor.this);
                                    View dialogView = inflater.inflate(R.layout.dialog_edit_class, null);

                                    EditText editYearSection = dialogView.findViewById(R.id.editYearSection);
                                    EditText editClassCode = dialogView.findViewById(R.id.editClassCode);
                                    EditText editSubjectName = dialogView.findViewById(R.id.editSubjectName);
                                    Button btnSave = dialogView.findViewById(R.id.btnSave);
                                    Button btnCancel = dialogView.findViewById(R.id.btnCancel);

                                    // Fetch current class details
                                    classesRef.child(classUid).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                String currentYearSection = snapshot.child("yearSection").getValue(String.class);
                                                String currentClassCode = snapshot.child("classCode").getValue(String.class);
                                                String currentSubjectName = snapshot.child("className").getValue(String.class);

                                                // Populate the dialog with the class details
                                                editYearSection.setText(currentYearSection);
                                                editClassCode.setText(currentClassCode);
                                                editSubjectName.setText(currentSubjectName);

                                                AlertDialog dialog = new AlertDialog.Builder(HomepageProfessor.this)
                                                        .setView(dialogView)
                                                        .setCancelable(false)
                                                        .create();
                                                hideProgressDialog();

                                                dialog.show();

                                                btnCancel.setOnClickListener(v -> dialog.dismiss());

                                                // Save button validation and update logic
                                                btnSave.setOnClickListener(v -> {
                                                    String newYearSection = editYearSection.getText().toString().trim();
                                                    String newClassCode = editClassCode.getText().toString().trim();
                                                    String newSubjectName = editSubjectName.getText().toString().trim();

                                                    String restrictedChars = "[#$\\[\\]/]";  // characters #, $, [, ], /

                                                    if (newYearSection.equals(currentYearSection) &&
                                                            newClassCode.equals(currentClassCode) &&
                                                            newSubjectName.equals(currentSubjectName)) {
                                                        Toast.makeText(HomepageProfessor.this, "No changes have been made.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }

                                                    if(TextUtils.isEmpty(newYearSection)){
                                                        Toast.makeText(HomepageProfessor.this, "Enter Year and Section", Toast.LENGTH_SHORT).show();
                                                        editYearSection.setError("Please fill Year and Section");
                                                        editYearSection.requestFocus();
                                                        return;
                                                    } else if (newYearSection.matches(".*" + restrictedChars + ".*")) {
                                                        editYearSection.setError("Year and Section contains invalid characters.");
                                                        editYearSection.requestFocus();
                                                        return;
                                                    }

                                                    if (TextUtils.isEmpty(newClassCode)) {
                                                        Toast.makeText(HomepageProfessor.this, "Enter Class Code", Toast.LENGTH_SHORT).show();
                                                        editClassCode.setError("Please fill Class Code");
                                                        editClassCode.requestFocus();
                                                        return;
                                                    } else if (newClassCode.matches(".*" + restrictedChars + ".*")) {
                                                        editClassCode.setError("Class Code contains invalid characters.");
                                                        editClassCode.requestFocus();
                                                        return;
                                                    }

                                                    if (TextUtils.isEmpty(newSubjectName)) {
                                                        Toast.makeText(HomepageProfessor.this, "Enter Subject Name", Toast.LENGTH_SHORT).show();
                                                        editSubjectName.setError("Please fill Subject Name");
                                                        editSubjectName.requestFocus();
                                                        return;
                                                    } else if (newSubjectName.matches(".*" + restrictedChars + ".*")) {
                                                        editSubjectName.setError("Subject Name contains invalid characters.");
                                                        editSubjectName.requestFocus();
                                                        return;
                                                    }

                                                    classesRef.orderByChild("classCode").equalTo(newClassCode).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if (snapshot.exists() && !snapshot.hasChild(classUid)) {
                                                                // Class code already exists in another class
                                                                editClassCode.setError("Class Code already exists.");
                                                                editClassCode.requestFocus();
                                                            } else {
                                                                // Prepare updates
                                                                Map<String, Object> updates = new HashMap<>();
                                                                updates.put("yearSection", newYearSection);
                                                                updates.put("classCode", newClassCode);
                                                                updates.put("className", newSubjectName);

                                                                // Update Firebase database
                                                                classesRef.child(classUid).updateChildren(updates)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Toast.makeText(HomepageProfessor.this, "Class updated successfully!", Toast.LENGTH_SHORT).show();
                                                                            dialog.dismiss();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Log.e("EditAction", "Error updating class: " + e.getMessage());
                                                                            Toast.makeText(HomepageProfessor.this, "Failed to update class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        });

                                                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                                                if (currentUser != null) {
                                                                    String userId = currentUser.getUid();
                                                                    loadClasses(userId);
                                                                } else {
                                                                    Toast.makeText(HomepageProfessor.this, "User not logged in!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Log.e("EditAction", "Error checking class code availability: " + error.getMessage());
                                                            Toast.makeText(HomepageProfessor.this, "Error checking class code availability: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                });
                                            } else {
                                                Toast.makeText(HomepageProfessor.this, "Class details not found", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("EditAction", "Error querying class details: " + error.getMessage());
                                            Toast.makeText(HomepageProfessor.this, "Error querying class details: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                // Class with the given classCode not found
                                Toast.makeText(HomepageProfessor.this, "Class not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("EditAction", "Error querying Firebase: " + error.getMessage());
                            Toast.makeText(HomepageProfessor.this, "Error querying Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                deleteIcon.setOnClickListener(v -> {
                    showProgressDialog("Loading");
                    String classuid = (String) v.getTag(); // Retrieve classCode from tag
                    if (classuid == null) {
                        hideProgressDialog();
                        Toast.makeText(this, "Error: Class Code is null!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Log the classCode for debugging
                    Log.d("DeleteAction", "Class Code to delete: " + classuid);

                    // Reference to the Classes and Students paths in the Firebase database
                    DatabaseReference classesRef = FirebaseDatabase.getInstance().getReference("Classes");
                    DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");

                    // Query Firebase to find the classUID based on classCode
                    classesRef.orderByChild("classCode").equalTo(classuid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // We found the class, get the classUID
                                for (DataSnapshot classSnapshot : dataSnapshot.getChildren()) {
                                    String classUID = classSnapshot.getKey(); // Get the classUID
                                    Log.d("DeleteAction", "Class UID to delete: " + classUID);

                                    // Confirm deletion
                                    new AlertDialog.Builder(HomepageProfessor.this)
                                            .setTitle("Delete Confirmation")
                                            .setMessage("Are you sure you want to delete this class?")
                                            .setPositiveButton("Yes", (dialog, which) -> {
                                                // First, delete the class in the 'Classes' node
                                                classesRef.child(classUID).removeValue()
                                                        .addOnSuccessListener(aVoid -> {
                                                            hideProgressDialog();
                                                            Log.d("DeleteAction", "Successfully deleted class with UID: " + classUID);

                                                            // Now, delete the class in the 'Students' section
                                                            // Assuming the professor UID is stored as part of the path
                                                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                                            if (currentUser != null) {
                                                                String professorUID = currentUser.getUid();

                                                                // Now, delete the class based on yearSection, subjectName, and classCode
                                                                studentsRef.child(professorUID) // Go to the professor's node
                                                                        .child(finalYearSection) // Go to the year and section
                                                                        .child(finalSubjectName) // Go to the subject name
                                                                        .child(finalClassCode) // Use the classCode to locate the class
                                                                        .removeValue() // Remove the class
                                                                        .addOnSuccessListener(aVoid1 -> {
                                                                            Log.d("DeleteAction", "Successfully deleted class from Students section: " + finalClassCode);
                                                                            hideProgressDialog();
                                                                            Toast.makeText(HomepageProfessor.this, "Class deleted from Students section", Toast.LENGTH_SHORT).show();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            hideProgressDialog();
                                                                            Log.e("DeleteAction", "Error deleting class from Students: " + e.getMessage());
                                                                        });
                                                            }

                                                            // After successful deletion, remove the class from the UI
                                                            ((ViewGroup) cardLayout.getParent()).removeView(cardLayout);
                                                            hideProgressDialog();
                                                            Toast.makeText(HomepageProfessor.this, "Class deleted successfully", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            hideProgressDialog();
                                                            Log.e("DeleteAction", "Error deleting class with UID: " + classUID, e);
                                                            Toast.makeText(HomepageProfessor.this, "Failed to delete class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                            .show();
                                    hideProgressDialog();
                                }
                            } else {
                                // If no class with the given classCode is found
                                hideProgressDialog();
                                Toast.makeText(HomepageProfessor.this, "Class not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            hideProgressDialog();
                            Log.e("DeleteAction", "Error querying Firebase: " + databaseError.getMessage());
                            Toast.makeText(HomepageProfessor.this, "Error querying Firebase: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                cardLayout.setOnTouchListener((v, event) -> {
                    if (editIcon.getVisibility() == View.VISIBLE || deleteIcon.getVisibility() == View.VISIBLE) {
                        editIcon.setVisibility(View.GONE);
                        deleteIcon.setVisibility(View.GONE);
                        closeIcon.setVisibility(View.GONE);
                        menuIcon.setVisibility(View.VISIBLE);
                        return true;
                    }
                    return false;
                });

                cardLayout.setOnClickListener(v -> {
                    // Save class details in SharedPreferences
                    SharedPreferences sharedPreferences = this.getSharedPreferences("ClassDetails", this.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("classCode", finalClassCode);
                    editor.putString("subjectName", finalSubjectName);
                    editor.putString("yearSection", finalYearSection);
                    editor.apply();

                    // Navigate to the new activity
                    Intent intent = new Intent(this, Professor_manage_class_click.class);
                    startActivity(intent);
                });

                // Add the completed card layout to the container
                classContainer.addView(cardLayout);
                classViews.add(cardLayout);
            } catch (Exception e) {
                Log.e("addClassView", "Error adding class view: " + e.getMessage());
                Toast.makeText(this, "An error occurred while adding class view: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    private void filterAndHighlightClasses(String searchTerm) {
        if (searchTerm.trim().isEmpty()) {
            resetClassContainers(); // Reset if search term is empty
            return;
        }

        for (View classView : classViews) {
            LinearLayout textLayout = (LinearLayout) ((LinearLayout) classView).getChildAt(1); // Access text layout
            TextView tvClassCode = (TextView) textLayout.getChildAt(0); // Class code TextView
            TextView tvSubjectName = (TextView) textLayout.getChildAt(1); // Subject name TextView

            boolean isMatched = highlightTextView(tvClassCode, searchTerm) |
                    highlightTextView(tvSubjectName, searchTerm);

            classView.setVisibility(isMatched ? View.VISIBLE : View.GONE); // Show or hide the card
        }
    }

    private boolean highlightTextView(TextView textView, String searchTerm) {
        String text = textView.getText().toString();
        String lowerText = text.toLowerCase();
        String lowerSearchTerm = searchTerm.toLowerCase();

        if (lowerText.contains(lowerSearchTerm)) {
            SpannableString spannable = new SpannableString(text);
            int startIndex = lowerText.indexOf(lowerSearchTerm);

            while (startIndex != -1) {
                int endIndex = startIndex + searchTerm.length();
                spannable.setSpan(new BackgroundColorSpan(Color.YELLOW), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(Color.BLACK), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = lowerText.indexOf(lowerSearchTerm, endIndex); // Find next occurrence
            }

            textView.setText(spannable, TextView.BufferType.SPANNABLE);
            return true;
        } else {
            textView.setText(text, TextView.BufferType.NORMAL); // Reset text to normal if no match
            return false;
        }
    }
    private void resetClassContainers() {
        for (View classView : classViews) {
            classView.setVisibility(View.VISIBLE); // Make all views visible

            LinearLayout textLayout = (LinearLayout) ((LinearLayout) classView).getChildAt(1); // Access text layout
            TextView tvClassCode = (TextView) textLayout.getChildAt(0); // Class code TextView
            TextView tvSubjectName = (TextView) textLayout.getChildAt(1); // Subject name TextView

            // Reset to original text without highlights
            tvClassCode.setText(tvClassCode.getText().toString());
            tvSubjectName.setText(tvSubjectName.getText().toString());
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

        // Iterate through all child views of the container to find which card was touched
        for (int i = 0; i < classContainer.getChildCount(); i++) {
            View card = classContainer.getChildAt(i);
            View[] icons = (View[]) card.getTag(); // Get icons from tag
            ImageView editIcon = (ImageView) icons[0];
            ImageView deleteIcon = (ImageView) icons[1];
            ImageView closeIcon = (ImageView) icons[2];
            ImageView menuIcon = (ImageView) icons[3];

            if (editIcon != null && deleteIcon != null) {
                Rect menuRect = new Rect();
                card.getGlobalVisibleRect(menuRect); // Get the bounds of the current card

                if (!menuRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    // If touch is outside the card's menu area, hide the menu icons
                    editIcon.setVisibility(View.GONE);
                    deleteIcon.setVisibility(View.GONE);
                    closeIcon.setVisibility(View.GONE);
                    menuIcon.setVisibility(View.VISIBLE);
                }
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

            private void addClassToFirebase(String classCode, String subjectName, String yearSection, String userId) {
                String classId = database.push().getKey();
                // Create HashMap to store class details
                Map<String, Object> classDetails = new HashMap<>();
                classDetails.put("classCode", classCode);
                classDetails.put("className", subjectName);
                classDetails.put("yearSection", yearSection);
                classDetails.put("userId", userId);

                if (classId != null) {
                    // Add class to the Firebase database
                    database.child(classId).setValue(classDetails).addOnCompleteListener(task -> {
                        progress.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Class added successfully!", Toast.LENGTH_SHORT).show();
                            addClassView(classCode, subjectName, yearSection);
                            // Clear the input fields
                            classcode.setText("");
                            subjectname.setText("");
                            yearsection.setText("");
                            LinearLayout addClassForm = findViewById(R.id.addClassForm);
                        } else {
                            Toast.makeText(this, "Failed to add class.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
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

    @Override
    public void onBackPressed() {
        // Close the app when back is pressed
        super.onBackPressed();
        finishAffinity();  // Close all activities in the task stack
    }
}