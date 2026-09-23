import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

import { Game } from '../src/model/game';
import { JavaRandom } from '../src/model/random';
import { DIFFICULT, EASY, MEDIUM } from '../src/model/rule';
import { fromText, toText } from '../src/model/saveFile';
import { legalMoves, state } from './helpers';

function playRandomly(g: Game, steps: number, r: JavaRandom): void {
  for (let i = 0; i < steps; i++) {
    const moves = legalMoves(g);
    if (moves.length === 0) return;
    g.move(moves[r.nextInt(moves.length)]);
  }
}

describe('game', () => {
  it('deals three face-down cards in each of the first four columns', () => {
    const g = Game.deal(EASY, 42);
    expect(g.board.columns.map((c) => c.length)).toEqual([7, 7, 7, 7, 7, 7, 7]);
    expect([0, 1, 2, 3, 4, 5, 6].map((c) => g.board.faceDownCount(c))).toEqual([3, 3, 3, 3, 0, 0, 0]);
    expect(g.board.reserve).toHaveLength(3);
  });

  it('easy undoes everything back to the deal', () => {
    const g = Game.deal(EASY, 7);
    const start = state(g.board);
    playRandomly(g, 60, new JavaRandom(1));
    g.useReserve();
    expect(g.moveCount).toBeGreaterThan(0);
    while (g.canUndo()) g.undo();
    expect(g.moveCount).toBe(0);
    expect(state(g.board)).toBe(start);
  });

  it('redo replays undone steps', () => {
    const g = Game.deal(EASY, 8);
    playRandomly(g, 40, new JavaRandom(2));
    const end = state(g.board);
    for (let i = 0; i < 10; i++) g.undo();
    while (g.canRedo()) g.redo();
    expect(state(g.board)).toBe(end);
  });

  it('medium undo stops at the reserve deal', () => {
    const g = Game.deal(MEDIUM, 11);
    playRandomly(g, 5, new JavaRandom(3));
    g.useReserve();
    expect(g.canUndo()).toBe(false);
    playRandomly(g, 5, new JavaRandom(4));
    while (g.canUndo()) g.undo();
    expect(g.history[g.history.length - 1].kind).toBe('reserve');
  });

  it('difficult never undoes, but the computer can', () => {
    const g = Game.deal(DIFFICULT, 12);
    const start = state(g.board);
    playRandomly(g, 30, new JavaRandom(6));
    g.useReserve();
    expect(g.canUndo()).toBe(false);
    expect(g.undo()).toBeNull();
    while (g.forceUndo()) {
      /* back to the start */
    }
    expect(state(g.board)).toBe(start);
  });
});

describe('save files', () => {
  it('round-trip board, level, deal, time and history', () => {
    const g = Game.deal(MEDIUM, 99);
    playRandomly(g, 25, new JavaRandom(8));
    g.useReserve();
    g.elapsedMs = 123456;
    const copy = fromText(toText(g));
    expect(copy.rule).toBe(MEDIUM);
    expect(copy.dealNumber).toBe(99);
    expect(copy.elapsedMs).toBe(123456);
    expect(state(copy.board)).toBe(state(g.board));
    while (g.forceUndo());
    while (copy.forceUndo());
    expect(state(copy.board)).toBe(state(g.board));
  });

  it('read and write the Java version’s files byte for byte', () => {
    const text = readFileSync(new URL('./fixtures/java-save.dat', import.meta.url), 'utf8');
    const g = fromText(text);
    expect(g.moveCount).toBe(31);
    expect(g.board.reserveUsed).toBe(true);
    expect(toText(g)).toBe(text);
    // The history in the file can be undone all the way back to the deal.
    while (g.forceUndo());
    expect(state(g.board)).toBe(state(Game.deal(MEDIUM, 424242).board));
  });

  it('reject garbage', () => {
    expect(() => fromText('hello\n')).toThrow();
  });
});
