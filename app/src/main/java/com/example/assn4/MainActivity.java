package com.example.assn4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPhoto = findViewById(R.id.btnPhoto);
        Button btnDrawing = findViewById(R.id.btnDrawing);
        Button commentBoardBtn = findViewById(R.id.btn_comment_board);
        commentBoardBtn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, CommentBoardActivity.class);
            startActivity(i);
        });


        btnPhoto.setOnClickListener(v -> startActivity(new Intent(this, PhotoActivity.class)));
        btnDrawing.setOnClickListener(v -> startActivity(new Intent(this, DrawingActivity.class)));
    }
}
