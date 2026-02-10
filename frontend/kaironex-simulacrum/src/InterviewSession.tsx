
import React, { useRef, useEffect } from 'react';
import { useSimulacrumLive } from './hooks/useSimulacrumLive';
import type { SimulationContext } from './types';

interface InterviewSessionProps {
    context: SimulationContext;
}

const InterviewSession: React.FC<InterviewSessionProps> = ({ context }) => {
    const videoRef = useRef<HTMLVideoElement>(null);
    const { connect, disconnect, isConnected, isTalking, error } = useSimulacrumLive({
        context,
        onEnd: () => {
            console.log("Interview Ended");
            // Notify Android
            if ((window as any).Android) {
                (window as any).Android.onComplete();
            }
        },
        videoRef
    });

    useEffect(() => {
        connect();
        return () => disconnect();
    }, []);

    return (
        <div style={{ position: 'relative', width: '100vw', height: '100vh', background: '#000', overflow: 'hidden' }}>
            {/* Self View (Full Screen) */}
            <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                    transform: 'scaleX(-1)' // Mirror effect
                }}
            />

            {/* Overlay UI */}
            <div style={{
                position: 'absolute',
                top: 0, left: 0, right: 0,
                padding: '20px',
                background: 'linear-gradient(to bottom, rgba(0,0,0,0.7), transparent)',
                color: 'white',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <div>
                    <h2 style={{ margin: 0, fontSize: '18px' }}>{context.persona} Interview</h2>
                    <p style={{ margin: 0, fontSize: '14px', opacity: 0.8 }}>{context.jobTitle} @ {context.jobCompany}</p>
                </div>
                <div style={{
                    padding: '8px 16px',
                    borderRadius: '20px',
                    background: isConnected ? (isTalking ? '#00E676' : '#6200EA') : '#FF1744',
                    fontWeight: 'bold',
                    fontSize: '12px',
                    boxShadow: isTalking ? '0 0 10px #00E676' : 'none',
                    transition: 'all 0.3s ease'
                }}>
                    {isConnected ? (isTalking ? 'INTERVIEWER SPEAKING' : 'LISTENING') : 'CONNECTING...'}
                </div>
            </div>

            {error && (
                <div style={{
                    position: 'absolute',
                    top: '50%', left: '50%',
                    transform: 'translate(-50%, -50%)',
                    background: 'rgba(255, 0, 0, 0.8)',
                    padding: '20px',
                    borderRadius: '8px',
                    color: 'white',
                    textAlign: 'center'
                }}>
                    <h3>Connection Error</h3>
                    <p>{error}</p>
                    <button onClick={() => window.location.reload()} style={{ padding: '8px 16px', marginTop: '10px' }}>Retry</button>
                </div>
            )}

            {/* End Call Button */}
            <div style={{
                position: 'absolute',
                bottom: '30px',
                left: '50%',
                transform: 'translateX(-50%)'
            }}>
                <button
                    onClick={disconnect}
                    style={{
                        width: '60px', height: '60px',
                        borderRadius: '50%',
                        background: '#FF1744',
                        border: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
                        cursor: 'pointer'
                    }}
                >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="white">
                        <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.7-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.36 7.46 6 12 6s8.66 2.36 11.71 5.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.7.29-.27 0-.52-.11-.7-.28-.79-.74-1.69-1.36-2.67-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z" />
                    </svg>
                </button>
            </div>
        </div>
    );
};

export default InterviewSession;
