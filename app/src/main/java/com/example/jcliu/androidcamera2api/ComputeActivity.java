package com.example.jcliu.androidcamera2api;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ComputeActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCamera2API";
    protected static final int PROGRESS = 0x10000, PROGRESS2 = 0x10001, PROGRESS3 = 0x10002, PROGRESS4 = 0x10003;
    private ImageView iv;
    private Bitmap bitmap = null;
    // public stored 1-D spectrum
    public static double[] lightsourceH;//, lightsourceV1, lightsourceV2, lightsourceV3;
    public static double[][][] signalSource; // class: White, Air, Water, 3 parts: Left, Center, Right, 1-D pos

    int sourceIdx = 0;
    //
    int peakPos = 0, peakPosL, peakPosR, w, i1=0, i2=0;
    // store the segmented region index
    public static int x1_l, x1_r, x2_l, x2_r, x3_l, x3_r;
    //
    private ProgressBar pbar;
    private String[] classname = {"Light", "Air", "Water"};

    // option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compute, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.spectrum_option:
                Intent it = new Intent(ComputeActivity.this, ChartActivity.class);
                Log.d(TAG, "option spectrum");
                it.putExtra("title", classname[sourceIdx]+"Source Raw Spectrum");
                it.putExtra("numChart", 3);
                it.putExtra("lightsource1", signalSource[sourceIdx][1]);
                it.putExtra("signal name 1", " Center ");
                it.putExtra("lightsource2", signalSource[sourceIdx][2]);
                it.putExtra("signal name 2", " Right ");
                it.putExtra("lightsource3", signalSource[sourceIdx][0]);
                it.putExtra("signal name 3", " Left ");
                startActivity(it);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute);
        iv = (ImageView) findViewById(R.id.imageView);
        pbar = (ProgressBar) findViewById(R.id.progressBar) ;
        //
        if(signalSource == null){
            Log.d("TAG", "initialize 3-D signal source");
            //Toast.makeText(ComputeActivity.this, "initialize 3-D signal source", Toast.LENGTH_SHORT).show();
            signalSource = new double[5][][];
            for(int i=0; i<5; i++){
                signalSource[i] = new double[3][];
            }
        }
        //
        Intent it = getIntent();
        String fname = it.getStringExtra("filename");
        Log.d(TAG, "Compute, filename =" + fname);
        sourceIdx = it.getIntExtra("class", 0);
        Log.d(TAG, "class =" + sourceIdx);
        //
        //fname = "WhiteISO500Exp10_1507187344233.jpg";
        //fname = "WaterISO3000Exp100_1507187197782.jpg";

        //File f = new File(Environment.getExternalStorageDirectory()+"/"+fname);
        File f = new File(fname);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        showImg();
        // 重新計算分割?
        AlertDialog.Builder builder = new AlertDialog.Builder(ComputeActivity.this);
        builder.setTitle("Perform sample segmentation?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.segmented = false;
                //lightsourceCalc();
                waveCalcThd();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.segmented = true;
                waveCalcThd();
            }
        }).show();
        //if(!MainActivity.segmented) Toast.makeText(ComputeActivity.this, "Perfrom sample segmentation", Toast.LENGTH_SHORT).show();
        //waveCalcThd();
    }

    void showImg() {
        int iw, ih, vw, vh;
        boolean needRotate;

        iw = bitmap.getWidth();
        ih = bitmap.getHeight();

        if (iw < ih) {
            needRotate = false;
        } else {
            needRotate = true;
        }
        Log.d(TAG, "iw=" + iw + ",ih=" + ih);

        if (needRotate) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        iv.setImageBitmap(bitmap);
    }

    void showSegImg(){
        int opaqueRed = 0xffff0000; // from a color int
        for(int y=0; y<bitmap.getHeight(); y++){
            bitmap.setPixel(x1_l-1, y, opaqueRed);
            bitmap.setPixel(x1_l, y, opaqueRed);
            bitmap.setPixel(x1_l+1, y, opaqueRed);
            bitmap.setPixel(x1_r-1, y, opaqueRed);
            bitmap.setPixel(x1_r, y, opaqueRed);
            bitmap.setPixel(x1_r+1, y, opaqueRed);
            bitmap.setPixel(x2_l-1, y, opaqueRed);
            bitmap.setPixel(x2_l, y, opaqueRed);
            bitmap.setPixel(x2_l+1, y, opaqueRed);
            bitmap.setPixel(x2_r-1, y, opaqueRed);
            bitmap.setPixel(x2_r, y, opaqueRed);
            bitmap.setPixel(x2_r+1, y, opaqueRed);
            bitmap.setPixel(x3_l-1, y, opaqueRed);
            bitmap.setPixel(x3_l, y, opaqueRed);
            bitmap.setPixel(x3_l+1, y, opaqueRed);
            bitmap.setPixel(x3_r-1, y, opaqueRed);
            bitmap.setPixel(x3_r, y, opaqueRed);
            bitmap.setPixel(x3_r+1, y, opaqueRed);
        }
        iv.setImageBitmap(bitmap);
    }

    // calculate light source wavelength
    public void waveCalcThd(){
        pbar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Progress bar: set Visible");
        pbar.setMax(100);
        pbar.setProgress(0);

        Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if (bitmap != null) {
                                lightsourceCalc();
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        pbar.setVisibility(View.GONE);
                                        showSegImg();
                                        Intent it = new Intent(ComputeActivity.this, ChartActivity.class);
                                        it.putExtra("title", classname[sourceIdx]+"Source Raw Spectrum");
                                        it.putExtra("numChart", 3);
                                        it.putExtra("lightsource1", signalSource[sourceIdx][1]);
                                        it.putExtra("signal name 1", " Center ");
                                        it.putExtra("lightsource2", signalSource[sourceIdx][2]);
                                        it.putExtra("signal name 2", " Right ");
                                        it.putExtra("lightsource3", signalSource[sourceIdx][0]);
                                        it.putExtra("signal name 3", " Left ");
                                        startActivity(it);
                                    }
                                }
                        );
                    }
                }
        );
        thread.start();
    }

    public void lightsourceCalc(){

        double peak = 0, l_min=1e100, threshold = 0.75, w_ratio=1.5;
        double th1, th2;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, "image height=" + height + ", width="+ width);

        lightsourceH = null;
        lightsourceH = new double[width]; // horizontal
        if(signalSource[sourceIdx][0] == null) {
            for (int i = 0; i < 3; i++) {
                signalSource[sourceIdx][i] = new double[height];
                Log.d(TAG, "initialize source array[" + sourceIdx +"]["+ i + "]");
            }
        }
        Log.d(TAG, "segmented?" + MainActivity.segmented);

        if(!MainActivity.segmented) {
            // find max peak
            for (int x = 0; x < width; x++) {
                double accValue = 0;
                for (int y = 0; y < height; y++) {
                    int c = bitmap.getPixel(x, y);
                    double value = Color.red(c) + Color.green(c) + Color.blue(c);
                    //gray[y][x] = value;
                    accValue += value;
                }
                lightsourceH[x] = accValue;

                if (accValue > peak) {
                    peak = accValue;
                    peakPos = x;
                }
                if (accValue < l_min) l_min = accValue;

                Message msg = new Message();
                msg.what = PROGRESS;
                msg.arg1 = width;
                msg.arg2 = x;
                //Log.v("progress", "y="+Integer.toString(y));
                mHandler.sendMessage(msg);

            }
            Log.d(TAG, "peak value=" + peak + ", pos=" + peakPos + ", min=" + l_min);
            th1 = threshold * peak;
            th2 = (threshold-0.1) * peak;

            for (int x = 0; x < width; x++) {
                if (lightsourceH[x] > th1) {
                    x1_l = x;
                    break;
                }
            }
            for (int x = x1_l; x < width; x++) {
                if (lightsourceH[x] < th2) {
                    x1_r = x;
                    break;
                }
            }
            Log.d(TAG, "x1_l=" + x1_l + ", x1_r=" + x1_r);
            for (int x = x1_r; x < width; x++) {
                if (lightsourceH[x] > th1) {
                    x2_l = x;
                    break;
                }
            }
            for (int x = x2_l; x < width; x++) {
                if (lightsourceH[x] < th2) {
                    x2_r = x;
                    break;
                }
            }
            Log.d(TAG, "x2_l=" + x2_l + ", x2_r=" + x2_r + ", width=" + lightsourceH.length);

            for (int x = x2_r; x < width; x++) {
                //Log.d(TAG, "x="+x+", lightsourceH="+lightsourceH[x]);
                if (lightsourceH[x] > th1) {
                    //Log.d(TAG, "x3_l="+x);
                    x3_l = x;
                    break;
                }
            }

            for (int x = x3_l; x < width; x++) {
                if (lightsourceH[x] < th2) {
                    x3_r = x;
                    break;
                }
            }
            Log.d(TAG, "x3_l=" + x3_l + ", x3_r=" + x3_r);

            // integral
            for (int y = 0; y < height; y++) {
                double accValue = 0;
                for (int x = x2_l; x <= x2_r; x++) {
                    int c = bitmap.getPixel(x, y);
                    double value = Color.red(c) + Color.green(c) + Color.blue(c);
                    //accValue += gray[y][x];
                    accValue += value;
                }
                //lightsourceV1[height-y-1] = accValue;
                //lightsourceV1[y] = accValue;
                signalSource[sourceIdx][1][y] = accValue;

                Message msg = new Message();
                msg.what = PROGRESS2;
                msg.arg1 = height;
                msg.arg2 = 1;
                mHandler.sendMessage(msg);
            }
            Log.d(TAG, "lightsource1:" + signalSource[sourceIdx][1][0] + ", " + signalSource[sourceIdx][1][height / 2]);

            // integral
            for (int y = 0; y < height; y++) {
                double accValue = 0;
                for (int x = x3_l; x <= x3_r; x++) {
                    int c = bitmap.getPixel(x, y);
                    double value = Color.red(c) + Color.green(c) + Color.blue(c);
                    //accValue += gray[y][x];
                    accValue += value;
                }
                //lightsourceV2[height-y-1] = accValue;
                //lightsourceV2[y] = accValue;
                signalSource[sourceIdx][2][y] = accValue;

                Message msg = new Message();
                msg.what = PROGRESS2;
                msg.arg1 = height;
                msg.arg2 = 1;
                //Log.v("progress", "y="+Integer.toString(y));
                mHandler.sendMessage(msg);
            }

            for (int y = 0; y < height; y++) {
                double accValue = 0;
                for (int x = x1_l; x <= x1_r; x++) {
                    int c = bitmap.getPixel(x, y);
                    double value = Color.red(c) + Color.green(c) + Color.blue(c);
                    //accValue += gray[y][x];
                    accValue += value;
                }
                //lightsourceV3[height-y-1] = accValue;
                //lightsourceV3[y] = accValue;
                signalSource[sourceIdx][0][y] = accValue;

                Message msg = new Message();
                msg.what = PROGRESS4;
                msg.arg1 = height;
                msg.arg2 = y;
                //Log.v("progress", "y="+Integer.toString(y));
                mHandler.sendMessage(msg);
            }
            Log.d(TAG, "lightsource3:" + signalSource[sourceIdx][0][0] + ", " + signalSource[sourceIdx][0][height / 2]);
        }
    }

    private float progress_count=0;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){

            switch (msg.what){
                case PROGRESS:
                    int total = msg.arg1;
                    int current = msg.arg2;
                    progress_count = (current*100/total)*7/10;
                    pbar.setProgress((int)progress_count);
                    //Log.v("progress", "current:"+Integer.toString(current)+", now:"+Integer.toString(count));
                    break;
                case PROGRESS2:
                    total = msg.arg1;
                    current = msg.arg2;
                    progress_count = progress_count+((float)current*10/(float)total);
                    pbar.setProgress((int)progress_count);
                    break;
            }
        }
    };
}
