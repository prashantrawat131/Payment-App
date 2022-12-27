package com.example.mobilepaymentapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mobilepaymentapp.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MoneyTransaction {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private DatabaseReference mDatabase;
    String userPhoneNumber = "";
    Context context;

    public MoneyTransaction(Context context) {
        this.context = context;

        sharedPreferences = context.getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        userPhoneNumber = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");

        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void sendMoney(String receiverPhoneNumber, double amount, TransactionListeners listeners) {
        mDatabase.child("users")
                .child(userPhoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    try {
                        User user = task.getResult().getValue(User.class);
                        if (user.getBalance() > amount) {
                            performTransaction(user, receiverPhoneNumber, amount, listeners);
                        } else {
                            CommonOperations.toast("Insufficient balance", context);
                            listeners.insufficientAmount();
                        }
                    } catch (Exception e) {
                        CommonOperations.log("Error: " + e.getMessage());
                        listeners.error(e.getMessage());
                    }
                });
    }

    private void performTransaction(User user, String receiverPhoneNumber, double amount, TransactionListeners listeners) {
        mDatabase.child("users").child(receiverPhoneNumber).get()
                .addOnCompleteListener(task -> {
                    try {
                        user.setBalance(user.getBalance() - amount);
                        mDatabase.child("users").child(user.getPhoneNumber()).setValue(user);

                        User receiver = task.getResult().getValue(User.class);
                        receiver.setBalance(receiver.getBalance() + amount);
                        mDatabase.child("users").child(receiverPhoneNumber).setValue(receiver);

                        listeners.success();
                    } catch (Exception e) {
//                Rolling back the transaction
                        user.setBalance(user.getBalance() + amount);
                        mDatabase.child("users").child(user.getPhoneNumber()).setValue(user);
                        listeners.error(e.getMessage());
                    }
                });
    }
}
