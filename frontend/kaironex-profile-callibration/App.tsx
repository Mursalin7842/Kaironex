import React, { useState, useCallback } from 'react';
import { useGeminiLive } from './hooks/useGeminiLive';
import { UserProfile, GenesisStage, getStageForField } from './types';
import StageProgress from './components/StageProgress';
import Visualizer from './components/Visualizer';
import { isInterviewComplete } from './utils/profileUtils';

const App: React.FC = () => {
  console.log("App: Initializing with FIXED LAYOUT & LOGIC ENABLED");

  const [profile, setProfile] = useState<UserProfile>({});
  const [status, setStatus] = useState<string>("Ready");

  // Input states initialized from URL params
  const [userName] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('userName') || "Student";
  });

  const [agentName] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('agentName') || "Kaironex";
  });

  const handleProfileUpdate = useCallback((field: string, value: any) => {
    console.log("App: Profile Updated", field, value);
    setProfile(prev => ({ ...prev, [field]: value }));
  }, []);

  const handleInterviewComplete = useCallback(() => {
    console.log("App: Interview Complete");
    setStatus("Interview Complete");
  }, []);

  const { connect, disconnect, isConnected, isTalking, error } = useGeminiLive({
    onProfileUpdate: handleProfileUpdate,
    onInterviewComplete: handleInterviewComplete
  });

  const handleStart = async () => {
    console.log("App: Starting connection...");
    setStatus("Connecting...");
    await connect(userName, agentName, profile);
    setStatus("Interview in Progress");
  };

  const handleStop = () => {
    console.log("App: Stopping connection...");
    disconnect();
    setStatus("Stopped");
  };

  const handleProceed = useCallback(() => {
    console.log("App: Proceeding manually...");
    disconnect();
    if ((window as any).Android) {
      (window as any).Android.onComplete();
    }
    handleInterviewComplete();
  }, [disconnect, handleInterviewComplete]);

  return (
    // FIXED POITION STRATEGY: Forces the app to fill the WebView viewport strictly.
    // Removed specific yellow background/border, using standard app styling.
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 9999 }} className="flex flex-col bg-slate-50 font-sans selection:bg-indigo-100 overflow-hidden">

      {/* Scrollable Main Content (Header + Grid) taking available space */}
      <div className="flex-1 overflow-hidden flex flex-col relative min-h-0">

        {/* Header */}
        <div className="flex-none bg-white border-b border-indigo-100 shadow-sm px-4 py-3 z-20">
          <div className="max-w-5xl mx-auto flex flex-col items-center justify-center gap-1">
            <div className="text-[10px] font-black text-indigo-600 mb-0.5 tracking-[0.3em] uppercase animate-pulse drop-shadow-sm">
              {isConnected ? 'Live Session' : 'Ready'}
            </div>
            <div className="text-lg font-black text-slate-800 tracking-tight text-center drop-shadow-sm truncate w-full">
              {isConnected ? `Talking with ${agentName}` : `Start with ${agentName}`}
            </div>
          </div>
        </div>

        {/* Global Progress Section */}
        <div className="flex-none bg-white/80 backdrop-blur-md border-b border-indigo-50 px-4 py-2 z-10">
          <StageProgress profile={profile} />
        </div>

        {/* Data Grid - Scrollable */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-4 bg-gradient-to-br from-slate-50 to-indigo-50/30">
          <div className="max-w-5xl mx-auto pb-4">
            {Object.keys(profile).length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center opacity-60">
                <div className="w-12 h-12 bg-white rounded-2xl shadow-[0_10px_30px_-10px_rgba(99,102,241,0.2)] flex items-center justify-center mb-4 animate-float">
                  <svg className="w-6 h-6 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                </div>
                <p className="text-indigo-900/40 text-xs font-bold tracking-widest uppercase">Waiting to start...</p>
                <p className="text-[10px] text-slate-300 mt-2">v4.0 Fixed</p>
              </div>
            ) : (
              <div className="space-y-6 animate-fade-in">
                {Object.values(GenesisStage).map(stage => {
                  const stageFields = Object.entries(profile).filter(([key]) => getStageForField(key as any) === stage);
                  if (stageFields.length === 0) return null;

                  return (
                    <div key={stage} className="space-y-3">
                      <div className="flex items-center gap-3 px-1">
                        <span className="text-[9px] font-black text-indigo-600 uppercase tracking-[0.25em] drop-shadow-sm">{stage}</span>
                        <div className="h-px flex-1 bg-gradient-to-r from-indigo-200 to-transparent"></div>
                      </div>
                      <div className="grid grid-cols-1 gap-3">
                        {stageFields.map(([key, value]) => (
                          <div key={key} className="bg-white border border-indigo-50 rounded-xl p-4 shadow-sm">
                            <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest block mb-1">
                              {key.replace(/([A-Z])/g, ' $1').trim()}
                            </span>
                            <span className="text-sm text-slate-800 font-bold break-words leading-relaxed">
                              {String(value)}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Interaction Width - Fixed Height at Bottom in Portrait */}
      <div className="flex-none bg-white border-t border-indigo-100 shadow-[0_-10px_40px_-10px_rgba(99,102,241,0.1)] z-50 flex flex-col justify-center py-4 pb-6 min-h-[160px]">
        <div className="flex flex-col items-center w-full max-w-sm mx-auto px-6">

          {/* Interaction Orb */}
          <div
            onClick={isConnected ? handleStop : handleStart}
            className="w-full flex flex-col items-center cursor-pointer active:scale-95 transition-transform"
          >
            <Visualizer isTalking={isTalking} isConnected={isConnected} hasStarted={Object.keys(profile).length > 0} />
          </div>

          <p className="mt-4 text-[10px] font-medium text-slate-400 text-center max-w-[200px] leading-tight">
            {isConnected ? "Listening... Tap to stop" : "Tap the orb to start"}
          </p>

          {/* Manual Exit Button */}
          <button
            onClick={handleProceed}
            className="mt-3 px-6 py-2 rounded-full text-[9px] font-black uppercase tracking-[0.2em] text-indigo-500 hover:bg-slate-50 transition-colors"
          >
            {isInterviewComplete(profile) ? 'Complete' : 'Skip / Manual'}
          </button>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="absolute top-0 left-0 right-0 -translate-y-full bg-red-500 text-white text-[10px] font-bold text-center py-2 animate-slide-up">
            {error}
          </div>
        )}
      </div>

    </div>
  );
};

export default App;