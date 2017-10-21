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

    public static final int SRCNUM = 7; // max. number of input source
    private static final String TAG = "AndroidCamera2API";
    protected static final int PROGRESS = 0x10000, PROGRESS2 = 0x10001, PROGRESS3 = 0x10002, PROGRESS4 = 0x10003;
    private ImageView iv;
    private Bitmap bitmap = null;
    // public stored 1-D spectrum
    public static double[] lightsourceH;//, lightsourceV1, lightsourceV2, lightsourceV3;
    public static double[][][] signalSource; // class: White, Air, Water, 3 parts: Left, Center, Right, 1-D pos
    public static double[][][] NsignalSource; // normalized signal source
    int sourceIdx = 0;
    //
    int peakPos = 0, peakPosL, peakPosR, w, i1=0, i2=0;
    // store the segmented region index
    //public static int x1_l, x1_r, x2_l, x2_r, x3_l, x3_r;
    public static int [][] bound = new int[3][2];
    //
    private ProgressBar pbar;
    private String[] classname = {"Light", "Air", "Water", "Fluid1", "Fluid2", "Fluid2", "Fluid3", "Fluid4"};
    boolean Red_only = false;

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
                it.putExtra("title", classname[sourceIdx]+" Source Raw Spectrum");
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
            signalSource = new double[SRCNUM][][];
            NsignalSource = new double[SRCNUM][][];
            for(int i=0; i<SRCNUM; i++){
                signalSource[i] = new double[3][];  //left, center, right
                NsignalSource[i] = new double[3][];  //left, center, right
            }
        }
        //
        Intent it = getIntent();
        String fname = it.getStringExtra("filename");
        Log.d(TAG, "Compute, filename =" + fname);
        sourceIdx = it.getIntExtra("class", 0); //important, which water source id
        Log.d(TAG, "class =" + sourceIdx);
        Red_only = it.getBooleanExtra("Red", false);
        //
        //fname = "WhiteISO500Exp10_1507187344233.jpg";
        //fname = "WaterISO3000Exp100_1507187197782.jpg";

        //File f = new File(Environment.getExternalStorageDirectory()+"/"+fname);
        File f = new File(fname);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
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
        int [] colorMargin = {0xffff0000, 0xffffffff, 0xffff0000};

        for(int y=0; y<bitmap.getHeight(); y++){
            for(int i=0; i<3; i++){
                for(int j=0; j<2; j++){
                    //Log.d(TAG, "bound["+ i + "][ " + j + "]= " + bound[i][j] + ", y=" + y + ", pixel=" + bitmap.getPixel(bound[i][j]-1, y));
                    bitmap.setPixel(bound[i][j]-1, y, colorMargin[i]);
                    bitmap.setPixel(bound[i][j], y, colorMargin[i]);
                    bitmap.setPixel(bound[i][j]+1, y, colorMargin[i]);
                }
            }
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

        double peak = 0, l_min=1e100, threshold = 0.6;
        double th1, th2;
        double [] th = new double[2];
        double [] peaks = new double[3];
        int [] peaksPos = new int[3];

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, "image height=" + height + ", width="+ width);

        lightsourceH = null;
        lightsourceH = new double[width]; // horizontal
        if(signalSource[sourceIdx][0] == null) {
            for (int i = 0; i < 3; i++) {
                signalSource[sourceIdx][i] = new double[height];
                NsignalSource[sourceIdx][i] = new double[height];
                Log.d(TAG, "initialize source array[" + sourceIdx +"]["+ i + "]");
                Log.d(TAG, "NsignalSource= " + NsignalSource[sourceIdx][0][0]);
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
            //th1 = threshold * peak;
            //th2 = (threshold-0.1) * peak;
            th[0] = threshold*peak;
            th[1] = (threshold-0.1) * peak;

            // find intervals
            int start, cnt=0;
            boolean redo_flag = false; // do not find 3 intervals

            do {
                start = 0;
                redo_flag = false;
                for (int i = 0; i < 3; i++) {
                    // left end point
                    bound[i][0] = -1;
                    for (int x = start; x < width; x++) {
                        if (lightsourceH[x] > th[0]) {
                            bound[i][0] = x;
                            break;
                        }
                    }
                    if (bound[i][0] == -1) {
                        redo_flag = true;
                        Log.d(TAG, "Redo");
                        break;
                    }
                    //
                    peaks[i] = 0; peaksPos[i] = -1;
                    int gapCnt = 0;
                    for (int x = bound[i][0]; x < width; x++) {
                        if(lightsourceH[x] > peaks[i]){
                            peaks[i] = lightsourceH[x];
                            peaksPos[i] = x;
                        }
                        if (lightsourceH[x] < th[1]) {
                            bound[i][1] = x;
                            gapCnt ++;
                            if(gapCnt > 5) break;
                        }
                    }
                    start = bound[i][1];
                    Log.d(TAG, i + " :left bound=" + bound[i][0] + ", right bound=" + bound[i][1] + ", peak value=" + peaks[i] + ", peaksPos=" + peaksPos[i]);
                }
                cnt++;
                th[0] = (threshold-0.5*cnt) * peak;
                th[1] = (threshold-0.5*cnt-0.1) * peak;

            } while (redo_flag && cnt < 5);

             // equalize segment size

            int min_width = width;
            for(int i=0; i<3; i++){
                if((bound[i][1]-bound[i][0]) < min_width) min_width = bound[i][1]-bound[i][0];
            }

            int half_width = min_width/2 + 1;
            for(int i=0; i<3; i++){
                bound[i][0]= peaksPos[i] - half_width;
                bound[i][1] = peaksPos[i] + half_width;
                Log.d(TAG, "lef bound=" + bound[i][0] + ", right bound=" + bound[i][1] + ", peak value=" + peaks[i] + ", peaksPos=" + peaksPos[i]);
            }
        }

        // integral - center
        for(int i=0; i<3; i++) {
            for (int y = 0; y < height; y++) {
                double accValue = 0;
                for (int x = bound[i][0]; x <= bound[i][1]; x++) {
                    int c = bitmap.getPixel(x, y);
                    double value = Color.red(c) + Color.green(c) + Color.blue(c);
                    if(Red_only)
                        accValue += (double) Color.red(c);
                    else
                        accValue += value;
                }
                signalSource[sourceIdx][i][y] = accValue;

                Message msg = new Message();
                msg.what = PROGRESS2;
                msg.arg1 = height;
                msg.arg2 = 1;
                mHandler.sendMessage(msg);
            }
            Log.d(TAG, "lightsource" + i + ":" + signalSource[sourceIdx][i][0] + ", " + signalSource[sourceIdx][i][height / 2]);
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
