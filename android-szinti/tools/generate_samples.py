import math, wave, struct, os, random

RATE = 44100
OUT = 'app/src/main/res/raw'
os.makedirs(OUT, exist_ok=True)
random.seed(7)

INSTRUMENTS = ['piano','bass','violin','xylophone','cimbalom','synth']
ROOTS = [('c4',60),('c5',72)]

def hz(midi):
    return 440.0 * (2.0 ** ((midi - 69) / 12.0))

def env_exp(t, attack, decay, floor=0.0):
    if t < attack:
        return t / max(attack, 1e-6)
    return floor + (1.0-floor) * math.exp(-(t-attack)/decay)

def sat(x):
    return math.tanh(x)

def sample(inst, midi):
    f = hz(midi)
    if inst == 'xylophone': dur = 1.25
    elif inst == 'cimbalom': dur = 1.8
    elif inst == 'piano': dur = 2.25
    else: dur = 2.8
    n = int(RATE * dur)
    data = []
    for i in range(n):
        t = i / RATE
        x = 0.0
        if inst == 'piano':
            e = env_exp(t, 0.006, 0.58)
            x = e*(0.72*math.sin(2*math.pi*f*t) + 0.23*math.sin(2*math.pi*f*2.01*t) + 0.10*math.sin(2*math.pi*f*3.97*t) + 0.05*math.sin(2*math.pi*f*6.02*t))
            x += math.exp(-t/0.025) * 0.10 * (random.random()*2-1)
        elif inst == 'bass':
            e = env_exp(t, 0.012, 1.25, 0.10)
            x = e*(0.78*math.sin(2*math.pi*f*t) + 0.22*math.sin(2*math.pi*f*2*t) + 0.09*math.sin(2*math.pi*f*3*t))
            x += math.exp(-t/0.04)*0.08*(random.random()*2-1)
        elif inst == 'violin':
            attack = min(1.0, t/0.10)
            release = max(0.0, min(1.0, (dur-t)/0.35))
            e = attack*release*0.78
            vib = 1.0 + 0.0035*math.sin(2*math.pi*5.4*t)
            for k, a in [(1,.50),(2,.23),(3,.15),(4,.10),(5,.07),(6,.05),(7,.035)]:
                x += a*math.sin(2*math.pi*f*k*vib*t)
            x = e*sat(x*1.1) + e*0.018*(random.random()*2-1)
        elif inst == 'xylophone':
            e = math.exp(-t/0.34)
            x = e*(0.86*math.sin(2*math.pi*f*t) + 0.28*math.sin(2*math.pi*f*3.04*t) + 0.12*math.sin(2*math.pi*f*6.15*t))
            x += math.exp(-t/0.012)*0.10*(random.random()*2-1)
        elif inst == 'cimbalom':
            e = math.exp(-t/0.72)
            x = e*(0.60*math.sin(2*math.pi*f*t) + 0.28*math.sin(2*math.pi*f*2.006*t) + 0.18*math.sin(2*math.pi*f*3.91*t) + 0.10*math.sin(2*math.pi*f*6.08*t) + 0.07*math.sin(2*math.pi*f*8.9*t))
            x += math.exp(-t/0.018)*0.12*(random.random()*2-1)
        else:
            attack = min(1.0, t/0.018)
            release = max(0.0, min(1.0, (dur-t)/0.26))
            e = attack*release*0.62
            for k in range(1,9):
                x += (1.0/k)*math.sin(2*math.pi*f*k*t)
            x = e*sat(x*0.8)
        data.append(x)
    peak = max(1e-9, max(abs(v) for v in data))
    scale = 0.48 / peak
    return [max(-1.0, min(1.0, v*scale)) for v in data]

def write_wav(path, vals):
    with wave.open(path, 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(RATE)
        frames = bytearray()
        for v in vals:
            frames += struct.pack('<h', int(v*32767))
        wf.writeframes(frames)

for inst in INSTRUMENTS:
    for label, midi in ROOTS:
        print('generating', inst, label)
        write_wav(os.path.join(OUT, f'{inst}_{label}.wav'), sample(inst, midi))
