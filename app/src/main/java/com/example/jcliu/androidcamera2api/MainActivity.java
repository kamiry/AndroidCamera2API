package com.example.jcliu.androidcamera2api;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private Button takePictureBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        textureView = (TextureView) findViewById(R.id.textureView);
        assert textureView != null;
        //textureView.setSurfaceTextureListener(textureListener);
        //
        takePictureBtn = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureBtn != null;
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //takePicture();
            }
        });
    }

}
