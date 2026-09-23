import { COLUMNS, RESERVE_SIZE } from './card';
import type { Step } from './board';
import { Game } from './game';
import { ruleFor } from './rule';

/*
 * The same text format as the Java version's save files:
 *
 *   level
 *   reserve cards, or 99 once dealt
 *   one line per column, top card first
 *   deal    number          (optional)
 *   time    milliseconds    (optional)
 *   steps   history         (optional)
 *
 * Card values are 0-51, negative (-value-1) when face down; fields are tab-separated.
 */

export function toText(game: Game): string {
  const b = game.board;
  const lines: string[] = [game.rule.level];
  const reserve = b.reserveUsed ? [99, 99, 99] : b.reserve.map((c) => -c - 1);
  lines.push(reserve.map((v) => v + '\t').join(''));
  for (const col of b.columns) lines.push(col.map((s) => (s.up ? s.card : -s.card - 1) + '\t').join(''));
  lines.push(`deal\t${game.dealNumber}`);
  lines.push(`time\t${Math.round(game.elapsedMs)}`);
  const steps = game.history.map((s) =>
    s.kind === 'reserve'
      ? 'r'
      : `m,${s.move.fromColumn},${s.move.fromRow},${s.move.toColumn},${s.count},${s.flipped ? 1 : 0}`,
  );
  lines.push(['steps', ...steps].join('\t'));
  return lines.join('\n') + '\n';
}

export function fromText(text: string): Game {
  const lines = text.split(/\r?\n/);
  const int = (s: string): number => {
    if (!/^-?\d+$/.test(s.trim())) throw new Error('Not a saved game');
    return Number(s);
  };
  const game = new Game(ruleFor(lines[0] ?? ''));
  const b = game.board;

  const reserve = (lines[1] ?? '').split('\t').slice(0, RESERVE_SIZE).map(int);
  if (reserve.length !== RESERVE_SIZE) throw new Error('Not a saved game');
  b.reserve = reserve[0] === 99 ? [] : reserve.map((v) => (v < 0 ? -v - 1 : v));

  for (let c = 0; c < COLUMNS; c++) {
    if (lines[2 + c] === undefined) throw new Error('Not a saved game');
    b.columns[c] = lines[2 + c]
      .split('\t')
      .filter((f) => f.length > 0)
      .map(int)
      .map((v) => (v < 0 ? { card: -v - 1, up: false } : { card: v, up: true }));
  }

  const steps: Step[] = [];
  for (const line of lines.slice(2 + COLUMNS)) {
    const fields = line.split('\t');
    if (fields[0] === 'deal') game.dealNumber = int(fields[1]);
    else if (fields[0] === 'time') game.elapsedMs = int(fields[1]);
    else if (fields[0] === 'steps') {
      for (const f of fields.slice(1)) {
        if (f === 'r') {
          steps.push({ kind: 'reserve' });
        } else {
          const p = f.split(',');
          steps.push({
            kind: 'move',
            move: { fromColumn: int(p[1]), fromRow: int(p[2]), toColumn: int(p[3]) },
            count: int(p[4]),
            flipped: p[5] === '1',
          });
        }
      }
    }
  }
  game.restoreHistory(steps);
  return game;
}
