import type { Board, Move } from '../model/board';
import { CARDS, CARDS_IN_SUIT, COLUMNS, RESERVE_SIZE } from '../model/card';
import type { Rule } from '../model/rule';

/** Stands for dealing the reserve in a solution. */
export const DEAL_RESERVE = 'deal' as const;
export type Play = Move | typeof DEAL_RESERVE;

export type Outcome = 'solved' | 'unsolvable' | 'gave-up';

export interface Result {
  outcome: Outcome;
  /** The plays to win, in order; empty unless solved. */
  solution: Play[];
  /** Positions examined. */
  positions: number;
}

const DOWN = 64;
const CARD = 63;
const ROWS = 64; // column stride in cols
const DEAL = -1;
/** Bytes per stored position: reserve flag, 7 column lengths, up to 52 cards. */
const STRIDE = 1 + COLUMNS + CARDS;

const move = (from: number, row: number, to: number): number => (from << 12) | (row << 6) | to;
const fromOf = (m: number): number => m >> 12;
const rowOf = (m: number): number => (m >> 6) & 63;
const toOf = (m: number): number => m & 7;
/** The King of each card's suit, by card (face-down values map past the end). */
const TOP = new Uint8Array(128).fill(255);
for (let c = 0; c < CARDS; c++) TOP[c] = Math.floor(c / CARDS_IN_SUIT) * CARDS_IN_SUIT + CARDS_IN_SUIT - 1;

// Zobrist tables, two 32-bit lanes, from a fixed seed so runs are repeatable.
const ZOBRIST_A = new Int32Array((CARDS + 1) * 128);
const ZOBRIST_B = new Int32Array((CARDS + 1) * 128);
{
  let s = 0x5c0e910;
  const next = (): number => {
    s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return t ^ (t >>> 14);
  };
  for (let i = 0; i < ZOBRIST_A.length; i++) {
    ZOBRIST_A[i] = next();
    ZOBRIST_B[i] = next();
  }
}

const mix = (h: number): number => {
  h = Math.imul(h ^ (h >>> 16), 0x85ebca6b);
  h = Math.imul(h ^ (h >>> 13), 0xc2b2ae35);
  return h ^ (h >>> 16);
};

/**
 * Finds a way to win from a position, knowing where every card is, face down
 * or not. The same search as the Java version's Solver:
 *
 * A best-first search: it always continues from the most promising position
 * seen so far (fewest cards face down, most cards already on their same-suit
 * neighbour), never visits a position twice, and gives up after a set number
 * of positions. Columns the rules treat alike count as the same position.
 *
 * The only move it never tries is taking a card off its same-suit neighbour
 * (say 5♥ off 6♥). That move is never needed: whatever card would use the
 * uncovered 6♥ can go where the 5♥ would have gone instead. So a search that
 * ends before the limit proves the position cannot be won.
 */
export class Solver {
  private readonly followers: number[][] = [];
  private cancelled = false;

  // The position being looked at
  private readonly cols = new Uint8Array(COLUMNS * ROWS);
  private readonly len = new Int32Array(COLUMNS);
  private readonly where = new Int32Array(CARDS); // column*64 + row of face-up cards, else -1
  private readonly reserve = new Int32Array(RESERVE_SIZE);
  private reserveUsed = false;
  private readonly moves = new Int32Array(COLUMNS * 8 + 1);

  // Positions seen: stored states, how each was reached, and a priority queue
  private states = new Uint8Array(0);
  private parent = new Int32Array(0);
  private via = new Int32Array(0);
  private depth = new Int32Array(0);
  private nodes = 0;
  private heap = new Float64Array(1024);
  private heapSize = 0;
  private seenA = new Int32Array(0);
  private seenB = new Int32Array(0);

  constructor(
    rule: Rule,
    private readonly limit: number,
  ) {
    for (let onto = 0; onto < CARDS; onto++) {
      const list: number[] = [];
      for (let card = 0; card < CARDS; card++) if (rule.canStack(card, onto)) list.push(card);
      this.followers.push(list);
    }
  }

  /** Stops the search; it then reports 'gave-up'. */
  cancel(): void {
    this.cancelled = true;
  }

  solve(board: Board): Result {
    this.load(board);
    const capacity = 1 << Math.ceil(Math.log2(Math.max(1024, this.limit) * 4));
    this.seenA = new Int32Array(capacity);
    this.seenB = new Int32Array(capacity);
    this.states = new Uint8Array(1024 * STRIDE);
    this.parent = new Int32Array(1024);
    this.via = new Int32Array(1024);
    this.depth = new Int32Array(1024);
    this.nodes = 0;
    this.heapSize = 0;

    if (this.isWon()) return { outcome: 'solved', solution: [], positions: 0 };
    this.markSeen();
    this.push(-1, 0);

    while (this.heapSize > 0) {
      if (this.nodes >= this.limit || this.cancelled) return { outcome: 'gave-up', solution: [], positions: this.nodes };
      const node = this.pop();
      this.decode(node);
      const n = this.generate();
      for (let k = 0; k < n; k++) {
        const m = this.moves[k];
        let carried = 0;
        let flipped = false;
        if (m === DEAL) {
          this.dealReserve();
        } else {
          carried = this.len[fromOf(m)] - rowOf(m);
          flipped = this.apply(fromOf(m), rowOf(m), toOf(m));
        }
        if (this.isWon()) return { outcome: 'solved', solution: this.pathTo(node, m), positions: this.nodes };
        if (this.markSeen()) this.push(node, m);
        if (m === DEAL) this.undealReserve();
        else this.unapply(fromOf(m), toOf(m), carried, flipped);
      }
    }
    return { outcome: this.cancelled ? 'gave-up' : 'unsolvable', solution: [], positions: this.nodes };
  }

  private pathTo(node: number, last: number): Play[] {
    const toPlay = (m: number): Play =>
      m === DEAL ? DEAL_RESERVE : { fromColumn: fromOf(m), fromRow: rowOf(m), toColumn: toOf(m) };
    const path = [toPlay(last)];
    for (let i = node; this.parent[i] >= 0; i = this.parent[i]) path.push(toPlay(this.via[i]));
    return path.reverse();
  }

  /** Every legal move from the current position; returns how many. */
  private generate(): number {
    const { cols, len, where, moves } = this;
    let n = 0;
    let sawSymmetricEmpty = false;
    for (let to = 0; to < COLUMNS; to++) {
      if (len[to] === 0) {
        // Empty columns the rules treat alike are one target.
        if (this.reserveUsed || to >= RESERVE_SIZE) {
          if (sawSymmetricEmpty) continue;
          sawSymmetricEmpty = true;
        }
        for (let s = 0; s < 4; s++) {
          const loc = where[s * CARDS_IN_SUIT + CARDS_IN_SUIT - 1];
          if (loc < 0) continue;
          const from = loc >> 6;
          const row = loc & 63;
          if (from === to) continue;
          // A King already at the top gains nothing by moving, unless the
          // move changes which columns the reserve will land on.
          if (row === 0 && (this.reserveUsed || (from >= RESERVE_SIZE && to >= RESERVE_SIZE))) continue;
          moves[n++] = move(from, row, to);
        }
      } else {
        for (const card of this.followers[cols[to * ROWS + len[to] - 1]]) {
          const loc = where[card];
          if (loc < 0 || loc >> 6 === to) continue;
          const from = loc >> 6;
          const row = loc & 63;
          // Already on its same-suit neighbour; see class comment.
          if (row > 0 && cols[from * ROWS + row - 1] - card === 1 && card % CARDS_IN_SUIT !== CARDS_IN_SUIT - 1) continue;
          moves[n++] = move(from, row, to);
        }
      }
    }
    if (!this.reserveUsed) moves[n++] = DEAL;
    return n;
  }

  private load(board: Board): void {
    for (let c = 0; c < COLUMNS; c++) {
      const col = board.columns[c];
      this.len[c] = col.length;
      for (let r = 0; r < col.length; r++) this.cols[c * ROWS + r] = col[r].card | (col[r].up ? 0 : DOWN);
    }
    this.reserveUsed = board.reserveUsed;
    for (let i = 0; i < RESERVE_SIZE; i++) this.reserve[i] = board.reserveUsed ? -1 : board.reserve[i];
    this.indexCards();
  }

  private indexCards(): void {
    this.where.fill(-1);
    for (let c = 0; c < COLUMNS; c++)
      for (let r = 0; r < this.len[c]; r++) {
        const v = this.cols[c * ROWS + r];
        if ((v & DOWN) === 0) this.where[v] = c * 64 + r;
      }
  }

  private apply(from: number, row: number, to: number): boolean {
    const { cols, len, where } = this;
    for (let r = row; r < len[from]; r++) {
      const card = cols[from * ROWS + r];
      cols[to * ROWS + len[to]] = card;
      where[card] = to * 64 + len[to];
      len[to]++;
    }
    len[from] = row;
    if (row > 0 && (cols[from * ROWS + row - 1] & DOWN) !== 0) {
      const card = cols[from * ROWS + row - 1] & CARD;
      cols[from * ROWS + row - 1] = card;
      where[card] = from * 64 + row - 1;
      return true;
    }
    return false;
  }

  private unapply(from: number, to: number, count: number, flipped: boolean): void {
    const { cols, len, where } = this;
    if (flipped) {
      const i = from * ROWS + len[from] - 1;
      where[cols[i]] = -1;
      cols[i] |= DOWN;
    }
    for (let k = len[to] - count; k < len[to]; k++) {
      const card = cols[to * ROWS + k];
      cols[from * ROWS + len[from]] = card;
      where[card] = from * 64 + len[from];
      len[from]++;
    }
    len[to] -= count;
  }

  private dealReserve(): void {
    for (let i = 0; i < RESERVE_SIZE; i++) {
      this.cols[i * ROWS + this.len[i]] = this.reserve[i];
      this.where[this.reserve[i]] = i * 64 + this.len[i];
      this.len[i]++;
    }
    this.reserveUsed = true;
  }

  private undealReserve(): void {
    for (let i = 0; i < RESERVE_SIZE; i++) {
      this.len[i]--;
      this.where[this.reserve[i]] = -1;
    }
    this.reserveUsed = false;
  }

  private isWon(): boolean {
    if (!this.reserveUsed) return false;
    for (let c = 0; c < COLUMNS; c++) {
      const n = this.len[c];
      if (n === 0) continue;
      const base = c * ROWS;
      if (n !== CARDS_IN_SUIT || this.cols[base] !== TOP[this.cols[base]]) return false;
      for (let r = 1; r < CARDS_IN_SUIT; r++) if (this.cols[base + r - 1] - this.cols[base + r] !== 1) return false;
    }
    return true;
  }

  /**
   * How far from won, lower is better: face-down cards and the reserve count
   * most, then every card not yet resting on its same-suit neighbour.
   */
  private distance(): number {
    let d = this.reserveUsed ? 0 : 3 * 4;
    for (let c = 0; c < COLUMNS; c++) {
      const base = c * ROWS;
      for (let r = 0; r < this.len[c]; r++) {
        const card = this.cols[base + r];
        if ((card & DOWN) !== 0) d += 4;
        else if (r === 0) {
          if (card !== TOP[card]) d += 1;
        } else if (this.cols[base + r - 1] - card !== 1 || card % CARDS_IN_SUIT === CARDS_IN_SUIT - 1) d += 1;
      }
    }
    return d;
  }

  // ---- stored positions and the queue ---------------------------------------

  private push(from: number, m: number): void {
    if (this.nodes === this.parent.length) {
      const n = this.nodes * 2;
      const grow = <T extends Int32Array | Uint8Array>(a: T, size: number): T => {
        const b = new (a.constructor as new (n: number) => T)(size);
        b.set(a);
        return b;
      };
      this.states = grow(this.states, n * STRIDE);
      this.parent = grow(this.parent, n);
      this.via = grow(this.via, n);
      this.depth = grow(this.depth, n);
    }
    const node = this.nodes++;
    this.encode(node);
    this.parent[node] = from;
    this.via[node] = m;
    this.depth[node] = from < 0 ? 0 : this.depth[from] + 1;
    // Mostly greedy; the small weight on depth keeps solutions short.
    this.heapAdd((this.distance() * 8 + this.depth[node]) * 4194304 + node);
  }

  private encode(node: number): void {
    let i = node * STRIDE;
    const s = this.states;
    s[i++] = this.reserveUsed ? 1 : 0;
    for (let c = 0; c < COLUMNS; c++) s[i++] = this.len[c];
    for (let c = 0; c < COLUMNS; c++) {
      const base = c * ROWS;
      for (let r = 0; r < this.len[c]; r++) s[i++] = this.cols[base + r];
    }
  }

  private decode(node: number): void {
    let i = node * STRIDE;
    const s = this.states;
    this.reserveUsed = s[i++] === 1;
    for (let c = 0; c < COLUMNS; c++) this.len[c] = s[i++];
    const where = this.where;
    where.fill(-1);
    for (let c = 0; c < COLUMNS; c++) {
      const base = c * ROWS;
      for (let r = 0; r < this.len[c]; r++) {
        const v = s[i++];
        this.cols[base + r] = v;
        if ((v & DOWN) === 0) where[v] = c * 64 + r;
      }
    }
  }

  private heapAdd(v: number): void {
    if (this.heapSize === this.heap.length) {
      const bigger = new Float64Array(this.heap.length * 2);
      bigger.set(this.heap);
      this.heap = bigger;
    }
    const heap = this.heap;
    let i = this.heapSize++;
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (heap[p] <= v) break;
      heap[i] = heap[p];
      i = p;
    }
    heap[i] = v;
  }

  private pop(): number {
    const heap = this.heap;
    const top = heap[0];
    const last = heap[--this.heapSize];
    let i = 0;
    for (;;) {
      let child = 2 * i + 1;
      if (child >= this.heapSize) break;
      if (child + 1 < this.heapSize && heap[child + 1] < heap[child]) child++;
      if (heap[child] >= last) break;
      heap[i] = heap[child];
      i = child;
    }
    heap[i] = last;
    return top % 4194304;
  }

  // ---- positions seen: 64-bit hashes in two 32-bit lanes -------------------

  private readonly colA = new Int32Array(COLUMNS);
  private readonly colB = new Int32Array(COLUMNS);
  private readonly order = new Int32Array(COLUMNS);

  /** Adds the current position to the seen set; false if it was already there. */
  private markSeen(): boolean {
    const { cols, len, colA, colB, order } = this;
    for (let c = 0; c < COLUMNS; c++) {
      let a = Math.imul(len[c] + 1, 0x9e3779b1);
      let b = Math.imul(len[c] + 1, 0x7f4a7c15);
      for (let r = 0; r < len[c]; r++) {
        const z = r * 128 + (cols[c * ROWS + r] & 127);
        a ^= ZOBRIST_A[z];
        b ^= ZOBRIST_B[z];
      }
      colA[c] = a;
      colB[c] = b;
      order[c] = c;
    }
    // Once the reserve is gone the columns are interchangeable; before that
    // only the last four are, since the reserve lands on the first three.
    const first = this.reserveUsed ? 0 : RESERVE_SIZE;
    for (let i = first + 1; i < COLUMNS; i++) {
      const o = order[i];
      let j = i - 1;
      while (j >= first && (colA[order[j]] > colA[o] || (colA[order[j]] === colA[o] && colB[order[j]] > colB[o]))) {
        order[j + 1] = order[j];
        j--;
      }
      order[j + 1] = o;
    }
    let a = this.reserveUsed ? 1 : 2;
    let b = this.reserveUsed ? 3 : 4;
    for (let i = 0; i < COLUMNS; i++) {
      a = mix(Math.imul(a, 31) + colA[order[i]]);
      b = mix(Math.imul(b, 37) + colB[order[i]]);
    }
    a |= 1; // 0 marks an empty slot

    const mask = this.seenA.length - 1;
    let i = (a ^ Math.imul(b, 0x27d4eb2d)) & mask;
    while (this.seenA[i] !== 0) {
      if (this.seenA[i] === a && this.seenB[i] === b) return false;
      i = (i + 1) & mask;
    }
    this.seenA[i] = a;
    this.seenB[i] = b;
    return true;
  }
}
