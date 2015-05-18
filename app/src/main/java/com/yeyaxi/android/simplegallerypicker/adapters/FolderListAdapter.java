package com.yeyaxi.android.simplegallerypicker.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.yeyaxi.android.simplegallerypicker.R;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FolderListAdapter extends SimpleCursorAdapter {

    private Context context;
    private ArrayList<String> folderNameList;

    public FolderListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
        folderNameList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        if (folderNameList.size() > 0) {
            return folderNameList.size();
        } else {
            return getCursor() == null ? 0 : getCursor().getCount();
        }
    }

    @Override
    public Object getItem(int position) {
        return folderNameList.get(position);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        if (cursor != null) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String folderName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                if (!folderNameList.contains(folderName)) {
                    folderNameList.add(folderName);
                }
            }
            notifyDataSetChanged();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        if (v == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            v = inflater.inflate(R.layout.library_picker_drawer_item, parent, false);
            holder = new ViewHolder(v);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.folderName.setText(folderNameList.get(position));
        return v;
    }

    static class ViewHolder {
        @InjectView(R.id.folder_icon)
        ImageView folderIcon;
        @InjectView(R.id.folder_name)
        TextView folderName;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
            folderIcon.setImageResource(R.drawable.icon_library_folder);
        }
    }
}
