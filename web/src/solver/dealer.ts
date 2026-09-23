import { Game } from '../model/game';
import type { Rule } from '../model/rule';
import { fromText, toText } from '../model/saveFile';
import { type Outcome, type Play, Solver } from './solver';

/** Deal numbers run from 1 to this. */
export const MAX_DEAL = 999999;

/** Positions to search before calling a deal too hard to check. */
export const LIMIT = 300000;

/** A random deal number that the solver can win at this level. */
export function winnableDeal(rule: Rule, random: () => number = Math.random): number {
  const solver = new Solver(rule, LIMIT);
  let n = 1;
  for (let attempt = 0; attempt < 1000; attempt++) {
    n = 1 + Math.floor(random() * MAX_DEAL);
    if (solver.solve(Game.deal(rule, n).board).outcome === 'solved') return n;
  }
  return n;
}

export function check(rule: Rule, n: number): Outcome {
  return new Solver(rule, LIMIT).solve(Game.deal(rule, n).board).outcome;
}

/** How to finish a game: take back `undo` steps, then play `plays`. */
export interface Plan {
  undo: number;
  plays: Play[];
}

/**
 * Finds the latest point in the game's history from which it can still be
 * won, trying the current position, then 1, 2, 4, 8... steps back, and
 * finally the start. Returns null if even the start cannot be won.
 */
export function finish(game: Game): Plan | null {
  const saved = toText(game);
  const steps = game.moveCount;
  const solver = new Solver(game.rule, LIMIT);
  for (let back = 0; ; back = back === 0 ? 1 : Math.min(back * 2, steps)) {
    const copy = fromText(saved);
    for (let i = 0; i < back; i++) copy.forceUndo();
    const result = solver.solve(copy.board);
    if (result.outcome === 'solved') return { undo: back, plays: result.solution };
    if (back === steps) return null;
  }
}
