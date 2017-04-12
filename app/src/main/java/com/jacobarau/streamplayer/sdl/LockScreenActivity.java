package com.jacobarau.streamplayer.sdl;

import android.app.Activity;
import android.os.Bundle;

import com.jacobarau.streamplayer.R;

public class LockScreenActivity extends Activity {
    LockScreenPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter = new LockScreenPresenter(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.onStop();
        presenter = null;
    }
}
