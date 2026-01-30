import React from 'react';
import { UserProfile, GenesisStage, getStageForField, ProfileField, FIELD_ORDER } from '../types';
interface StageProgressProps {
  profile: UserProfile;
}

const StageProgress: React.FC<StageProgressProps> = ({ profile }) => {
  const completedFields = Object.keys(profile) as ProfileField[];
  const stages = Object.values(GenesisStage);

  // Dynamic Total Calculation
  let totalCalculated = FIELD_ORDER.length;

  // Handle "false" string or boolean false
  const isInt = profile.isInternational;
  const isInternationalFalse = isInt === false || isInt === "false";

  const job = profile.hasJob;
  const hasJobFalse = job === false || job === "false";

  if (isInternationalFalse) {
    totalCalculated -= 3; // Skips: hostCountry, homeCountry, visaStatus
  }

  if (hasJobFalse) {
    totalCalculated -= 2; // Skips: jobPosition, jobSchedule
  }

  const progressPercent = Math.min(Math.round((completedFields.length / totalCalculated) * 100), 100);

  // Determine current stage based on the last completed field
  const lastField = completedFields.length > 0 ? completedFields[completedFields.length - 1] : null;
  const currentStage = lastField ? getStageForField(lastField) : GenesisStage.ACADEMIC;

  return (
    <div className="w-full max-w-2xl mx-auto px-4 py-2">
      <div className="flex justify-between items-center gap-1 mb-3">
        {stages.map((stage, idx) => {
          const isActive = stage === currentStage;
          const isCompleted = stages.indexOf(currentStage) > idx;

          return (
            <div key={stage} className="flex flex-col items-center flex-1 min-w-0">
              <div
                className={`w-2 h-2 rounded-full transition-all duration-500 mb-2
                        ${isActive ? 'bg-indigo-500 scale-125 shadow-[0_0_8px_rgba(99,102,241,0.8)]' :
                    isCompleted ? 'bg-purple-500' : 'bg-gray-200'}
                        `}
              />
              <span className={`text-[8px] md:text-[10px] font-bold tracking-tighter md:tracking-widest uppercase truncate w-full text-center ${isActive ? 'text-indigo-600' : 'text-gray-400'}`}>
                {stage}
              </span>
            </div>
          )
        })}
      </div>

      {/* Progress Bar Line */}
      <div className="relative h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-600 transition-all duration-700 ease-out shadow-[0_0_10px_rgba(99,102,241,0.4)]"
          style={{ width: `${progressPercent}%` }}
        ></div>
      </div>

      <div className="flex justify-between items-center mt-2 px-1">
        <div className="flex items-center gap-2">
          <span className="text-[10px] text-slate-400 font-bold tracking-widest">PROGRESS</span>
          <div className="flex gap-0.5">
            {[...Array(5)].map((_, i) => (
              <div key={i} className={`h-1 w-3 rounded-full ${progressPercent > (i * 20) ? 'bg-indigo-400' : 'bg-slate-200'}`}></div>
            ))}
          </div>
        </div>
        <span className="text-xs text-indigo-600 font-black font-mono">{progressPercent}%</span>
      </div>
    </div>
  );
};

export default StageProgress;