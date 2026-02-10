/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useRef, useEffect, useCallback } from 'react';
import { GoogleGenAI, LiveServerMessage, Modality } from "@google/genai";
import { float32To16BitPCM, bytesToBase64, decodeAudioData, base64ToBytes } from '../utils/audioUtils';

export const useGeminiLive = () => {
    const [isConnected, setIsConnected] = useState(false);
    const [isTalking, setIsTalking] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const audioContextRef = useRef<AudioContext | null>(null);
    const micContextRef = useRef<AudioContext | null>(null);
    const micStreamRef = useRef<MediaStream | null>(null);
    const outputNodeRef = useRef<GainNode | null>(null);
    const nextStartTimeRef = useRef<number>(0);
    const sourcesRef = useRef<Set<AudioBufferSourceNode>>(new Set());

    const sessionRef = useRef<Promise<any> | null>(null);
    const isConnectedRef = useRef<boolean>(false);

    const connect = useCallback(async (apiKey: string, systemInstruction?: string) => {
        try {
            setError(null);
            console.log("🚀 Starting connection...");

            const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
            audioContextRef.current = new AudioContextClass({ sampleRate: 24000 });
            outputNodeRef.current = audioContextRef.current.createGain();
            outputNodeRef.current.connect(audioContextRef.current.destination);

            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    sampleRate: 16000,
                    channelCount: 1,
                    echoCancellation: true,
                    autoGainControl: true,
                    noiseSuppression: true
                }
            });
            micStreamRef.current = stream;

            const ai = new GoogleGenAI({ apiKey });
            // Check for agent call system instruction from window (injected by index.html)
            const windowInstruction = (window as any).AGENT_SYSTEM_INSTRUCTION;
            const instruction = windowInstruction || systemInstruction || "You are Kaironex. Keep responses concise and conversational.";
            console.log("🎙️ Using system instruction:", instruction.substring(0, 100) + "...");

            const sessionPromise = ai.live.connect({
                model: 'gemini-2.5-flash-native-audio-preview-12-2025',
                config: {
                    responseModalities: [Modality.AUDIO],
                    systemInstruction: {
                        parts: [{ text: instruction }]
                    }
                },
                callbacks: {
                    onopen: async () => {
                        console.log("Connected to Kairo");
                        setIsConnected(true);
                        isConnectedRef.current = true;

                        if (!audioContextRef.current) return;
                        const micContext = new AudioContext({ sampleRate: 16000 });
                        micContextRef.current = micContext;

                        const workletCode = `
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
                        for (let i = 0; i < inputChannel.length; i++) {
                            this.buffer[this.bufferIndex] = inputChannel[i];
                            this.bufferIndex++;
                            if (this.bufferIndex >= this.bufferSize) {
                                this.flush();
                            }
                        }
                    }
                    return true;
                }

                flush() {
                    const bufferToSend = this.buffer.slice(0, this.bufferSize);
                    this.port.postMessage({
                        eventType: 'audio_data',
                        audioBuffer: bufferToSend
                    });
                    this.bufferIndex = 0;
                }
            }
            registerProcessor('audio-recorder-processor', AudioRecorderProcessor);
            `;

                        const blob = new Blob([workletCode], { type: "application/javascript" });
                        const workletUrl = URL.createObjectURL(blob);

                        try {
                            await micContext.audioWorklet.addModule(workletUrl);
                        } catch (err) {
                            console.error(err);
                            return;
                        }

                        const source = micContext.createMediaStreamSource(stream);
                        const workletNode = new AudioWorkletNode(micContext, 'audio-recorder-processor');

                        workletNode.port.onmessage = (event) => {
                            if (!isConnectedRef.current) return;
                            if (event.data.eventType === 'audio_data') {
                                const float32Array = event.data.audioBuffer;
                                const pcm16 = float32To16BitPCM(float32Array);
                                const base64Data = bytesToBase64(new Uint8Array(pcm16.buffer));

                                sessionPromise.then((session: any) => {
                                    if (isConnectedRef.current) {
                                        session.sendRealtimeInput({
                                            media: {
                                                mimeType: 'audio/pcm;rate=16000',
                                                data: base64Data
                                            }
                                        });
                                    }
                                });
                            }
                        };

                        source.connect(workletNode);
                        workletNode.connect(micContext.destination);

                        // Kickstart
                        sessionPromise.then((session: any) => {
                            try {
                                if (typeof session.send === 'function') {
                                    session.send({ parts: [{ text: "Hello!" }], turnComplete: true });
                                } else {
                                    console.warn("Session.send is not a function", session);
                                }
                            } catch (e) {
                                console.error("Kickstart failed", e);
                            }
                        });
                    },
                    onmessage: async (msg: LiveServerMessage) => {
                        const audioData = msg.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
                        if (audioData) {
                            if (!audioContextRef.current) return;
                            setIsTalking(true);

                            const audioBytes = base64ToBytes(audioData);
                            const audioBuffer = await decodeAudioData(audioBytes, audioContextRef.current);

                            const currentTime = audioContextRef.current.currentTime;
                            if (nextStartTimeRef.current < currentTime) {
                                nextStartTimeRef.current = currentTime;
                            }

                            const source = audioContextRef.current.createBufferSource();
                            source.buffer = audioBuffer;
                            if (outputNodeRef.current) {
                                source.connect(outputNodeRef.current);
                            }

                            source.start(nextStartTimeRef.current);
                            nextStartTimeRef.current += audioBuffer.duration;

                            sourcesRef.current.add(source);
                            source.onended = () => {
                                sourcesRef.current.delete(source);
                                if (sourcesRef.current.size === 0) {
                                    setIsTalking(false);
                                }
                            };
                        }
                    },
                    onclose: (event: any) => {
                        console.log("Closed", event);
                        setIsConnected(false);
                        isConnectedRef.current = false;
                        setIsTalking(false);
                    },
                    onerror: (err: any) => {
                        console.error("Gemini Error:", err);
                        setError("Connection Error");
                        setIsConnected(false);
                        isConnectedRef.current = false;
                    }
                }
            } as any); // Cast options to any to support callbacks

            sessionRef.current = sessionPromise;

        } catch (e: any) {
            console.error(e);
            setError(e.message);
        }
    }, []);

    const disconnect = useCallback(() => {
        isConnectedRef.current = false;
        if (audioContextRef.current) audioContextRef.current.close();
        if (micContextRef.current) micContextRef.current.close();
        setIsConnected(false);
    }, []);

    useEffect(() => {
        return () => disconnect();
    }, [disconnect]);

    return { connect, disconnect, isConnected, isTalking, error };
};
