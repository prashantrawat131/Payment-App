package com.example.mobilepaymentapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilepaymentapp.databinding.ActivityTransactionHistoryBinding;
import com.example.mobilepaymentapp.databinding.HistoryItem2Binding;
import com.example.mobilepaymentapp.model.Transaction;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class TransactionHistoryActivity extends AppCompatActivity {

    ActivityTransactionHistoryBinding binding;
    ArrayList<Transaction> arrayList;
    SharedPreferences sharedPreferences;
    HistoryRvAdapter adapter;
    String userNumber;

    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        database = FirebaseDatabase.getInstance().getReference();

        setUpRecyclerView();

        populateArrayList();
    }

    private void populateArrayList() {
        userNumber = sharedPreferences.getString(Constants.SP_USER_PHONE_NUMBER, "-1");
        database.child("transactions").child(userNumber).get().addOnCompleteListener(task -> {
            CommonOperations.log("Result: " + task.getResult());
            DataSnapshot dataSnapshot = task.getResult();
            for (DataSnapshot data : dataSnapshot.getChildren()) {
                Transaction transaction = data.getValue(Transaction.class);
                arrayList.add(transaction);
            }
            Collections.sort(arrayList, new Comparator<Transaction>() {
                @Override
                public int compare(Transaction o1, Transaction o2) {
                    return Long.compare(o2.getTime(), o1.getTime());
                }
            });
            adapter.notifyDataSetChanged();
        });
    }

    private void setUpRecyclerView() {
        arrayList = new ArrayList<>();
        adapter = new HistoryRvAdapter();
        binding.historyRv.setAdapter(adapter);
        binding.historyRv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private class HistoryRvAdapter extends RecyclerView.Adapter<HistoryRvAdapter.HistoryViewHolder> {
        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HistoryViewHolder(HistoryItem2Binding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            Transaction transaction = arrayList.get(position);

            try {
                if (transaction.getSenderNumber().equals(userNumber)) {

                    holder.binding.otherPartyDetailTv.setText(transaction.getReceiverNumber());
                    holder.binding.sentOrReceivedTv.setTextColor(Color.parseColor("#FF0000"));
                } else {
                    holder.binding.otherPartyDetailTv.setText(transaction.getSenderNumber());
                    holder.binding.sentOrReceivedTv.setTextColor(Color.parseColor("#00ff00"));
                }
                holder.binding.sentOrReceivedTv.setText("₹" + transaction.getAmount());

            } catch (Exception e) {
                CommonOperations.log("Error: " + e.getMessage());
            }

            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(transaction.getTime());
                SimpleDateFormat dateParser = new SimpleDateFormat("MMM");
                holder.binding.dateTv.setText(calendar.get(Calendar.DATE) + "\n" + dateParser.format(calendar.getTime()));
                SimpleDateFormat timeParser = new SimpleDateFormat("hh:mm a");
//                holder.binding.dateTv.setText(dateParser.format(calendar.getTime()));
                holder.binding.timeTv.setText(timeParser.format(calendar.getTime()));
            } catch (Exception e) {
                CommonOperations.log("Error: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        private class HistoryViewHolder extends RecyclerView.ViewHolder {
            HistoryItem2Binding binding;

            public HistoryViewHolder(HistoryItem2Binding b) {
                super(b.getRoot());
                binding = b;
            }
        }
    }
}