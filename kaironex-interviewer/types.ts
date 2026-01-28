export enum GenesisStage {
  ACADEMIC = 'ACADEMIC',
  GOALS = 'GOALS',
  RHYTHM = 'RHYTHM',
  CONSTRAINTS = 'CONSTRAINTS',
  CONFIRMATION = 'CONFIRMATION',
}

export type ProfileField = 
  | 'university'
  | 'major'
  | 'semester'
  | 'currentCgpa'
  | 'isInternationalStudent'
  | 'hasJob'
  | 'wantsJobHelp'
  | 'jobDescription'
  | 'commuteDuration'
  | 'energyPreference'
  | 'dailyFocusCapacity'
  | 'nonNegotiables'
  | 'learningStyle'
  | 'stressResponse'
  | 'failureCause';

export interface UserProfile {
  university?: string;
  major?: string;
  semester?: string;
  currentCgpa?: string;
  isInternationalStudent?: boolean;
  hasJob?: boolean;
  wantsJobHelp?: boolean;
  jobDescription?: string;
  commuteDuration?: string;
  energyPreference?: string;
  dailyFocusCapacity?: string;
  nonNegotiables?: string;
  learningStyle?: string;
  stressResponse?: string;
  failureCause?: string;
}

export const FIELD_ORDER: ProfileField[] = [
  'university',
  'major',
  'semester',
  'currentCgpa',
  'isInternationalStudent',
  'hasJob',
  'wantsJobHelp',
  'jobDescription',
  'commuteDuration',
  'energyPreference',
  'dailyFocusCapacity',
  'nonNegotiables',
  'learningStyle',
  'stressResponse',
  'failureCause'
];

export const getStageForField = (field: ProfileField): GenesisStage => {
  switch (field) {
    case 'university':
    case 'major':
    case 'semester':
    case 'currentCgpa':
    case 'isInternationalStudent':
      return GenesisStage.ACADEMIC;
    case 'hasJob':
    case 'wantsJobHelp':
    case 'jobDescription':
    case 'commuteDuration':
      return GenesisStage.GOALS;
    case 'energyPreference':
    case 'dailyFocusCapacity':
      return GenesisStage.RHYTHM;
    case 'nonNegotiables':
    case 'learningStyle':
    case 'stressResponse':
      return GenesisStage.CONSTRAINTS;
    case 'failureCause':
      return GenesisStage.CONFIRMATION;
    default:
      return GenesisStage.ACADEMIC;
  }
};