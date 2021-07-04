package com.app.imageeditor.view.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.exifinterface.media.ExifInterface;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.app.imageeditor.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditActivity extends AppCompatActivity {

    private Uri uri;
    private ImageView image;
    private BottomNavigationView menu;
    private Bitmap bitmap;
    private boolean save = false;
    private boolean cropped = false;
    private static final String TAG = EditActivity.class.getCanonicalName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getSupportActionBar().setTitle("Edit Image");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        uri = getIntent().getParcelableExtra("uri");
        Log.i(TAG,"image path "+uri);
        image = findViewById(R.id.image);
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            image.setImageBitmap(bitmap);
        }catch (Exception e){
            e.printStackTrace();
        }
        menu = findViewById(R.id.bottom_menu);
        menu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.info){
                    showInfo(uri);
                }else if(item.getItemId() == R.id.crop){
                    if(cropped){
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            image.setImageBitmap(bitmap);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        cropped = false;
                        Toast.makeText(EditActivity.this, "Rectangle Crop", Toast.LENGTH_SHORT).show();
                        save = true;
                    }else{
                        bitmap = squareCrop(bitmap);
                        image.setImageBitmap(bitmap);
                        cropped = true;
                        Toast.makeText(EditActivity.this, "Square Crop", Toast.LENGTH_SHORT).show();
                        save = true;
                    }
                }else if(item.getItemId()== R.id.vertical){
                    bitmap = flipImage(bitmap,true,false);
                    image.setImageBitmap(bitmap);
                    save = true;
                }else{
                    bitmap = flipImage(bitmap,false,true);
                    image.setImageBitmap(bitmap);
                    save = true;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
        }else if(item.getItemId() == R.id.save){
            if(save){
                saveEdit(bitmap);
            }else{
                Toast.makeText(this, "Please edit the image to save.", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    * Some of the Exif information is not available for images received via whatsapp,
    *  but it is available for images captured via phone
    * */
    private void showInfo(Uri uri){
        InputStream in=null;
        try {
            in = getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(in);
            String meta="";
            meta += getTagString(ExifInterface.TAG_DATETIME, exif);
            meta += getTagString(ExifInterface.TAG_FLASH, exif);
            meta += getTagString(ExifInterface.TAG_GPS_LATITUDE, exif);
            meta += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
            meta += getTagString(ExifInterface.TAG_GPS_LONGITUDE, exif);
            meta += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
            meta += getTagString(ExifInterface.TAG_IMAGE_LENGTH, exif);
            meta += getTagString(ExifInterface.TAG_IMAGE_WIDTH, exif);
            meta += getTagString(ExifInterface.TAG_MAKE, exif);
            meta += getTagString(ExifInterface.TAG_MODEL, exif);
            meta += getTagString(ExifInterface.TAG_ORIENTATION, exif);
            meta += getTagString(ExifInterface.TAG_WHITE_BALANCE, exif);
            AlertDialog.Builder info = new AlertDialog.Builder(this);
            info.setTitle("Image Information");
            info.setMessage(meta);
            info.setPositiveButton("Ok",null);
            info.setCancelable(true);
            info.create();
            info.show();
        } catch (IOException e) {
            // Handle any errors
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private String getTagString(String tag, ExifInterface exif) {
        String value = exif.getAttribute(tag);
        return(tag + " : " + (value!=null?value:"not available") + "\n");
    }

    public Bitmap flipImage(Bitmap source, boolean xFlip, boolean yFlip) {
        Matrix matrix = new Matrix();
        matrix.postScale(xFlip ? -1 : 1, yFlip ? -1 : 1, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap squareCrop(Bitmap srcBmp){
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        }else{

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }



    private void saveEdit(Bitmap bmp) {
        File f = null;
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.ENGLISH).format(new Date());
        String mImageName="SKY_"+ timeStamp +".jpg";
        try {
            f = new File(path,mImageName);
            fOut = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream
            MediaStore.Images.Media.insertImage(getContentResolver(), f.getAbsolutePath(), f.getName(), f.getName());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(f!=null && f.exists()){
                Toast.makeText(this, "Image Saved at "+f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Unable to save Image.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}