package com.finals.appdev50;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private Button login, loginprof;
    private TextView register;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        noInternet.showPendulumDialog(this, getLifecycle());

        register = findViewById(R.id.btnRegister);
        register.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        login = findViewById(R.id.btnStudent);
        login.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        loginprof = findViewById(R.id.btnProfessor);
        loginprof.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, login_professor.class);
            startActivity(intent);
        });
    }
}