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

  // Android Interface Definition
  interface AndroidInterface {
    onAgentState: (isTalking: boolean, isConnected: boolean) => void;
    onProfileUpdate: (field: string, value: string) => void;
    onComplete: () => void;
  }

  const connect = useCallback(async (userName: string, agentName: string, existingProfile: UserProfile) => {
    try {
      setError(null);
      // Try injecting from Android WebView first, then fallback to build-time env
      const apiKey = (window as any).ANDROID_API_KEY || process.env.API_KEY;
      console.log(`🔑 API Key Verification: ${apiKey ? "FOUND" : "MISSING"}`);

      if (!apiKey) {
        throw new Error("API Key not found in environment.");
      }

      nextStartTimeRef.current = 0;
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

      // Convert existing profile to a readable string for the AI
      const knownData = JSON.stringify(existingProfile, null, 2);
      const hasKnownData = Object.keys(existingProfile).length > 0;

      const sessionPromise = ai.live.connect({
        model: 'gemini-2.5-flash-native-audio-preview-12-2025',
        config: {
          responseModalities: [Modality.AUDIO],
          systemInstruction: `
            You are ${agentName}, a friendly AI interviewer for Kaironex.
            You are interviewing ${userName} to calibrate their profile.
            
            Here is what we know about the user so far:
            ${knownData}

            INSTRUCTIONS FOR RESUMING:
            1. Review the 'knownData' above.
            2. If a field is already present in 'knownData', DO NOT ask for it again.
            3. SKIP directly to the next missing field in the flow below.
            4. If we are resuming (knownData is not empty), start by saying: "Welcome back ${userName}! Let's pick up where we left off." and then ask the next missing question.
            5. If knownData is empty, start from Phase 1.

            Follow this EXACT flow. Do not skip questions unless they are already in knownData. Call 'saveField' immediately after receiving a clear answer for a field.
            
            Phase 1: Introduction (SKIP IF RESUMING)
            - Say: "Hello ${userName} welcome to Kaironex. I am ${agentName}, I will be helping you calibrate your profile. Are you ready?"

            Phase 2: Academic Life (Part 1)
            1. Ask for University name. -> save 'university'
            2. Ask for Major and degree (e.g., BSc, Masters, MBBS). -> save 'degreeMajor'
            3. Ask for Total Semesters. -> save 'totalSemesters'
            4. Ask for Current Semester. -> save 'currentSemester'
            5. Ask for Current CGPA. -> save 'currentCGPA'
            6. Ask for Desired CGPA. -> save 'desiredCGPA'
            7. Ask for the reason for this desired CGPA. -> save 'desiredCGPAReason'

            Phase 3: Academic Life (Part 2 - International)
            1. (Condition: isInternational is not saved yet) Ask: "Are you an international student?" -> save 'isInternational' (true/false)
            IF YES (or isInternational is true in knownData):
               - Ask: "Which country are you in now?" -> save 'hostCountry'
               - Ask: "What is your home country?" -> save 'homeCountry'
               - Ask: "You are under F1 visa right? If yes say yes, if not, what is your visa status?" -> save 'visaStatus'
            
            Phase 4: Instruction
            - Say: "Please upload your class schedule on the profile confirmation page or add it into your google drive folder."

            Phase 5: Work Life
            1. (Condition: hasJob is not saved yet) Ask: "Do you have any part time job or an internship?" -> save 'hasJob' (true/false)
            IF YES (or hasJob is true in knownData):
               - Ask: "What is your position or description? (e.g. developer, chef)" -> save 'jobPosition'
               - Ask: "Please share your job schedule: Total hours/week, which days, start and finish times." -> save 'jobSchedule' (summarize user answer)

            Phase 6: Travel Time
            1. Ask: "How long is your travel time from Home to University and University to Home?"
            IF hasJob is TRUE (in knownData or just answered):
               - Also ask: "Do you go to work from home or from university? What is the travel time for that route?"
            -> save 'commuteTime' (summarize all routes mentioned)

            Phase 7: Personalization
            - Say: "Ok, now I have useful information for the Kaironex engine. Now for some personalization."
            1. Ask: "Do you have any Non-negotiables like family time, prayer, gym, etc? If yes, what are they?" -> save 'nonNegotiables'
            2. Ask: "How do you learn best and what type of learner do you think you are?" -> save 'learningStyle'
            3. Ask: "What kind of resources help you most?" -> save 'preferredResources'
            4. Ask: "What kills your productivity or motivation? (distraction, confusion, clarity, etc)" -> save 'productivityKiller'
            5. Ask: "How many hours can you study non-stop (maximum focus time)?" -> save 'focusCapacity'
            6. Ask: "Do you prefer being a Night Owl or a Morning Person?" -> save 'chronotype'

            Phase 8: Closing
            - Say: "Thank you, I have all I need. Thanks for your patience. Do you need any of the information corrected or is everything right?"
            - If confirmed, call 'endInterview'.
            - Final signoff: "Ok thank you, make sure you upload your resources in your google drive folder and connect it to the kaironex system on the next screen. Whenever you need me just say the magic word 'Hey ${agentName}'."
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

            // Use the main context (24kHz is fine, we can resample or let the worklet handle raw data if needed)
            // But for compatibility with the Gemini 16kHz requirement, creating a 16kHz context is still safest 
            // if we want to avoid complex manual resampling in the worklet.
            // NOTE: AudioWorklet must be added to the context being used.
            const micContext = new AudioContext({ sampleRate: 16000 });
            micContextRef.current = micContext;

            // Worklet Code Inlined for WebView Compatibility
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
              console.log("Audio Worklet Loaded from Blob");
            } catch (err) {
              console.error("Failed to load audio worklet from Blob", err);
              setError("Audio setup failed. Please restart.");
              return;
            }

            const source = micContext.createMediaStreamSource(stream);
            const workletNode = new AudioWorkletNode(micContext, 'audio-recorder-processor');

            workletNode.port.onmessage = (event) => {
              if (!isConnectedRef.current) return;

              if (event.data.eventType === 'audio_data') {
                const float32Array = event.data.audioBuffer;
                // Convert Float32 to Int16
                const pcm16 = float32To16BitPCM(float32Array);
                const base64Data = bytesToBase64(new Uint8Array(pcm16.buffer));

                sessionPromise.then(session => {
                  if (isConnectedRef.current) {
                    try {
                      session.sendRealtimeInput({
                        media: {
                          mimeType: 'audio/pcm;rate=16000',
                          data: base64Data
                        }
                      });
                    } catch (e) {
                      console.warn("Socket send failed", e);
                    }
                  }
                });
              }
            };

            source.connect(workletNode);
            workletNode.connect(micContext.destination); // Keep line alive

            inputSourceRef.current = source;
            // processorRef is typed as ScriptProcessorNode, we can cast or just ignore since we don't need it.
            // But we should track the worklet node to disconnect it.
            (processorRef.current as any) = workletNode;

            // Initial trigger removed - user will start by speaking.
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
                  if ((window as any).Android) {
                    (window as any).Android.onProfileUpdate(field, String(value));
                  }
                  functionResponses.push({
                    id: fc.id,
                    name: fc.name,
                    response: { result: "Field saved successfully." }
                  });
                } else if (fc.name === 'endInterview') {
                  onInterviewComplete();
                  if ((window as any).Android) {
                    (window as any).Android.onComplete();
                  }
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