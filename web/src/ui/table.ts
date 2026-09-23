import type { Move } from '../model/board';
import { type Card, COLUMNS, KING, cardWords, rank, suit } from '../model/card';
import type { Game } from '../model/game';
import { isValidMove } from '../model/rule';
import { faceUrl } from './art';
import { type Insets, Layout, type Point } from './layout';

export interface TableListener {
  /** The player changed the game. */
  playerMoved(): void;
}

export const QUICK = 140;
export const NORMAL = 240;
export const SLOW = 340;

/** Added to a card's layer while it moves, to lift it over the rest. */
const LIFTED = 500;

interface Drag {
  column: number;
  row: number;
  pointerId: number;
  start: Point;
  dx: number;
  dy: number;
  started: boolean;
}

/**
 * The table: an element per card, positioned from a Layout. Moving a card is
 * just giving it a new position; CSS transitions glide it there and flip it
 * over. Dragging, tapping and double-clicking come in as pointer events.
 */
export class Table {
  private game!: Game;
  private lay!: Layout;
  private readonly cards: HTMLElement[] = [];
  private readonly slots: HTMLElement[] = [];
  private readonly reserveSlot: HTMLElement;
  private drag: Drag | null = null;
  private dropColumn = -1;
  private interactive = true;
  private lastTap = { card: -1, time: 0 };
  private hintTimer = 0;
  private movingTimer = 0;

  constructor(
    private readonly root: HTMLElement,
    private readonly listener: TableListener,
  ) {
    for (let c = 0; c < COLUMNS; c++) this.slots.push(this.element('slot', root));
    this.reserveSlot = this.element('slot reserve', root);
    this.reserveSlot.setAttribute('role', 'button');
    this.reserveSlot.setAttribute('aria-label', 'Deal the reserve');
    for (let card = 0; card < 52; card++) {
      const el = this.element('card', root);
      el.innerHTML = `<div class="inner"><img class="face" alt="" draggable="false" src="${faceUrl(card)}"><img class="back" alt="" draggable="false"></div>`;
      el.dataset.card = String(card);
      this.cards.push(el);
    }

    root.addEventListener('pointerdown', (e) => this.pointerDown(e));
    root.addEventListener('pointermove', (e) => this.pointerMove(e));
    root.addEventListener('pointerup', (e) => this.pointerUp(e));
    root.addEventListener('pointercancel', () => this.cancelDrag());
    root.addEventListener('contextmenu', (e) => e.preventDefault());
    new ResizeObserver(() => this.render(0)).observe(root);
  }

  setGame(game: Game): void {
    this.game = game;
    this.drag = null;
    this.clearHint();
    this.render(0);
  }

  setBack(url: string): void {
    for (const el of this.cards) (el.querySelector('.back') as HTMLImageElement).src = url;
  }

  setInteractive(on: boolean): void {
    this.interactive = on;
    this.root.classList.toggle('locked', !on);
    if (!on) this.cancelDrag();
  }

  // ---- drawing -------------------------------------------------------------

  /** Places every card; cards whose place changed glide there over `duration` ms. */
  render(duration = NORMAL): void {
    if (!this.game) return;
    const lay = (this.lay = this.layout());
    this.root.style.setProperty('--card-w', `${lay.cardW}px`);
    this.root.style.setProperty('--card-h', `${lay.cardH}px`);
    this.root.style.setProperty('--move-ms', `${duration}ms`);
    this.root.classList.toggle('instant', duration === 0);

    this.slots.forEach((el, c) => place(el, { x: lay.columnX[c], y: lay.top }));
    place(this.reserveSlot, lay.reserve);
    this.reserveSlot.classList.toggle('used', this.game.board.reserveUsed);

    // Stacking follows rows. Cards in flight are lifted above everything
    // until they land, keeping their row order among themselves.
    const moved: [HTMLElement, number][] = [];
    const show = (card: Card, at: Point, up: boolean, z: number, label: string) => {
      const el = this.cards[card];
      const was = el.style.transform;
      place(el, at);
      const flying = duration > 0 && !!was && was !== el.style.transform;
      if (flying) moved.push([el, z]);
      el.classList.toggle('down', !up);
      el.style.zIndex = String(flying ? LIFTED + z : z);
      el.setAttribute('aria-label', up ? label : 'Face-down card');
    };
    this.game.board.reserve.forEach((card, i) => show(card, lay.reserveCard(i), false, 1 + i, ''));
    this.game.board.columns.forEach((col, c) =>
      col.forEach((slot, row) => show(slot.card, lay.card(c, row), slot.up, 10 + row, cardWords(slot.card))),
    );

    clearTimeout(this.movingTimer);
    if (moved.length) {
      this.movingTimer = window.setTimeout(() => moved.forEach(([el, z]) => (el.style.zIndex = String(z))), duration + 60);
    }
  }

  private layout(): Layout {
    // The table's padding holds the safe-area insets (see style.css).
    const style = getComputedStyle(this.root);
    const insets: Insets = {
      left: parseFloat(style.paddingLeft) || 0,
      right: parseFloat(style.paddingRight) || 0,
      bottom: parseFloat(style.paddingBottom) || 0,
    };
    return new Layout(this.root.clientWidth, this.root.clientHeight, this.game.board, insets);
  }

  /** Applies a change to the game and animates the cards it moved. */
  animate(change: () => unknown, duration = NORMAL): void {
    change();
    this.render(duration);
  }

  /** Deals: every card flies out from the reserve corner, one after another. */
  animateDeal(): void {
    const lay = this.layout();
    this.root.classList.add('instant');
    for (const el of this.cards) {
      place(el, lay.reserve);
      el.classList.add('down');
    }
    void this.root.offsetWidth; // commit the starting positions before animating
    this.root.classList.remove('instant');
    let order = 0;
    for (let row = 0; row < 7; row++)
      for (let c = 0; c < COLUMNS; c++) {
        const slot = this.game.board.columns[c][row];
        if (slot) this.cards[slot.card].style.transitionDelay = `${order++ * 16}ms`;
      }
    this.render(280);
    window.setTimeout(() => this.cards.forEach((el) => (el.style.transitionDelay = '')), order * 16 + 400);
  }

  // ---- hints -----------------------------------------------------------------

  /** Flashes the cards to move, then where they go. */
  showHint(move: Move): void {
    this.clearHint();
    const col = this.game.board.columns[move.fromColumn];
    const from = col.slice(move.fromRow).map((s) => this.cards[s.card]);
    const target = this.game.board.last(move.toColumn);
    const to = target ? [this.cards[target.card]] : [this.slots[move.toColumn]];
    from.forEach((el) => el.classList.add('hint'));
    this.hintTimer = window.setTimeout(() => {
      from.forEach((el) => el.classList.remove('hint'));
      to.forEach((el) => el.classList.add('hint'));
      this.hintTimer = window.setTimeout(() => this.clearHint(), 650);
    }, 650);
  }

  showReserveHint(): void {
    this.clearHint();
    this.reserveSlot.classList.add('hint');
    this.hintTimer = window.setTimeout(() => this.clearHint(), 1200);
  }

  private clearHint(): void {
    clearTimeout(this.hintTimer);
    this.root.querySelectorAll('.hint').forEach((el) => el.classList.remove('hint'));
  }

  // ---- pointer ---------------------------------------------------------------

  private hit(e: PointerEvent): { column: number; row: number } | null {
    const el = (e.target as HTMLElement).closest<HTMLElement>('.card');
    if (!el) return null;
    const card = Number(el.dataset.card);
    for (let c = 0; c < COLUMNS; c++) {
      const row = this.game.board.columns[c].findIndex((s) => s.card === card);
      if (row >= 0) return { column: c, row };
    }
    return null;
  }

  private onReserve(e: PointerEvent): boolean {
    const t = e.target as HTMLElement;
    if (t.closest('.reserve')) return true;
    const el = t.closest<HTMLElement>('.card');
    return !!el && this.game.board.reserve.includes(Number(el.dataset.card));
  }

  private pointerDown(e: PointerEvent): void {
    if (!this.interactive || !this.game || e.button > 2) return;
    this.clearHint();
    if (this.onReserve(e)) {
      if (!this.game.board.reserveUsed && e.button === 0) {
        this.animate(() => this.game.useReserve(), SLOW);
        this.listener.playerMoved();
      }
      return;
    }
    const at = this.hit(e);
    if (!at) return;
    const slot = this.game.board.columns[at.column][at.row];
    if (!slot.up) return;
    if (e.button === 2) {
      this.peek(slot.card, true);
      return;
    }
    this.drag = { ...at, pointerId: e.pointerId, start: { x: e.clientX, y: e.clientY }, dx: 0, dy: 0, started: false };
    try {
      this.root.setPointerCapture(e.pointerId); // keep getting moves if the finger leaves the table
    } catch {
      // Not a live pointer (synthetic events); dragging still works inside the table.
    }
  }

  private pointerMove(e: PointerEvent): void {
    const d = this.drag;
    if (!d || e.pointerId !== d.pointerId) return;
    d.dx = e.clientX - d.start.x;
    d.dy = e.clientY - d.start.y;
    if (!d.started && Math.hypot(d.dx, d.dy) < 5) return;
    d.started = true;
    const col = this.game.board.columns[d.column];
    for (let r = d.row; r < col.length; r++) {
      const el = this.cards[col[r].card];
      const p = this.lay.card(d.column, r);
      el.classList.add('dragging');
      el.style.zIndex = String(LIFTED * 2 + r); // above everything, in row order
      place(el, { x: p.x + d.dx, y: p.y + d.dy });
    }
    this.setDropColumn(this.dropTarget());
  }

  private pointerUp(e: PointerEvent): void {
    this.peek(-1, false);
    const d = this.drag;
    if (!d || e.pointerId !== d.pointerId) return;
    const target = d.started ? this.dropTarget() : -1;
    this.drag = null;
    this.setDropColumn(-1);
    this.root.querySelectorAll('.dragging').forEach((el) => el.classList.remove('dragging'));
    if (!d.started) {
      this.tapped(d.column, d.row);
      return;
    }
    if (target >= 0 && this.game.move({ fromColumn: d.column, fromRow: d.row, toColumn: target })) {
      this.render(QUICK);
      this.listener.playerMoved();
    } else {
      this.render(NORMAL); // slide back
    }
  }

  private cancelDrag(): void {
    if (!this.drag) return;
    this.drag = null;
    this.setDropColumn(-1);
    this.root.querySelectorAll('.dragging').forEach((el) => el.classList.remove('dragging'));
    this.render(NORMAL);
  }

  /** Double-click or double-tap: send the card to its best place. */
  private tapped(column: number, row: number): void {
    const card = this.game.board.columns[column][row].card;
    const now = performance.now();
    const double = this.lastTap.card === card && now - this.lastTap.time < 400;
    this.lastTap = { card, time: double ? 0 : now };
    if (!double) return;
    const best = bestMove(this.game, column, row);
    if (best) {
      this.animate(() => this.game.move(best));
      this.listener.playerMoved();
    }
  }

  /** Right button held on a partly covered card: show all of it. */
  private peek(card: Card, on: boolean): void {
    this.root.querySelectorAll('.peek').forEach((el) => el.classList.remove('peek'));
    if (on) this.cards[card].classList.add('peek');
  }

  /** The legal column the dragged cards overlap most, or -1. */
  private dropTarget(): number {
    const d = this.drag;
    if (!d) return -1;
    const lay = this.lay;
    const held = lay.card(d.column, d.row);
    const x = held.x + d.dx;
    const y = held.y + d.dy;
    let best = -1;
    let bestArea = 0;
    for (let c = 0; c < COLUMNS; c++) {
      if (c === d.column || !isValidMove(this.game.rule, { fromColumn: d.column, fromRow: d.row, toColumn: c }, this.game.board))
        continue;
      const t = lay.target(c);
      const w = Math.min(x, t.x) + lay.cardW - Math.max(x, t.x);
      const h = Math.min(y, t.y) + lay.cardH - Math.max(y, t.y);
      const area = w > 0 && h > 0 ? w * h : 0;
      if (area > bestArea) {
        bestArea = area;
        best = c;
      }
    }
    return best;
  }

  private setDropColumn(c: number): void {
    if (c === this.dropColumn) return;
    this.root.querySelectorAll('.drop').forEach((el) => el.classList.remove('drop'));
    this.dropColumn = c;
    if (c < 0) return;
    const last = this.game.board.last(c);
    (last ? this.cards[last.card] : this.slots[c]).classList.add('drop');
  }

  private element(className: string, parent: HTMLElement): HTMLElement {
    const el = document.createElement('div');
    el.className = className;
    parent.appendChild(el);
    return el;
  }
}

function place(el: HTMLElement, p: Point): void {
  el.style.transform = `translate(${Math.round(p.x * 2) / 2}px, ${Math.round(p.y * 2) / 2}px)`;
}

/**
 * Where a double-clicked card should go: onto the next card up in its own
 * suit if possible, then to an empty column if it is a King, then anywhere legal.
 */
export function bestMove(game: Game, column: number, row: number): Move | null {
  const card = game.board.columns[column][row].card;
  let fallback: Move | null = null;
  let toEmpty: Move | null = null;
  for (let c = 0; c < COLUMNS; c++) {
    const m = { fromColumn: column, fromRow: row, toColumn: c };
    if (!isValidMove(game.rule, m, game.board)) continue;
    const onto = game.board.last(c);
    if (!onto) {
      if (row > 0 && rank(card) === KING && !toEmpty) toEmpty = m;
    } else if (suit(onto.card) === suit(card)) {
      return m;
    } else if (!fallback) {
      fallback = m;
    }
  }
  return toEmpty ?? fallback;
}
