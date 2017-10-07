package com.example.jcliu.androidcamera2api;

import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CalActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCamera2API";
    protected static final int PROGRESS = 0x10000, PROGRESS2 = 0x10001, PROGRESS3 = 0x10002, PROGRESS4 = 0x10003;
    private ImageView iv;
    private Bitmap bitmap = null;
    protected double[] calsource;
    public static double[] wavelength;
    int peakPos = 0, peakPosL, peakPosR, w, i1=0, i2=0;
    private ProgressBar pbar;

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
                Log.d(TAG, "option spectrum:" + calsource[0] + ", " + calsource[1000]);
                Intent it = new Intent(CalActivity.this, CalChartActivity.class);
                //it.putExtra("wavelength", wavelength);
                it.putExtra("calsource", calsource);
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
        Intent it = getIntent();
        String fname = it.getStringExtra("filename");
        Log.d(TAG, "Compute, filename =" + fname);
        //
        fname = "CalISO200Exp100_1507187696092.jpg";

        File f = new File(Environment.getExternalStorageDirectory()+"/"+fname);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        showImg();
        waveThd();
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

    // calculate light source wavelength
    private void waveThd(){
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
                                Log.d(TAG, "Run wavelengthCalc()");
                                wavelengthCalc();
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        pbar.setVisibility(View.GONE);
                                        Intent it = new Intent(CalActivity.this, CalChartActivity.class);
                                        //it.putExtra("wavelength", wavelength);
                                        it.putExtra("calsource", calsource);
                                        //it.putExtra("lightsource2", lightsourceV2);
                                        //it.putExtra("lightsource3", lightsourceV3);
                                        startActivity(it);
                                    }
                                }
                        );
                    }
                }
        );
        thread.start();
    }

    private void wavelengthCalc(){

        double peak = 0, l_min=1e100;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, "wavelengthCals(): image height=" + height + ", width="+ width);

        calsource = null;
        calsource = new double[height]; // vertical

        // integral
        double R_max=0, G_max=0, B_max=0;
        int R_pos=0, G_pos=0, B_pos=0;
        //double[] nm = {436.6, 487.7, 546.5, 587.6, 611.6};
        double[] nm = {460, 532, 590};
        Log.d(TAG, "enter loop");
        for(int y=0; y< height; y++){
            double accValue = 0,  accValue_R=0, accValue_G=0, accValue_B=0;
            for (int x=0; x< width; x++) {
                int c = bitmap.getPixel(x,y);
                double value = Color.red(c)+Color.green(c)+Color.blue(c);
                accValue += value;
                accValue_R += Color.red(c);
                accValue_G += Color.green(c);
                accValue_B += Color.blue(c);
                //Log.d(TAG, "x=" + x + ", y=" + y + ", accValue=" + accValue);
            }
            calsource[y] = accValue;
            //Log.d(TAG, "y=" + y + ", accValue" + calsource[y]);
            if(accValue_R > R_max){
                R_max = accValue_R;
                R_pos = y;
            }
            if(accValue_G > G_max){
                G_max = accValue_G;
                G_pos = y;
            }
            if(accValue_B > B_max){
                B_max = accValue_B;
                B_pos = y;
            }
            Message msg = new Message();
            msg.what = PROGRESS;
            msg.arg1 = height;
            msg.arg2 = y;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
        Log.d(TAG, "calsource: " + calsource[0] + ", " + calsource[height/2]);
        Log.d(TAG, "R max = " + R_max+ " at " + R_pos + ", G max=" + G_max + " at " + G_pos + ", B max=" + B_max + " at " + B_pos);

        //
        double slope1 = (nm[1]-nm[2])/((double)G_pos-(double)R_pos);
        Log.d(TAG, "R pos=" + R_pos + ", G pos=" + G_pos + ", slope=" + slope1);
        double slope2 = (nm[0]-nm[1])/((double)B_pos-(double)G_pos);
        Log.d(TAG, "G pos=" + G_pos + ", B pos=" + B_pos + ", slope=" + slope2);
        double slope = (slope1+slope2)/2;
        // calculate wavelength at each pixel
        if (wavelength != null) // garbage collection?
            wavelength = null;
        wavelength = new double[height];
        for(int y=0; y<height; y++)
            wavelength[y] = (double)(y-G_pos)*slope+nm[1];
        Log.d(TAG, "wavelength[0]=" + wavelength[0] + ", wavelength[R]=" + wavelength[R_pos] + ", wavelength[B]=" + wavelength[B_pos] + ",  wavelength[max]=" + wavelength[height-1]);

    }


    private float progress_count=0;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){

            switch (msg.what){
                case PROGRESS:
                    int total = msg.arg1;
                    int current = msg.arg2;
                    progress_count = (current*100/total);
                    pbar.setProgress((int)progress_count);
                    //Log.d(TAG, "current:"+Integer.toString(current)+", now:"+Integer.toString((int)progress_count));
                    break;
            }
        }
    };

}

