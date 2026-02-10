
export const float32To16BitPCM = (float32Array: Float32Array): Int16Array => {
    const buffer = new Int16Array(float32Array.length);
    for (let i = 0; i < float32Array.length; i++) {
        const s = Math.max(-1, Math.min(1, float32Array[i]));
        buffer[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }
    return buffer;
};

export const bytesToBase64 = (bytes: Uint8Array): string => {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
};

export const base64ToBytes = (base64: string): Uint8Array => {
    const binaryString = atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
};

export const pcm16ToAudioBuffer = (audioBytes: Uint8Array, context: AudioContext, sampleRate: number = 24000): AudioBuffer => {
    // 1. Convert Uint8Array (bytes) to Int16Array
    // Ensure we are aligned to 16-bit boundaries
    if (audioBytes.byteLength % 2 !== 0) {
        console.warn("Audio bytes length is not multiple of 2, truncating.");
        audioBytes = audioBytes.subarray(0, audioBytes.byteLength - (audioBytes.byteLength % 2));
    }
    const int16 = new Int16Array(audioBytes.buffer);

    // 2. Create Float32Array for Web Audio API
    const float32 = new Float32Array(int16.length);
    for (let i = 0; i < int16.length; i++) {
        // Normalize Int16 (-32768 to 32767) to Float32 (-1.0 to 1.0)
        float32[i] = int16[i] / 32768.0;
    }

    // 3. Create AudioBuffer
    const buffer = context.createBuffer(1, float32.length, sampleRate);
    buffer.copyToChannel(float32, 0);

    return buffer;
};
