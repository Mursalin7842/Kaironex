import React, { useState, useCallback } from 'react';
import { useGeminiLive } from './hooks/useGeminiLive';
import { UserProfile } from './types';

const App: React.FC = () => {
  const [profile, setProfile] = useState<UserProfile>({});
  const [status, setStatus] = useState<string>("Ready");

  const handleProfileUpdate = useCallback((field: string, value: any) => {
    setProfile(prev => ({ ...prev, [field]: value }));
  }, []);

  const handleInterviewComplete = useCallback(() => {
    setStatus("Interview Complete");
  }, []);

  const { connect, disconnect, isConnected, isTalking, error } = useGeminiLive({
    onProfileUpdate: handleProfileUpdate,
    onInterviewComplete: handleInterviewComplete
  });

  const handleStart = async () => {
    setStatus("Connecting...");
    await connect();
    setStatus("Interview in Progress");
  };

  const handleStop = () => {
    disconnect();
    setStatus("Stopped");
  };

  // Expose control to Android
  React.useEffect(() => {
    console.log("🚀 React App Mounted. Waiting for commands...");
    (window as any).startInterview = () => {
      console.log("🚀 window.startInterview() called from Android!");
      handleStart();
    };
    (window as any).stopInterview = handleStop;

    // Auto-start check if configured or simply expose
    if ((window as any).ANDROID_AUTO_START) {
      console.log("🚀 Auto-start flag detected immediately.");
      handleStart();
    }
  }, [handleStart, handleStop]);

  const handleDownload = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(profile, null, 2));
    const anchor = document.createElement('a');
    anchor.href = dataStr;
    anchor.download = "kaironex_profile.json";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  };

  // --- UI RENDER ---
  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center bg-gradient-to-b from-[#0F0F1A] to-[#1A1A2E] text-white overflow-hidden relative font-sans">

      {/* Background Ambient Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-blue-900/20 rounded-full blur-[100px] pointer-events-none" />

      {/* Main Content */}
      <div className="z-10 flex flex-col items-center gap-12">

        {/* Status Text (Fade In) */}
        <div className="text-center space-y-2 opacity-80 h-16">
          <h2 className="text-2xl font-light tracking-wide text-blue-100">
            {status === "Connecting..." ? "Connecting to Kairo..." :
              status === "Interview in Progress" ? (isTalking ? "Kairo is speaking" : "Listening...") :
                status}
          </h2>
        </div>

        {/* THE ORB */}
        <div className="relative flex items-center justify-center">
          {/* Outer Rings (Ripple) */}
          <div className={`absolute w-64 h-64 rounded-full border border-blue-500/30 transition-all duration-1000 ${isTalking ? 'scale-150 opacity-0' : 'scale-100 opacity-20'}`} />

          {/* Glow Halo */}
          <div className={`absolute w-48 h-48 rounded-full bg-blue-500/20 blur-xl transition-all duration-500 ${isTalking ? 'scale-125 opacity-50' : 'scale-100 opacity-30'}`} />

          {/* Core Orb */}
          <div
            className={`w-32 h-32 rounded-full bg-gradient-to-br from-blue-400 to-blue-700 shadow-[0_0_50px_rgba(59,130,246,0.6)] flex items-center justify-center transition-transform duration-300 ease-out`}
            style={{
              transform: isTalking ? 'scale(1.2)' : 'scale(1.0)',
              animation: isTalking ? 'none' : 'breathe 3s infinite ease-in-out'
            }}
          >
            <div className="w-28 h-28 rounded-full bg-gradient-to-tr from-white/10 to-transparent" />
          </div>
        </div>

        {/* Sub-status / Captions placeholder */}
        <div className="text-blue-200/50 text-sm font-medium tracking-widest uppercase mt-8">
          {isConnected ? "Live Connection Active" : "Initializing..."}
        </div>
      </div>

      {/* Hidden Debug / Manual Start Overlay (Bottom Right) */}
      <div
        className="absolute bottom-0 right-0 p-8 w-24 h-24 opacity-0 z-50"
        onClick={handleStart}
      />

      {/* Tailwind Global Styles for Animations */}
      <style>{`
        @keyframes breathe {
          0%, 100% { transform: scale(0.95); opacity: 0.9; }
          50% { transform: scale(1.05); opacity: 1; }
        }
      `}</style>
    </div>
  );
};

export default App;