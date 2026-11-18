export type HapticKind =
  | 'tick'
  | 'snap'
  | 'editEnter'
  | 'press'
  | 'pageSettle'
  | 'remove'
  | 'error';

export interface HapticEvent {
  id: number;
  kind: HapticKind;
  label: string;
  x?: number;
  y?: number;
  timestamp: number;
}

const HAPTIC_PATTERNS: Record<HapticKind, number | number[]> = {
  tick: 8,
  snap: 18,
  editEnter: [20, 30, 32],
  press: 10,
  pageSettle: 12,
  remove: [15, 25, 15],
  error: [25, 25, 25],
};

const HAPTIC_LABELS: Record<HapticKind, string> = {
  tick: 'Feiner Tick',
  snap: 'Einrast-Klick',
  editEnter: 'Bearbeiten-Impuls',
  press: 'Tippen',
  pageSettle: 'Seiten-Tick',
  remove: 'Entfernt',
  error: 'Limit',
};

type HapticListener = (event: HapticEvent) => void;

const listeners = new Set<HapticListener>();
let eventSeq = 1;

export function subscribeHaptics(listener: HapticListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function triggerHaptic(
  kind: HapticKind,
  options?: { enabled?: boolean; x?: number; y?: number }
): void {
  const enabled = options?.enabled ?? true;
  if (!enabled) return;

  if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
    try {
      navigator.vibrate(HAPTIC_PATTERNS[kind]);
    } catch {
      // Ignore browser permission restrictions on desktop
    }
  }

  eventSeq += 1;
  const evt: HapticEvent = {
    id: eventSeq,
    kind,
    label: HAPTIC_LABELS[kind],
    x: options?.x,
    y: options?.y,
    timestamp: Date.now(),
  };

  listeners.forEach((fn) => fn(evt));
}
