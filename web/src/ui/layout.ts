import type { Board } from '../model/board';
import { COLUMNS } from '../model/card';

/** Height over width of every card. */
export const ASPECT = 333 / 234;

export interface Point {
  x: number;
  y: number;
}

/** Space to keep clear at each edge, such as a phone's notch. */
export interface Insets {
  left: number;
  right: number;
  bottom: number;
}

const NO_INSETS: Insets = { left: 0, right: 0, bottom: 0 };

/**
 * Where everything goes on a table of the given size. Wide tables put the
 * reserve to the left of the seven columns, as the desktop game does; tall
 * ones (phones held upright) put it above them, so the columns get the full
 * width.
 */
export class Layout {
  readonly cardW: number;
  readonly cardH: number;
  readonly reserve: Point;
  readonly columnX: number[] = [];
  readonly top: number;
  private readonly downStep: number[] = [];
  private readonly upStep: number[] = [];

  constructor(
    fullWidth: number,
    height: number,
    private readonly board: Board,
    insets: Insets = NO_INSETS,
  ) {
    const width = fullWidth - insets.left - insets.right;
    height -= insets.bottom;
    const margin = Math.max(8, Math.min(width, height) * 0.02);
    const gapRatio = 0.14;
    const portrait = height > width * 1.1;
    let cardW: number;
    let x0: number;
    if (portrait) {
      cardW = (width - 2 * margin) / (COLUMNS + (COLUMNS - 1) * gapRatio);
      cardW = Math.min(cardW, (height - 2 * margin) / (ASPECT * 3.6));
      const gap = cardW * gapRatio;
      x0 = insets.left + (width - (COLUMNS * cardW + (COLUMNS - 1) * gap)) / 2;
      for (let c = 0; c < COLUMNS; c++) this.columnX.push(x0 + c * (cardW + gap));
      this.reserve = { x: x0, y: margin };
      this.top = margin + cardW * ASPECT + gap * 1.5;
    } else {
      cardW = (width - 2 * margin) / (8 + COLUMNS * gapRatio + 0.35);
      cardW = Math.min(cardW, (height - 2 * margin) / (ASPECT * 2.6));
      const gap = cardW * gapRatio;
      x0 = insets.left + (width - (8 * cardW + COLUMNS * gap + cardW * 0.35)) / 2;
      for (let c = 0; c < COLUMNS; c++) this.columnX.push(x0 + cardW * 1.35 + gap + c * (cardW + gap));
      this.reserve = { x: x0, y: margin };
      this.top = margin;
    }
    this.cardW = cardW;
    this.cardH = cardW * ASPECT;

    // Face-up cards squeeze together when a column would run off the bottom.
    const bottom = height - margin;
    for (let c = 0; c < COLUMNS; c++) {
      const col = board.columns[c];
      const down = board.faceDownCount(c);
      const up = col.length - down;
      this.downStep.push(this.cardH * 0.1);
      let step = this.cardH * 0.25;
      if (up > 1) {
        const room = bottom - this.top - this.cardH - down * this.downStep[c];
        step = Math.max(this.cardH * 0.08, Math.min(step, room / (up - 1)));
      }
      this.upStep.push(step);
    }
  }

  /** Top-left corner of the card at row of column c. */
  card(c: number, row: number): Point {
    const col = this.board.columns[c];
    let y = this.top;
    for (let r = 0; r < row; r++) y += col[r].up ? this.upStep[c] : this.downStep[c];
    return { x: this.columnX[c], y };
  }

  /** Where a reserve card sits: the three are fanned slightly. */
  reserveCard(i: number): Point {
    const off = i * this.cardW * 0.035;
    return { x: this.reserve.x + off, y: this.reserve.y + off };
  }

  /** The spot a card dropped on column c would land on: its last card, or the empty slot. */
  target(c: number): Point {
    const n = this.board.columns[c].length;
    return n === 0 ? { x: this.columnX[c], y: this.top } : this.card(c, n - 1);
  }
}
