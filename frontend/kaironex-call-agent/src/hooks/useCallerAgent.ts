/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useRef, useCallback, useEffect } from 'react';
import { GoogleGenAI, LiveServerMessage, Modality } from "@google/genai";
import { float32To16BitPCM, bytesToBase64, decodeAudioData, base64ToBytes } from '../utils/audioUtils';

interface CallerAgentConfig {
    agentType: string;      // STUDY, VITALITY, CAMPAIGN, RADIUS, SUPERVISOR
    agentName: string;      // "Study Agent", "Vitality Agent", etc.
    callReason: string;     // Why the agent is calling
    callContext: string;    // Additional context from the brain
    userProfile: string;    // JSON string of user profile
}

export const useCallerAgent = () => {
    const [isConnected, setIsConnected] = useState(false);
    const [isTalking, setIsTalking] = useState(false);
    const [callState, setCallState] = useState<'idle' | 'connecting' | 'ringing' | 'active' | 'ended'>('idle');
    const [error, setError] = useState<string | null>(null);
    const [transcript, setTranscript] = useState<string[]>([]);

    const audioContextRef = useRef<AudioContext | null>(null);
    const micContextRef = useRef<AudioContext | null>(null);
    const micStreamRef = useRef<MediaStream | null>(null);
    const outputNodeRef = useRef<GainNode | null>(null);
    const nextStartTimeRef = useRef<number>(0);
    const sourcesRef = useRef<Set<AudioBufferSourceNode>>(new Set());
    const sessionRef = useRef<any | null>(null);
    const isConnectedRef = useRef<boolean>(false);

    const buildSystemInstruction = (config: CallerAgentConfig): string => {
        const agentRoles: Record<string, string> = {
            'STUDY': `You are the Study Agent. You manage study schedules, track academic progress, and optimize learning efficiency. You know the student's courses, deadlines, and study patterns.`,
            'VITALITY': `You are the Vitality Agent. You monitor wellness, manage budgets, suggest breaks, and ensure the student stays healthy. You track sleep, meals, exercise, and stress levels.`,
            'CAMPAIGN': `You are the Campaign Agent. You handle long-term goals, GPA targets, career planning, and milestone tracking. You help the student achieve their dreams.`,
            'RADIUS': `You are the Radius Agent. You help international students with cultural navigation, visa matters, housing, and local resources. You understand the challenges of living abroad.`,
            'SUPERVISOR': `You are the Supervisor Agent. You coordinate all other agents and handle critical multi-domain situations. You have authority over the entire Kaironex system.`
        };

        return `
${agentRoles[config.agentType] || 'You are a Kaironex Agent.'}

YOUR NAME: ${config.agentName}

YOU ARE CALLING THE STUDENT BECAUSE: ${config.callReason}

ADDITIONAL CONTEXT FROM THE BRAIN:
${config.callContext}

STUDENT PROFILE:
${config.userProfile}

═══════════════════════════════════════════════════════════
CRITICAL BEHAVIOR - YOU MUST FOLLOW THESE RULES:
═══════════════════════════════════════════════════════════

1. YOU SPEAK FIRST
   - Start by greeting and introducing yourself briefly
   - Immediately explain WHY you're calling
   - Keep it to 2-3 sentences max

2. BE A HELPFUL FRIEND, NOT A BOT
   - Warm, supportive, conversational tone
   - Acknowledge their feelings if they seem stressed
   - Don't lecture - negotiate and suggest

3. GET TO THE POINT
   - State the issue clearly
   - Offer specific help or solutions
   - Ask if they need anything else

4. NEGOTIATE IF NEEDED
   - If they can't do something, offer alternatives
   - "Can't study now? How about in 30 minutes?"
   - Be flexible but remember the goal

5. REPORT BACK (INTERNAL)
   - At the end, you will summarize what happened
   - Note: user agreed/disagreed/rescheduled/needs follow-up

═══════════════════════════════════════════════════════════

START THE CALL NOW. Greet the student and explain why you're calling.
`.trim();
    };

    const startCall = useCallback(async (apiKey: string, config: CallerAgentConfig) => {
        try {
            setError(null);
            setCallState('connecting');
            setTranscript([]);
            console.log("📞 Caller Agent: Initiating call...");

            // Setup audio output
            const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
            audioContextRef.current = new AudioContextClass({ sampleRate: 24000 });
            outputNodeRef.current = audioContextRef.current.createGain();
            outputNodeRef.current.connect(audioContextRef.current.destination);

            // Get microphone
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

            setCallState('ringing');

            // Connect to Gemini Live
            const ai = new GoogleGenAI({ apiKey });
            const systemInstruction = buildSystemInstruction(config);

            console.log("📞 Caller Agent: System instruction built, connecting to Gemini...");

            const sessionPromise = ai.live.connect({
                model: 'gemini-2.5-flash-native-audio-preview-12-2025',
                config: {
                    responseModalities: [Modality.AUDIO],
                    systemInstruction: {
                        parts: [{ text: systemInstruction }]
                    }
                },
                callbacks: {
                    onopen: async () => {
                        console.log("📞 Caller Agent: Connected to Gemini Live");
                        setIsConnected(true);
                        isConnectedRef.current = true;
                        setCallState('active');

                        // Notify Android
                        if ((window as any).Android?.onAgentState) {
                            (window as any).Android.onAgentState(false, true);
                        }

                        // Setup microphone streaming
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
                                process(inputs) {
                                    const input = inputs[0];
                                    if (input && input.length > 0) {
                                        const inputChannel = input[0];
                                        for (let i = 0; i < inputChannel.length; i++) {
                                            this.buffer[this.bufferIndex] = inputChannel[i];
                                            this.bufferIndex++;
                                            if (this.bufferIndex >= this.bufferSize) {
                                                this.port.postMessage({ audioBuffer: this.buffer.slice(0) });
                                                this.bufferIndex = 0;
                                            }
                                        }
                                    }
                                    return true;
                                }
                            }
                            registerProcessor('mic-processor', AudioRecorderProcessor);
                        `;

                        const blob = new Blob([workletCode], { type: "application/javascript" });
                        await micContext.audioWorklet.addModule(URL.createObjectURL(blob));

                        const source = micContext.createMediaStreamSource(stream);
                        const workletNode = new AudioWorkletNode(micContext, 'mic-processor');

                        workletNode.port.onmessage = (event) => {
                            if (!isConnectedRef.current) return;
                            const pcm16 = float32To16BitPCM(event.data.audioBuffer);
                            const base64Data = bytesToBase64(new Uint8Array(pcm16.buffer));

                            sessionPromise.then((session: any) => {
                                if (isConnectedRef.current) {
                                    session.sendRealtimeInput({
                                        media: { mimeType: 'audio/pcm;rate=16000', data: base64Data }
                                    });
                                }
                            });
                        };

                        source.connect(workletNode);
                        workletNode.connect(micContext.destination);

                        // KICKSTART - Agent speaks first!
                        sessionPromise.then((session: any) => {
                            console.log("📞 Caller Agent: Sending kickstart...");
                            session.send({
                                parts: [{ text: "Start the call now. Greet and explain why you're calling." }],
                                turnComplete: true
                            });
                        });
                    },
                    onmessage: async (msg: LiveServerMessage) => {
                        // Handle text transcript
                        const textPart = msg.serverContent?.modelTurn?.parts?.find((p: any) => p.text);
                        if (textPart?.text) {
                            setTranscript(prev => [...prev, `Agent: ${textPart.text}`]);
                        }

                        // Handle audio
                        const audioData = msg.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
                        if (audioData && audioContextRef.current) {
                            setIsTalking(true);
                            if ((window as any).Android?.onAgentState) {
                                (window as any).Android.onAgentState(true, true);
                            }

                            const audioBytes = base64ToBytes(audioData);
                            const audioBuffer = await decodeAudioData(audioBytes, audioContextRef.current);

                            const currentTime = audioContextRef.current.currentTime;
                            if (nextStartTimeRef.current < currentTime) {
                                nextStartTimeRef.current = currentTime;
                            }

                            const source = audioContextRef.current.createBufferSource();
                            source.buffer = audioBuffer;
                            source.connect(outputNodeRef.current!);
                            source.start(nextStartTimeRef.current);
                            nextStartTimeRef.current += audioBuffer.duration;

                            sourcesRef.current.add(source);
                            source.onended = () => {
                                sourcesRef.current.delete(source);
                                if (sourcesRef.current.size === 0) {
                                    setIsTalking(false);
                                    if ((window as any).Android?.onAgentState) {
                                        (window as any).Android.onAgentState(false, true);
                                    }
                                }
                            };
                        }
                    },
                    onclose: () => {
                        console.log("📞 Caller Agent: Connection closed");
                        setIsConnected(false);
                        isConnectedRef.current = false;
                        setCallState('ended');
                        if ((window as any).Android?.onCallEnded) {
                            (window as any).Android.onCallEnded();
                        }
                    },
                    onerror: (err: any) => {
                        console.error("📞 Caller Agent Error:", err);
                        setError("Connection error");
                        setCallState('ended');
                    }
                }
            } as any);

            sessionRef.current = sessionPromise;

        } catch (e: any) {
            console.error("📞 Caller Agent: Failed to start", e);
            setError(e.message);
            setCallState('ended');
        }
    }, []);

    const endCall = useCallback(() => {
        console.log("📞 Caller Agent: Ending call");
        isConnectedRef.current = false;
        sourcesRef.current.forEach(s => s.stop());
        sourcesRef.current.clear();
        if (audioContextRef.current) audioContextRef.current.close();
        if (micContextRef.current) micContextRef.current.close();
        if (micStreamRef.current) micStreamRef.current.getTracks().forEach(t => t.stop());
        setIsConnected(false);
        setCallState('ended');
        if ((window as any).Android?.onCallEnded) {
            (window as any).Android.onCallEnded();
        }
    }, []);

    useEffect(() => {
        return () => endCall();
    }, [endCall]);

    return { startCall, endCall, isConnected, isTalking, callState, error, transcript };
};
