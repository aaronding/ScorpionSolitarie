import type { Level } from '../model/rule';
import type { Plan } from '../solver/dealer';
import type { Outcome } from '../solver/solver';
import type { Request } from '../solver/worker';

type Pending = Map<number, (result: unknown) => void>;
/** A request without its id; spelled out per kind so each keeps its fields. */
type Ask = Request extends infer R ? (R extends Request ? Omit<R, 'id'> : never) : never;

function start(pending: Pending): Worker {
  const worker = new Worker(new URL('../solver/worker.ts', import.meta.url), { type: 'module' });
  worker.onmessage = (e: MessageEvent<{ id: number; result: unknown }>) => {
    pending.get(e.data.id)?.(e.data.result);
    pending.delete(e.data.id);
  };
  return worker;
}

/**
 * Talks to the solver in background workers. Dealing and finishing share
 * one; the check after each move gets its own, so a new move can simply
 * stop the check still running for the last one.
 */
export class SolverClient {
  private nextId = 1;
  private readonly pending: Pending = new Map();
  private readonly main = start(this.pending);
  private checker: Worker | null = null;
  private checkerId = 0;

  private ask<T>(worker: Worker, req: Ask): Promise<T> {
    const id = this.nextId++;
    if (worker === this.checker) this.checkerId = id;
    return new Promise<T>((resolve) => {
      this.pending.set(id, resolve as (r: unknown) => void);
      worker.postMessage({ ...req, id });
    });
  }

  /** A random deal number the solver can win at this level. */
  deal(level: Level): Promise<number> {
    return this.ask(this.main, { kind: 'deal', level });
  }

  check(level: Level, n: number): Promise<Outcome> {
    return this.ask(this.main, { kind: 'check', level, n });
  }

  finish(save: string): Promise<Plan | null> {
    return this.ask(this.main, { kind: 'finish', save });
  }

  /** Whether the saved position can still be won; replaces any check still running. */
  deadEnd(save: string): Promise<Outcome> {
    this.cancelDeadEnd();
    this.checker = start(this.pending);
    return this.ask(this.checker, { kind: 'deadEnd', save });
  }

  /** Stops the check; its promise is dropped and never settles. */
  cancelDeadEnd(): void {
    this.checker?.terminate();
    this.checker = null;
    this.pending.delete(this.checkerId);
  }
}
