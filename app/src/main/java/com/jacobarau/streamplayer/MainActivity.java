package com.jacobarau.streamplayer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SdlService serviceInstance = SdlService.getInstance();
        if (serviceInstance == null) {
            Intent startIntent = new Intent(this.getApplicationContext(), SdlService.class);
            Intent bsIntent = new Intent();
            startIntent.putExtras(bsIntent);
            this.getApplicationContext().startService(startIntent);
        }


//        StreamingService.startPlaying(this, "http://188.65.154.167:8500");
    }
}
