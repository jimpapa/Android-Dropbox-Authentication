package com.example.myfirstapplication;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toolbar;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

import android.net.Uri;
import android.database.Cursor;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.v2.users.FullAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity{
    private int IMAGE_REQUEST_CODE = 1;
    private String ACCESS_TOKEN;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);


        if (!tokenExists()) {
            //No token
            //Back to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        ACCESS_TOKEN = retrieveAccessToken();
        getUserAccount();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });
    }

    private boolean tokenExists() {
        SharedPreferences prefs = getSharedPreferences("com.example.myfirstapplication", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        return accessToken != null;
    }

    private String retrieveAccessToken() {
        //check if ACCESS_TOKEN is stored on previous app launches
        SharedPreferences prefs = getSharedPreferences("com.example.myfirstapplication", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "No token found");
            return null;
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists");
            return accessToken;
        }
    }

    private void upload() {
        if (ACCESS_TOKEN == null)return;
        //Select image to upload
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent,
                "Upload to Dropbox"), IMAGE_REQUEST_CODE);
    }

    private void updateUI(FullAccount account) {
        ImageView profile = (ImageView) findViewById(R.id.imageView);
        TextView name = (TextView) findViewById(R.id.name_textView);
        TextView email = (TextView) findViewById(R.id.email_textView);

        name.setText(account.getName().getDisplayName());
        email.setText(account.getEmail());
        Picasso.with(this)
                .load(account.getProfilePhotoUrl())
                .resize(200, 200)
                .into(profile);
    }

    protected void getUserAccount() {
        if (ACCESS_TOKEN == null)return;
        new UserAccountTask(DropboxClient.getClient(ACCESS_TOKEN), new UserAccountTask.TaskDelegate() {
            @Override
            public void onAccountReceived(FullAccount account) {
                //Print account's info
                Log.d("User", account.getEmail());
                Log.d("User", account.getName().getDisplayName());
                Log.d("User", account.getAccountType().name());
                updateUI(account);
            }
            @Override
            public void onError(Exception error) {
                Log.d("User", "Error receiving account details.");
            }
        }).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(MainActivity.this, "Image uploaded successfully1", Toast.LENGTH_SHORT).show();
        if (resultCode != RESULT_OK || data == null) return;
        // Check which request we're responding to
        if (requestCode == IMAGE_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                //Image URI received

                File file = new File(getPath(getApplication(), data.getData()));
                if (file != null) {
                    //Initialize UploadTask
                    new UploadTask(DropboxClient.getClient(ACCESS_TOKEN), file, getApplicationContext()).execute();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
