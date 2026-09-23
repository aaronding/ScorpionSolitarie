import { readFileSync } from 'node:fs';
import { gunzipSync } from 'node:zlib';
import { describe, expect, it } from 'vitest';

import { Game } from '../src/model/game';
import { JavaRandom } from '../src/model/random';
import { ruleFor } from '../src/model/rule';
import { legalMoves, state } from './helpers';

/**
 * Plays the same 30 random games as the Java RuleGoldenTest and compares every
 * board, legal move, hint and win check with its recording. This proves the
 * port deals the same cards for a deal number and plays by the same rules.
 */
const RECORDING = new URL('../../test/com/family/solitaire/model/rules-golden.txt.gz', import.meta.url);

function play(games: number): string[] {
  const out: string[] = [];
  const levels = ['easy', 'medium', 'difficult'];
  for (let seed = 1; seed <= games; seed++) {
    const level = levels[seed % 3];
    const r = new JavaRandom(seed);
    for (let i = 51; i > 0; i--) r.nextInt(i + 1); // the recording drew its shuffle from this generator
    const g = Game.deal(ruleFor(level), seed);
    out.push(`GAME ${seed} ${level}`);
    for (let step = 0; step < 400; step++) {
      out.push('S ' + state(g.board));
      const moves = legalMoves(g);
      out.push('V ' + moves.map((m) => `${m.fromColumn}.${m.fromRow}>${m.toColumn}`).join(' '));
      const hints = g.hints().map((m) => `${m.fromColumn}.${m.fromRow}>${m.toColumn}`).sort();
      out.push('H ' + (hints.length === 0 ? 'null' : hints.join(' ')));
      out.push('W ' + g.isWon());
      const reserveLeft = !g.board.reserveUsed;
      if (moves.length === 0 && !reserveLeft) break;
      if (reserveLeft && (moves.length === 0 || r.nextInt(25) === 0)) {
        g.useReserve();
        out.push('R');
        continue;
      }
      const m = moves[r.nextInt(moves.length)];
      g.move(m);
      out.push(`M ${m.fromColumn}.${m.fromRow}>${m.toColumn}`);
    }
  }
  return out;
}

describe('rules', () => {
  it('match the recording from the Java version', () => {
    const expected = gunzipSync(readFileSync(RECORDING)).toString('utf8').trimEnd().split('\n');
    const actual = play(30);
    for (let i = 0; i < expected.length; i++) {
      if (actual[i] !== expected[i]) expect(actual[i], `line ${i + 1}`).toBe(expected[i]);
    }
    expect(actual.length).toBe(expected.length);
  });

  it('reproduce java.util.Random', () => {
    // Values printed by Java: new Random(42), then new Random(-7)
    const r = new JavaRandom(42);
    expect([0, 1, 2, 3, 4].map(() => r.nextInt(100))).toEqual([30, 63, 48, 84, 70]);
    expect(r.nextInt(1 << 20)).toBe(987835);
    expect(r.nextInt(999999)).toBe(22100);
    const q = new JavaRandom(-7);
    expect([q.nextInt(52), q.nextInt(7)]).toEqual([6, 0]);
  });
});
