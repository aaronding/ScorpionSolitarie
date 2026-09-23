import { Board, type Move, type Step } from './board';
import { type Rule, hints, isValidMove, isWon } from './rule';

/**
 * A game in progress: the board, the rule it is played under, and its history.
 * Every change goes through here so it can be checked and undone.
 */
export class Game {
  board = new Board();
  rule: Rule;
  /** The deal number, or 0 if unknown. */
  dealNumber = 0;
  elapsedMs = 0;
  private done: Step[] = [];
  private undone: Step[] = [];

  constructor(rule: Rule) {
    this.rule = rule;
  }

  /** Deals game number n; the same number always deals the same cards. */
  static deal(rule: Rule, n: number): Game {
    const game = new Game(rule);
    game.board.deal(n);
    game.dealNumber = n;
    return game;
  }

  move(move: Move): boolean {
    if (!isValidMove(this.rule, move, this.board)) return false;
    this.done.push(this.board.moveCards(move));
    this.undone = [];
    return true;
  }

  useReserve(): boolean {
    if (this.board.reserveUsed) return false;
    this.done.push(this.board.dealReserve());
    this.undone = [];
    return true;
  }

  canUndo(): boolean {
    const last = this.done[this.done.length - 1];
    return !!last && this.rule.mayUndo(last);
  }

  /** Takes back the last step if the rule allows it. */
  undo(): Step | null {
    return this.canUndo() ? this.undoLast() : null;
  }

  /** Takes back the last step whatever the rule says, for finishing a game the player gave up. */
  forceUndo(): Step | null {
    return this.done.length > 0 ? this.undoLast() : null;
  }

  private undoLast(): Step {
    const step = this.done.pop()!;
    if (step.kind === 'reserve') this.board.undealReserve();
    else this.board.unmoveCards(step);
    this.undone.push(step);
    return step;
  }

  canRedo(): boolean {
    return this.undone.length > 0;
  }

  redo(): Step | null {
    const step = this.undone.pop();
    if (!step) return null;
    const again = step.kind === 'reserve' ? this.board.dealReserve() : this.board.moveCards(step.move);
    this.done.push(again);
    return again;
  }

  isWon(): boolean {
    return isWon(this.board);
  }

  hints(): Move[] {
    return hints(this.rule, this.board);
  }

  /** No hint left and the reserve is gone. */
  noMovesLeft(): boolean {
    return this.board.reserveUsed && this.hints().length === 0;
  }

  get history(): readonly Step[] {
    return this.done;
  }

  get moveCount(): number {
    return this.done.length;
  }

  /** Replaces the history, for loading a saved game. */
  restoreHistory(steps: Step[]): void {
    this.done = [...steps];
    this.undone = [];
  }
}
