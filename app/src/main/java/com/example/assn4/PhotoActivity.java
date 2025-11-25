package com.example.assn4;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 1;
    private static final String KEY = "AIzaSyBU7VRMU1HfyMThQvG18GhOFYAyl6RPebw";

    private ImageView imageView;
    private EditText editTags, editFind;
    private ListView listView;
    private Button btnCamera, btnTags, btnSave, btnFind, btnBack;

    private Uri photoUri;
    private File photoFile;
    private Database db;

    private int currentTagRequestId = 0;
    private Thread runningTagThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        imageView = findViewById(R.id.imageView);
        editTags = findViewById(R.id.editTags);
        editFind = findViewById(R.id.editFind);
        listView = findViewById(R.id.listView);
        btnCamera = findViewById(R.id.btnCamera);
        btnTags = findViewById(R.id.btnTags);
        btnSave = findViewById(R.id.btnSave);
        btnFind = findViewById(R.id.btnFind);
        btnBack = findViewById(R.id.btnBack);

        db = new Database(this);

        btnCamera.setOnClickListener(v -> openCamera());

        btnTags.setOnClickListener(v -> {
            Bitmap bmp = getCurrentBitmap();
            if (bmp != null) {
                if (runningTagThread != null && runningTagThread.isAlive()) {
                    runningTagThread.interrupt();
                }
                currentTagRequestId++;
                int requestId = currentTagRequestId;
                runningTagThread = classifyImage(bmp, requestId);
            } else {
                Toast.makeText(this, "Take a photo first", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> saveImage());
        btnFind.setOnClickListener(v -> findImages());
        btnBack.setOnClickListener(v -> finish());
    }

    private void openCamera() {
        editTags.setText("");
        photoFile = createPhotoFile();
        if (photoFile == null) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }
        photoUri = FileProvider.getUriForFile(this, "com.example.assn4.fileprovider", photoFile);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQ_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            Bitmap img = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            imageView.setImageBitmap(img);
            editTags.setText("");
            if (runningTagThread != null && runningTagThread.isAlive()) {
                runningTagThread.interrupt();
            }
            currentTagRequestId++;
            int requestId = currentTagRequestId;
            runningTagThread = classifyImage(img, requestId);
        }
    }

    private File createPhotoFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) return null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, "IMG_" + timeStamp + ".jpg");
    }

    private Thread classifyImage(Bitmap bitmap, int requestId) {
        editTags.setText("Classifying...");
        Thread t = new Thread(() -> {
            try {
                if (Thread.interrupted()) return;

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

                if (Thread.interrupted()) return;

                List<com.google.api.services.vision.v1.model.EntityAnnotation> labels =
                        response.getResponses().get(0).getLabelAnnotations();
                if (labels == null || labels.isEmpty()) return;

                StringBuilder tags = new StringBuilder();
                int count = Math.min(2, labels.size());
                for (int i = 0; i < count; i++) {
                    tags.append(labels.get(i).getDescription().toLowerCase());
                    if (i < count - 1) tags.append(",");
                }

                if (!Thread.interrupted() && requestId == currentTagRequestId) {
                    runOnUiThread(() -> editTags.setText(tags.toString()));
                }

            } catch (IOException e) {
                if (!Thread.interrupted()) {
                    runOnUiThread(() -> editTags.setText("Error: " + e.getMessage()));
                }
            }
        });
        t.start();
        return t;
    }

    private void saveImage() {
        Bitmap bmp = getCurrentBitmap();
        if (bmp == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }
        String tags = editTags.getText().toString().trim().toLowerCase();
        byte[] bytes = toJpeg(bmp, 90);
        long now = System.currentTimeMillis();
        db.insertPhoto(bytes, now, tags);
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
    }

    private void findImages() {
        String query = editFind.getText().toString().trim().toLowerCase();
        List<Database.PhotoRecord> results = db.findByTag(query);
        PhotoListAdapter adapter = new PhotoListAdapter(this, results);
        listView.setAdapter(adapter);
    }

    private Bitmap getCurrentBitmap() {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null;
    }

    private static byte[] toJpeg(Bitmap bmp, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return bos.toByteArray();
    }
}
