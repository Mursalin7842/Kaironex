import { UserProfile, FIELD_ORDER } from '../types';

export const calculateProgress = (profile: UserProfile): number => {
    const completedFields = Object.keys(profile);

    // Dynamic Total Calculation
    let totalCalculated = FIELD_ORDER.length;

    // Handle "false" string or boolean false
    const isInt = (profile as any).isInternational;
    const isInternationalFalse = isInt === false || isInt === "false";

    const job = (profile as any).hasJob;
    const hasJobFalse = job === false || job === "false";

    if (isInternationalFalse) {
        totalCalculated -= 3; // Skips: hostCountry, homeCountry, visaStatus
    }

    if (hasJobFalse) {
        totalCalculated -= 2; // Skips: jobPosition, jobSchedule
    }

    // Cap at 100%
    return Math.min(Math.round((completedFields.length / totalCalculated) * 100), 100);
};

export const isInterviewComplete = (profile: UserProfile): boolean => {
    return calculateProgress(profile) >= 100;
};
