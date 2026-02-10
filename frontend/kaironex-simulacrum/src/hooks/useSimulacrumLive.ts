
import { useState, useRef, useEffect, useCallback } from 'react';
import { GoogleGenAI, Type, Modality } from "@google/genai";
import type { LiveServerMessage, FunctionDeclaration } from "@google/genai";
import type { SimulationContext } from '../types';
import { float32To16BitPCM, bytesToBase64, pcm16ToAudioBuffer, base64ToBytes } from '../utils/audioUtils';

interface UseSimulacrumLiveProps {
    context: SimulationContext;
    onEnd: () => void;
    videoRef: React.RefObject<HTMLVideoElement | null>; // To capture frames
}

export const useSimulacrumLive = ({ context, onEnd, videoRef }: UseSimulacrumLiveProps) => {
    const [isConnected, setIsConnected] = useState(false);
    const [isTalking, setIsTalking] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Audio Refs
    const audioContextRef = useRef<AudioContext | null>(null);
    const micContextRef = useRef<AudioContext | null>(null);
    const micStreamRef = useRef<MediaStream | null>(null);
    const nextStartTimeRef = useRef<number>(0);
    const sourcesRef = useRef<Set<AudioBufferSourceNode>>(new Set());

    // Video Loop Ref
    const videoIntervalRef = useRef<number | null>(null);

    // Session Refs
    const sessionPromiseRef = useRef<Promise<any> | null>(null);
    const isConnectedRef = useRef<boolean>(false);
    const isEndingRef = useRef<boolean>(false);

    // Tools
    const endInterviewTool: FunctionDeclaration = {
        name: 'endInterview',
        description: 'Call this when the interview is concluded.',
        parameters: { type: Type.OBJECT, properties: {} }
    };

    const connect = useCallback(async () => {
        try {
            setError(null);
            console.log("🚀 Starting Simulacrum session...");

            const apiKey = (window as any).ANDROID_API_KEY || new URLSearchParams(window.location.search).get('apiKey') || import.meta.env.VITE_API_KEY;

            if (!apiKey) throw new Error("API Key missing.");

            const ai = new GoogleGenAI({ apiKey });

            // Audio Context Setup
            const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
            audioContextRef.current = new AudioContextClass({ sampleRate: 24000 });

            // Connect Mic
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    sampleRate: 16000,
                    channelCount: 1,
                    echoCancellation: true,
                    autoGainControl: true,
                    noiseSuppression: true
                },
                video: {
                    width: { ideal: 640 },
                    height: { ideal: 480 },
                    frameRate: { ideal: 15 } // Lower fps for bandwidth
                }
            });
            micStreamRef.current = stream;

            // Preview Video
            if (videoRef.current) {
                videoRef.current.srcObject = stream;
            }

            // Construct System Instruction based on Persona
            const instruction = `
            You are a ${context.persona} interviewing ${context.userName} for the role of ${context.jobTitle} at ${context.jobCompany}.
            
            JOB DESCRIPTION:
            ${context.jobDescription}

            YOUR PERSONA:
            ${context.persona === 'Recruiter' ? "Friendly but professional. Focus on culture fit, basic skills, and enthusiasm." :
                    context.persona === 'HiringManager' ? "Serious and result-oriented. Focus on past projects, problem solving, and impact." :
                        context.persona === 'CTO' ? "Technical and high-level. Focus on architecture, scalability, and system design." :
                            "Casual and collaborative. Focus on team fit and day-to-day work."}

            DIFFICULTY: ${context.difficulty} (Adjust your probing depth accordingly).
            
            INSTRUCTIONS:
            1. Start by introducing yourself and the role.
            2. Ask one question at a time.
            3. Listen to the user's answer (audio and video). Use visual cues found in the video stream if relevant (e.g. "I see you are enthusiastic").
            4. Dig deeper if the answer is vague.
            5. When satisfied or if time is up, thank the user and call 'endInterview'.
            `;

            // Live Connect
            const sessionPromise = ai.live.connect({
                model: 'gemini-2.5-flash-native-audio-preview-12-2025',
                config: {
                    responseModalities: [Modality.AUDIO], // We only want Audio back, not text/video
                    systemInstruction: instruction,
                    tools: [{ functionDeclarations: [endInterviewTool] }],
                },
                callbacks: {
                    onopen: async () => {
                        console.log("✅ Simulacrum Connected");
                        setIsConnected(true);
                        isConnectedRef.current = true;

                        // Start Audio Streaming (Worklet)
                        setupAudioStreaming(stream, sessionPromise);

                        // Start Video Streaming
                        startVideoStreaming(sessionPromise);
                    },
                    onmessage: async (msg: LiveServerMessage) => {
                        // Handle Audio Output
                        const audioData = msg.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
                        if (audioData) {
                            playAudio(audioData);
                        }

                        // Handle Tools
                        if (msg.toolCall?.functionCalls) {
                            for (const fc of msg.toolCall.functionCalls) {
                                if (fc.name === 'endInterview') {
                                    console.log("🛑 Agent requested end of interview. Waiting for audio...");
                                    isEndingRef.current = true;

                                    // If we are not currently playing audio, end immediately
                                    // But wait a tick just in case audio packet is processing
                                    setTimeout(() => {
                                        if (sourcesRef.current.size === 0) {
                                            onEnd();
                                        }
                                    }, 500);

                                    // Send empty response to acknowledge
                                    sessionPromise.then((s: any) => s.sendToolResponse({ functionResponses: [{ id: fc.id, name: fc.name, response: { result: 'ok' } }] }));
                                }
                            }
                        }
                    },
                    onclose: () => {
                        console.log("❌ Connection Closed");
                        setIsConnected(false);
                        isConnectedRef.current = false;
                    },
                    onerror: (err: any) => {
                        console.error("Gemini Error:", err);
                        setError("Connection Error");
                    }
                }
            });

            sessionPromiseRef.current = sessionPromise;

        } catch (e: any) {
            console.error(e);
            setError(e.message || "Failed to connect");
        }
    }, [context]);

    // Helper: Audio Streaming
    const setupAudioStreaming = async (stream: MediaStream, sessionPromise: Promise<any>) => {
        if (!audioContextRef.current) return;

        const micContext = new AudioContext({ sampleRate: 16000 });
        micContextRef.current = micContext;

        await micContext.audioWorklet.addModule("data:text/javascript;base64," + btoa(`
            class AudioProcessor extends AudioWorkletProcessor {
                constructor() { super(); this.buffer = new Float32Array(2048); this.idx = 0; }
                process(inputs) {
                    const input = inputs[0];
                    if (input && input.length > 0) {
                        const channel = input[0];
                        for (let i=0; i<channel.length; i++) {
                            this.buffer[this.idx++] = channel[i];
                            if (this.idx >= 2048) {
                                this.port.postMessage(this.buffer.slice(0, 2048));
                                this.idx = 0;
                            }
                        }
                    }
                    return true;
                }
            }
            registerProcessor('audio-processor', AudioProcessor);
        `));

        const source = micContext.createMediaStreamSource(stream);
        const processor = new AudioWorkletNode(micContext, 'audio-processor');

        processor.port.onmessage = (e) => {
            if (!isConnectedRef.current) return;
            const pcm16 = float32To16BitPCM(e.data);
            const b64 = bytesToBase64(new Uint8Array(pcm16.buffer));

            sessionPromise.then(s => s.sendRealtimeInput({
                media: { mimeType: 'audio/pcm;rate=16000', data: b64 }
            }));
        };

        source.connect(processor);
        processor.connect(micContext.destination);
    };

    // Helper: Video Streaming
    const startVideoStreaming = (sessionPromise: Promise<any>) => {
        if (!videoRef.current) return;

        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        const FPS = 1; // Send 1 frame per second to save bandwidth but give visual context

        videoIntervalRef.current = window.setInterval(async () => {
            if (!isConnectedRef.current || !videoRef.current) return;

            canvas.width = videoRef.current.videoWidth * 0.5; // Scale down
            canvas.height = videoRef.current.videoHeight * 0.5;
            ctx?.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);

            const base64 = canvas.toDataURL('image/jpeg', 0.6).split(',')[1];

            sessionPromise.then(s => s.sendRealtimeInput({
                media: { mimeType: 'image/jpeg', data: base64 }
            })).catch(e => console.warn("Video send failed", e));

        }, 1000 / FPS);
    };

    // Helper: Play Audio
    const playAudio = async (base64Audio: string) => {
        if (!audioContextRef.current) return;
        setIsTalking(true);

        const audioBytes = base64ToBytes(base64Audio);
        const buffer = pcm16ToAudioBuffer(audioBytes, audioContextRef.current, 24000);

        const source = audioContextRef.current.createBufferSource();
        source.buffer = buffer;
        source.connect(audioContextRef.current.destination);

        const now = audioContextRef.current.currentTime;
        const start = Math.max(now, nextStartTimeRef.current);
        source.start(start);
        nextStartTimeRef.current = start + buffer.duration;

        sourcesRef.current.add(source);
        source.onended = () => {
            sourcesRef.current.delete(source);
            if (sourcesRef.current.size === 0) {
                setIsTalking(false);
                // If the agent requested to end, and we just finished the last sentence:
                if (isEndingRef.current) {
                    console.log("🛑 Audio finished. Ending session now.");
                    // Verify empty again after small delay (sometimes packets come in chunks)
                    setTimeout(() => {
                        if (sourcesRef.current.size === 0) onEnd();
                    }, 500);
                }
            }
        };
    };

    const disconnect = useCallback(() => {
        isConnectedRef.current = false;
        if (videoIntervalRef.current) clearInterval(videoIntervalRef.current);
        if (sessionPromiseRef.current) {
            // connection closing handled implicitly
        }

        micStreamRef.current?.getTracks().forEach(t => t.stop());
        micContextRef.current?.close();
        audioContextRef.current?.close();

        setIsConnected(false);
    }, []);

    useEffect(() => { return () => disconnect(); }, [disconnect]);

    return { connect, disconnect, isConnected, isTalking, error };
};
