package com.example.jcliu.androidcamera2api;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.os.Environment;
import android.os.HandlerThread;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.ArrayList;
import java.util.Arrays;
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
    Long expTime, expTimeMax, expTimeMin;
    int ISOvalue, ISOmin, ISOmax;
    private boolean AFmode = true;
    private TextView ISOTextView, ExpTextView;
    private float minFocusD, focusDist;
    private SeekBar focusSeekBar;
    String fname_prefix="White";
    String[] fname_options = {"Cal", "White", "Air", "Water"};
    private int photoOption=0;
    private String [] photoFilename = new String[4];

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
            case R.id.photo_option:
                Log.d(TAG, "option 1");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.photo_object)
                        .setItems(R.array.photo_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fname_prefix = fname_options[i];
                                photoOption = i;
                                Log.d(TAG, "photo option:"+fname_prefix);
                            }
                        }).show();
                break;
            case R.id.computation:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                builder2.setTitle(R.string.compute_object)
                        .setItems(R.array.photo_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch(i){
                                    case 0: //calibrate
                                        Intent it = new Intent(MainActivity.this, CalActivity.class);
                                        it.putExtra("filename", photoFilename[0]);
                                        startActivity(it);
                                        break;
                                    case 1: //whitelight
                                        Intent it2 = new Intent(MainActivity.this, ComputeActivity.class);
                                        it2.putExtra("filename", photoFilename[1]);
                                        startActivity(it2);
                                        break;
                                }
                            }
                        }).show();
                break;
            case R.id.spectrum_view:
                if(ComputeActivity.lightsourceV1 != null) {
                    Intent it3 = new Intent(this, ChartActivity.class);
                    it3.putExtra("lightsource1", ComputeActivity.lightsourceV1);
                    it3.putExtra("lightsource2", ComputeActivity.lightsourceV2);
                    it3.putExtra("lightsource3", ComputeActivity.lightsourceV3);
                    startActivity(it3);
                } else
                    Toast.makeText(MainActivity.this, "No lightsource spectrum", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        textureView = (TextureView) findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        ISOText = (EditText) findViewById(R.id.editText);
        ISOvalue = Integer.parseInt(ISOText.getHint().toString());
        expTimeText = (EditText) findViewById(R.id.editText2);
        expTime = Long.parseLong(expTimeText.getHint().toString())*1000000;
        ISOTextView = (TextView) findViewById(R.id.textView);
        ExpTextView = (TextView) findViewById(R.id.textView2);
        // seekBar
        focusSeekBar = (SeekBar) findViewById(R.id.focusSeekBar);
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
                takePicture();
            }
        });
        AFoffBtn = (Button) findViewById(R.id.btn_AFOff);
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
            Log.d(TAG, "onOpened");
            myCameraDevice = cameraDevice;
            Log.d(TAG, "call createCameraPreview");
            createCameraPreview();
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
        Log.d(TAG, "is camera open?");
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
        mBackgroundThread = new HandlerThread("Camera Background");
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
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if(textureView.isAvailable()){
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        stopBackgroundThread();
    }

    protected void takePicture(){
        if (null == myCameraDevice) {
            Log.e(TAG, "camera device is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(myCameraDevice.getId()); // cameraId ???
            Size [] jpegSizes = null;
            if (characteristics != null){
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
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
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            //captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
            //captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISOvalue);
            // orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation)); // orientation not consistent?
            // file name
            String prefix = fname_prefix + "ISO" + ISOvalue + "Exp" + (expTime/1000000) + "_" + System.currentTimeMillis();
            String fname = prefix+".jpg";
            photoFilename[photoOption] = fname; // record for calculating spectrum
            final File file = new File(Environment.getExternalStorageDirectory()+"/"+fname);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener(){

                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try {
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
                    Toast.makeText(MainActivity.this, "saved:"+file+"\n"+mExposureTime/1e9+" sec", Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            myCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        // important!!!
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


}
