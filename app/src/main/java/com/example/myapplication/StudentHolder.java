package com.example.myapplication;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class StudentHolder extends RecyclerView.ViewHolder {
    public TextView nameTextView;
    public TextView ageTextView;
    public ImageView imageView;

    public StudentHolder(@NonNull View itemView) {
        super(itemView);
        nameTextView = itemView.findViewById(R.id.name_text);
        ageTextView = itemView.findViewById(R.id.age_text);
        imageView = itemView.findViewById(R.id.imageView);
    }
}
