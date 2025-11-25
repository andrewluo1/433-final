package com.example.assn4;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiCommentGenerator {

    private final Context ctx;
    private final Random rng = new Random();
    private final OkHttpClient client;

    public GeminiCommentGenerator(Context c) {
        ctx = c;
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public List<CommentItem> generateComments(String tags, List<String> previous, List<FriendPersona> friends) {
        List<CommentItem> out = new ArrayList<>();

        int n = 7 + rng.nextInt(4);

        for (int i = 0; i < n; i++) {
            FriendPersona f = friends.get(rng.nextInt(friends.size()));
            String prompt = buildPrompt(f, tags, previous);
            String comment = callGemini(prompt);

            out.add(new CommentItem(
                    f.avatarRes,
                    f.name,
                    comment,
                    System.currentTimeMillis()
            ));

            previous.add(comment);
        }
        return out;
    }

    private String buildPrompt(FriendPersona f, String tags, List<String> prev) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are ").append(f.name).append(". ");
        sb.append("Your comment style: ").append(f.style).append(". ");
        sb.append("Relevant image tags: ").append(tags).append(". ");

        if (!prev.isEmpty()) {
            sb.append("Previous comments: ");
            for (String c : prev) sb.append(c).append(" ");
        }

        sb.append("Write a new comment under 20 words. Stay in character.");

        return sb.toString();
    }

    private String callGemini(String prompt) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        try {
            // Updated model name to a valid one
            String endpoint =
                    "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyC7BFIXtRutBqt4GJ3y8iomCItDss_4ydY";

            JSONObject req = new JSONObject();
            JSONArray contents = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONObject content = new JSONObject();
            content.put("parts", new JSONArray().put(part));

            contents.put(content);
            req.put("contents", contents);

            RequestBody body = RequestBody.create(req.toString(), JSON);

            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                if (response.isSuccessful()) {
                    JSONObject obj = new JSONObject(result);
                    JSONArray candidates = obj.getJSONArray("candidates");
                    JSONObject cand = candidates.getJSONObject(0);

                    JSONArray parts = cand.getJSONObject("content")
                            .getJSONArray("parts");

                    return parts.getJSONObject(0).getString("text").trim();
                } else {
                    Log.e("Gemini", "HTTP Error " + response.code() + ": " + result);
                    return "Error: " + response.code();
                }
            }

        } catch (Exception e) {
            Log.e("Gemini", "Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}

