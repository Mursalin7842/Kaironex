/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from 'react';
import { useCallerAgent } from './hooks/useCallerAgent';
import './App.css';

function App() {
    const { startCall, endCall, isTalking, callState, error } = useCallerAgent();
    const [agentName, setAgentName] = useState('Kaironex Agent');
    const [agentColor, setAgentColor] = useState('#6366F1');

    useEffect(() => {
        // Parse URL params
        const params = new URLSearchParams(window.location.search);
        const apiKey = params.get('apiKey') || (window as any).ANDROID_API_KEY;
        const agentType = params.get('agentType') || 'SUPERVISOR';
        const agentNameParam = params.get('agentName') || 'Kaironex Agent';
        const callReason = params.get('callReason') || 'checking in with you';

        // Get config from Android if available
        const androidConfig = (window as any).AGENT_CALL_CONFIG;

        const config = {
            agentType: androidConfig?.agentType || agentType,
            agentName: androidConfig?.agentName || agentNameParam,
            callReason: androidConfig?.callReason || callReason,
            callContext: androidConfig?.callContext || '',
            userProfile: androidConfig?.userProfile || '{}'
        };

        setAgentName(config.agentName);

        // Set color based on agent type
        const colors: Record<string, string> = {
            'STUDY': '#4285F4',
            'VITALITY': '#34A853',
            'CAMPAIGN': '#FBBC04',
            'RADIUS': '#9C27B0',
            'SUPERVISOR': '#EA4335'
        };
        setAgentColor(colors[config.agentType] || '#6366F1');

        if (apiKey) {
            // Auto-start the call
            startCall(apiKey, config);
        }
    }, [startCall]);

    const handleEndCall = () => {
        endCall();
        if ((window as any).Android?.onCallEnded) {
            (window as any).Android.onCallEnded();
        }
    };

    return (
        <div className="caller-container" style={{ '--agent-color': agentColor } as any}>
            {/* Background gradient */}
            <div className="background-gradient" />

            {/* Agent Avatar */}
            <div className={`agent-avatar ${isTalking ? 'talking' : ''} ${callState}`}>
                <div className="avatar-glow" />
                <div className="avatar-ring" />
                <div className="avatar-core">
                    <span className="avatar-icon">
                        {agentName.charAt(0).toUpperCase()}
                    </span>
                </div>
            </div>

            {/* Agent Name */}
            <h1 className="agent-name">{agentName}</h1>

            {/* Call Status */}
            <p className="call-status">
                {callState === 'connecting' && 'Connecting...'}
                {callState === 'ringing' && 'Ringing...'}
                {callState === 'active' && (isTalking ? 'Speaking...' : 'Listening...')}
                {callState === 'ended' && 'Call ended'}
                {callState === 'idle' && 'Ready'}
            </p>

            {/* Waveform indicator when talking */}
            {callState === 'active' && (
                <div className="waveform">
                    {[...Array(5)].map((_, i) => (
                        <div key={i} className={`wave-bar ${isTalking ? 'active' : ''}`} style={{ animationDelay: `${i * 0.1}s` }} />
                    ))}
                </div>
            )}

            {/* Error message */}
            {error && <p className="error-message">{error}</p>}

            {/* End Call Button */}
            {callState === 'active' && (
                <button className="end-call-btn" onClick={handleEndCall}>
                    End Call
                </button>
            )}
        </div>
    );
}

export default App;
