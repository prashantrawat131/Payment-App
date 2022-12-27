package com.example.mobilepaymentapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.mobilepaymentapp.model.User;
import com.google.firebase.database.DatabaseReference;

public class CommonOperations {
    private static final String TAG = "tagForPaymentApp";

    public static void log(String str) {
        Log.d(TAG, str);
    }

    public static void toast(String str, Context context) {
        Toast.makeText(context, "" + str, Toast.LENGTH_SHORT).show();
    }

    public static void getAccountBalance(DatabaseReference databaseReference, String phoneNumber) {
        databaseReference.child("users").child(phoneNumber).get().addOnCompleteListener(task -> {
            User user = task.getResult().getValue(User.class);
            log("Account balance for " + phoneNumber + " is " + user.getBalance());
        });
    }
}
