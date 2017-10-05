package com.example.jcliu.androidcamera2api;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ComputeActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCamera2API";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute);

        Bitmap bitmap = null;
        File f = new File(_path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        image.setImageBitmap(bitmap);

        //lightsourceCalc();
    }

/*
    public void lightsourceCalc(){
        AssetManager assetManager = getAssets();
        InputStream inputStream = null;
        Bitmap bitmap;
        double peak = 0, l_min=1e100;


        Log.v(TAG, "enter lightsourceCalc()");
        try {
            //inputStream = assetManager.open("lightsource.jpg");
            inputStream = assetManager.open("lightsource2.jpg");
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("progress", "exception");
        }
        Log.v("progress", "assets ok");
        bitmap = BitmapFactory.decodeStream(inputStream);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.v("progress", "height=" + height + ", width="+ width);

        lightsourceH = null;
        lightsourceV = null;
        lightsourceH = new double[width]; // horizontal
        lightsourceV = new double[height]; // vertical
        //double[][] gray = new double[height][width];  // gray image

        // find peak index in center column, convert to gray image in gray[][]
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
        Log.v("progress", "peak value="+peak+", pos="+peakPos+", min="+l_min);
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
        Log.v("progress", "i1="+i1+", i2="+i2+", w="+w);
        i1 = peakPos - Math.round(w/2);
        i2 = peakPos + Math.round(w/2);
        Log.v("progress", "i1="+i1+", i2="+i2+", w="+w);

        // integral
        for(int y=0; y<height; y++){
            double accValue = 0;
            for(int x=i1; x<=i2; x++){
                int c = bitmap.getPixel(x, y);
                double value = Color.red(c) + Color.green(c) + Color.blue(c);
                //accValue += gray[y][x];
                accValue += value;
            }
            lightsourceV[height-y-1] = accValue;

            Message msg = new Message();
            msg.what = PROGRESS2;
            msg.arg1 = height;
            msg.arg2 = y;
            //Log.v("progress", "y="+Integer.toString(y));
            mHandler.sendMessage(msg);
        }
    }
*/
}
