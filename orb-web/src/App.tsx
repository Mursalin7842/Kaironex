/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from 'react';
import { useGeminiLive } from './hooks/useGeminiLive';
import './App.css';

function App() {
  const { connect, isConnected, isTalking, error } = useGeminiLive();
  const [apiKey, setApiKey] = useState<string>("");

  useEffect(() => {
    // 1. Try URL Param
    const params = new URLSearchParams(window.location.search);
    const key = params.get('apiKey');

    // 2. Try Android Interface
    const androidKey = (window as any).Android?.getApiKey?.();

    const finalKey = key || androidKey;

    if (finalKey) {
      setApiKey(finalKey);
      connect(finalKey);
    }
  }, [connect]);

  return (
    <div className="orb-container">
      <div className={`orb ${isConnected ? 'connected' : ''} ${isTalking ? 'talking' : ''} ${error ? 'error' : ''}`}>
        <div className="core"></div>
        <div className="glow"></div>
      </div>
      {error && <div className="status">{error}</div>}
      {!apiKey && !isConnected && <div className="status">Waiting for API Key...</div>}
    </div>
  );
}

export default App;
