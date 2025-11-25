package com.example.assn4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ImageListAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<Database.PhotoRecord> items;
    private int selectedIndex = -1;

    public ImageListAdapter(Context c, List<Database.PhotoRecord> it) {
        ctx = c;
        items = it;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Database.PhotoRecord getItem(int pos) { return items.get(pos); }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        Holder h;
        if (convert == null) {
            convert = LayoutInflater.from(ctx).inflate(R.layout.item_image_list, parent, false);
            h = new Holder();
            h.check = convert.findViewById(R.id.item_check);
            h.img = convert.findViewById(R.id.item_img);
            h.tags = convert.findViewById(R.id.item_tags);
            h.time = convert.findViewById(R.id.item_time);
            convert.setTag(h);
        } else h = (Holder) convert.getTag();

        Database.PhotoRecord p = items.get(pos);

        Bitmap bmp = BitmapFactory.decodeByteArray(p.image, 0, p.image.length);
        h.img.setImageBitmap(bmp);

        h.tags.setText(p.tags);

        CharSequence date = DateFormat.format("MMM d, yyyy - h:mm a", p.savedAtMillis);
        h.time.setText(date);

        h.check.setChecked(pos == selectedIndex);
        h.check.setOnClickListener(v -> {
            selectedIndex = pos;
            notifyDataSetChanged();
        });

        return convert;
    }

    static class Holder {
        CheckBox check;
        ImageView img;
        TextView tags, time;
    }

    public Database.PhotoRecord getSelected() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return null;
        return items.get(selectedIndex);
    }
}
