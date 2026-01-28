import React from 'react';
import { GenesisStage, getStageForField, ProfileField } from '../types';

interface StageProgressProps {
  completedFields: ProfileField[];
}

const StageProgress: React.FC<StageProgressProps> = ({ completedFields }) => {
  const stages = Object.values(GenesisStage);
  
  // Determine current stage based on the last completed field
  const lastField = completedFields.length > 0 ? completedFields[completedFields.length - 1] : null;
  const currentStage = lastField ? getStageForField(lastField) : GenesisStage.ACADEMIC;

  return (
    <div className="w-full max-w-2xl px-6 py-4">
      <div className="flex justify-between items-center mb-2">
        {stages.map((stage, idx) => {
            const isActive = stage === currentStage;
            const isCompleted = stages.indexOf(currentStage) > idx;

            return (
                <div key={stage} className="flex flex-col items-center gap-2">
                    <div 
                        className={`w-3 h-3 rounded-full transition-all duration-500 
                        ${isActive ? 'bg-sky-400 scale-125 shadow-[0_0_10px_rgba(56,189,248,0.8)]' : 
                          isCompleted ? 'bg-emerald-500' : 'bg-slate-700'}
                        `}
                    />
                    <span className={`text-[10px] font-bold tracking-widest uppercase ${isActive ? 'text-sky-400' : 'text-slate-600'}`}>
                        {stage}
                    </span>
                </div>
            )
        })}
      </div>
      
      {/* Progress Bar Line */}
      <div className="h-0.5 w-full bg-slate-800 rounded-full overflow-hidden mt-4">
        <div 
            className="h-full bg-gradient-to-r from-sky-500 to-indigo-500 transition-all duration-700 ease-out"
            style={{ width: `${(completedFields.length / 15) * 100}%` }}
        ></div>
      </div>
      <div className="flex justify-between mt-1">
         <span className="text-xs text-slate-500 font-mono">PROGRESS</span>
         <span className="text-xs text-sky-400 font-mono">{Math.round((completedFields.length / 15) * 100)}%</span>
      </div>
    </div>
  );
};

export default StageProgress;