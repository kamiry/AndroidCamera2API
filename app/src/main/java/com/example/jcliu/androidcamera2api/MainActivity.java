package com.example.jcliu.androidcamera2api;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.HandlerThread;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCamera2API";
    private TextureView textureView;
    private Button takePictureBtn, updateBtn, AFoffBtn;
    protected CameraDevice myCameraDevice;
    private Size imageDimension;
    private String cameraId;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraCaptureSession cameraCaptureSessions;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    Bundle bundle = null;
    //
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    // view-related
    private EditText ISOText;
    private EditText expTimeText;
    static Long expTime = Long.valueOf(0);
    Long expTimeMax, expTimeMin;
    static int ISOvalue = -1;
    int ISOmin, ISOmax;
    static boolean AFmode = true;
    private TextView ISOTextView, ExpTextView;
    static float minFocusD;
    static float focusDist = -1;
    private SeekBar focusSeekBar;
    String fname_prefix="White";
    String[] fname_options = {"CAL", "WHITE", "AIR", "WATER", "DELAY"};
    static int photoOption=0;
    private String [] photoFilename = new String[5];
    public static boolean segmented = false;
    String filename;
    protected static int spectrum_choice=0;
    static int delayShotNum = 0;
    boolean returnFromReceiver;
    public static String fullDirName;
    public static String dFullDirName;
    // for multiple curve chart
    int maxChkNum=0;
    CheckBox addChkBox [] = new CheckBox[10]; // at most 10 more checkBox
    String[] signalName = {"Air", "Water", "Fluid1", "Fluid2", "Fluid3", "Fluid4", "Fluid5"};

    // option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            /*
            case R.id.photo_option:
                Log.d(TAG, "拍攝標的");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.photo_object)
                        .setItems(R.array.photo_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //fname_prefix = fname_options[i];
                                photoOption = i;
                                Log.d(TAG, "photo option:"+fname_options[photoOption]);
                            }
                        }).show();
                break;
            case R.id.computation:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                builder2.setTitle(R.string.compute_object)
                        .setItems(R.array.photo_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                                it.setType("image/*");
                                startActivityForResult(it, i);
                                Log.d(TAG, "selection over");
                            }
                        }).show();
                break;
                */
            case R.id.spectrum_view:

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle(R.string.spectrum_checklist);
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                View mView = inflater.inflate(R.layout.spectrum_checklist, null);
                final LinearLayout linearLayout = (LinearLayout) mView.findViewById(R.id.spectrum_layout);

                final CheckBox checkBoxCal = (CheckBox) mView.findViewById(R.id.chkCal);
                final CheckBox checkBoxWhite = (CheckBox) mView.findViewById(R.id.chkWhite);
                final CheckBox checkBoxAir = (CheckBox) mView.findViewById(R.id.chkAir);
                final CheckBox checkBoxWater = (CheckBox) mView.findViewById(R.id.chkWater);
                final CheckBox checkBoxWater1 = (CheckBox) mView.findViewById(R.id.chkWater1);
                final CheckBox checkBoxWater2 = (CheckBox) mView.findViewById(R.id.chkWater2);
                final CheckBox checkBoxWater3 = (CheckBox) mView.findViewById(R.id.chkWater3);
                final CheckBox checkBoxWater4 = (CheckBox) mView.findViewById(R.id.chkWater4);

                if(CalActivity.wavelength == null) {
                    checkBoxCal.setChecked(false);
                    checkBoxCal.setEnabled(true);
                }
                else{
                    checkBoxCal.setChecked(true);
                    //checkBoxCal.setEnabled(false);
                }

                if(ComputeActivity.signalSource == null){
                    checkBoxWhite.setChecked(false);
                    checkBoxWhite.setEnabled(true);
                } else {
                    if(ComputeActivity.signalSource[0][0] == null) {
                        checkBoxWhite.setChecked(false);
                        checkBoxWhite.setEnabled(true);
                    }
                    else{
                        checkBoxWhite.setChecked(true);
                        //checkBoxWhite.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[1][0] == null) {
                        checkBoxAir.setChecked(false);
                        checkBoxAir.setEnabled(true);
                    }
                    else{
                        checkBoxAir.setChecked(true);
                        //checkBoxAir.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[2][0] == null) {
                        checkBoxWater.setChecked(false);
                        checkBoxWater.setEnabled(true);
                    }
                    else{
                        checkBoxWater.setChecked(true);
                        //checkBoxWater.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[3][0] == null) {
                        checkBoxWater1.setChecked(false);
                        checkBoxWater1.setEnabled(true);
                    }
                    else{
                        checkBoxWater1.setChecked(true);
                        //checkBoxWater.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[4][0] == null) {
                        checkBoxWater2.setChecked(false);
                        checkBoxWater2.setEnabled(true);
                    }
                    else{
                        checkBoxWater2.setChecked(true);
                        //checkBoxWater.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[5][0] == null) {
                        checkBoxWater3.setChecked(false);
                        checkBoxWater3.setEnabled(true);
                    }
                    else{
                        checkBoxWater3.setChecked(true);
                        //checkBoxWater.setEnabled(false);
                    }
                    if(ComputeActivity.signalSource[6][0] == null) {
                        checkBoxWater4.setChecked(false);
                        checkBoxWater4.setEnabled(true);
                    }
                    else{
                        checkBoxWater4.setChecked(true);
                        //checkBoxWater.setEnabled(false);
                    }
                }

                final CheckBox.OnCheckedChangeListener chklistener = new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(compoundButton == checkBoxCal){
                            Log.d(TAG, "Cal");
                            checkBoxCal.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 0);
                        }
                        if(compoundButton == checkBoxWhite){
                            Log.d(TAG, "White");
                            checkBoxWhite.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 1);
                        }
                        if(compoundButton == checkBoxAir){
                            Log.d(TAG, "Air");
                            checkBoxAir.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 2);
                        }
                        if(compoundButton == checkBoxWater){
                            Log.d(TAG, "Water");
                            checkBoxWater.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 3);
                        }
                        if(compoundButton == checkBoxWater1){
                            maxChkNum = 3;
                            Log.d(TAG, "Water1");
                            checkBoxWater1.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 4);
                        }
                        if(compoundButton == checkBoxWater2){
                            maxChkNum = 4;
                            Log.d(TAG, "Water2");
                            checkBoxWater2.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 5);
                        }
                        if(compoundButton == checkBoxWater3){
                            maxChkNum = 5;
                            Log.d(TAG, "Water3");
                            checkBoxWater3.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 6);
                        }
                        if(compoundButton == checkBoxWater4){
                            maxChkNum = 6;
                            Log.d(TAG, "Water4");
                            checkBoxWater4.setChecked(true);
                            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                            it.setType("image/*");
                            startActivityForResult(it, 7);
                        }
/*
                        for(int i=0; i<chkNum; i++){
                            if(compoundButton == addChkBox[i]){
                                Log.d(TAG, "Additional CheckBox " + i+1);
                                if(chkNum == chkNum-1) { // the last additional checkbox added
                                    chkNum ++;
                                    addChkBox[chkNum-1] = new CheckBox(MainActivity.this);
                                    addChkBox[chkNum-1].setText("新增溶液中樣品 " + chkNum);
                                    linearLayout.addView(addChkBox[chkNum-1]);
                                }
                                addChkBox[i].setChecked(true);
                                Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                                it.setType("image/*");
                                startActivityForResult(it, 4+i);
                            }
                        }
                        */
                    }
                };

                checkBoxCal.setOnCheckedChangeListener(chklistener);
                checkBoxWhite.setOnCheckedChangeListener(chklistener);
                checkBoxAir.setOnCheckedChangeListener(chklistener);
                checkBoxWater.setOnCheckedChangeListener(chklistener);
                checkBoxWater1.setOnCheckedChangeListener(chklistener);
                checkBoxWater2.setOnCheckedChangeListener(chklistener);
                checkBoxWater3.setOnCheckedChangeListener(chklistener);
                checkBoxWater4.setOnCheckedChangeListener(chklistener);

                builder.setView(mView);
                builder.setPositiveButton("進行分析", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(checkBoxAir.isChecked() && checkBoxCal.isChecked() && checkBoxWater.isChecked() && checkBoxWhite.isChecked()) {
                            if(maxChkNum == 0)
                                maxChkNum = 2;
                            // calculate normalized spectrum
                            Log.d(TAG, "Normalized");
                            spectrum_choice = 1;
                            // initialized normalized spectrum array
                            int length = ComputeActivity.signalSource[0][0].length;
                            Log.d(TAG, "length =" + length);
                            if (ComputeActivity.NsignalSource[1][0] == null) { // 初始化正規光譜陣列
                                for(int srcidx=1; srcidx<ComputeActivity.SRCNUM; srcidx++) {
                                    for (int j = 0; j < 3; j++) {
                                        ComputeActivity.NsignalSource[srcidx][j] = new double[length];
                                        Log.d(TAG, "initialize source array[3,4][" + j + "]");
                                    }
                                }
                            }
                            // compute all normalized spectrum (left, center, right)
                            //for(int srcidx=1; srcidx<ComputeActivity.SRCNUM; srcidx++) {  // *** get from checkbox max index
                            for(int srcidx=1; srcidx<=maxChkNum; srcidx++) {
                                for (int k = 0; k < 3; k++) {
                                    for (int j = 0; j < length; j++) {
                                        //Log.d(TAG, "i="+i+", j="+j);
                                        if (ComputeActivity.signalSource[0][k][j] != 0) {
                                            ComputeActivity.NsignalSource[srcidx][k][j] = ComputeActivity.signalSource[srcidx][k][j] / ComputeActivity.signalSource[0][k][j];
                                        } else {
                                            ComputeActivity.NsignalSource[srcidx][k][j] = 0;
                                        }
                                        //Log.d(TAG, "source[3][0][" + j + "]=" + ComputeActivity.signalSource[3][0][j] + ", source[1][0][" + j + "]=" + ComputeActivity.signalSource[1][0][j]);
                                    }
                                }
                            }
                            Log.d(TAG, "Normalized ok");
                            // 20171203
                            // smoothing the result
                            double [] tempSignal  = new double[length];

                            for(int srcidx=1; srcidx<=maxChkNum; srcidx++) {
                                for (int k = 0; k < 3; k++) {

                                    for (int j = 3; j < length-3; j++) {
                                        //Log.d(TAG, "i="+i+", j="+j);
                                        tempSignal[j] =  0.1752*ComputeActivity.NsignalSource[srcidx][k][j] +
                                                0.1063*ComputeActivity.NsignalSource[srcidx][k][j-3] + 0.1403*ComputeActivity.NsignalSource[srcidx][k][j-2] + 0.1658*ComputeActivity.NsignalSource[srcidx][k][j-1] +
                                                0.1063*ComputeActivity.NsignalSource[srcidx][k][j+3] + 0.1403*ComputeActivity.NsignalSource[srcidx][k][j+2] + 0.1658*ComputeActivity.NsignalSource[srcidx][k][j+1];
                                    }
                                    for (int j = 3; j < length-3; j++) {
                                        ComputeActivity.NsignalSource[srcidx][k][j] =  tempSignal[j];
                                    }
                                }
                            }
                            //
                            Intent it = new Intent(MainActivity.this, ChartActivity.class);
                            it.putExtra("title", "Central Sample Spectrum");
                            it.putExtra("is_menu", true);
                            it.putExtra("do_norm", true);
                            it.putExtra("numChart", maxChkNum);
                            for(int k=1; k<=maxChkNum; k++){
                                Log.d(TAG, "putExtra: signal "+ k);
                                it.putExtra("lightsource"+k, ComputeActivity.NsignalSource[k][spectrum_choice]);
                                it.putExtra("signal name "+k, " In "+signalName[k-1]);
                            }
                            //it.putExtra("lightsource1", ComputeActivity.NsignalSource[1][spectrum_choice]);
                            //it.putExtra("signal name 1", " In Air ");
                            //it.putExtra("lightsource2", ComputeActivity.NsignalSource[2][spectrum_choice]);
                            //it.putExtra("signal name 2", " In Water ");
                            Log.d(TAG, "start chart activity");
                            startActivity(it);
                        } else
                            Toast.makeText(MainActivity.this, "資料不足，請確認清單選項皆已勾選", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
                break;
            case R.id.delay_shot:
                builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle(R.string.delay_time);
                inflater = MainActivity.this.getLayoutInflater();
                mView = inflater.inflate(R.layout.dialog_delaytime, null);
                final EditText editTextCnt = (EditText) mView.findViewById(R.id.delay_count);
                final EditText editTextHr = (EditText) mView.findViewById(R.id.delay_hr);
                final EditText editTextMin = (EditText) mView.findViewById(R.id.delay_min);
                final EditText editTextSec = (EditText) mView.findViewById(R.id.delay_sec);

                builder.setView(mView);
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
                        String subDir = "D" + dateFormat.format(new Date());
                        dFullDirName = createStorageDir("SpectroMeterPro/"+subDir);

                        int count, hr, min, sec;
                        if(!editTextCnt.getText().toString().trim().equals("")){
                            count = Integer.parseInt(editTextCnt.getText().toString());
                        } else
                            count = 0;
                        if(!editTextHr.getText().toString().trim().equals("")){
                            hr = Integer.parseInt(editTextHr.getText().toString());
                        } else
                            hr = 0;
                        if(!editTextMin.getText().toString().trim().equals("")){
                            min = Integer.parseInt(editTextMin.getText().toString());
                        } else
                            min = 0;
                        if(!editTextSec.getText().toString().trim().equals("")){
                            sec = Integer.parseInt(editTextSec.getText().toString());
                        } else
                            sec = 0;
                        Log.d(TAG, "Delay count =" + count + ", hr = " + hr + ", min=" + min + ", sec=" + sec);
                        for(int j=0; j<count; j++) {
                            Calendar cal = Calendar.getInstance();
                            // 設定於 ??? 後執行
                            cal.add(Calendar.HOUR, hr*(j+1));
                            cal.add(Calendar.MINUTE, min*(j+1));
                            cal.add(Calendar.SECOND, sec*(j+1));
                            Intent intent = new Intent(MainActivity.this, ShotReceiver.class);
                            intent.addCategory("D" + j);
                            PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
                            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                        }
                    }
                });
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri imgUri;
            imgUri = data.getData();
            Log.d(TAG, "Gallery:" + imgUri.toString());
            filename = getPath(this, imgUri);
            Log.d(TAG, "Gallery:" + filename);
            try {
                cameraCaptureSessions.stopRepeating();
                //updatePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Intent it = null;
            switch (requestCode) {
                case 0: // calibrate
                    it = new Intent(MainActivity.this, CalActivity.class);
                    break;
                /*
                case 1: //whitelight
                    it = new Intent(MainActivity.this, ComputeActivity.class);
                    it.putExtra("class", 0); //0: White light
                    break;
                case 2: //Air
                    it = new Intent(MainActivity.this, ComputeActivity.class);
                    it.putExtra("class", 1);
                    break;
                case 3: //Water
                    it = new Intent(MainActivity.this, ComputeActivity.class);
                    it.putExtra("class", 2);
                    break;
                    */
                default:
                    it = new Intent(MainActivity.this, ComputeActivity.class);
                    it.putExtra("class", requestCode-1);
                    if(requestCode <=2)
                        it.putExtra("Red", false);
                    else
                        it.putExtra("Red", true);
                    break;
            }
            it.putExtra("filename", filename);
            startActivity(it);
        } else {
            Toast.makeText(this, "放棄選圖", Toast.LENGTH_LONG).show();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        fullDirName = createStorageDir("SpectroMeterPro");

        textureView = (TextureView) findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        ISOText = (EditText) findViewById(R.id.editText);
        if(ISOvalue == -1) // initail value only
            ISOvalue = Integer.parseInt(ISOText.getHint().toString());
        else
            ISOText.setText(String.valueOf(ISOvalue));
        expTimeText = (EditText) findViewById(R.id.editText2);
        if(expTime == 0)
            expTime = Long.parseLong(expTimeText.getHint().toString())*1000000;
        else
            expTimeText.setText(String.valueOf((float)expTime/1000000));
        ISOTextView = (TextView) findViewById(R.id.textView);
        ExpTextView = (TextView) findViewById(R.id.textView2);
        // seekBar
        focusSeekBar = (SeekBar) findViewById(R.id.focusSeekBar);
        // initial seekbar
        if(focusDist != -1){
            int pos = (int)(100-(focusDist/minFocusD)*100);
            focusSeekBar.setProgress(pos);
        }
        focusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                focusDist = minFocusD*((float)(100-i)/100);
                AFmode = false;
                AFoffBtn.setText("AF off");
                //Log.d(TAG, "current focus=" + focusDist);
                try {
                    cameraCaptureSessions.stopRepeating();
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        //
        takePictureBtn = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureBtn != null;
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "take picture button");
                Log.d(TAG, "拍攝標的");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.photo_object)
                        .setItems(R.array.photo_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                photoOption = i;
                                Log.d(TAG, "photo option:"+fname_options[photoOption]);
                                takePicture(fullDirName);
                            }
                        })
                        .show();
            }
        });
        AFoffBtn = (Button) findViewById(R.id.btn_AFOff);
        if(AFmode)
            AFoffBtn.setText("AF on");
        else
            AFoffBtn.setText("AF off");
        AFoffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AFmode = !AFmode;
                if(AFmode)
                    AFoffBtn.setText("AF on");
                else
                    AFoffBtn.setText("AF off");
                try {
                    cameraCaptureSessions.stopRepeating();
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        updateBtn = (Button) findViewById(R.id.btn_update);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ISOText.length() !=0) {
                    Log.d(TAG, "ISO: " + ISOText.getText().toString());
                    ISOvalue = Integer.parseInt(ISOText.getText().toString());
                }
                if( expTimeText.length() != 0){
                    Log.d(TAG, "Exposure time: " + expTimeText.getText().toString());
                    Double expTimeD = Double.parseDouble(expTimeText.getText().toString())*1000000; //ms to ns
                    expTime = expTimeD.longValue();
                }
                else
                    Log.d(TAG, "Exposure time null");
                Log.d(TAG, "stop repeating request (for preview)");
                try {
                    cameraCaptureSessions.stopRepeating();
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            myCameraDevice = cameraDevice;
            //
            Log.d(TAG, "stateCallback.onOpened()");
            //createCameraPreview();
            //

            //if(ShotReceiver.Delay_shot == true){
            if(bundle != null){
                Log.d(TAG, "Delay shot");
                returnFromReceiver = bundle.getBoolean("Delay_shot");
                int delayShotID = bundle.getInt("Delay_shot_ID");
                String dirName = bundle.getString("dirName");
                dFullDirName = dirName;
                Log.d(TAG,"Dealy shot =" +  returnFromReceiver);
                Log.d(TAG, "Shot ID="+delayShotID+", ISO value = " + ISOvalue + ", Exposure time = " + expTime + ", Focus distance =" + focusDist + ", photo option=" + photoOption + ", AFmode = " + AFmode);
                //takePictureBtn.callOnClick();
                //if(ShotReceiver.delayShotID > delayShotNum) {
                if(delayShotID > delayShotNum) {
                    delayShotNum = delayShotID;
                    photoOption = 4;
                    takePicture(dirName);
                } else{
                    createCameraPreview();
                }
            } else{
                Log.d(TAG, "Null bundle");
                createCameraPreview();
            }

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            myCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            if(myCameraDevice != null) myCameraDevice.close();
            myCameraDevice = null;
        }
    };

    private void openCamera(){
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "openCamera(): is camera open?");
        try{
            cameraId = manager.getCameraIdList() [0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            //
            Range<Integer> ISOrange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            ISOmin = ISOrange.getLower();
            ISOmax = ISOrange.getUpper();
            Log.d(TAG, "ISO min=" + ISOmin);
            Log.d(TAG, "ISO max=" + ISOmax);
            ISOTextView.setText("ISO ("+ISOmin+" to "+ISOmax+"): ");
            Range<Long> expTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            expTimeMax = expTimeRange.getUpper();
            expTimeMin = expTimeRange.getLower();
            Log.d(TAG, "Exposure time min=" + expTimeMin);
            Log.d(TAG, "Exposure time max=" + expTimeMax);
            ExpTextView.setText("Exposure time ("+expTimeMin.doubleValue()/1000000+" to "+expTimeMax.doubleValue()/1000000+"): ");
            /*float[] focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            for(int i=0; i<focusRange.length; i++)
                Log.d(TAG,"focus =" + focusRange[i]);
                */
            minFocusD = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            Log.d(TAG, "min focus distance = " + minFocusD);
            //
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class) [0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){
                ActivityCompat.requestPermissions(MainActivity.this, new String [] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                Log.d(TAG, "request permission");
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "openCamera ok");
    }


    protected void createCameraPreview(){
        try{
            Log.d(TAG, "createCameraPreview");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = myCameraDevice.createCaptureRequest(myCameraDevice.TEMPLATE_PREVIEW);
            //captureRequestBuilder = myCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            captureRequestBuilder.addTarget(surface);
            myCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == myCameraDevice)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview(){
        if(null == myCameraDevice) {
            Log.d(TAG, "updatePreview error, device null");
        }
        //captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        //captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        if(AFmode)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        else {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDist);
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        //captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        //Log.d(TAG, "ISO: "+ ISOvalue);
        //Log.d(TAG, "Exposure time: " + expTime);
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime); //Long.valueOf("10000000")
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISOvalue);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("Start Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // 間隔拍攝
        Intent it = this.getIntent();
        bundle = it.getExtras();
        //
        if(textureView.isAvailable()){
            Log.d(TAG, "on Resume: open camera");
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
            Log.d(TAG, "on Resume: texture");
        }
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //
        ShotReceiver.Delay_shot = false;
        //
        stopBackgroundThread();
        //
        /*
        if(cameraCaptureSessions !=null) {
            try {
                cameraCaptureSessions.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }*/
    }

    protected void takePicture(String dirName){
        if (null == myCameraDevice) {
            Log.d(TAG, "takePicture(): camera device is null");
            return;
        }
        else
            Log.d(TAG, "takePicture() started");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(myCameraDevice.getId()); // cameraId ???
            Size [] jpegSizes = null;
            if (characteristics != null){
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                Log.d(TAG, "takePicture(): camera char ok");
            }
            int width = 640; //initial values
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length){
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = myCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            Log.d(TAG, "takePicture(): AFmode="+AFmode);
            if(AFmode)
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            else {
                Log.d(TAG, "takePicture(): focusDist = "+focusDist);
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDist);
            }
            //captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            //captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            //captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISOvalue);
            // orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation)); // orientation not consistent?
            // file name
            fname_prefix = fname_options[photoOption];
            //String prefix = fname_prefix + "ISO" + ISOvalue + "Exp" + (expTime/1000000) + "_" + System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmmss");
            String prefix = fname_prefix + dateFormat.format(new Date())+"_ISO" + ISOvalue + "Exp" + (expTime/1000000);
            final String fname = prefix+".jpg";
            photoFilename[photoOption] = fname; // record for calculating spectrum
            Log.d(TAG, "takePicture(): filename -" + fname);
            //final File file = new File(Environment.getExternalStorageDirectory()+"/"+fname);
            //final File file = new File(fullDirName+"/"+fname);
            final File file = new File(dirName+"/"+fname);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener(){

                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try {
                        Log.d(TAG, "takePicture(): onImageAvailable");
                        image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e){
                        e.printStackTrace();
                    } catch (IOException e){
                        e.printStackTrace();
                    } finally {
                        if(image != null)
                            image.close();
                    }
                }
                private void save (byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try{
                        output = new FileOutputStream(file);
                        output.write(bytes);
                        Log.d(TAG, "takePicture(): save ok - " +fname);
                    } finally {
                        if(null != output)
                            output.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback(){
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    double mExposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    Log.d(TAG, "takePicture(): onCaptureCompleted");
                    Toast.makeText(MainActivity.this, "saved:"+file+"\n"+mExposureTime/1e9+" sec", Toast.LENGTH_SHORT).show();
                    // 拍照後直接計算
                    if(!returnFromReceiver) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Perform spectrum computation?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent it = null;
                                if (photoOption == 0) {
                                    it = new Intent(MainActivity.this, CalActivity.class);
                                } else {
                                    it = new Intent(MainActivity.this, ComputeActivity.class);
                                    it.putExtra("class", photoOption - 1);
                                }
                                //it.putExtra("filename", Environment.getExternalStorageDirectory() + "/" + fname);
                                it.putExtra("filename", fullDirName + "/" + fname);
                                startActivity(it);
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.segmented = true;
                                createCameraPreview();
                            }
                        }).show();
                    } else {
                        returnFromReceiver = false;
                        createCameraPreview();
                    }
                }
            };
            myCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        // important!!!
                        Log.d(TAG, "takePicture(): createCaptureSession.onConfigured");
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                        Log.d(TAG, "takePicture(): call capture");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "takePicture(), camera exception");
            e.printStackTrace();
        }

    }

    public String createStorageDir(String dirname){
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath().toString();
        Log.d(TAG, "ExtStorage: " + dir);
        File file = new File(dir, dirname);
        //String fullDirName = file.toString();  // record the full dir name in the global variable
        if (!file.exists()){
            if (!file.mkdir()){
                Toast.makeText(this, "資料夾目錄建立失敗", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Create Directory:"+ file.toString() + " fail");
            }
            else
                Log.d(TAG, "Directory:"+ file.toString() + " success");
        }
        else
            Log.d(TAG, "Directory:"+ file.toString() + " already exists");
        return file.toString();
    }

    // file path
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
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
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
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
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
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

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
