package com.example.assn4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoListAdapter extends BaseAdapter {

    private final Context context;
    private final List<Database.PhotoRecord> items;
    private final LayoutInflater inflater;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy – ha", Locale.US);

    public PhotoListAdapter(Context context, List<Database.PhotoRecord> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) row = inflater.inflate(R.layout.list_item_photo, parent, false);

        ImageView imgThumb = row.findViewById(R.id.imgThumb);
        TextView tvTags = row.findViewById(R.id.tvTags);
        TextView tvTime = row.findViewById(R.id.tvTime);

        Database.PhotoRecord rec = items.get(position);
        if (rec.image != null && rec.image.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(rec.image, 0, rec.image.length);
            imgThumb.setImageBitmap(bmp);
        } else {
            imgThumb.setImageResource(android.R.color.darker_gray);
        }


        tvTags.setText(rec.tags);
        String time = fmt.format(new Date(rec.savedAtMillis)).replace("AM", "am").replace("PM", "pm");
        tvTime.setText(time);
        return row;
    }
}
