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

    private final Context context;
    private final SoundPool pool;
    private final int[][] sampleIds = new int[6][2];
    private final SparseIntArray streamToSound = new SparseIntArray();

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
        String[] names = {"piano", "bass", "violin", "xylophone", "cimbalom", "synth"};
        for (int inst = 0; inst < names.length; inst++) {
            sampleIds[inst][0] = loadRaw(names[inst] + "_c4");
            sampleIds[inst][1] = loadRaw(names[inst] + "_c5");
        }
    }

    private int loadRaw(String name) {
        int resId = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        return resId == 0 ? 0 : pool.load(context, resId, 1);
    }

    public int play(int instrument, int midi) {
        if (instrument < 0 || instrument >= sampleIds.length) return 0;
        int anchor = midi < 72 ? 60 : 72;
        int slot = midi < 72 ? 0 : 1;
        int soundId = sampleIds[instrument][slot];
        if (soundId == 0) return 0;
        float rate = (float)Math.pow(2.0, (midi - anchor) / 12.0);
        if (rate < 0.5f) rate = 0.5f;
        if (rate > 1.95f) rate = 1.95f;
        float vol = instrument == XYLOPHONE || instrument == CIMBALOM ? 0.34f : 0.30f;
        int stream = pool.play(soundId, vol, vol, 1, 0, rate);
        if (stream != 0) streamToSound.put(stream, soundId);
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
