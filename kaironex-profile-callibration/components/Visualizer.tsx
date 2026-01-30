import React from 'react';

interface VisualizerProps {
  isTalking: boolean;
  isConnected: boolean;
  hasStarted?: boolean;
  size?: 'normal' | 'small';
}

const Visualizer: React.FC<VisualizerProps> = ({ isTalking, isConnected, hasStarted = false, size = 'normal' }) => {
  const isSmall = size === 'small';

  if (isSmall) {
    return (
      <div className="flex items-center gap-1 h-6">
        {[...Array(4)].map((_, i) => (
          <div
            key={i}
            className={`w-1 rounded-full transition-all duration-300 ${isTalking ? 'bg-purple-500 animate-bounce' : 'bg-slate-200 h-1'
              }`}
            style={{
              height: isTalking ? `${Math.random() * 100 + 40}%` : '4px',
              animationDelay: `${i * 0.1}s`
            }}
          />
        ))}
      </div>
    );
  }

  return (
    <div className="relative flex flex-col items-center justify-center w-full max-w-xs mx-auto py-1">
      {/* Visualizer Area */}
      <div className="relative flex items-center justify-center w-32 h-32 mb-2 group cursor-pointer">
        {/* Outer Glow Ring */}
        <div
          className={`absolute inset-0 rounded-full border border-violet-500/20 transition-all duration-1000 
            ${isConnected ? 'scale-100 opacity-100' : 'scale-50 opacity-0'}
            ${isTalking ? 'animate-[ping_3s_ease-in-out_infinite]' : ''}
          `}
        ></div>

        {/* Middle Ring */}
        <div
          className={`absolute inset-4 rounded-full border border-indigo-500/30 transition-all duration-700
            ${isConnected ? 'scale-100 opacity-100' : 'scale-50 opacity-0'}
            ${isTalking ? 'animate-[pulse_2s_ease-in-out_infinite]' : ''}
          `}
        ></div>

        {/* Core Orb */}
        <div
          className={`relative z-10 w-20 h-20 rounded-full shadow-[0_0_40px_-10px_rgba(99,102,241,0.5)] flex items-center justify-center transition-all duration-500 overflow-hidden
            ${isConnected ? 'bg-gradient-to-br from-indigo-500 to-purple-600 scale-100' : 'bg-gradient-to-br from-indigo-400 to-violet-500 scale-90 opacity-90 hover:scale-95'}
            ${isTalking ? 'shadow-[0_0_70px_-10px_rgba(124,58,237,0.7)] scale-110' : ''}
          `}
        >
          {/* Animated Gradient Overlay */}
          <div className={`absolute inset-0 bg-gradient-to-tr from-white/20 to-transparent transition-opacity duration-1000 ${isTalking ? 'opacity-100' : 'opacity-0 animate-pulse'}`}></div>
          <div className="w-full h-full rounded-full opacity-40 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] mix-blend-overlay"></div>

          {/* Internal Icon for Start (only when disconnected) */}
          {!isConnected && (
            <svg className="w-8 h-8 text-white/90 ml-1 transition-transform group-hover:scale-110" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}

          {/* Active indicator */}
          {isConnected && isTalking && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="w-10 h-1 rounded-full bg-white/30 animate-pulse blur-[1px]"></div>
            </div>
          )}
        </div>
      </div>

      {/* Status & Instructions */}
      <div className="flex flex-col items-center gap-1.5">
        {!isConnected ? (
          <div className="flex flex-col items-center animate-fade-in">
            <span className="text-xs font-black text-indigo-600 uppercase tracking-[0.2em] mb-0.5">
              {hasStarted ? "Connection Dropped" : "Begin Calibration"}
            </span>
            <span className="text-[9px] font-bold text-slate-400 opacity-80 uppercase tracking-widest">
              {hasStarted ? "Tap to Resume" : "Tap the Orb and say hello"}
            </span>
          </div>
        ) : (
          <div className="flex flex-col items-center">
            <span className={`text-[9px] font-black tracking-[0.3em] uppercase transition-colors duration-300 ${isTalking ? 'text-purple-600' : 'text-indigo-600'}`}>
              {isTalking ? 'Kairo is Speaking' : 'Listening...'}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};

export default Visualizer;