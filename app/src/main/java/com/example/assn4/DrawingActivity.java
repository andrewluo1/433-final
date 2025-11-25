package com.example.assn4;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class DrawingActivity extends AppCompatActivity {

    private static final String KEY = "AIzaSyBU7VRMU1HfyMThQvG18GhOFYAyl6RPebw";

    private DrawingView drawView;
    private EditText editTags, editFind;
    private ListView listView;
    private Button btnTags, btnClear, btnSave, btnFind, btnBack;

    private Database db;
    private int currentTagRequestId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        drawView = findViewById(R.id.drawView);
        editTags = findViewById(R.id.editTags);
        editFind = findViewById(R.id.editFind);
        listView = findViewById(R.id.listView);
        btnTags = findViewById(R.id.btnTags);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        btnFind = findViewById(R.id.btnFind);
        btnBack = findViewById(R.id.btnBack);

        db = new Database(this);

        btnTags.setOnClickListener(v -> {
            Bitmap bmp = drawView.getBitmap();
            if (bmp != null) {
                currentTagRequestId++;
                int requestId = currentTagRequestId;
                classifyDrawing(bmp, requestId);
            } else {
                Toast.makeText(this, "Draw something first", Toast.LENGTH_SHORT).show();
            }
        });

        btnClear.setOnClickListener(v -> {
            drawView.clear();
            editTags.setText("");
        });

        btnSave.setOnClickListener(v -> saveDrawing());
        btnFind.setOnClickListener(v -> findDrawings());
        btnBack.setOnClickListener(v -> finish());
    }

    private void classifyDrawing(Bitmap bitmap, int requestId) {
        editTags.setText("Classifying...");
        new Thread(() -> {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
                Image img = new Image();
                img.encodeContent(output.toByteArray());

                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(img);
                Feature feature = new Feature();
                feature.setType("LABEL_DETECTION");
                feature.setMaxResults(3);
                request.setFeatures(List.of(feature));

                HttpTransport http = AndroidHttp.newCompatibleTransport();
                GsonFactory gson = GsonFactory.getDefaultInstance();
                Vision vision = new Vision.Builder(http, gson, null)
                        .setVisionRequestInitializer(new VisionRequestInitializer(KEY))
                        .build();

                BatchAnnotateImagesRequest batch = new BatchAnnotateImagesRequest();
                batch.setRequests(List.of(request));
                Vision.Images.Annotate annotate = vision.images().annotate(batch);
                annotate.setDisableGZipContent(true);
                BatchAnnotateImagesResponse response = annotate.execute();

                List<com.google.api.services.vision.v1.model.EntityAnnotation> labels =
                        response.getResponses().get(0).getLabelAnnotations();

                if (labels == null || labels.isEmpty()) return;
                StringBuilder tags = new StringBuilder();
                int count = Math.min(2, labels.size());
                for (int i = 0; i < count; i++) {
                    tags.append(labels.get(i).getDescription().toLowerCase());
                    if (i < count - 1) tags.append(",");
                }

                if (requestId == currentTagRequestId) {
                    runOnUiThread(() -> editTags.setText(tags.toString()));
                }
            } catch (IOException e) {
                runOnUiThread(() -> editTags.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void saveDrawing() {
        Bitmap bmp = drawView.getBitmap();
        if (bmp == null) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }
        String tags = editTags.getText().toString().trim().toLowerCase();
        byte[] bytes = toJpeg(bmp, 90);
        long now = System.currentTimeMillis();
        db.insertSketch(bytes, now, tags);
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
    }

    private void findDrawings() {
        String query = editFind.getText().toString().trim().toLowerCase();
        List<Database.PhotoRecord> results;
        if (query.isEmpty()) {
            results = db.getAllSketches();
        } else {
            results = db.findSketchesByTags(List.of(query));
        }
        PhotoListAdapter adapter = new PhotoListAdapter(this, results);
        listView.setAdapter(adapter);
    }

    private static byte[] toJpeg(Bitmap bmp, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return bos.toByteArray();
    }
}
