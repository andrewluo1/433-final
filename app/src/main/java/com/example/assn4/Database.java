package com.example.assn4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {

    public static final String DB_NAME = "photos.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_PHOTOS = "photos";
    private static final String TABLE_SKETCHES = "sketches";

    private static final String COL_ID = "id";
    private static final String COL_IMAGE = "image";
    private static final String COL_SAVED_AT = "savedAtMillis";
    private static final String COL_TAGS = "tags";

    public Database(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PHOTOS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_IMAGE + " BLOB, " +
                COL_SAVED_AT + " INTEGER, " +
                COL_TAGS + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_SKETCHES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_IMAGE + " BLOB, " +
                COL_SAVED_AT + " INTEGER, " +
                COL_TAGS + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SKETCHES + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_IMAGE + " BLOB, " +
                    COL_SAVED_AT + " INTEGER, " +
                    COL_TAGS + " TEXT)");
        }
    }

    public long insertPhoto(byte[] img, long when, String tags) {
        ContentValues cv = new ContentValues();
        cv.put(COL_IMAGE, img);
        cv.put(COL_SAVED_AT, when);
        cv.put(COL_TAGS, tags);
        return getWritableDatabase().insert(TABLE_PHOTOS, null, cv);
    }

    public long insertSketch(byte[] img, long when, String tags) {
        ContentValues cv = new ContentValues();
        cv.put(COL_IMAGE, img);
        cv.put(COL_SAVED_AT, when);
        cv.put(COL_TAGS, tags);
        return getWritableDatabase().insert(TABLE_SKETCHES, null, cv);
    }

    public List<PhotoRecord> getAllPhotos() {
        return simpleQuery("SELECT * FROM " + TABLE_PHOTOS + " ORDER BY " + COL_SAVED_AT + " DESC");
    }

    public List<PhotoRecord> getAllSketches() {
        return simpleQuery("SELECT * FROM " + TABLE_SKETCHES + " ORDER BY " + COL_SAVED_AT + " DESC");
    }

    public List<PhotoRecord> findByTag(String tag) {
        List<String> tags = new ArrayList<>();
        tags.add(tag);
        return findPhotosByTags(tags);
    }

    public List<PhotoRecord> findPhotosByTags(List<String> searchTerms) {
        if (searchTerms.isEmpty()) return getAllPhotos();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_PHOTOS).append(" WHERE ");
        for (int i = 0; i < searchTerms.size(); i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(COL_TAGS).append(" LIKE ?");
        }
        sb.append(" ORDER BY ").append(COL_SAVED_AT).append(" DESC");
        return simpleQuery(sb.toString(), buildTagArgs(searchTerms));
    }

    public List<PhotoRecord> findSketchesByTags(List<String> searchTerms) {
        if (searchTerms.isEmpty()) return getAllSketches();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(TABLE_SKETCHES).append(" WHERE ");
        for (int i = 0; i < searchTerms.size(); i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(COL_TAGS).append(" LIKE ?");
        }
        sb.append(" ORDER BY ").append(COL_SAVED_AT).append(" DESC");
        return simpleQuery(sb.toString(), buildTagArgs(searchTerms));
    }

    private String[] buildTagArgs(List<String> tags) {
        String[] arr = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++)
            arr[i] = "%" + tags.get(i).trim() + "%";
        return arr;
    }

    private List<PhotoRecord> simpleQuery(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        List<PhotoRecord> out = new ArrayList<>();
        if (c.moveToFirst()) {
            do out.add(PhotoRecord.fromCursor(c));
            while (c.moveToNext());
        }
        c.close();
        return out;
    }

    public static class PhotoRecord {
        public long id;
        public byte[] image;
        public long savedAtMillis;
        public String tags;

        public static PhotoRecord fromCursor(Cursor c) {
            PhotoRecord p = new PhotoRecord();
            p.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
            p.image = c.getBlob(c.getColumnIndexOrThrow(COL_IMAGE));
            p.savedAtMillis = c.getLong(c.getColumnIndexOrThrow(COL_SAVED_AT));
            p.tags = c.getString(c.getColumnIndexOrThrow(COL_TAGS));
            return p;
        }
    }
}
