package com.yumemoenova.whalecatcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public final class MainActivity extends Activity {
    private CatcherView game;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        game = new CatcherView(this);
        setContentView(game);
        immersive();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    @Override protected void onResume() {
        super.onResume();
        if (game != null) game.setActive(true);
    }

    @Override protected void onPause() {
        if (game != null) game.setActive(false);
        super.onPause();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
