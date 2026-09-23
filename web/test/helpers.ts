import type { Board, Move } from '../src/model/board';
import type { Game } from '../src/model/game';
import { isValidMove } from '../src/model/rule';

/** The board as text, in the same form as the Java tests' recordings. */
export function state(b: Board): string {
  let s = '';
  for (let i = 0; i < 3; i++) s += (b.reserveUsed ? '_' : String(-b.reserve[i] - 1)) + ',';
  for (const col of b.columns) {
    s += '|';
    for (const slot of col) s += (slot.up ? slot.card : -slot.card - 1) + ',';
  }
  return s;
}

export function legalMoves(g: Game): Move[] {
  const moves: Move[] = [];
  for (let c = 0; c < 7; c++)
    for (let row = 0; row < g.board.columns[c].length; row++)
      for (let t = 0; t < 7; t++) {
        const m = { fromColumn: c, fromRow: row, toColumn: t };
        if (isValidMove(g.rule, m, g.board)) moves.push(m);
      }
  return moves;
}
