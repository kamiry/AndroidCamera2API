package com.example.jcliu.androidcamera2api;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


/**
 * Created by jcliu on 2017/10/8.
 */

public class ShotReceiver extends BroadcastReceiver {

    static int delayShotID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "Alarm", Toast.LENGTH_SHORT).show();
        delayShotID ++;
        Intent it = new Intent(context, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("Delay_shot", true);
        bundle.putInt("Delay_shot_ID", delayShotID);
        it.putExtras(bundle);
        context.startActivity(it);
    }
}
