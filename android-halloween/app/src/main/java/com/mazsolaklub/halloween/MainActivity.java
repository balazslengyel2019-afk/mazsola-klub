package com.mazsolaklub.halloween;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;

public class MainActivity extends Activity {
    private HalloweenGameView gameView;
    private VideoView introView;
    private FrameLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean introShowing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyImmersiveMode();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        // The main menu is already rendered behind the intro, so there is never a black gap.
        gameView = new HalloweenGameView(this);
        gameView.setVisibility(View.VISIBLE);
        root.addView(gameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        introView = new VideoView(this);
        introView.setBackgroundColor(Color.TRANSPARENT);
        introView.setMediaController(null);

        FrameLayout.LayoutParams introParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(introView, introParams);

        setContentView(root);

        Uri introUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro);
        introView.setVideoURI(introUri);

        introView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            mp.setVolume(0f, 0f);

            int videoW = mp.getVideoWidth();
            int videoH = mp.getVideoHeight();

            if (videoW > 0 && videoH > 0) {
                int screenW = getResources().getDisplayMetrics().widthPixels;
                int targetH = Math.round(screenW * (videoH / (float) videoW));

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        screenW,
                        targetH,
                        Gravity.CENTER);
                introView.setLayoutParams(lp);
            }

            introView.setAlpha(1f);
            introView.start();

            // Exactly 3 seconds visible, then a short crossfade into the already-rendered main menu.
            handler.postDelayed(this::fadeOutIntro, 3000);
        });

        introView.setOnErrorListener((mp, what, extra) -> {
            finishIntro();
            return true;
        });
    }

    private void fadeOutIntro() {
        if (!introShowing || introView == null) return;

        introView.animate()
                .alpha(0f)
                .setDuration(450)
                .withEndAction(this::finishIntro)
                .start();
    }

    private void finishIntro() {
        if (!introShowing) return;
        introShowing = false;
        handler.removeCallbacksAndMessages(null);

        try {
            if (introView != null) introView.stopPlayback();
        } catch (Exception ignored) {}

        if (introView != null) {
            root.removeView(introView);
            introView = null;
        }

        if (gameView != null) gameView.onResumeGame();
        applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveMode();

        if (!introShowing && gameView != null) {
            gameView.onResumeGame();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onPauseGame();

        if (introView != null && introView.isPlaying()) {
            introView.pause();
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (introShowing) {
            finishIntro();
            return;
        }

        if (gameView != null && gameView.handleBack()) return;
        super.onBackPressed();
    }
}
