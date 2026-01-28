import { useState, useRef, useEffect, useCallback } from 'react';
import { GoogleGenAI, LiveServerMessage, Modality, FunctionDeclaration, Type } from "@google/genai";
import { UserProfile, FIELD_ORDER } from '../types';
import { float32To16BitPCM, bytesToBase64, decodeAudioData, base64ToBytes } from '../utils/audioUtils';

interface UseGeminiLiveProps {
  onProfileUpdate: (field: string, value: any) => void;
  onInterviewComplete: () => void;
}

export const useGeminiLive = ({ onProfileUpdate, onInterviewComplete }: UseGeminiLiveProps) => {
  const [isConnected, setIsConnected] = useState(false);
  const [isTalking, setIsTalking] = useState(false); // Model is outputting audio
  const [error, setError] = useState<string | null>(null);

  // Audio Refs
  const audioContextRef = useRef<AudioContext | null>(null);
  const micContextRef = useRef<AudioContext | null>(null);
  const micStreamRef = useRef<MediaStream | null>(null);
  const inputSourceRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const processorRef = useRef<ScriptProcessorNode | null>(null);
  const outputNodeRef = useRef<GainNode | null>(null);
  const nextStartTimeRef = useRef<number>(0);
  const sourcesRef = useRef<Set<AudioBufferSourceNode>>(new Set());

  // Session Refs
  // Using 'any' for session type as LiveSession is not explicitly exported in this version
  const sessionRef = useRef<Promise<any> | null>(null);
  const sessionInstanceRef = useRef<any | null>(null); // Keep track of the actual session object
  const isConnectedRef = useRef<boolean>(false);

  // Function Declaration for Saving Fields
  const saveFieldTool: FunctionDeclaration = {
    name: 'saveField',
    description: 'Saves a validated answer for a specific user profile field. Call this immediately after the user provides an answer.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        field: {
          type: Type.STRING,
          enum: FIELD_ORDER,
          description: 'The specific field being saved.'
        },
        value: {
          type: Type.STRING,
          description: 'The extracted and validated value from the user answer.'
        }
      },
      required: ['field', 'value']
    }
  };

  const endInterviewTool: FunctionDeclaration = {
    name: 'endInterview',
    description: 'Call this when all questions have been asked and the interview is complete.',
    parameters: {
      type: Type.OBJECT,
      properties: {},
    }
  };

};

// Android Interface Definition
interface AndroidInterface {
  onAgentState: (isTalking: boolean, isConnected: boolean) => void;
  onProfileUpdate: (field: string, value: string) => void;
  onComplete: () => void;
}

const connect = useCallback(async () => {
  try {
    setError(null);
    // Try injecting from Android WebView first, then fallback to build-time env
    const apiKey = (window as any).ANDROID_API_KEY || process.env.API_KEY;
    if (!apiKey) {
      throw new Error("API Key not found in environment.");
    }

    const ai = new GoogleGenAI({ apiKey });

    // Setup Audio Contexts
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
    audioContextRef.current = new AudioContextClass({ sampleRate: 24000 });
    outputNodeRef.current = audioContextRef.current.createGain();
    outputNodeRef.current.connect(audioContextRef.current.destination);

    // Input Audio (Mic)
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

    // --- Live API Connection ---
    // Notify Android
    if ((window as any).Android) {
      (window as any).Android.onAgentState(false, true); // Connected, Listening
    }

    const sessionPromise = ai.live.connect({
      model: 'gemini-2.5-flash-native-audio-preview-12-2025',
      config: {
        responseModalities: [Modality.AUDIO],
        systemInstruction: `
            You are Kairo, a professional, warm, and efficient AI interviewer for 'Kaironex'.
            Your goal is to calibrate a user's profile by asking specific questions one by one.
            
            **PROTOCOL:**
            1. Introduce yourself briefly ("Hello, I am Kairo. Welcome to Kaironex. I am here to calibrate your profile. Are you ready?").
            2. Wait for user confirmation.
            3. Ask the questions in this EXACT order:
               - University Name?
               - Major/Field of study?
               - Current Semester (e.g., Fall 2024, 5th Semester)?
               - Current CGPA?
               - Are you an International Student? (Yes/No)
               - Do you currently have a job? (Yes/No)
               - Do you want help finding a job? (Yes/No)
               - Describe your ideal job?
               - What is your commute duration?
               - Do you prefer High or Low Energy environments?
               - What is your daily focus capacity (in hours)?
               - What are your non-negotiables?
               - What is your learning style?
               - How do you respond to stress?
               - What is a common cause of failure for you?
            
            **RULES:**
            - ASK ONE QUESTION AT A TIME.
            - When the user answers, validate the answer. 
            - IMMEDIATELY call the 'saveField' tool with the corresponding field name (university, major, etc.) and the value.
            - WAIT for the tool execution to complete.
            - THEN acknowledge briefly and ask the next question.
            - If the user is unclear, ask for clarification before saving.
            - At the end, call 'endInterview'.
          `,
        tools: [{ functionDeclarations: [saveFieldTool, endInterviewTool] }],
      },
      callbacks: {
        onopen: async () => {
          console.log("Connected to Kairo");
          setIsConnected(true);
          isConnectedRef.current = true;

          // Setup Mic Streaming
          if (!audioContextRef.current) return;

          // We need a separate input context for 16kHz usually, but resampling handles it or we create one.
          // Simplified for this demo: use the stream directly with ScriptProcessor
          const micContext = new AudioContextClass({ sampleRate: 16000 });
          micContextRef.current = micContext;

          const source = micContext.createMediaStreamSource(stream);
          // Buffer size 4096, 1 input, 1 output
          const processor = micContext.createScriptProcessor(4096, 1, 1);

          processor.onaudioprocess = (e) => {
            if (!isConnectedRef.current) return;

            const inputData = e.inputBuffer.getChannelData(0);
            // Convert Float32 to Int16
            const pcm16 = float32To16BitPCM(inputData);
            const base64Data = bytesToBase64(new Uint8Array(pcm16.buffer));

            sessionPromise.then(session => {
              if (isConnectedRef.current) {
                session.sendRealtimeInput({
                  media: {
                    mimeType: 'audio/pcm;rate=16000',
                    data: base64Data
                  }
                });
              }
            });
          };

          source.connect(processor);
          processor.connect(micContext.destination);

          inputSourceRef.current = source;
          processorRef.current = processor;

          // Trigger the intro
          /* 
          sessionPromise.then(session => {
              session.sendRealtimeInput({
                  media: {
                      mimeType: 'text/plain',
                      data: btoa('Start the interview now.')
                  }
              });
          }); 
          */
        },
        onmessage: async (msg: LiveServerMessage) => {
          // Handle Tool Calls (Saving Data)
          if (msg.toolCall) {
            const functionResponses = [];
            for (const fc of msg.toolCall.functionCalls) {
              if (fc.name === 'saveField') {
                const { field, value } = fc.args as any;
                console.log(`Saving ${field}: ${value}`);
                onProfileUpdate(field, value);
                functionResponses.push({
                  id: fc.id,
                  name: fc.name,
                  response: { result: "Field saved successfully." }
                });
              } else if (fc.name === 'endInterview') {
                onInterviewComplete();
                functionResponses.push({
                  id: fc.id,
                  name: fc.name,
                  response: { result: "Interview ended." }
                });
              }
            }

            if (functionResponses.length > 0) {
              sessionPromise.then(session => {
                session.sendToolResponse({ functionResponses });
              });
            }
          }

          // Handle Audio Output from Model
          const audioData = msg.serverContent?.modelTurn?.parts?.[0]?.inlineData?.data;
          if (audioData) {
            if (!audioContextRef.current) return;
            setIsTalking(true);
            if ((window as any).Android) (window as any).Android.onAgentState(true, true);

            const audioBytes = base64ToBytes(audioData);
            const audioBuffer = await decodeAudioData(audioBytes, audioContextRef.current);

            // Scheduling
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
                if ((window as any).Android) (window as any).Android.onAgentState(false, true);
              }
            };
          }
        },
        onclose: (event: any) => {
          console.log("Connection closed", event.code, event.reason);
          setIsConnected(false);
          isConnectedRef.current = false;
          setIsTalking(false);
          if ((window as any).Android) (window as any).Android.onAgentState(false, false);
        },
        onerror: (err) => {
          console.error("Gemini Live Error:", err);
          setError("Connection error. Please refresh.");
          setIsConnected(false);
          isConnectedRef.current = false;
        }
      }
    });

    sessionRef.current = sessionPromise;
    sessionPromise.then(s => {
      sessionInstanceRef.current = s;
    });

  } catch (e: any) {
    console.error(e);
    setError(e.message || "Failed to initialize audio.");
  }
}, [onProfileUpdate, onInterviewComplete]);

const disconnect = useCallback(() => {
  isConnectedRef.current = false;

  if (processorRef.current) {
    processorRef.current.disconnect();
    processorRef.current = null;
  }
  if (inputSourceRef.current) {
    inputSourceRef.current.disconnect();
    inputSourceRef.current = null;
  }
  if (micContextRef.current) {
    if (micContextRef.current.state !== 'closed') {
      micContextRef.current.close();
    }
    micContextRef.current = null;
  }
  if (micStreamRef.current) {
    micStreamRef.current.getTracks().forEach(track => track.stop());
    micStreamRef.current = null;
  }

  if (audioContextRef.current) {
    if (audioContextRef.current.state !== 'closed') {
      audioContextRef.current.close();
    }
    audioContextRef.current = null;
  }

  // Stop all playing sources
  sourcesRef.current.forEach(source => source.stop());
  sourcesRef.current.clear();

  // Close session logic
  // We cannot explicitly close the session object in this version easily if close() is missing on type.
  // But stopping inputs is enough.

  setIsConnected(false);
  setIsTalking(false);
}, []);

useEffect(() => {
  return () => {
    disconnect();
  };
}, [disconnect]);

return { connect, disconnect, isConnected, isTalking, error };
};