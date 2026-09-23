import { type Card, CARDS, COLUMNS, RESERVE_SIZE } from './card';
import { JavaRandom } from './random';

/** A card on the table and which side is up. */
export interface Slot {
  card: Card;
  up: boolean;
}

/** Moves the card at fromRow of fromColumn, and every card below it, onto toColumn. */
export interface Move {
  fromColumn: number;
  fromRow: number;
  toColumn: number;
}

/** One entry in a game's history, with what is needed to take it back. */
export type Step =
  | { kind: 'move'; move: Move; count: number; flipped: boolean }
  | { kind: 'reserve' };

/**
 * The seven columns (top card first) and the reserve. Applies and reverses
 * steps without checking the rules; Game does that.
 */
export class Board {
  readonly columns: Slot[][] = Array.from({ length: COLUMNS }, () => []);
  /** The three reserve cards, or empty once dealt. */
  reserve: Card[] = [];

  get reserveUsed(): boolean {
    return this.reserve.length === 0;
  }

  /** Deals game number n: seven cards to each column, then three to the reserve. */
  deal(n: number): void {
    const deck = Array.from({ length: CARDS }, (_, i) => i);
    const random = new JavaRandom(n);
    for (let i = CARDS - 1; i > 0; i--) {
      const j = random.nextInt(i + 1);
      [deck[i], deck[j]] = [deck[j], deck[i]];
    }
    for (let c = 0; c < COLUMNS; c++) {
      this.columns[c] = [];
      for (let r = 0; r < 7; r++) this.columns[c].push({ card: deck[c * 7 + r], up: !(c < 4 && r < 3) });
    }
    this.reserve = deck.slice(49, 49 + RESERVE_SIZE);
  }

  faceDownCount(column: number): number {
    const col = this.columns[column];
    let n = 0;
    while (n < col.length && !col[n].up) n++;
    return n;
  }

  last(column: number): Slot | undefined {
    const col = this.columns[column];
    return col[col.length - 1];
  }

  /** Where a face-up card lies, or null. */
  findFaceUp(card: Card): { column: number; row: number } | null {
    for (let c = 0; c < COLUMNS; c++) {
      const row = this.columns[c].findIndex((s) => s.card === card);
      if (row >= 0 && this.columns[c][row].up) return { column: c, row };
    }
    return null;
  }

  moveCards(move: Move): Step {
    const from = this.columns[move.fromColumn];
    const cards = from.splice(move.fromRow);
    this.columns[move.toColumn].push(...cards);
    const last = from[from.length - 1];
    const flipped = !!last && !last.up;
    if (flipped) last.up = true;
    return { kind: 'move', move, count: cards.length, flipped };
  }

  unmoveCards(step: Extract<Step, { kind: 'move' }>): void {
    const from = this.columns[step.move.fromColumn];
    if (step.flipped) from[from.length - 1].up = false;
    const to = this.columns[step.move.toColumn];
    from.push(...to.splice(to.length - step.count));
  }

  /** Turns the reserve cards up onto the first three columns. */
  dealReserve(): Step {
    this.reserve.forEach((card, i) => this.columns[i].push({ card, up: true }));
    this.reserve = [];
    return { kind: 'reserve' };
  }

  undealReserve(): void {
    this.reserve = [];
    for (let i = 0; i < RESERVE_SIZE; i++) this.reserve.push(this.columns[i].pop()!.card);
  }

  clone(): Board {
    const b = new Board();
    this.columns.forEach((col, c) => (b.columns[c] = col.map((s) => ({ ...s }))));
    b.reserve = [...this.reserve];
    return b;
  }
}
