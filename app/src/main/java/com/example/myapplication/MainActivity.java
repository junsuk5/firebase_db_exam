package com.example.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.models.Student;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText mNameEditText;
    private EditText mAgeEditText;
    private RecyclerView mRecyclerView;

    private FirestoreRecyclerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNameEditText = findViewById(R.id.name_edit);
        mAgeEditText = findViewById(R.id.age_edit);
        mRecyclerView = findViewById(R.id.recycler_view);

        findViewById(R.id.button).setOnClickListener(v -> {
            // Firebase에 추가
            String name = mNameEditText.getText().toString();
            int age = Integer.parseInt(mAgeEditText.getText().toString());

            Map<String, Object> student = new HashMap<>();
            student.put("name", name);
            student.put("age", age);

            addStudent(student);
        });

        queryData();
    }

    private void queryData() {
        Query query = FirebaseFirestore.getInstance()
                .collection("student")
                .orderBy("age", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Student> options = new FirestoreRecyclerOptions.Builder<Student>()
                .setQuery(query, Student.class)
                .build();

        // Bind the Chat object to the ChatHolder
// ...
// Create a new instance of the ViewHolder, in this case we are using a custom
// layout called R.layout.message for each item
        mAdapter = new FirestoreRecyclerAdapter<Student, StudentHolder>(options) {
            @Override
            public void onBindViewHolder(StudentHolder holder, int position, Student model) {
                // Bind the Chat object to the ChatHolder
                // ...
                holder.nameTextView.setText(model.getName());
                holder.ageTextView.setText(model.getAge() + "");
            }

            @Override
            public StudentHolder onCreateViewHolder(ViewGroup group, int i) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(group.getContext())
                        .inflate(R.layout.item_student, group, false);

                return new StudentHolder(view);
            }
        };

        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    private void addStudent(Map<String, Object> student) {
        db.collection("student")
                .add(student)
                .addOnSuccessListener(doc -> {
                    // 성공
                    Toast.makeText(this, "성공", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, "실패", Toast.LENGTH_SHORT).show();
                });
    }
}