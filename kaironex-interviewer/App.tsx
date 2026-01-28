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

  const handleDownload = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(profile, null, 2));
    const anchor = document.createElement('a');
    anchor.href = dataStr;
    anchor.download = "kaironex_profile.json";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  };

  return (
    <div className="min-h-screen bg-gray-50 p-8 font-sans">
      <div className="max-w-2xl mx-auto bg-white shadow-sm border border-gray-200 rounded-lg p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Kaironex Profile Interviewer</h1>

        <div className="flex flex-col gap-4 mb-8">
          <div className="flex items-center justify-between bg-gray-100 p-4 rounded text-sm">
            <div>
              <span className="font-semibold text-gray-600">Status: </span>
              <span className={`font-medium ${isConnected ? 'text-green-600' : 'text-gray-800'}`}>{status}</span>
            </div>
            <div>
              <span className="font-semibold text-gray-600">Agent: </span>
              <span className={`font-medium ${isTalking ? 'text-blue-600 animate-pulse' : 'text-gray-500'}`}>
                {isTalking ? "Speaking..." : "Listening"}
              </span>
            </div>
          </div>

          {error && (
            <div className="bg-red-50 text-red-700 p-4 rounded border border-red-200">
              Error: {error}
            </div>
          )}

          <div className="flex gap-3 mt-2">
            {!isConnected ? (
              <button
                onClick={handleStart}
                className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-6 rounded transition-colors"
              >
                Start Interview
              </button>
            ) : (
              <button
                onClick={handleStop}
                className="bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-6 rounded transition-colors"
              >
                End Session
              </button>
            )}
            
            <button
              onClick={handleDownload}
              disabled={Object.keys(profile).length === 0}
              className="bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 font-medium py-2 px-6 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Download JSON
            </button>
          </div>
        </div>

        <div className="border-t border-gray-100 pt-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Captured Profile Data</h2>
          
          {Object.keys(profile).length === 0 ? (
            <div className="text-center py-10 text-gray-400 bg-gray-50 rounded border border-dashed border-gray-200">
              No data collected yet. Start the interview to begin.
            </div>
          ) : (
            <div className="bg-gray-50 rounded border border-gray-200 overflow-hidden">
              <table className="w-full text-sm text-left">
                <thead className="bg-gray-100 text-gray-600 font-medium border-b border-gray-200">
                  <tr>
                    <th className="px-4 py-3 w-1/3">Field</th>
                    <th className="px-4 py-3">Value</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {Object.entries(profile).map(([key, value]) => (
                    <tr key={key} className="hover:bg-gray-100 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-700 capitalize">
                        {key.replace(/([A-Z])/g, ' $1').trim()}
                      </td>
                      <td className="px-4 py-3 text-gray-900 break-words">
                        {String(value)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default App;