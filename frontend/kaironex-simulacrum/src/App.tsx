
import { useEffect, useState } from 'react';
import InterviewSession from './InterviewSession';
import type { SimulationContext } from './types';

function App() {
  const [context, setContext] = useState<SimulationContext | null>(null);

  useEffect(() => {
    // Parse URL params
    const params = new URLSearchParams(window.location.search);

    // In WebView, we might want to get these from a JS Interface or JSON payload
    // For now, URL params are robust.
    const userName = params.get('userName') || 'Candidate';
    const jobTitle = params.get('jobTitle') || 'Software Engineer';
    const jobCompany = params.get('jobCompany') || 'Tech Corp';
    const jobDescription = decodeURIComponent(params.get('jobDesc') || 'Standard JD');
    const persona = (params.get('persona') as any) || 'Recruiter';
    const difficulty = (params.get('difficulty') as any) || 'Medium';

    setContext({
      userName,
      jobTitle,
      jobCompany,
      jobDescription,
      persona,
      difficulty
    });
  }, []);

  if (!context) return <div style={{ color: 'white', background: 'black', height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Loading Interview...</div>;

  return (
    <InterviewSession context={context} />
  );
}

export default App;
