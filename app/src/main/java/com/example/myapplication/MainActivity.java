package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myapplication.models.Student;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    private EditText mNameEditText;
    private EditText mAgeEditText;
    private RecyclerView mRecyclerView;

    private FirestoreRecyclerAdapter mAdapter;

    private ImageView mPreviewImageView;

    private ProgressBar mProgressBar;

    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // 로그인 안 되었음
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            mUser = FirebaseAuth.getInstance().getCurrentUser();
        }

        mPreviewImageView = findViewById(R.id.preview_image);
        mProgressBar = findViewById(R.id.progressBar);

        mNameEditText = findViewById(R.id.name_edit);
        mAgeEditText = findViewById(R.id.age_edit);
        mRecyclerView = findViewById(R.id.recycler_view);

        findViewById(R.id.button).setOnClickListener(v -> {
            // Firebase에 추가
            dispatchTakePictureIntent();
        });

        findViewById(R.id.upload_button).setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            uploadPicture();
        });

        queryData();
    }

    private void writeDb(Uri downloadUri) {
        String name = mNameEditText.getText().toString();
        int age = Integer.parseInt(mAgeEditText.getText().toString());

        Map<String, Object> student = new HashMap<>();
        student.put("name", name);
        student.put("age", age);
        student.put("downloadUrl", downloadUri.toString());
        student.put("uid", mUser.getUid());

        addStudent(student);
    }

    private void queryData() {
        Query query = FirebaseFirestore.getInstance()
                .collection("student")
                .whereEqualTo("uid", mUser.getUid())
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

                Glide.with(MainActivity.this)
                        .load(model.getDownloadUrl())
                        .centerCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .into(holder.imageView);
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
                    mProgressBar.setVisibility(View.GONE);
                    // 성공
                    Toast.makeText(this, "성공", Toast.LENGTH_SHORT).show();
                    // 맨 위로
                    mRecyclerView.smoothScrollToPosition(0);
                })
                .addOnFailureListener(e -> {
                    mProgressBar.setVisibility(View.GONE);
                    // 실패
                    Toast.makeText(this, "실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            mPreviewImageView.setImageBitmap(imageBitmap);
        }
    }

    private void uploadPicture() {
        StorageReference storageRef = storage.getReference()
                .child("images/" + System.currentTimeMillis() + ".jpg");

        mPreviewImageView.setDrawingCacheEnabled(true);
        mPreviewImageView.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) mPreviewImageView.getDrawable()).getBitmap();

        // 이미지 줄이기
        bitmap = resizeBitmapImage(bitmap, 300);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            mProgressBar.setVisibility(View.GONE);
            // 실패
            Log.d(TAG, "uploadPicture: " + exception.getLocalizedMessage());
            Toast.makeText(this, "업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }).addOnSuccessListener(taskSnapshot -> {
            // 성공
            storageRef.getDownloadUrl().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Log.d(TAG, "uploadPicture: " + downloadUri);

                    writeDb(downloadUri);
                } else {
                    // Handle failures
                    // ...
                }
            });
        });
    }

    public Bitmap resizeBitmapImage(Bitmap source, int maxResolution) {
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = width;
        int newHeight = height;
        float rate = 0.0f;

        if (width > height) {
            if (maxResolution < width) {
                rate = maxResolution / (float) width;
                newHeight = (int) (height * rate);
                newWidth = maxResolution;
            }
        } else {
            if (maxResolution < height) {
                rate = maxResolution / (float) height;
                newWidth = (int) (width * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_logout:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(task -> {
                            // 로그아웃
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}