package com.yeyaxi.android.simplegallerypicker.activities;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.yeyaxi.android.simplegallerypicker.R;
import com.yeyaxi.android.simplegallerypicker.fragments.DrawerFragment;
import com.yeyaxi.android.simplegallerypicker.fragments.GalleryFragment;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class GalleryActivity extends ActionBarActivity {

    private static final java.lang.String KEY_PHOTO_COUNT = "KEY_PHOTO_COUNT";
    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
//    @InjectView(R.id.toolbar)
//    Toolbar toolbar;

    private String[] projectionFull = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA};
    private Uri uriFull = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerFragment drawerFragment;
    private GalleryFragment galleryFragment;
    private int photoCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ButterKnife.inject(this);

        if (getIntent() != null
                && getIntent().getExtras() != null) {
            photoCount = getIntent().getExtras().getInt(KEY_PHOTO_COUNT);
        }

        drawerFragment = DrawerFragment.newInstance(uriFull, projectionFull);
        drawerFragment.setFolderSelectionDelegate(delegate);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.navigation_drawer, drawerFragment)
                .commit();

        galleryFragment = GalleryFragment.newInstance(photoCount);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, galleryFragment)
                .commit();

//        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.title_photos);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                null,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // the menu item inside activity is a fake one, we display its disabled state only
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        MenuItem item = menu.findItem(R.id.action_add_from_gallery);
        if (item != null) {
            item.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        scanForImages();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawers();
        } else {
            setResult(RESULT_CANCELED);
        }
    }

    private void scanForImages() {
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

//        File[] folderList = new File[] {dcim, pictures, downloads};
//        ArrayList<File> refreshList = new ArrayList<>();
//        for (File each : folderList) {
//            if (each.isDirectory()) {
//                File[] files = each.listFiles();
//                refreshList.addAll(Arrays.asList(files));
//            } else {
//                refreshList.add(each);
//            }
//        }
//        ArrayList<String> pathList = new ArrayList<>();
//        for (File each : refreshList) {
//            pathList.add(each.getAbsolutePath());
//        }
        MediaScannerConnection.scanFile(this,
                new String[]{dcim.getAbsolutePath(), pictures.getAbsolutePath(), downloads.getAbsolutePath()}, new String[]{"image/*", "image/*", "image/*"},
//                pathList.toArray(new String[pathList.size()]), null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });

        // expensive and no effects
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File[] fileArray = new File[]{dcim, pictures, downloads};
//        for (int i = 0; i < fileArray.length; i++) {
//            Uri contentUri = Uri.fromFile(fileArray[i]);
//            mediaScanIntent.setData(contentUri);
//            sendBroadcast(mediaScanIntent);
//        }
    }

    private void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawers();
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    DrawerFragment.FolderSelectionDelegate delegate = new DrawerFragment.FolderSelectionDelegate() {
        @Override
        public void onFolderSelected(Long itemId, String bucketName) {
            toggleDrawer();
            galleryFragment.onFolderSelected(itemId, bucketName);
        }
    };

}
