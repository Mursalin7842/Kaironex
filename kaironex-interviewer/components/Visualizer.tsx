import React from 'react';

interface VisualizerProps {
  isTalking: boolean;
  isConnected: boolean;
}

const Visualizer: React.FC<VisualizerProps> = ({ isTalking, isConnected }) => {
  return (
    <div className="relative flex items-center justify-center w-64 h-64 mb-12">
      {/* Outer Glow Ring */}
      <div 
        className={`absolute inset-0 rounded-full border border-sky-500/30 transition-all duration-1000 
          ${isConnected ? 'scale-100 opacity-100' : 'scale-50 opacity-0'}
          ${isTalking ? 'animate-[ping_3s_ease-in-out_infinite]' : ''}
        `}
      ></div>

      {/* Middle Ring */}
      <div 
        className={`absolute inset-4 rounded-full border border-sky-400/40 transition-all duration-700
          ${isConnected ? 'scale-100 opacity-100' : 'scale-50 opacity-0'}
          ${isTalking ? 'animate-[pulse_2s_ease-in-out_infinite]' : ''}
        `}
      ></div>

      {/* Core Orb */}
      <div 
        className={`relative z-10 w-32 h-32 rounded-full bg-gradient-to-br from-sky-400 to-indigo-600 shadow-[0_0_60px_-10px_rgba(79,70,229,0.5)] flex items-center justify-center transition-all duration-500
          ${isConnected ? 'scale-100' : 'scale-0'}
          ${isTalking ? 'shadow-[0_0_100px_-10px_rgba(79,70,229,0.8)] scale-110' : ''}
        `}
      >
        {/* Inner detail */}
        <div className="w-full h-full rounded-full opacity-50 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] mix-blend-overlay"></div>
        
        {!isConnected && (
            <span className="absolute text-white font-mono text-xs tracking-widest uppercase">Offline</span>
        )}
      </div>

      {/* Connection Status Text */}
      <div className="absolute -bottom-12 text-center">
        <p className={`text-sm font-medium tracking-wide transition-colors duration-300 ${isTalking ? 'text-sky-400' : 'text-slate-500'}`}>
          {isConnected ? (isTalking ? 'KAIRO IS SPEAKING' : 'LISTENING...') : 'TAP TO START'}
        </p>
      </div>
    </div>
  );
};

export default Visualizer;