#!/usr/bin/env python3
import struct, math, wave, subprocess, os

SAMPLE_RATE = 48000
AMPLITUDE = 0.95
SOUNDS_DIR = "src/main/resources/assets/thrusted/sounds"

os.makedirs(SOUNDS_DIR, exist_ok=True)

def write_wav(path, samples):
    with wave.open(path + ".wav", "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        data = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 32767)))) for s in samples)
        w.writeframes(data)

def to_ogg(wav_path):
    ogg_path = wav_path.replace(".wav", ".ogg")
    subprocess.run(["ffmpeg", "-y", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5", ogg_path],
                   capture_output=True, check=True)
    os.remove(wav_path)

def env_adsr(t, attack=0.01, decay=0.05, sustain=0.6, release=0.3, dur=1.0, gate=0.0):
    gate = max(gate, 0.01)
    if t < attack:
        return t / attack
    elif t < attack + decay:
        return 1.0 - (1.0 - sustain) * (t - attack) / decay
    elif t < gate - release:
        return sustain
    elif t < gate:
        return sustain * (gate - t) / release
    return 0.0

# Shield bootup: rising hum with sparkle
def gen_bootup():
    dur, gate = 1.5, 1.4
    sr = SAMPLE_RATE
    n = int(sr * dur)
    samples = []
    for i in range(n):
        t = i / sr
        env = env_adsr(t, attack=0.005, decay=0.1, sustain=0.7, release=0.15, dur=dur, gate=gate)
        # Rising pitch sweep 100->800Hz
        freq = 100 + (t / gate) * 700
        s = math.sin(2 * math.pi * freq * t) * 0.35
        s += math.sin(2 * math.pi * freq * 2.0 * t) * 0.15
        s += math.sin(2 * math.pi * 2000 * t) * 0.08 * min(1, (t / gate) * 4)
        # Sparkle at start
        if t < 0.1:
            s += math.sin(2 * math.pi * 4000 * t) * 0.1 * (1 - t / 0.1)
        samples.append(s * env * AMPLITUDE * 1.0)
    return samples

# Shield shutdown: descending whine
def gen_shutdown():
    dur, gate = 1.8, 1.7
    sr = SAMPLE_RATE
    n = int(sr * dur)
    samples = []
    for i in range(n):
        t = i / sr
        env = env_adsr(t, attack=0.01, decay=0.05, sustain=0.8, release=0.3, dur=dur, gate=gate)
        # Descending pitch 600->60Hz
        progress = t / gate
        freq = 600 - progress * 540
        s = math.sin(2 * math.pi * freq * t) * 0.30
        s += math.sin(2 * math.pi * freq * 1.5 * t) * 0.12
        s += math.sin(2 * math.pi * 120 * t) * 0.10
        # Fade out at end
        if progress > 0.7:
            s *= (1 - (progress - 0.7) / 0.3)
        samples.append(s * env * AMPLITUDE * 0.7)
    return samples

# Shield ambient: deep drone with pulse
def gen_ambient():
    dur, gate = 4.0, 3.9
    sr = SAMPLE_RATE
    n = int(sr * dur)
    samples = []
    for i in range(n):
        t = i / sr
        env = env_adsr(t, attack=0.1, decay=0.05, sustain=0.5, release=0.2, dur=dur, gate=gate)
        # Deep 60Hz drone with harmonics
        s = math.sin(2 * math.pi * 60 * t) * 0.35
        s += math.sin(2 * math.pi * 120 * t) * 0.18
        s += math.sin(2 * math.pi * 180 * t) * 0.10
        # Slow pulse
        pulse = 0.5 + 0.5 * math.sin(2 * math.pi * 0.5 * t)
        s *= (0.6 + 0.4 * pulse)
        samples.append(s * env * AMPLITUDE * 0.9)
    return samples

# Shield hit: resonant impact
def gen_hit():
    dur, gate = 0.6, 0.55
    sr = SAMPLE_RATE
    n = int(sr * dur)
    samples = []
    for i in range(n):
        t = i / sr
        env = env_adsr(t, attack=0.001, decay=0.02, sustain=0.3, release=0.15, dur=dur, gate=gate)
        # Noise burst
        noise = (hash((i,)) % 65535) / 32768.0 - 1.0
        s = noise * 0.25
        # Resonant ring
        s += math.sin(2 * math.pi * 2500 * t) * 0.20 * math.exp(-t * 12)
        s += math.sin(2 * math.pi * 1200 * t) * 0.12 * math.exp(-t * 8)
        s += math.sin(2 * math.pi * 400 * t) * 0.08 * math.exp(-t * 6)
        samples.append(s * env * AMPLITUDE * 1.0)
    return samples

def main():
    for name, gen_fn in [("shield_bootup", gen_bootup), ("shield_shutdown", gen_shutdown),
                          ("shield_ambient", gen_ambient), ("shield_hit", gen_hit)]:
        path = os.path.join(SOUNDS_DIR, name)
        print(f"Generating {name}...")
        samples = gen_fn()
        write_wav(path, samples)
        to_ogg(path + ".wav")
        size = os.path.getsize(path + ".ogg")
        print(f"  {name}.ogg: {size} bytes")

if __name__ == "__main__":
    main()
