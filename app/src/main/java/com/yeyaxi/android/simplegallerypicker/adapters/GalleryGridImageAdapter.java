package com.yeyaxi.android.simplegallerypicker.adapters;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.yeyaxi.android.simplegallerypicker.R;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GalleryGridImageAdapter extends CursorRecyclerViewAdapter<GalleryGridImageAdapter.GalleryViewHolder> {

    private SparseBooleanArray selectedItemIds;
    private ImageSelectionDelegate delegate;
    private int photosLeft;
    private Picasso picasso;

    public interface ImageSelectionDelegate {
        public void onImageSelected();
    }

    public GalleryGridImageAdapter(Context context, Cursor c) {
        super(context, c);
        this.selectedItemIds = new SparseBooleanArray();
        picasso = picasso.with(context);
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor = getCursor();
        if ((cursor != null) && cursor.moveToPosition(position)) {
            return cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID));
        } else {
            return -1;
        }
    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_select_item, parent, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(GalleryViewHolder viewHolder, Cursor cursor) {
        String uri = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
        picasso.load(Uri.parse("file://" + uri)).into(viewHolder.galleryImage);
        final int position = cursor.getPosition();

        viewHolder.setSelected(selectedItemIds.get(position));

        viewHolder.galleryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSelection(position);
                delegate.onImageSelected();
            }
        });

//        viewHolder.deselectContainer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (v.getVisibility() == View.VISIBLE) {
//                    toggleSelection(position);
//                    delegate.onImageSelected();
//                }
//            }
//        });

    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        // selection must be cleared after cursor has been changed
        this.removeSelection();
    }

//    public void setPhotosLeft(int photoCount) {
//        photosLeft = ModelObject.config.maxImagesPerItem() - photoCount;
//    }

    public void setImageSelectionDelegate(ImageSelectionDelegate delegate) {
        this.delegate = delegate;
    }

    public void toggleSelection(int position) {
        selectView(position, !selectedItemIds.get(position));
    }

    private void selectView(int position, boolean selected) {
        if (selected) {
            if (photosLeft > 0) {
                selectedItemIds.put(position, selected);
                photosLeft--;
            }
        } else {
            if (photosLeft < 12) {
                selectedItemIds.delete(position);
                photosLeft++;
            }
        }
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedItemIds.size();
    }

    public ArrayList getSelectedItemIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        Cursor cursor = getCursor();
        for (int i = 0; i < selectedItemIds.size(); i++) {
            if (selectedItemIds.valueAt(i)) {
                int index = selectedItemIds.keyAt(i);
                if (cursor != null && cursor.moveToPosition(index)) {
                    ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID)));
                }
            }
        }
        return ids;
    }

    public void removeSelection() {
        selectedItemIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.gallery_image)
        ImageView galleryImage;
//        @InjectView(R.id.deselect_container)
//        View deselectContainer;
        private boolean selected;

        public GalleryViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
            galleryImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
//            this.deselectContainer.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }
}
