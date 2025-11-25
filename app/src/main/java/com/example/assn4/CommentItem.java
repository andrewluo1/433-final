package com.example.assn4;

public class CommentItem {
    public int friendImageRes;
    public String friendName;
    public String commentText;
    public long timestamp;

    public CommentItem(int img, String name, String text, long ts) {
        friendImageRes = img;
        friendName = name;
        commentText = text;
        timestamp = ts;
    }
}
