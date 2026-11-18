export type AnimationMood = 'verspielt' | 'normal' | 'ruhig';

export interface MotionSettings {
  stiffnessScale: number;
  dampingScale: number;
  mood: AnimationMood;
  hapticsEnabled: boolean;
  slowMotion: boolean;
}

export const DEFAULT_MOTION_SETTINGS: MotionSettings = {
  stiffnessScale: 1,
  dampingScale: 1,
  mood: 'normal',
  hapticsEnabled: true,
  slowMotion: false,
};

export type SpringRole =
  | 'dividerSnap'
  | 'pageSwipe'
  | 'tileSwap'
  | 'gapOpen'
  | 'pressFeedback'
  | 'groupExpand';

interface BaseSpringSpec {
  zeta: number;
  stiffness: number;
}

export const BASE_SPRINGS: Record<SpringRole, BaseSpringSpec> = {
  dividerSnap: { zeta: 0.75, stiffness: 400 },
  pageSwipe: { zeta: 0.85, stiffness: 300 },
  tileSwap: { zeta: 0.7, stiffness: 380 },
  gapOpen: { zeta: 0.8, stiffness: 500 },
  pressFeedback: { zeta: 0.6, stiffness: 1500 },
  groupExpand: { zeta: 0.85, stiffness: 260 },
};

const MOOD_MODIFIERS: Record<AnimationMood, { zetaMult: number; stiffnessMult: number }> = {
  verspielt: { zetaMult: 0.72, stiffnessMult: 1.12 },
  normal: { zetaMult: 1, stiffnessMult: 1 },
  ruhig: { zetaMult: 1.32, stiffnessMult: 0.82 },
};

export interface FramerSpringTransition {
  type: 'spring';
  stiffness: number;
  damping: number;
  mass: number;
  restDelta: number;
  restSpeed: number;
}

export function getSpringTransition(
  role: SpringRole,
  settings: MotionSettings
): FramerSpringTransition {
  const base = BASE_SPRINGS[role];
  const moodMod = MOOD_MODIFIERS[settings.mood];

  const rawStiffness = base.stiffness * settings.stiffnessScale * moodMod.stiffnessMult;
  const rawZeta = Math.max(0.32, base.zeta * settings.dampingScale * moodMod.zetaMult);
  const rawDamping = 2 * rawZeta * Math.sqrt(rawStiffness);

  // Slow motion 0.25x scales frequency omega by s=0.25 -> stiffness * s^2, damping * s
  const timeScale = settings.slowMotion ? 0.25 : 1;
  const stiffness = rawStiffness * timeScale * timeScale;
  const damping = rawDamping * timeScale;

  return {
    type: 'spring',
    stiffness,
    damping,
    mass: 1,
    restDelta: 0.001,
    restSpeed: 0.01,
  };
}
