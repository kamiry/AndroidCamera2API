package com.example.jcliu.androidcamera2api;

import android.content.Intent;
import android.content.res.AssetManager;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
    double[] lightsourceH, lightsourceV1, lightsourceV2, lightsourceV3;
    int peakPos = 0, peakPosL, peakPosR, w, i1=0, i2=0;
    private ProgressBar pbar;

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
        fname = "WhiteISO500Exp10_1507187344233.jpg";

        File f = new File(Environment.getExternalStorageDirectory()+"/"+fname);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        showImg();
        waveCalcThd();
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
                                        Intent it = new Intent(ComputeActivity.this, ChartActivity.class);
                                        //it.putExtra("wavelength", wavelength);
                                        it.putExtra("lightsource1", lightsourceV1);
                                        it.putExtra("lightsource2", lightsourceV2);
                                        //it.putExtra("darksource", darksource);
                                        //it.putExtra("spectro", spectro);
                                        //it.putExtra("SpectroR", spectroR);
                                        //it.putExtra("SpectroG", spectroG);
                                        //it.putExtra("SpectroB", spectroB);
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

        double peak = 0, l_min=1e100;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, "image height=" + height + ", width="+ width);

        lightsourceH = null;
        lightsourceV1 = null; lightsourceV2 = null; lightsourceV3 = null;
        lightsourceH = new double[width]; // horizontal
        lightsourceV1 = new double[height]; // vertical
        lightsourceV2 = new double[height]; // vertical
        lightsourceV3 = new double[height]; // vertical

        // find peak index in center column
        for(int x=0; x<width; x++){
            double accValue = 0;
            for(int y=0; y<height; y++){
                int c = bitmap.getPixel(x, y);
                double value = Color.red(c) + Color.green(c) + Color.blue(c);
                //gray[y][x] = value;
                accValue += value;
            }
            lightsourceH[x] = accValue;
            if(accValue > peak) {
                peak = accValue;
                peakPos = x;
            }
            if(accValue < l_min) l_min = accValue;

            Message msg = new Message();
            msg.what = PROGRESS;
            msg.arg1 = width;
            msg.arg2 = x;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
        Log.d(TAG, "peak value="+peak+", pos="+peakPos+", min="+l_min);

        // two side lobes
        double th = (l_min + (peak-l_min)*0.1353);
        for(int i=peakPos;i>=0;i--){
            if (lightsourceH[i] < th) {
                i1 = i;
                break;
            }
        }
        for(int i=peakPos;i<width;i++){
            if (lightsourceH[i] < th) {
                i2 = i;
                break;
            }
        }
        w = 3*Math.max(peakPos-i1,i2-peakPos);
        Log.d(TAG, "i1="+i1+", i2="+i2+", w="+w);
        i1 = peakPos - Math.round(w/2);
        i2 = peakPos + Math.round(w/2);
        Log.d(TAG, "i1="+i1+", i2="+i2+", w="+w);

        // integral
        for(int y=0; y<height; y++){
            double accValue = 0;
            for(int x=i1; x<=i2; x++){
                int c = bitmap.getPixel(x, y);
                double value = Color.red(c) + Color.green(c) + Color.blue(c);
                //accValue += gray[y][x];
                accValue += value;
            }
            lightsourceV1[height-y-1] = accValue;

            Message msg = new Message();
            msg.what = PROGRESS2;
            msg.arg1 = height;
            msg.arg2 = y;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
        Log.d(TAG,"lightsource1:" + lightsourceV1[0] + ", " + lightsourceV1[height/2]);

        // find peak in Right
        peak = 0; l_min=1e100;
        for(int x=0; x<500; x++){
            double accValue = lightsourceH[i2+x];
            if(accValue > peak) {
                peak = accValue;
                peakPosR = i2+x;
            }
            if(accValue < l_min) l_min = accValue;

            Message msg = new Message();
            msg.what = PROGRESS3;
            msg.arg1 = 500;
            msg.arg2 = x;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
        Log.d(TAG, "Right peak value="+peak+", pos="+peakPosR+", min="+l_min);

        // integral
        i1 = peakPosR - Math.round(w/2);
        i2 = peakPosR + Math.round(w/2);
        Log.d(TAG, "i1="+i1+", i2="+i2+", w="+w);
        for(int y=0; y<height; y++){
            double accValue = 0;
            for(int x=i1; x<=i2; x++){
                int c = bitmap.getPixel(x, y);
                double value = Color.red(c) + Color.green(c) + Color.blue(c);
                //accValue += gray[y][x];
                accValue += value;
            }
            lightsourceV2[height-y-1] = accValue;

            Message msg = new Message();
            msg.what = PROGRESS4;
            msg.arg1 = height;
            msg.arg2 = y;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
        Log.d(TAG,"lightsource2:" + lightsourceV2[0] + ", " + lightsourceV2[height/2]);
    }


    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case PROGRESS:
                    int total = msg.arg1;
                    int current = msg.arg2;
                    int count = (current*100/total)*5/10;
                    pbar.setProgress(count);
                    //Log.v("progress", "current:"+Integer.toString(current)+", now:"+Integer.toString(count));
                    break;
                case PROGRESS2:
                    total = msg.arg1;
                    current = msg.arg2;
                    count = 50+(current*100/total)/10;
                    pbar.setProgress(count);
                    break;
                case PROGRESS3:
                    total = msg.arg1;
                    current = msg.arg2;
                    count = 60+(current*100/total)/10;
                    pbar.setProgress(count);
                    break;
                case PROGRESS4:
                    total = msg.arg1;
                    current = msg.arg2;
                    count = 70+(current*100/total)/10;
                    pbar.setProgress(count);
                    break;
            }
        }
    };
}
