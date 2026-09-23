import { type Card, ACE, CARDS_IN_SUIT, COLUMNS, KING, cardOf, color, rank, suit } from './card';
import type { Board, Move, Step } from './board';

export type Level = 'easy' | 'medium' | 'difficult';

/**
 * The rules for each difficulty level. The levels differ in which card may be
 * placed on which, and in how far the player may undo; everything else is shared.
 */
export interface Rule {
  level: Level;
  name: string;
  /** One line describing the level. */
  summary: string;
  /** True if card (with anything below it) may be placed on onto. */
  canStack(card: Card, onto: Card): boolean;
  /** True if the player may take back step, the most recent one. */
  mayUndo(step: Step): boolean;
}

const oneRankBelow = (card: Card, onto: Card): boolean => rank(onto) - rank(card) === 1;

export const EASY: Rule = {
  level: 'easy',
  name: 'Easy',
  summary: 'Build down in any suit. Undo anything.',
  canStack: oneRankBelow,
  mayUndo: () => true,
};

export const MEDIUM: Rule = {
  level: 'medium',
  name: 'Medium',
  summary: 'Build down in the same color. Undo until the reserve is dealt.',
  canStack: (card, onto) => color(card) === color(onto) && oneRankBelow(card, onto),
  mayUndo: (step) => step.kind !== 'reserve',
};

export const DIFFICULT: Rule = {
  level: 'difficult',
  name: 'Difficult',
  summary: 'Build down in the same suit. No undo.',
  canStack: (card, onto) => suit(card) === suit(onto) && oneRankBelow(card, onto),
  mayUndo: () => false,
};

export const RULES: readonly Rule[] = [EASY, MEDIUM, DIFFICULT];

export function ruleFor(level: string): Rule {
  const rule = RULES.find((r) => r.level === level.trim().toLowerCase());
  if (!rule) throw new Error(`invalid level: ${level}`);
  return rule;
}

export function isValidMove(rule: Rule, move: Move, board: Board): boolean {
  if (move.toColumn < 0 || move.toColumn >= COLUMNS || move.fromColumn === move.toColumn) return false;
  const from = board.columns[move.fromColumn];
  if (move.fromRow < 0 || move.fromRow >= from.length) return false;
  const moving = from[move.fromRow];
  if (!moving.up) return false;
  const target = board.last(move.toColumn);
  if (!target) return rank(moving.card) === KING;
  return rule.canStack(moving.card, target.card);
}

/** Won when every column is empty or runs King down to Ace in one suit. */
export function isWon(board: Board): boolean {
  return board.columns.every((col) => {
    if (col.length === 0) return true;
    if (col.length !== CARDS_IN_SUIT || rank(col[0].card) !== KING) return false;
    return col.every((s, i) => i === 0 || col[i - 1].card - s.card === 1);
  });
}

/** Moves worth suggesting as hints; empty if there are none. */
export function hints(rule: Rule, board: Board): Move[] {
  const moves: Move[] = [];
  for (let j = 0; j < COLUMNS; j++) {
    const target = board.last(j)?.card;
    for (const card of candidatesFor(rule, target)) {
      const pos = board.findFaceUp(card);
      if (!pos || pos.column === j) continue;
      if (rank(card) === KING && pos.row === 0) continue;
      if (pos.row !== 0 && target !== undefined) {
        // Skip a card already on the next card up in its own suit: moving it off never helps.
        const above = board.columns[pos.column][pos.row - 1];
        if (above.up && suit(above.card) === suit(card) && rank(above.card) - rank(card) === 1) continue;
      }
      moves.push({ fromColumn: pos.column, fromRow: pos.row, toColumn: j });
    }
  }
  return moves;
}

// Cards that may go on target: Kings for an empty column, same suit first.
function candidatesFor(rule: Rule, target: Card | undefined): Card[] {
  if (target === undefined) return [0, 1, 2, 3].map((s) => cardOf(s, KING));
  if (rank(target) === ACE) return [];
  const below = rank(target) - 1;
  const ret = [cardOf(suit(target), below)];
  for (let s = 0; s < 4; s++) {
    const card = cardOf(s, below);
    if (s !== suit(target) && rule.canStack(card, target)) ret.push(card);
  }
  return ret;
}
