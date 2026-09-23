import type { Level } from '../model/rule';
import { DEFAULT_BACK } from './art';

/*
 * Everything the game remembers, in the browser's local storage. Reads and
 * writes are wrapped because storage can be unavailable (private windows,
 * blocked site data); the game then simply starts fresh each time.
 */

const PREFIX = 'scorpion.';

function read<T>(key: string, fallback: T): T {
  try {
    const text = localStorage.getItem(PREFIX + key);
    return text === null ? fallback : (JSON.parse(text) as T);
  } catch {
    return fallback;
  }
}

function write(key: string, value: unknown): void {
  try {
    if (value === undefined) localStorage.removeItem(PREFIX + key);
    else localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    // Full or blocked: not fatal.
  }
}

export interface Settings {
  /** The level for the next game; unset the first time. */
  level?: Level;
  back: string;
  /** The player's own picture, as a data URL. */
  customBack?: string;
}

export const loadSettings = (): Settings => ({ back: DEFAULT_BACK, ...read<Partial<Settings>>('settings', {}) });
export const saveSettings = (s: Settings): void => write('settings', s);

export interface Stats {
  played: number;
  won: number;
  streak: number;
  best: number;
}

const EMPTY: Stats = { played: 0, won: 0, streak: 0, best: 0 };

export const loadStats = (level: Level): Stats => ({ ...EMPTY, ...read<Partial<Stats>>(`stats.${level}`, {}) });

export function recordResult(level: Level, won: boolean): void {
  const s = loadStats(level);
  s.played++;
  if (won) {
    s.won++;
    s.streak++;
    s.best = Math.max(s.best, s.streak);
  } else {
    s.streak = 0;
  }
  write(`stats.${level}`, s);
}

export function resetStats(): void {
  for (const level of ['easy', 'medium', 'difficult']) write(`stats.${level}`, undefined);
}

/** The game in progress, in the save-file format. */
export const loadAutosave = (): string | null => read<string | null>('autosave', null);
export const saveAutosave = (text: string | null): void => write('autosave', text ?? undefined);
