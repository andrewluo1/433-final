package com.example.assn4;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommentBoardActivity extends AppCompatActivity {

    private EditText searchInput;
    private CheckBox photosOnlyCheck;
    private Button findBtn, commentBtn, backBtn;

    private ListView imageListView;
    private ListView commentListView;

    private ImageListAdapter imageAdapter;
    private CommentListAdapter commentAdapter;

    private Database db;
    private Database.PhotoRecord selectedItem;

    private GeminiCommentGenerator gemini;
    private List<FriendPersona> friends;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_comment_board);

        db = new Database(this);
        gemini = new GeminiCommentGenerator(this);

        searchInput = findViewById(R.id.cb_search_input);
        photosOnlyCheck = findViewById(R.id.cb_photos_only);
        findBtn = findViewById(R.id.cb_find_btn);
        commentBtn = findViewById(R.id.cb_comment_btn);
        backBtn = findViewById(R.id.cb_back_btn);

        imageListView = findViewById(R.id.cb_image_list);
        commentListView = findViewById(R.id.cb_comment_list);

        friends = FriendPersona.defaultFriends(this);

        loadInitialImages();

        findBtn.setOnClickListener(v -> applySearch());
        photosOnlyCheck.setOnCheckedChangeListener((a, bChecked) -> applySearch());

        imageListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedItem = imageAdapter.getItem(position);
            Toast.makeText(this, "You selected: " + selectedItem.tags, Toast.LENGTH_SHORT).show();
        });

        commentBtn.setOnClickListener(v -> generateComments());
        
        backBtn.setOnClickListener(v -> finish());
    }

    private void loadInitialImages() {
        List<Database.PhotoRecord> all = new ArrayList<>();
        all.addAll(db.getAllPhotos());
        // Checkbox defaults to true ("Include sketches"), so include them initially if checked
        if (photosOnlyCheck.isChecked()) {
            all.addAll(db.getAllSketches());
        }
        imageAdapter = new ImageListAdapter(this, all);
        imageListView.setAdapter(imageAdapter);
        commentAdapter = new CommentListAdapter(this, new ArrayList<>());
        commentListView.setAdapter(commentAdapter);
    }

    private void applySearch() {
        String text = searchInput.getText().toString().trim();
        List<String> terms = text.isEmpty() ? new ArrayList<>() :
                Arrays.asList(text.split(","));

        List<Database.PhotoRecord> result = new ArrayList<>();

        result.addAll(db.findPhotosByTags(terms));

        // Fix: If checkbox is checked, we SHOULD include sketches
        if (photosOnlyCheck.isChecked()) {
            result.addAll(db.findSketchesByTags(terms));
        }

        imageAdapter = new ImageListAdapter(this, result);
        imageListView.setAdapter(imageAdapter);
        selectedItem = null;
    }

    private void generateComments() {
        // Fix: Get the selected item from the adapter, as the user likely clicked the checkbox
        Database.PhotoRecord currentSelection = imageAdapter.getSelected();
        if (currentSelection == null) {
            // Fallback to list item click selection if adapter selection is empty
            currentSelection = selectedItem;
        }

        if (currentSelection == null) {
            Toast.makeText(this, "Select an item first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating comments...", Toast.LENGTH_SHORT).show();

        Database.PhotoRecord finalSelection = currentSelection;
        new Thread(() -> {
            List<String> previous = new ArrayList<>();
            List<CommentItem> generated = gemini.generateComments(
                    finalSelection.tags, previous, friends
            );

            runOnUiThread(() -> {
                commentAdapter = new CommentListAdapter(CommentBoardActivity.this, generated);
                commentListView.setAdapter(commentAdapter);
            });
        }).start();
    }
}
