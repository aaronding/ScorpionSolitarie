/// <reference lib="webworker" />
import { Game } from '../model/game';
import { ruleFor } from '../model/rule';
import { fromText } from '../model/saveFile';
import { check, finish, winnableDeal } from './dealer';
import { Solver } from './solver';

/** Requests the page sends; each reply carries the same id. */
export type Request =
  | { id: number; kind: 'deal'; level: string }
  | { id: number; kind: 'check'; level: string; n: number }
  | { id: number; kind: 'finish'; save: string }
  | { id: number; kind: 'deadEnd'; save: string };

// The solver runs here, off the page's thread, so the page never stalls.
self.onmessage = (e: MessageEvent<Request>) => {
  const req = e.data;
  let result: unknown;
  switch (req.kind) {
    case 'deal':
      result = winnableDeal(ruleFor(req.level));
      break;
    case 'check':
      result = check(ruleFor(req.level), req.n);
      break;
    case 'finish':
      result = finish(fromText(req.save));
      break;
    case 'deadEnd': {
      const game: Game = fromText(req.save);
      result = new Solver(game.rule, 400000).solve(game.board).outcome;
      break;
    }
  }
  postMessage({ id: req.id, result });
};
