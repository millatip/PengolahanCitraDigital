package com.dicoding.millatip.mypcd;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    Bitmap bi = null;

    boolean isColored;

    LinearLayout view, view_color;
    Button histogram, upload;

    boolean flag;

    private int SIZE = 256;
    // Red, Green, Blue
    private int NUMBER_OF_COLOURS = 3;

    public final int RED = 0;
    public final int GREEN = 1;
    public final int BLUE = 2;

    private int[][] colourBins;
    private volatile boolean loaded = false;
    private int maxY;

    float offset = 1;
    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if(metrics.densityDpi== DisplayMetrics.DENSITY_LOW)
            offset = 0.75f;
        else if(metrics.densityDpi== DisplayMetrics.DENSITY_MEDIUM)
            offset = 1f;
        else if(metrics.densityDpi== DisplayMetrics.DENSITY_TV)
            offset = 1.33f;
        else if(metrics.densityDpi== DisplayMetrics.DENSITY_HIGH)
            offset = 1.5f;
        else if(metrics.densityDpi== DisplayMetrics.DENSITY_XHIGH)
            offset = 2f;

        Log.e("Milla",""+offset);

        colourBins = new int[NUMBER_OF_COLOURS][];

        for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
            colourBins[i] = new int[SIZE];
        }

        loaded = false;

        view = findViewById(R.id.lyt);
        upload = findViewById(R.id.upload);
        upload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if (flag) {
                    view_color.removeAllViews();
                    view.removeAllViews();
                }
                Intent it = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(it, 101);

                flag = true;
            }
        });

        view_color = findViewById(R.id.lyt_color);
        histogram = findViewById(R.id.hst_btn);
        histogram.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (bi != null) {
                    isColored = false;
                    view.addView(new MyHistogram(getApplicationContext(), bi));
                }
            }
        });
        Button histogram_color = findViewById(R.id.hst_color_btn);
        histogram_color.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (bi != null) {
                    isColored = true;
                    view_color.addView(new MyHistogram(getApplicationContext(),
                            bi));
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (EasyPermissions.hasPermissions(this, galleryPermissions)) {
            Uri selectedImage = null;
            if (imageReturnedIntent != null) {
                selectedImage = imageReturnedIntent.getData();
            }
            String filename = null;
            if (selectedImage != null) {
                filename = getRealPathFromURI(selectedImage);
            }
            bi = BitmapFactory.decodeFile(filename);
            if(bi!=null)
            {
                try {
                    new MyAsyncTaskImage().execute();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);
        }
    }



    private String getRealPathFromURI(Uri contentUri) {
        Log.e("TEST", "GetRealPath : " + contentUri);

        try {
            if (contentUri.toString().contains("video")) {
                String[] proj = { MediaStore.Video.Media.DATA };
                Cursor cursor = managedQuery(contentUri, proj, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else {
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor cursor = managedQuery(contentUri, proj, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    class MyAsyncTaskImage extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                load(bi);
            }catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            ImageView img = findViewById(R.id.img);
            img.setImageBitmap(bi);
        }
    }

    public void load(Bitmap bi) throws IOException {

        if (bi != null) {
            // Reset all the bins
            for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
                for (int j = 0; j < SIZE; j++) {
                    colourBins[i][j] = 0;
                }
            }

            for (int x = 0; x < bi.getWidth(); x++) {
                for (int y = 0; y < bi.getHeight(); y++) {

                    int pixel = bi.getPixel(x, y);

                    colourBins[RED][Color.red(pixel)]++;
                    colourBins[GREEN][Color.green(pixel)]++;
                    colourBins[BLUE][Color.blue(pixel)]++;
                }
            }

            maxY = 0;

            for (int i = 0; i < NUMBER_OF_COLOURS; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (maxY < colourBins[i][j]) {
                        maxY = colourBins[i][j];
                    }
                }
            }
            loaded = true;
        } else {
            loaded = false;
        }
    }

    class MyHistogram extends View {

        public MyHistogram(Context context, Bitmap bi) {
            super(context);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);

            if (loaded) {
                canvas.drawColor(Color.GRAY);

                Log.e("MILLA", "Height : " + getHeight() + ", Width : "
                        + getWidth());

                int xInterval = (int) ((double) getWidth() / ((double) SIZE + 1));

                for (int i = 0; i < NUMBER_OF_COLOURS; i++) {

                    Paint wallpaint;

                    wallpaint = new Paint();
                    if (isColored) {
                        if (i == RED) {
                            wallpaint.setColor(Color.RED);
                        } else if (i == GREEN) {
                            wallpaint.setColor(Color.GREEN);
                        } else if (i == BLUE) {
                            wallpaint.setColor(Color.BLUE);
                        }
                    } else {
                        wallpaint.setColor(Color.WHITE);
                    }

                    wallpaint.setStyle(Paint.Style.FILL);

                    Path wallpath = new Path();
                    wallpath.reset();
                    wallpath.moveTo(0, getHeight());
                    for (int j = 0; j < SIZE - 1; j++) {
                        int value = (int) (((double) colourBins[i][j] / (double) maxY) * (getHeight()+100));

                        wallpath.lineTo(j * xInterval * offset, getHeight() - value);

                    }
                    wallpath.lineTo(SIZE * offset, getHeight());
                    canvas.drawPath(wallpath, wallpaint);
                }

            }

        }
    }


}
