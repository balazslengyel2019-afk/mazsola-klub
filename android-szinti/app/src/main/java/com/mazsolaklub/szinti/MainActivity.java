package com.mazsolaklub.szinti;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SampleEngine audio;
    private SynthViewV25 synthView;
    private VideoView introVideo;
    private boolean synthShown = false;

    private final Runnable introFallback = this::showSynth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        audio = new SampleEngine(this); // keep the approved instrument/audio engine unchanged
        showIntro();
    }

    private void showIntro() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        introVideo = new VideoView(this);
        root.addView(introVideo, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro);
            introVideo.setVideoURI(uri);
            introVideo.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                mp.setVolume(0f, 0f);
                introVideo.start();
            });
            introVideo.setOnCompletionListener(mp -> showSynth());
            introVideo.setOnErrorListener((mp, what, extra) -> {
                showSynth();
                return true;
            });
            handler.postDelayed(introFallback, 3400L);
        } catch (Throwable ignored) {
            handler.postDelayed(introFallback, 300L);
        }
    }

    private void showSynth() {
        if (synthShown || isFinishing()) return;
        synthShown = true;
        handler.removeCallbacks(introFallback);
        try {
            if (introVideo != null) {
                introVideo.stopPlayback();
                introVideo = null;
            }
        } catch (Throwable ignored) {}
        try {
            synthView = new SynthViewV25(this, audio);
            setContentView(synthView);
            hideSystemUi();
        } catch (Throwable fatal) {
            View fallback = new View(this);
            fallback.setBackgroundColor(0xFF79D9FF);
            setContentView(fallback);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (synthView != null) synthView.resumeAnimations();
    }

    @Override
    protected void onPause() {
        if (synthView != null) synthView.pauseAudio();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { if (synthView != null) synthView.release(); } catch (Throwable ignored) {}
        try { if (audio != null) audio.release(); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    private void hideSystemUi() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        } catch (Throwable ignored) {}
    }
}
