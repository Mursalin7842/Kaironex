/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from 'react';
import { useGeminiLive } from './hooks/useGeminiLive';
import './App.css';

function App() {
  const { connect, isConnected, isTalking, error } = useGeminiLive();
  const [apiKey, setApiKey] = useState<string>("");
  const [agentName, setAgentName] = useState<string>("Kaironex");
  const [isAgentCall, setIsAgentCall] = useState<boolean>(false);

  useEffect(() => {
    // Parse URL params
    const params = new URLSearchParams(window.location.search);
    const key = params.get('apiKey');
    const agentCall = params.get('agentCall') === 'true';
    const agentType = params.get('agentType');
    const agentNameParam = params.get('agentName');

    // Try Android Interface for API key
    const androidKey = (window as any).Android?.getApiKey?.();
    const finalKey = key || androidKey;

    // Get agent call config from Android if available
    const agentConfig = (window as any).AGENT_CALL_CONFIG;

    if (agentCall || agentConfig) {
      setIsAgentCall(true);
      setAgentName(agentNameParam || agentConfig?.agentName || 'Kaironex Agent');
    }

    if (finalKey) {
      setApiKey(finalKey);

      // Build system instruction based on agent type
      let systemInstruction = "You are Kaironex. Keep responses concise and conversational.";

      if (agentCall && agentType) {
        const agentRoles: Record<string, string> = {
          'STUDY': 'You are the Study Agent. You manage study schedules, track progress, and optimize learning efficiency.',
          'VITALITY': 'You are the Vitality Agent. You monitor wellness, manage budgets, suggest breaks, and ensure students stay healthy.',
          'CAMPAIGN': 'You are the Campaign Agent. You handle long-term goals, GPA targets, career planning, and milestone tracking.',
          'RADIUS': 'You are the Radius Agent. You help with cultural navigation, visa matters, housing, and local resources.',
          'SUPERVISOR': 'You are the Supervisor Agent. You coordinate all agents and handle critical situations.'
        };

        const callReason = agentConfig?.callReason || 'checking in with the student';

        systemInstruction = `
          ${agentRoles[agentType] || 'You are a Kaironex Agent.'}

          You are CALLING the student on your own initiative because: ${callReason}

          CRITICAL BEHAVIOR:
          - You speak FIRST. Introduce yourself briefly and explain why you're calling.
          - Be warm, direct, and conversational. Get to the point in 2-3 sentences.
          - Listen to the student's response and provide helpful guidance.
          - Keep the conversation natural and supportive.

          Start the call now - introduce yourself and explain why you're calling.
        `;
      }

      connect(finalKey, systemInstruction);
    }
  }, [connect]);

  return (
    <div className="orb-container">
      <div className={`orb ${isConnected ? 'connected' : ''} ${isTalking ? 'talking' : ''} ${error ? 'error' : ''}`}>
        <div className="core"></div>
        <div className="glow"></div>
      </div>
      {isAgentCall && isConnected && (
        <div className="agent-label">{agentName}</div>
      )}
      {error && <div className="status">{error}</div>}
      {!apiKey && !isConnected && <div className="status">Waiting for API Key...</div>}
    </div>
  );
}

export default App;
