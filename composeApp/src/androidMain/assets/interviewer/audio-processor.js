class AudioRecorderProcessor extends AudioWorkletProcessor {
    constructor() {
        super();
        this.bufferSize = 4096;
        this.buffer = new Float32Array(this.bufferSize);
        this.bufferIndex = 0;
    }

    process(inputs, outputs, parameters) {
        const input = inputs[0];
        if (input && input.length > 0) {
            const inputChannel = input[0];

            // Fill the buffer
            for (let i = 0; i < inputChannel.length; i++) {
                this.buffer[this.bufferIndex] = inputChannel[i];
                this.bufferIndex++;

                // When buffer is full, flush it
                if (this.bufferIndex >= this.bufferSize) {
                    this.flush();
                }
            }
        }
        return true; // Keep processor alive
    }

    flush() {
        // Clone buffer to send
        const bufferToSend = this.buffer.slice(0, this.bufferSize);

        // Post to main thread
        this.port.postMessage({
            eventType: 'audio_data',
            audioBuffer: bufferToSend
        });

        // Reset index (reuse buffer array)
        this.bufferIndex = 0;
    }
}

registerProcessor('audio-recorder-processor', AudioRecorderProcessor);
