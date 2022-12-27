package com.example.mobilepaymentapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(getApplicationContext());

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        if (sharedPreferences.getBoolean(Constants.SP_USER_LOGGED_IN, false)) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
        } else {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        finish();
    }
}