
export interface SimulationContext {
    userName: string;
    jobTitle: string;
    jobCompany: string;
    jobDescription: string; // The full JD
    persona: 'Recruiter' | 'HiringManager' | 'Peer' | 'CTO';
    difficulty: 'Easy' | 'Medium' | 'Hard';
}

export type AgentState = 'listening' | 'thinking' | 'speaking';

export interface InterviewSessionProps {
    context: SimulationContext;
    onEnd: () => void;
}
