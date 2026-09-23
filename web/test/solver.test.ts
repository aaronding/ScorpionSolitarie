import { describe, expect, it } from 'vitest';

import { Game } from '../src/model/game';
import { JavaRandom } from '../src/model/random';
import { DIFFICULT, RULES } from '../src/model/rule';
import { fromText } from '../src/model/saveFile';
import { check, finish, winnableDeal } from '../src/solver/dealer';
import { DEAL_RESERVE, type Play, Solver } from '../src/solver/solver';
import { legalMoves } from './helpers';

function play(game: Game, plays: Play[]): void {
  for (const p of plays) expect(p === DEAL_RESERVE ? game.useReserve() : game.move(p), JSON.stringify(p)).toBe(true);
}

/** A seeded stand-in for Math.random. */
const seeded = (seed: number) => {
  const r = new JavaRandom(seed);
  return () => r.nextInt(1 << 30) / (1 << 30);
};

// The solver is the slow part of the suite; give its tests room.
describe('solver', { timeout: 60000 }, () => {
  it('finds solutions that really win', () => {
    for (const rule of RULES) {
      const solver = new Solver(rule, 200000);
      let solved = 0;
      for (let n = 1; n <= 12; n++) {
        const game = Game.deal(rule, n);
        const result = solver.solve(game.board);
        if (result.outcome !== 'solved') continue;
        solved++;
        play(game, result.solution);
        expect(game.isWon(), `${rule.level} deal ${n}`).toBe(true);
      }
      expect(solved, rule.level).toBeGreaterThan(0);
    }
  });

  it('deals only winnable games', () => {
    for (const rule of RULES) expect(check(rule, winnableDeal(rule, seeded(3)))).toBe('solved');
  });

  it('finishes a game from wherever the player left off', () => {
    const random = new JavaRandom(11);
    for (const rule of RULES) {
      const n = winnableDeal(rule, seeded(rule.level.length));
      const game = Game.deal(rule, n);
      for (let i = 0; i < 40; i++) {
        const moves = legalMoves(game);
        if (moves.length === 0) break;
        game.move(moves[random.nextInt(moves.length)]);
      }
      const plan = finish(game);
      expect(plan, `${rule.level} game ${n}`).not.toBeNull();
      for (let i = 0; i < plan!.undo; i++) expect(game.forceUndo()).not.toBeNull();
      play(game, plan!.plays);
      expect(game.isWon()).toBe(true);
    }
  });

  it('proves a dead end', () => {
    // Reserve gone; the Ace of hearts is stuck between the 4 and the 3.
    const game = fromText(
      'difficult\n99\t99\t99\t\n' +
        '12\t11\t10\t9\t8\t7\t6\t5\t4\t3\t2\t1\t0\t\n' +
        '25\t24\t23\t22\t21\t20\t19\t18\t17\t16\t13\t15\t14\t\n' +
        '38\t37\t36\t35\t34\t33\t32\t31\t30\t29\t28\t27\t26\t\n' +
        '51\t50\t49\t48\t47\t46\t45\t44\t43\t42\t41\t40\t39\t\n\n\n\n',
    );
    expect(new Solver(DIFFICULT, 1000).solve(game.board).outcome).toBe('unsolvable');
  });
});
