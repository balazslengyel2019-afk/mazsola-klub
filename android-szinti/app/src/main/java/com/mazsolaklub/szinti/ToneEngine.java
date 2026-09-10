package com.mazsolaklub.szinti;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ToneEngine {
    public static final int PIANO = 0;
    public static final int BASS = 1;
    public static final int VIOLIN = 2;
    public static final int XYLOPHONE = 3;
    public static final int CIMBALOM = 4;
    public static final int SYNTH = 5;

    private final int sampleRate;
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, Voice> voices = new ConcurrentHashMap<>();

    public ToneEngine(Context context) {
        int sr = 48000;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            try {
                String value = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                if (value != null) sr = Integer.parseInt(value);
            } catch (Exception ignored) {}
        }
        sampleRate = Math.max(22050, Math.min(sr, 96000));
    }

    public int noteOn(int midi, int instrument) {
        int id = nextId.getAndIncrement();
        Voice v = new Voice(id, midiToHz(midi), instrument);
        voices.put(id, v);
        v.start();
        return id;
    }

    public void noteOff(int id) {
        Voice v = voices.get(id);
        if (v != null) v.releaseVoice();
    }

    public void stopAll() {
        for (Voice v : voices.values()) v.releaseFast();
        voices.clear();
    }

    private static double midiToHz(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    private final class Voice extends Thread {
        private final int id;
        private final double frequency;
        private final int instrument;
        private volatile boolean released = false;
        private volatile boolean fastRelease = false;
        private long releaseSample = -1;
        private long sampleIndex = 0;
        private double phase = 0.0;
        private double vibratoPhase = 0.0;

        Voice(int id, double frequency, int instrument) {
            super("MazsolaVoice-" + id);
            this.id = id;
            this.frequency = frequency;
            this.instrument = instrument;
        }

        void releaseVoice() { released = true; }
        void releaseFast() { fastRelease = true; released = true; }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(minBuffer, 4096);
            AudioTrack track = null;
            try {
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(bufferBytes)
                        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                        .build();

                short[] pcm = new short[384];
                track.play();
                boolean done = false;
                while (!done && !isInterrupted()) {
                    for (int i = 0; i < pcm.length; i++) {
                        if (released && releaseSample < 0) releaseSample = sampleIndex;
                        double t = sampleIndex / (double) sampleRate;
                        double env = envelope(t, sampleIndex);
                        double s = waveform();
                        double value = Math.max(-1.0, Math.min(1.0, s * env * 0.50));
                        pcm[i] = (short) (value * 32767.0);
                        sampleIndex++;
                        if (env < 0.0007 && (released || autoDecays())) {
                            done = true;
                            for (int j = i + 1; j < pcm.length; j++) pcm[j] = 0;
                            break;
                        }
                        if (t > 12.0) { done = true; break; }
                    }
                    int wrote = track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
                    if (wrote < 0) break;
                }
            } catch (Throwable ignored) {
            } finally {
                try {
                    if (track != null) {
                        track.pause(); track.flush(); track.stop(); track.release();
                    }
                } catch (Throwable ignored) {}
                voices.remove(id);
            }
        }

        private boolean autoDecays() {
            return instrument == PIANO || instrument == XYLOPHONE || instrument == CIMBALOM;
        }

        private double envelope(double t, long n) {
            double attack = Math.min(1.0, t / 0.012);
            double base;
            switch (instrument) {
                case PIANO:
                    base = 0.92 * Math.exp(-1.85 * t) + 0.06 * Math.exp(-0.22 * t); break;
                case BASS:
                    base = t < 0.12 ? 1.0 : 0.74; break;
                case VIOLIN:
                    base = t < 0.10 ? t / 0.10 : 0.72; break;
                case XYLOPHONE:
                    base = Math.exp(-4.25 * t); break;
                case CIMBALOM:
                    base = 0.88 * Math.exp(-2.05 * t) + 0.10 * Math.exp(-0.44 * t); break;
                default:
                    base = t < 0.035 ? t / 0.035 : 0.68; break;
            }
            base *= attack;
            if (releaseSample >= 0) {
                double releaseSeconds = fastRelease ? 0.06 : (instrument == VIOLIN ? 0.34 : 0.20);
                double rt = (n - releaseSample) / (double) sampleRate;
                double r = Math.max(0.0, 1.0 - rt / releaseSeconds);
                base *= r * r;
            }
            return base;
        }

        private double waveform() {
            double p = phase;
            double value;
            switch (instrument) {
                case PIANO:
                    value = 0.72*Math.sin(p)+0.19*Math.sin(2.01*p)+0.08*Math.sin(3*p)+0.035*Math.sin(5.02*p); break;
                case BASS:
                    value = 0.86*Math.sin(p*0.5)+0.20*Math.sin(p)+0.07*Math.sin(1.5*p); break;
                case VIOLIN:
                    vibratoPhase += 2.0*Math.PI*5.2/sampleRate;
                    double pv = p*(1.0 + Math.sin(vibratoPhase)*0.010);
                    value = 0.66*Math.sin(pv)+0.28*Math.sin(2*pv)+0.14*Math.sin(3*pv)+0.08*Math.sin(4*pv); break;
                case XYLOPHONE:
                    value = 0.82*Math.sin(p)+0.24*Math.sin(3.02*p)+0.10*Math.sin(6.11*p); break;
                case CIMBALOM:
                    value = 0.62*Math.sin(p)+0.24*Math.sin(2.01*p)+0.14*Math.sin(3.97*p)+0.08*Math.sin(6.07*p); break;
                default:
                    value = 0.64*Math.sin(p)+0.30*Math.sin(2*p)+0.18*Math.sin(3*p)+0.10*Math.sin(4*p)+0.06*Math.sin(5*p); break;
            }
            phase += 2.0*Math.PI*frequency/sampleRate;
            if (phase > Math.PI*2.0) phase -= Math.PI*2.0;
            return value;
        }
    }
}
