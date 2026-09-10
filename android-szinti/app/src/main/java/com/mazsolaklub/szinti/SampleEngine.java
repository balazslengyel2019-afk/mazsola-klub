package com.mazsolaklub.szinti;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.SparseIntArray;

public final class SampleEngine {
    public static final int PIANO = 0;
    public static final int BASS = 1;
    public static final int VIOLIN = 2;
    public static final int XYLOPHONE = 3;
    public static final int CIMBALOM = 4;
    public static final int SYNTH = 5;
    public static final int TONGUEDRUM = 6;

    private final Context context;
    private final SoundPool pool;
    private final int[][] sampleIds = new int[7][2];
    private final SparseIntArray streamToSound = new SparseIntArray();
    private int metronomeClickId = 0;

    public SampleEngine(Context context) {
        this.context = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(attrs)
                .build();
        preload();
    }

    private void preload() {
        // The first six names and samples are intentionally unchanged from the approved v2.2 build.
        String[] names = {"piano", "bass", "violin", "xylophone", "cimbalom", "synth", "tonguedrum"};
        for (int inst = 0; inst < names.length; inst++) {
            sampleIds[inst][0] = loadRaw(names[inst] + "_c4");
            sampleIds[inst][1] = loadRaw(names[inst] + "_c5");
        }
        metronomeClickId = loadRaw("metronome_click");
    }

    private int loadRaw(String name) {
        int resId = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        return resId == 0 ? 0 : pool.load(context, resId, 1);
    }

    public int play(int instrument, int midi) {
        return playInternal(instrument, midi, 1.0f);
    }

    // Used only by the optional Concert Hall effect. Normal playing uses play() above unchanged.
    public int playEcho(int instrument, int midi, float volumeScale) {
        return playInternal(instrument, midi, Math.max(0f, Math.min(1f, volumeScale)));
    }

    private int playInternal(int instrument, int midi, float volumeScale) {
        if (instrument < 0 || instrument >= sampleIds.length) return 0;
        int anchor = midi < 72 ? 60 : 72;
        int slot = midi < 72 ? 0 : 1;
        int soundId = sampleIds[instrument][slot];
        if (soundId == 0) return 0;
        float rate = (float)Math.pow(2.0, (midi - anchor) / 12.0);
        if (rate < 0.5f) rate = 0.5f;
        if (rate > 1.95f) rate = 1.95f;
        float baseVol = instrument == XYLOPHONE || instrument == CIMBALOM ? 0.34f : 0.30f;
        if (instrument == TONGUEDRUM) baseVol = 0.33f;
        float vol = baseVol * volumeScale;
        int stream = pool.play(soundId, vol, vol, 1, 0, rate);
        if (stream != 0) streamToSound.put(stream, soundId);
        return stream;
    }

    public int playMetronomeClick() {
        if (metronomeClickId == 0) return 0;
        int stream = pool.play(metronomeClickId, 0.43f, 0.43f, 2, 0, 1.0f);
        if (stream != 0) streamToSound.put(stream, metronomeClickId);
        return stream;
    }

    public void stop(int streamId) {
        if (streamId != 0) {
            try { pool.stop(streamId); } catch (Throwable ignored) {}
            streamToSound.delete(streamId);
        }
    }

    public void stopAll() {
        for (int i = 0; i < streamToSound.size(); i++) {
            try { pool.stop(streamToSound.keyAt(i)); } catch (Throwable ignored) {}
        }
        streamToSound.clear();
    }

    public void release() {
        stopAll();
        try { pool.release(); } catch (Throwable ignored) {}
    }
}
