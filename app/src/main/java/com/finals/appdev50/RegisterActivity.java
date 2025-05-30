package com.finals.appdev50;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText firstname, middlename, lastname, email, dateofBirth, mobileNumber, password, confirmPassword;
    private ProgressBar progressbar;
    private RadioGroup gender;
    private RadioButton genderselected;
    private Button register;
    private DatePickerDialog picker;
    private TextView textview;
    FirebaseAuth mAuth;
    private ImageView hideshowpass;
    private ImageView hideshowconfirmpass;
    private Spinner spinnerProvince, spinnerCity, spinnerBarangay;
    private ArrayAdapter<String> cityAdapter;
    private ArrayAdapter<String> barangayAdapter;
    private List<String> cityList = new ArrayList<>();
    private List<String> barangayList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        noInternet.showPendulumDialog(this, getLifecycle());

        Toast.makeText(this, "You can now Register", Toast.LENGTH_SHORT).show();
        mAuth= FirebaseAuth.getInstance();
        spinnerProvince = findViewById(R.id.spinner_province);
        spinnerCity = findViewById(R.id.spinner_city);
        spinnerBarangay = findViewById(R.id.spinner_barangay);
        firstname = findViewById(R.id.etFirstName);
        middlename = findViewById(R.id.etMiddleName);
        lastname = findViewById(R.id.etLastName);
        email = findViewById(R.id.etEmail);
        dateofBirth = findViewById(R.id.etDateofBirth);
        mobileNumber = findViewById(R.id.etMobileNumber);
        password = findViewById(R.id.etPassword);
        TextView tvMinCharacters = findViewById(R.id.tv_min_characters);
        TextView tvUppercase = findViewById(R.id.tv_uppercase);
        TextView tvNumber = findViewById(R.id.tv_number);
        TextView tvSpecialChar = findViewById(R.id.tv_special_char);

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Closes the activity and goes back to the previous one
            }
        });

        // Initialize Province Spinner
        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(
                this, R.array.province_array, android.R.layout.simple_spinner_item);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        // Set default adapters for City and Barangay spinners
        cityList.add("Select City/Town");
        barangayList.add("Select Barangay");

        cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
        spinnerCity.setEnabled(false); // Initially disabled

        barangayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangayList);
        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(barangayAdapter);
        spinnerBarangay.setEnabled(false); // Initially disabled

// Province Spinner Selection Listener
        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedProvince = parentView.getItemAtPosition(position).toString();

                // Get the city array ID based on selected province
                int cityArrayId = getCityArrayId(selectedProvince);

                if (cityArrayId != -1) {
                    // Enable City spinner and populate cities
                    spinnerCity.setEnabled(true);

                    // Clear and reset city and barangay lists
                    cityList.clear();
                    barangayList.clear();
                    cityList.add("Select City/Town");
                    barangayList.add("Select Barangay");

                    // Populate the city spinner dynamically based on the province selected
                    cityAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, getResources().getStringArray(cityArrayId));
                    cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCity.setAdapter(cityAdapter);

                    // Reset Barangay spinner to default state (disabled and empty)
                    spinnerBarangay.setEnabled(false);
                    barangayAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, barangayList);
                    barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBarangay.setAdapter(barangayAdapter);

                } else {
                    // If no valid city data is available for the province, disable the City and Barangay spinners
                    spinnerCity.setEnabled(false);
                    cityList.clear();
                    cityList.add("Select City/Town");
                    cityAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, cityList);
                    cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCity.setAdapter(cityAdapter);

                    spinnerBarangay.setEnabled(false);
                    barangayList.clear();
                    barangayList.add("Select Barangay");
                    barangayAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, barangayList);
                    barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBarangay.setAdapter(barangayAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // No action when nothing is selected
            }
        });

// City Spinner Selection Listener
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedCity = parentView.getItemAtPosition(position).toString();

                if (!"Select City/Town".equals(selectedCity)) {
                    // Enable Barangay spinner and populate barangays once a city is selected
                    spinnerBarangay.setEnabled(true);

                    // Clear and populate barangays based on the selected city
                    barangayList.clear();
                    barangayList.add("Select Barangay");

                    int barangayArrayId = getBarangayArrayId(selectedCity);

                    if (barangayArrayId != -1) {
                        // Populate barangays for the selected city
                        barangayAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, getResources().getStringArray(barangayArrayId));
                        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBarangay.setAdapter(barangayAdapter);
                    }
                } else {
                    // Disable Barangay spinner if no city is selected
                    spinnerBarangay.setEnabled(false);
                    barangayList.clear();
                    barangayList.add("Select Barangay");
                    barangayAdapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, barangayList);
                    barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBarangay.setAdapter(barangayAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // No action when nothing is selected
            }
        });

        Spinner spinnerRole = findViewById(R.id.spinner_role);
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(
                this, R.array.roles_array, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// Set adapter to the spinner
        spinnerRole.setAdapter(roleAdapter);

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String password = charSequence.toString();

                // Check if password meets conditions and update text color
                // At least 8 characters
                if (password.length() >= 8) {
                    tvMinCharacters.setTextColor(Color.GREEN);
                } else {
                    tvMinCharacters.setTextColor(Color.RED);
                }

                // At least one uppercase letter
                if (password.matches(".*[A-Z].*")) {
                    tvUppercase.setTextColor(Color.GREEN);
                } else {
                    tvUppercase.setTextColor(Color.RED);
                }

                // At least one number
                if (password.matches(".*\\d.*")) {
                    tvNumber.setTextColor(Color.GREEN);
                } else {
                    tvNumber.setTextColor(Color.RED);
                }

                // At least one special character
                if (password.matches(".*[!@#$%^&*(),.?\":{}|<>_].*")) {
                    tvSpecialChar.setTextColor(Color.GREEN);
                } else {
                    tvSpecialChar.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        confirmPassword = findViewById(R.id.etConfirmPassword);
        gender = findViewById(R.id.rgGender);
        gender.clearCheck();
        textview = findViewById(R.id.tvloginnow);
        progressbar = findViewById(R.id.pbProgressBar);
        textview.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

        hideshowpass = findViewById(R.id.showpassword);
        hideshowpass.setImageResource(R.drawable.hide);
        hideshowpass.setOnClickListener(v -> {
            if(password.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowpass.setImageResource(R.drawable.hide);
            } else {
                password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowpass.setImageResource(R.drawable.visible);
            }
        });

        hideshowconfirmpass = findViewById(R.id.showconfirmpassword);
        hideshowconfirmpass.setImageResource(R.drawable.hide);
        hideshowconfirmpass.setOnClickListener(v -> {
            if(confirmPassword.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                hideshowconfirmpass.setImageResource(R.drawable.hide);
            } else {
                confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                hideshowconfirmpass.setImageResource(R.drawable.visible);
            }
        });

        mobileNumber.setText("09"); // Initialize with "09"
        mobileNumber.setSelection(mobileNumber.getText().length()); // Set cursor position at the end

        mobileNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Ensure the text starts with "09"
                if (!s.toString().startsWith("09")) {
                    mobileNumber.setText("09");
                    mobileNumber.setSelection(mobileNumber.getText().length()); // Keep cursor at the end
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Limit input to exactly 11 characters (including "09")
                if (s.length() > 11) {
                    s.delete(11, s.length());
                }
            }
        });

// Prevent the cursor from being placed before "09"
        mobileNumber.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (mobileNumber.getSelectionStart() < 2) {
                    mobileNumber.setSelection(mobileNumber.getText().length());
                }
            }
            return false;
        });


        dateofBirth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);

            // Calculate the maximum allowable date (today - 8 years)
            calendar.add(Calendar.YEAR, -8);
            long maxDate = calendar.getTimeInMillis();

            // Reset calendar to the current date
            calendar = Calendar.getInstance();

            picker = new DatePickerDialog(RegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                    dateofBirth.setText(i2 + "/" + (i1 + 1) + "/" + i);
                }
            }, year, month, day);

            // Set the maximum date to today - 8 years
            picker.getDatePicker().setMaxDate(maxDate);

            picker.show();
        });

        register = findViewById(R.id.btnRegister2);
        register.setOnClickListener(v -> {
            int selectedgenderid = gender.getCheckedRadioButtonId();
            genderselected = findViewById(selectedgenderid);

            String fname = capitalizeWords(firstname.getText().toString());
            String mname = capitalizeWords(middlename.getText().toString());
            String lname = capitalizeWords(lastname.getText().toString());
            String emailtext = email.getText().toString();
            String dateofbirthtext = dateofBirth.getText().toString();
            String mobilenumbertext = mobileNumber.getText().toString();
            String province = spinnerProvince.getSelectedItem().toString();
            String city = spinnerCity.getSelectedItem().toString();
            String barangay = spinnerBarangay.getSelectedItem().toString();

            // Concatenate Barangay, City, and Province to form the full address
            String addressText = barangay + ", " + city + ", " + province;
            String passwordtext = password.getText().toString();
            String confirmpasswordtext = confirmPassword.getText().toString();
            String selectedRole = spinnerRole.getSelectedItem().toString();

            if(TextUtils.isEmpty(fname)){
                Toast.makeText(this, "Please Enter your Full Name", Toast.LENGTH_SHORT).show();
                firstname.setError("Please enter your full name.");
                firstname.requestFocus();
            } else if(TextUtils.isEmpty(lname)){
                Toast.makeText(this, "Please Enter your Full Name", Toast.LENGTH_SHORT).show();
                lastname.setError("Please enter your full name.");
                lastname.requestFocus();
            } else if (TextUtils.isEmpty(emailtext)) {
                Toast.makeText(this, "Please Enter your Email Address", Toast.LENGTH_SHORT).show();
                email.setError("Please enter your email address.");
                email.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailtext).matches()){
                Toast.makeText(this, "Please Re-enter your Email Address", Toast.LENGTH_SHORT).show();
                email.setError("Valid email address is required.");
                email.requestFocus();
            } else if (TextUtils.isEmpty(dateofbirthtext)){
                Toast.makeText(this, "Please Enter your date of birth", Toast.LENGTH_SHORT).show();
                dateofBirth.setError("Please enter your date of birth.");
                dateofBirth.requestFocus();
            } else if (gender.getCheckedRadioButtonId() == -1){
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(mobilenumbertext)) {
                Toast.makeText(this, "Please Enter your mobile number", Toast.LENGTH_SHORT).show();
                mobileNumber.setError("Please enter your mobile number.");
                mobileNumber.requestFocus();
            } else if (mobilenumbertext.length() != 11 || !mobilenumbertext.startsWith("09")) {
                Toast.makeText(this, "Please re-enter mobile number", Toast.LENGTH_SHORT).show();
                mobileNumber.setError("Valid mobile number is required (11 digits starting with 09).");
                mobileNumber.requestFocus();
            } else if ("Select Province".equals(province)) {
                Toast.makeText(this, "Please select your province", Toast.LENGTH_SHORT).show();
                ((TextView) spinnerProvince.getSelectedView()).setError("Please select your province.");
                ((TextView) spinnerProvince.getSelectedView()).setTextColor(Color.RED); // Highlight in red
                spinnerProvince.requestFocus();
            } else if ("Select City/Town".equals(city)) {
                Toast.makeText(this, "Please select your city/town", Toast.LENGTH_SHORT).show();
                ((TextView) spinnerCity.getSelectedView()).setError("Please select your city/town.");
                ((TextView) spinnerCity.getSelectedView()).setTextColor(Color.RED); // Highlight in red
                spinnerCity.requestFocus();
            } else if ("Select Barangay".equals(barangay)) {
                Toast.makeText(this, "Please select your barangay", Toast.LENGTH_SHORT).show();
                ((TextView) spinnerBarangay.getSelectedView()).setError("Please select your barangay.");
                ((TextView) spinnerBarangay.getSelectedView()).setTextColor(Color.RED); // Highlight in red
                spinnerBarangay.requestFocus();
            } else if (TextUtils.isEmpty(passwordtext)){
                Toast.makeText(this, "Please Enter password", Toast.LENGTH_SHORT).show();
                password.setError("Please enter password.");
                password.requestFocus();
            } else if (!isValidPassword(passwordtext)){
                Toast.makeText(this, "Password too weak", Toast.LENGTH_SHORT).show();
                password.setError("Enter strong password.");
                password.requestFocus();
            } else if (TextUtils.isEmpty(confirmpasswordtext)){
                Toast.makeText(this, "Please Enter your password to confirm", Toast.LENGTH_SHORT).show();
                confirmPassword.setError("Enter password confirmation.");
                confirmPassword.requestFocus();
            } else if (!passwordtext.equals(confirmpasswordtext)){
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show();
                password.setError("Password Confirmation is required.");
                password.requestFocus();
                password.getText().clear();
                confirmPassword.getText().clear();
            } else if (selectedRole.equals("Select Role")) {
                Toast.makeText(RegisterActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                ((TextView) spinnerRole.getSelectedView()).setError("Please select your city/town.");
                ((TextView) spinnerRole.getSelectedView()).setTextColor(Color.RED); // Highlight in red
                spinnerRole.requestFocus();
            }else {

                genderselected = findViewById(selectedgenderid);
                String gendertext = genderselected.getText().toString();
                progressbar.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(emailtext, passwordtext)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressbar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {

                                    FirebaseUser firebaseuser = mAuth.getCurrentUser();

                                    try {
                                        ReadWriteUserDetails writeUserDetails = new ReadWriteUserDetails(fname, mname, lname, dateofbirthtext, gendertext, mobilenumbertext, addressText, selectedRole, emailtext);
                                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                                        referenceProfile.child(firebaseuser.getUid()).setValue(writeUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this, "Account Created.", Toast.LENGTH_SHORT).show();

                                                    // Now redirect to MainActivity after registration
                                                    if(selectedRole.equals("Student")){
                                                        Intent intent = new Intent(RegisterActivity.this, ValidateEmail.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear back stack
                                                        startActivity(intent);
                                                        finish();
                                                    } else if(selectedRole.equals("Professor")){
                                                        Intent intent = new Intent(RegisterActivity.this, ValidateEmail.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear back stack
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Clear back stack
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                } else {
                                                    Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } catch (Exception ex) {
                                        Toast.makeText(RegisterActivity.this, "Registration failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // If sign in fails, display a message to the user.
                                    try {
                                        // Throw the exception to catch the error
                                        throw task.getException();
                                    } catch (FirebaseAuthWeakPasswordException e) {
                                        Toast.makeText(RegisterActivity.this, "Error: Weak Password.", Toast.LENGTH_SHORT).show();
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        Toast.makeText(RegisterActivity.this, "Error: Invalid Email.", Toast.LENGTH_SHORT).show();
                                    } catch (FirebaseAuthUserCollisionException e) {
                                        Toast.makeText(RegisterActivity.this, "Error: Email Already Exists.", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        // Generic error message for other cases
                                        Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });

    }

    private int getCityArrayId(String province) {
        switch (province) {
            case "Laguna":
                return R.array.city_array;  // Ensure this array exists in strings.xml
            default:
                return -1;  // Return -1 if no matching province
        }
    }

    private int getBarangayArrayId(String city) {
        switch (city) {
            case "Alaminos": return R.array.AlaminosBarangay;
            case "Bay": return R.array.BayBarangay;
            case "Biñan": return R.array.BinanBarangay;
            case "Cabuyao": return R.array.CabuyaoBarangay;
            case "Calamba": return R.array.CalambaBarangay;
            case "Calauan": return R.array.CalauanBarangay;
            case "Cavinti": return R.array.CavintiBarangay;
            case "Famy": return R.array.FamyBarangay;
            case "Kalayaan": return R.array.KalayaanBarangay;
            case "Liliw": return R.array.LiliwBarangay;
            case "Los Baños": return R.array.LosBanosBarangay;
            case "Luisiana": return R.array.LuisianaBarangay;
            case "Lumban": return R.array.LumbanBarangay;
            case "Mabitac": return R.array.MabitacBarangay;
            case "Magdalena": return R.array.MagdalenaBarangay;
            case "Majayjay": return R.array.MajayjayBarangay;
            case "Nagcarlan": return R.array.NagcarlanBarangay;
            case "Paete": return R.array.PaeteBarangay;
            case "Pagsanjan": return R.array.PagsanjanBarangay;
            case "Pakil": return R.array.PakilBarangay;
            case "Pangil": return R.array.PangilBarangay;
            case "Pila": return R.array.PilaBarangay;
            case "Rizal": return R.array.RizalBarangay;
            case "San Pablo": return R.array.SanPabloBarangay;
            case "San Pedro": return R.array.SanPedroBarangay;
            case "Santa Cruz": return R.array.SantaCruzBarangay;
            case "Santa Maria": return R.array.SantaMariaBarangay;
            case "Santa Rosa": return R.array.SantaRosaBarangay;
            case "Siniloan": return R.array.SiniloanBarangay;
            case "Victoria": return R.array.VictoriaBarangay;
            default: return -1;
        }
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder capitalized = new StringBuilder();
        String[] words = input.split(" ");
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return capitalized.toString().trim();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")     // At least one uppercase letter
                && password.matches(".*[0-9].*")     // At least one digit
                && password.matches(".*[!@#$%^&*(),_.?\":{}|<>].*"); // At least one special character
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