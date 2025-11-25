package com.example.assn4;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CommentListAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<CommentItem> items;

    public CommentListAdapter(Context c, List<CommentItem> list) {
        ctx = c;
        items = list;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public CommentItem getItem(int pos) { return items.get(pos); }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        Holder h;
        if (convert == null) {
            convert = LayoutInflater.from(ctx).inflate(R.layout.item_comment, parent, false);
            h = new Holder();
            h.avatar = convert.findViewById(R.id.ci_avatar);
            h.name = convert.findViewById(R.id.ci_name);
            h.text = convert.findViewById(R.id.ci_text);
            h.time = convert.findViewById(R.id.ci_time);
            convert.setTag(h);
        } else h = (Holder) convert.getTag();

        CommentItem c = items.get(pos);

        h.avatar.setImageResource(c.friendImageRes);
        h.name.setText(c.friendName);
        h.text.setText(c.commentText);

        CharSequence t = DateFormat.format("MMM d · h:mm a", c.timestamp);
        h.time.setText(t);

        return convert;
    }

    static class Holder {
        ImageView avatar;
        TextView name, text, time;
    }
}
