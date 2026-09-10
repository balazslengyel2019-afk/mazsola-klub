import math, wave, struct, os, random

RATE = 44100
OUT = 'app/src/main/res/raw'
os.makedirs(OUT, exist_ok=True)
random.seed(23)

def hz(midi):
    return 440.0 * (2.0 ** ((midi - 69) / 12.0))

def write_wav(path, vals):
    with wave.open(path, 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(RATE)
        frames = bytearray()
        for v in vals:
            frames += struct.pack('<h', int(max(-1.0, min(1.0, v)) * 32767))
        wf.writeframes(frames)

def tongue_drum(midi):
    f = hz(midi)
    dur = 3.3
    n = int(RATE * dur)
    data = []
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.004)
        body = attack * math.exp(-t / 1.28)
        shimmer = attack * math.exp(-t / 0.62)
        x = body * (
            0.76 * math.sin(2 * math.pi * f * t) +
            0.20 * math.sin(2 * math.pi * f * 2.01 * t + 0.2) +
            0.12 * math.sin(2 * math.pi * f * 2.96 * t + 0.6)
        )
        x += shimmer * (
            0.075 * math.sin(2 * math.pi * f * 4.17 * t) +
            0.045 * math.sin(2 * math.pi * f * 5.73 * t)
        )
        x += math.exp(-t / 0.018) * 0.055 * (random.random() * 2 - 1)
        data.append(x)
    peak = max(1e-9, max(abs(v) for v in data))
    return [v * (0.47 / peak) for v in data]

def metronome_click():
    dur = 0.075
    n = int(RATE * dur)
    data = []
    for i in range(n):
        t = i / RATE
        env = math.exp(-t / 0.0105)
        wood = 0.56 * math.sin(2 * math.pi * 1760 * t) + 0.28 * math.sin(2 * math.pi * 910 * t)
        snap = 0.32 * (random.random() * 2 - 1)
        x = env * (wood + snap)
        data.append(x)
    peak = max(1e-9, max(abs(v) for v in data))
    return [v * (0.62 / peak) for v in data]

for label, midi in [('c4', 60), ('c5', 72)]:
    print('generating tongue drum', label)
    write_wav(os.path.join(OUT, f'tonguedrum_{label}.wav'), tongue_drum(midi))

print('generating mechanical metronome click')
write_wav(os.path.join(OUT, 'metronome_click.wav'), metronome_click())
