package com.example.jcliu.androidcamera2api;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Created by jcliu on 2017/10/8.
 */

public class ShotReceiver extends BroadcastReceiver {

    static int delayShotID = 0;
    static boolean Delay_shot=false;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "Alarm", Toast.LENGTH_SHORT).show();
        Log.d("AndroidCamera2API", "Receiver");
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        Log.d("AndroidCamera2API", "wakelock acquire");
        wakeLock.release();  // if not released, takePicture() will be hold? (unknown reason)
        Log.d("AndroidCamera2API", "wakelock release");

        Delay_shot = true;
        delayShotID ++;

        Intent it = new Intent(context, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("Delay_shot", true);
        bundle.putInt("Delay_shot_ID", delayShotID);
        it.putExtras(bundle);

        context.startActivity(it);

    }
}
