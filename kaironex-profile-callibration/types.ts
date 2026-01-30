export enum GenesisStage {
  ACADEMIC = 'ACADEMIC',
  INTERNATIONAL = 'INTERNATIONAL',
  WORK = 'WORK',
  TRAVEL = 'TRAVEL',
  PERSONALIZATION = 'PERSONALIZATION',
  CONFIRMATION = 'CONFIRMATION',
}

export type ProfileField =
  | 'university'
  | 'degreeMajor'
  | 'totalSemesters'
  | 'currentSemester'
  | 'currentCGPA'
  | 'desiredCGPA'
  | 'desiredCGPAReason'
  | 'isInternational'
  | 'hostCountry'
  | 'homeCountry'
  | 'visaStatus'
  | 'hasJob'
  | 'jobPosition'
  | 'jobSchedule'
  | 'commuteTime'
  | 'nonNegotiables'
  | 'learningStyle'
  | 'preferredResources'
  | 'productivityKiller'
  | 'focusCapacity'
  | 'chronotype';

export interface UserProfile {
  [key: string]: string | boolean | undefined;
  university?: string;
  degreeMajor?: string;
  totalSemesters?: string;
  currentSemester?: string;
  currentCGPA?: string;
  desiredCGPA?: string;
  desiredCGPAReason?: string;
  isInternational?: boolean;
  hostCountry?: string;
  homeCountry?: string;
  visaStatus?: string;
  hasJob?: boolean;
  jobPosition?: string;
  jobSchedule?: string;
  commuteTime?: string;
  nonNegotiables?: string;
  learningStyle?: string;
  preferredResources?: string;
  productivityKiller?: string;
  focusCapacity?: string;
  chronotype?: string; // Night owl vs morning person
}

export const FIELD_ORDER: ProfileField[] = [
  'university',
  'degreeMajor',
  'totalSemesters',
  'currentSemester',
  'currentCGPA',
  'desiredCGPA',
  'desiredCGPAReason',
  'isInternational',
  'hostCountry',
  'homeCountry',
  'visaStatus',
  'hasJob',
  'jobPosition',
  'jobSchedule',
  'commuteTime',
  'nonNegotiables',
  'learningStyle',
  'preferredResources',
  'productivityKiller',
  'focusCapacity',
  'chronotype'
];

export const getStageForField = (field: ProfileField): GenesisStage => {
  switch (field) {
    case 'university':
    case 'degreeMajor':
    case 'totalSemesters':
    case 'currentSemester':
    case 'currentCGPA':
    case 'desiredCGPA':
    case 'desiredCGPAReason':
      return GenesisStage.ACADEMIC;
    case 'isInternational':
    case 'hostCountry':
    case 'homeCountry':
    case 'visaStatus':
      return GenesisStage.INTERNATIONAL;
    case 'hasJob':
    case 'jobPosition':
    case 'jobSchedule':
      return GenesisStage.WORK;
    case 'commuteTime':
      return GenesisStage.TRAVEL;
    default:
      return GenesisStage.PERSONALIZATION;
  }
};