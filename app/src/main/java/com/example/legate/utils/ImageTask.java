package com.example.legate.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

public class ImageTask extends AsyncTask<String, Integer, Bitmap> {
    private static final String TAG = "ImageTask";

    private ImageView imageView;

    public ImageTask(ImageView view) {
        imageView = view;
    }

    /**
     *
     * @param strings - image parent file path (required), source url (required), name (optional)
     * @return Bitmap of specified image
     */
    @Override
    protected Bitmap doInBackground(String... strings) {
        if (strings.length < 2) {
            Log.e(TAG, "Implementation error: too few arguments");
            return null;
        }
        if (isCancelled()) return null;
        Log.d(TAG, String.format("ImageTask starting: %s, %s", (Object[]) strings));

        CacheManager cacheManager = new CacheManager();

        String imageName;
        if (strings.length == 3) imageName = strings[2];
        else imageName = "image.jpg";

        File imageFile = new File(strings[0], imageName);
        String imageUrl = strings[1];

        // Check if file exists
        if (!imageFile.exists()) {
            Log.d(TAG, "image.jpg not found, downloading through CacheManager");
            // Use CacheManager to download file
            if (0 != cacheManager.downloadFile(imageUrl, imageFile.getAbsolutePath(), null)) {
                Log.e(TAG, "Failed to download image file, using default");
                //return cacheManager.getDefaultAvatar();
                return null;
            }
            else return BitmapFactory.decodeFile(imageFile.getPath());
        }
        else return BitmapFactory.decodeFile(imageFile.getPath());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (null == bitmap) {
            Log.e(TAG, "(null == bitmap), exiting");
            return;
        }
        imageView.setImageBitmap(bitmap);
    }
}
