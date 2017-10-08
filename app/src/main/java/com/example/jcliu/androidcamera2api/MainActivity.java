package com.example.jcliu.androidcamera2api;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
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
    public static boolean segmented = false;
    String filename;

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
                                Intent it = new Intent(Intent.ACTION_GET_CONTENT);
                                it.setType("image/*");
                                startActivityForResult(it, i);
                                Log.d(TAG, "selection over");
                            }
                        }).show();
                break;
            case R.id.spectrum_view:
                boolean breaknote = false;
                String notify_msg = "";

                if(ComputeActivity.signalSource == null){
                    Toast.makeText(MainActivity.this, "Lack all spectrum data, perform spectrum calculation first", Toast.LENGTH_SHORT).show();
                    break;
                }
                if(ComputeActivity.signalSource[0][0] == null) {
                    notify_msg += "light source, ";
                    breaknote = true;
                }
                if(ComputeActivity.signalSource[1][0] == null) {
                    notify_msg += "sample in air, ";
                    breaknote = true;
                }
                if(ComputeActivity.signalSource[2][0] == null) {
                    notify_msg += "sample in water, ";
                    breaknote = true;
                }

                if(breaknote) {
                    Toast.makeText(MainActivity.this, "Lack spectrum of " + notify_msg + "please perform calculation first", Toast.LENGTH_LONG).show();
                    break;
                }

                AlertDialog.Builder builder3 = new AlertDialog.Builder(MainActivity.this);
                builder3.setTitle(R.string.spectrum_object)
                        .setItems(R.array.sample_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch(i){
                                    case 0: //Left sample
                                        Intent it5 = new Intent(MainActivity.this, ChartActivity.class);
                                        it5.putExtra("title", "Left Sample Spectrum");
                                        it5.putExtra("numChart", 3);
                                        it5.putExtra("lightsource1", ComputeActivity.signalSource[0][0]);
                                        it5.putExtra("signal name 1", " White Light ");
                                        it5.putExtra("lightsource2", ComputeActivity.signalSource[1][0]);
                                        it5.putExtra("signal name 2", " In Air ");
                                        it5.putExtra("lightsource3", ComputeActivity.signalSource[2][0]);
                                        it5.putExtra("signal name 3", " In Water ");
                                        startActivity(it5);
                                        break;
                                    case 1: //Central sample
                                        it5 = new Intent(MainActivity.this, ChartActivity.class);
                                        it5.putExtra("title", "Central Sample Spectrum");
                                        it5.putExtra("numChart", 3);
                                        it5.putExtra("lightsource1", ComputeActivity.signalSource[0][1]);
                                        it5.putExtra("signal name 1", " White Light ");
                                        it5.putExtra("lightsource2", ComputeActivity.signalSource[1][1]);
                                        it5.putExtra("signal name 2", " In Air ");
                                        it5.putExtra("lightsource3", ComputeActivity.signalSource[2][1]);
                                        it5.putExtra("signal name 3", " In Water ");
                                        startActivity(it5);
                                        break;
                                    case 2: //Right sample
                                        it5 = new Intent(MainActivity.this, ChartActivity.class);
                                        it5.putExtra("title", "Right Sample Spectrum");
                                        it5.putExtra("numChart", 3);
                                        it5.putExtra("lightsource1", ComputeActivity.signalSource[0][2]);
                                        it5.putExtra("signal name 1", " White Light ");
                                        it5.putExtra("lightsource2", ComputeActivity.signalSource[1][2]);
                                        it5.putExtra("signal name 2", " In Air ");
                                        it5.putExtra("lightsource3", ComputeActivity.signalSource[2][2]);
                                        it5.putExtra("signal name 3", " In Water ");
                                        startActivity(it5);
                                        break;
                                }
                            }
                        }).show();
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
            Intent it = null;
            switch (requestCode) {
                case 0: // calibrate
                    it = new Intent(MainActivity.this, CalActivity.class);
                    break;
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
