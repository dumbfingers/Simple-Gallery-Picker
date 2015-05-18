package com.yeyaxi.android.simplegallerypicker.fragments;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.yeyaxi.android.simplegallerypicker.R;
import com.yeyaxi.android.simplegallerypicker.adapters.FolderListAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DrawerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_URI = "KEY_URI";
    private static final String KEY_PROJECTION = "KEY_PROJECTION";
    private static final int LOADER_ID = 2;
    private FolderListAdapter adapter;

    @InjectView(R.id.library_folder_list)
    ListView folderListView;
    @InjectView(R.id.library_all)
    View allImageButton;

    private Uri uri;
    private String[] projection;
    private FolderSelectionDelegate delegate;

    public interface FolderSelectionDelegate {
        public void onFolderSelected(Long itemId, String bucketName);
    }


    public static DrawerFragment newInstance(Uri uri, String[] projection) {
        DrawerFragment fragment = new DrawerFragment();
        Bundle args = new Bundle();
        args.putString(KEY_URI, uri.toString());
        args.putStringArray(KEY_PROJECTION, projection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            uri = Uri.parse(args.getString(KEY_URI));
            projection = args.getStringArray(KEY_PROJECTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);

        adapter = new FolderListAdapter(
                getActivity(),
                R.layout.library_picker_drawer_item,
                null,
                new String[]{MediaStore.Images.Media.BUCKET_DISPLAY_NAME},
                new int[]{R.id.folder_name},
                0);
        folderListView.setAdapter(adapter);
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this);

        folderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                folderListView.setItemChecked(position, true);
                delegate.onFolderSelected((Long)adapter.getItemId(position), (String)adapter.getItem(position));
            }
        });
    }

    @OnClick(R.id.library_all)
    public void onAllImageClick() {
        delegate.onFolderSelected(null, null);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID:
                adapter.changeCursor(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.changeCursor(null);
    }

    public void setFolderSelectionDelegate(FolderSelectionDelegate delegate) {
        this.delegate = delegate;
    }
}


