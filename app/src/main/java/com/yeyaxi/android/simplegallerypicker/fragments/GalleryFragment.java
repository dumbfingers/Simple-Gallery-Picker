package com.yeyaxi.android.simplegallerypicker.fragments;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.yeyaxi.android.simplegallerypicker.R;
import com.yeyaxi.android.simplegallerypicker.adapters.GalleryGridImageAdapter;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GalleryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback {

    private static final int LOADER_ALL_IMAGES = 0xA1;
    private static final int LOADER_FOLDER_IMAGES = 0xA2;
    private static final int LOADER_SELECTED_IMAGES = 0xA3;
    private static final String KEY_BUCKET_NAME = "KEY_BUCKET_NAME";
    private static final String KEY_ID = "KEY_ID";
    public static final String KEY_SELECTION = "KEY_SELECTION";
    private static final String KEY_PHOTO_COUNT = "KEY_PHOTO_COUNT";

    private String[] projectionThumbnail = new String[]{MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
    private Uri uriThumbnail = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;

    private String[] projectionFull = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA};
    private Uri uriFull = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";
    private ActionMode actionMode;
    private GalleryGridImageAdapter adapter;
    private int photoCount = -1;

    @InjectView(R.id.gallery_recycler)
    RecyclerView gridView;

    public static GalleryFragment newInstance(Integer photoCount) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_PHOTO_COUNT, photoCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            photoCount = args.getInt(KEY_PHOTO_COUNT);
        }

        getActivity().setTitle(getString(R.string.title_photos));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        initDefaultGalleryImages();
    }

    //region Cursor Loader Callbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ALL_IMAGES:
                String searchParam = (args == null) ? null : args.getString(KEY_ID);
                return new CursorLoader(getActivity(), uriThumbnail, projectionThumbnail, searchParam, null, MediaStore.Images.Thumbnails._ID + " DESC");
            case LOADER_FOLDER_IMAGES:
                String bucketName = args.getString(KEY_BUCKET_NAME);
                return new CursorLoader(getActivity(), uriFull, projectionFull, "bucket_display_name = \"" + bucketName + "\"", null, sortOrder);
            case LOADER_SELECTED_IMAGES:
                String selectedParam = args.getString(KEY_SELECTION);
                return new CursorLoader(getActivity(), uriFull, projectionFull, selectedParam, null, sortOrder);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ALL_IMAGES:
                adapter.changeCursor(cursor);
                break;
            case LOADER_FOLDER_IMAGES:
                /*
                 * Loading folder images is doing the following:
                 * 1. Load ids of the images inside this folder
                 * 2. Use these ids to find thumbnails and load them
                */
                ArrayList<Integer> ids = getImageIdsFromBucket(cursor);
                String searchParams = getSearchTerm(MediaStore.Images.Thumbnails.IMAGE_ID, ids);
                LoaderManager lm = getActivity().getLoaderManager();
                Bundle args = new Bundle();
                args.putString(KEY_ID, searchParams);
                lm.destroyLoader(LOADER_ALL_IMAGES);
                lm.initLoader(LOADER_ALL_IMAGES, args, GalleryFragment.this);
                break;
            case LOADER_SELECTED_IMAGES:
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra(KEY_SELECTION, getSelectedImgUri(cursor));
                getActivity().setResult(getActivity().RESULT_OK, intent);
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.changeCursor(null);
    }
    //endregion

    //region Contextual Action Bar
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_gallery, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_from_gallery:
                ArrayList<Integer> selectedArray = adapter.getSelectedItemIds();
                initImageSelectedCursorLoader(selectedArray);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
//        if (photoCount >= 0) {
//            adapter.setPhotosLeft(photoCount);
//        }
        adapter.removeSelection();
    }
    //endregion

    private void onMultipleSelection() {
        boolean hasCheckedItems = adapter.getSelectedCount() > 0;
        if (photoCount <= 12) {
            if (hasCheckedItems && actionMode == null) {
                actionMode = getActivity().startActionMode(this);
            }
        }
        if (!hasCheckedItems && actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }

        if (actionMode != null) {
            actionMode.setTitle(String.format(getString(R.string.items_selected), adapter.getSelectedCount()));
        }
    }

    private void initDefaultGalleryImages() {
        adapter = new GalleryGridImageAdapter(getActivity(), null);
//        if (photoCount >= 0) {
//            adapter.setPhotosLeft(photoCount);
//        }
        adapter.setImageSelectionDelegate(delegate);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), 3, GridLayoutManager.VERTICAL, false);
        gridView.setLayoutManager(layoutManager);
        gridView.setAdapter(adapter);
        LoaderManager lm = getActivity().getLoaderManager();
        lm.destroyLoader(LOADER_ALL_IMAGES);
        lm.initLoader(LOADER_ALL_IMAGES, null, this);
    }

    private void initImageSelectedCursorLoader(ArrayList<Integer> selection) {
        Bundle args = new Bundle();
        String searchParams = getSearchTerm(MediaStore.Images.Media._ID, selection);
        args.putString(KEY_SELECTION, searchParams);
        LoaderManager lm = getActivity().getLoaderManager();
        lm.destroyLoader(LOADER_SELECTED_IMAGES);
        lm.initLoader(LOADER_SELECTED_IMAGES, args, this);
    }

    private ArrayList getSelectedImgUri(Cursor cursor) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                Uri uri = Uri.parse("file://" + cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
                uris.add(uri);
            }
        }
        return uris;
    }

    private ArrayList getImageIdsFromBucket(Cursor cursor) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     *
     * @param column For Thumbnails, use "image_id", for full-size pics, use "_id"
     * @param ids
     * @return
     */
    private String getSearchTerm(String column, ArrayList<Integer> ids) {
        String searchTerm = "";
        for (int i = 0; i < ids.size(); i++) {
            searchTerm += column + " = " + ids.get(i) + " OR ";
        }

        if (!searchTerm.isEmpty()) {
            searchTerm = searchTerm.substring(0, searchTerm.length() - 4); // subtract the length of " OR " at the last
            return searchTerm;
        } else {
            return null;
        }
    }

    GalleryGridImageAdapter.ImageSelectionDelegate delegate = new GalleryGridImageAdapter.ImageSelectionDelegate() {
        @Override
        public void onImageSelected() {
            onMultipleSelection();
        }
    };

    public void onFolderSelected(Long itemId, String bucketName) {
        if (actionMode != null) {
            // clear & reset the contextual action bar
            actionMode.finish();
        }
        if (itemId != null && bucketName != null) {
            LoaderManager lm = getActivity().getLoaderManager();
            lm.destroyLoader(LOADER_FOLDER_IMAGES);
            Bundle args = new Bundle();
            args.putString(KEY_BUCKET_NAME, bucketName);
            lm.initLoader(LOADER_FOLDER_IMAGES, args, GalleryFragment.this);
//            setFragmentTitle(bucketName);
        } else {
            LoaderManager lm = getActivity().getLoaderManager();
//            setFragmentTitle(getString(R.string.title_photos));
            lm.restartLoader(LOADER_ALL_IMAGES, null, GalleryFragment.this);
        }
    }
}
