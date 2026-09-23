import { Game } from '../model/game';
import { type Rule, EASY, ruleFor } from '../model/rule';
import { fromText, toText } from '../model/saveFile';
import { MAX_DEAL } from '../solver/dealer';
import { DEAL_RESERVE } from '../solver/solver';
import { CUSTOM_BACK, backUrl } from './art';
import {
  aboutDialog,
  confirm,
  dealNumberDialog,
  h,
  helpDialog,
  levelPicker,
  modal,
  settingsDialog,
  statsDialog,
} from './dialogs';
import { ICONS } from './icons';
import { SolverClient } from './solverClient';
import * as store from './storage';
import { NORMAL, QUICK, SLOW, Table } from './table';

declare const __APP_VERSION__: string;

interface Action {
  label: string;
  run: () => void;
  enabled?: () => boolean;
}

/**
 * Runs the game's life in the page: dealing, the clock, saving, statistics,
 * the toolbar, and the computer finishing a game the player gives up.
 */
export class App {
  private game = Game.deal(EASY, 1);
  private readonly table: Table;
  private readonly solver = new SolverClient();
  private settings = store.loadSettings();
  private over = false;
  private busy = false;
  private version = 0;
  private hintIndex = 0;
  private clockBase = 0;
  private clockSince = 0;
  private clockRunning = false;

  private readonly notice = document.getElementById('notice')!;
  private readonly info = document.getElementById('info')!;
  private readonly score = document.getElementById('score')!;
  private readonly status = document.getElementById('status')!;
  private readonly tools = document.getElementById('tools')!;
  private readonly buttons = new Map<Action, HTMLButtonElement>();

  // ---- actions ---------------------------------------------------------------

  private readonly newGame: Action = { label: 'New Game', run: () => void this.startNew() };
  private readonly undo: Action = {
    label: 'Undo',
    run: () => this.change(() => this.game.undo()),
    enabled: () => this.playing() && this.game.canUndo(),
  };
  private readonly redo: Action = {
    label: 'Redo',
    run: () => this.change(() => this.game.redo()),
    enabled: () => this.playing() && this.game.canRedo(),
  };
  private readonly hint: Action = { label: 'Hint', run: () => this.showHint(), enabled: () => this.playing() };
  private readonly giveUp: Action = { label: 'Give Up', run: () => void this.giveUpGame(), enabled: () => this.playing() };
  private readonly settingsAction: Action = { label: 'Settings', run: () => void this.showSettings(), enabled: () => !this.busy };
  private readonly restart: Action = {
    label: 'Restart This Game',
    run: () => void this.restartGame(),
    enabled: () => !this.busy && this.game.dealNumber > 0,
  };
  private readonly selectGame: Action = { label: 'Select Game Number…', run: () => void this.selectDeal(), enabled: () => !this.busy };
  private readonly exportGame: Action = { label: 'Save Game to File…', run: () => this.exportToFile(), enabled: () => this.playing() };
  private readonly importGame: Action = { label: 'Open Saved Game…', run: () => this.importFromFile(), enabled: () => !this.busy };
  private readonly stats: Action = { label: 'Statistics', run: () => void statsDialog() };
  private readonly help: Action = { label: 'How to Play', run: () => void helpDialog() };
  private readonly about: Action = { label: 'About', run: () => void aboutDialog(__APP_VERSION__) };

  constructor(tableElement: HTMLElement) {
    this.table = new Table(tableElement, { playerMoved: () => this.playerMoved() });
    this.applyBack();
    this.createToolbar();
    this.bindKeys();
    window.setInterval(() => this.updateStatus(), 1000);
    document.addEventListener('visibilitychange', () => (document.hidden ? this.pauseClock() : this.resumeClock()));
    window.addEventListener('pagehide', () => this.autosave());
  }

  /** Resumes the last game, or deals a new one. */
  async start(): Promise<void> {
    const saved = store.loadAutosave();
    if (saved) {
      try {
        const game = fromText(saved);
        if (!game.isWon()) {
          this.useGame(game);
          this.suggestInstall();
          return;
        }
      } catch {
        // unreadable: deal a fresh game instead
      }
    }
    this.useGame(this.game); // something on the table while we ask
    let level = this.settings.level;
    if (!level) {
      const picker = levelPicker('easy');
      await modal('Welcome to Scorpion Solitaire', [h('p', {}, 'Choose how hard the game should be. You can change it later in Settings.'), picker.element], ['Play']);
      level = picker.value();
      this.settings.level = level;
      store.saveSettings(this.settings);
    }
    await this.deal(ruleFor(level), 0);
    this.suggestInstall();
  }

  /**
   * In Safari on an iPhone or iPad the browser's bars take a lot of room, and
   * a web page can't hide them. Installed on the home screen, the game opens
   * without them; say so, once.
   */
  private suggestInstall(): void {
    const ios = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    const installed = matchMedia('(display-mode: standalone), (display-mode: fullscreen)').matches ||
      (navigator as Navigator & { standalone?: boolean }).standalone === true;
    if (!ios || installed || store.installTipShown()) return;
    store.setInstallTipShown();
    this.showNotice('Tip: tap Share, then “Add to Home Screen”, to play full screen without Safari’s bars.', {
      label: 'OK',
      run: () => this.hideNotice(),
    });
  }

  // ---- toolbar and keys --------------------------------------------------------

  private createToolbar(): void {
    const mod = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl+';
    const add = (action: Action, icon: string, key?: string) => {
      const b = h('button', { type: 'button', class: 'tool', title: key ? `${action.label} (${key})` : action.label });
      b.innerHTML = `${icon}<span>${action.label}</span>`;
      b.addEventListener('click', () => action.run());
      this.tools.append(b);
      this.buttons.set(action, b);
    };
    add(this.newGame, ICONS.newGame);
    add(this.undo, ICONS.undo, `${mod}Z`);
    add(this.redo, ICONS.redo, mod === '⌘' ? '⇧⌘Z' : 'Ctrl+Y');
    add(this.hint, ICONS.hint, 'H');
    add(this.giveUp, ICONS.giveUp);
    add(this.settingsAction, ICONS.settings);

    const more = h('button', { type: 'button', class: 'tool', title: 'More', 'aria-haspopup': 'menu' });
    more.innerHTML = `${ICONS.more}<span>More</span>`;
    const menu = h('div', { class: 'menu', role: 'menu', hidden: '' });
    const items: (Action | null)[] = [this.restart, this.selectGame, null, this.exportGame, this.importGame, null, this.stats, this.help, this.about];
    for (const item of items) {
      if (!item) {
        menu.append(h('hr'));
        continue;
      }
      const b = h('button', { type: 'button', role: 'menuitem' }, item.label);
      b.addEventListener('click', () => {
        menu.hidden = true;
        item.run();
      });
      this.buttons.set(item, b);
      menu.append(b);
    }
    more.addEventListener('click', (e) => {
      e.stopPropagation();
      this.updateActions();
      menu.hidden = !menu.hidden;
    });
    document.addEventListener('click', () => (menu.hidden = true));
    this.tools.append(h('span', { class: 'more' }, more, menu));
  }

  private bindKeys(): void {
    document.addEventListener('keydown', (e) => {
      if (document.querySelector('dialog[open]') || (e.target as HTMLElement).closest('input')) return;
      const mod = e.metaKey || e.ctrlKey;
      const key = e.key.toLowerCase();
      if (mod && key === 'z' && !e.shiftKey) this.run(this.undo, e);
      else if ((mod && key === 'z' && e.shiftKey) || (mod && key === 'y')) this.run(this.redo, e);
      else if (!mod && key === 'h') this.run(this.hint, e);
      else if (key === 'f2') this.run(this.newGame, e);
    });
  }

  private run(action: Action, e: Event): void {
    e.preventDefault();
    if (!action.enabled || action.enabled()) action.run();
  }

  private updateActions(): void {
    for (const [action, b] of this.buttons) b.disabled = !!action.enabled && !action.enabled();
  }

  private playing(): boolean {
    return !this.over && !this.busy;
  }

  // ---- dealing -----------------------------------------------------------------

  /** Deals game n, or a random winnable one if 0. */
  private async deal(rule: Rule, n: number): Promise<void> {
    this.setBusy(true);
    this.showNotice(n === 0 ? 'Shuffling for a winnable deal…' : `Dealing game #${n.toLocaleString()}…`);
    const number = n === 0 ? await this.solver.deal(rule.level) : n;
    const checked = n === 0 || (await this.solver.check(rule.level, number)) === 'solved';
    this.useGame(Game.deal(rule, number));
    this.table.animateDeal();
    if (!checked)
      this.showNotice(`The computer couldn't find a way to win game #${number.toLocaleString()} at this level. It may be impossible.`);
  }

  private useGame(game: Game): void {
    this.game = game;
    this.over = false;
    this.version++;
    this.hintIndex = 0;
    this.clockBase = game.elapsedMs;
    this.clockSince = 0;
    this.clockRunning = false;
    this.table.setGame(game);
    this.setBusy(false);
    this.hideNotice();
    this.autosave();
    this.updateStatus();
  }

  private preferredRule(): Rule {
    return this.settings.level ? ruleFor(this.settings.level) : this.game.rule;
  }

  private async startNew(): Promise<void> {
    if (await this.confirmAbandon('Start a new game?')) await this.deal(this.preferredRule(), 0);
  }

  private async restartGame(): Promise<void> {
    if (this.game.dealNumber > 0 && (await this.confirmAbandon('Start this game again from the beginning?')))
      await this.deal(this.game.rule, this.game.dealNumber);
  }

  private async selectDeal(): Promise<void> {
    const n = await dealNumberDialog(MAX_DEAL);
    if (n && (await this.confirmAbandon(`Start game #${n.toLocaleString()}?`))) await this.deal(this.preferredRule(), n);
  }

  /** Asks before leaving a game in progress, which then counts as lost. */
  private async confirmAbandon(question: string): Promise<boolean> {
    if (this.over || this.game.moveCount === 0) return true;
    if (!(await confirm('Scorpion Solitaire', `${question} The game in progress will count as a loss.`, 'OK'))) return false;
    store.recordResult(this.game.rule.level, false);
    return true;
  }

  // ---- playing -----------------------------------------------------------------

  private change(step: () => unknown): void {
    if (!this.playing()) return;
    this.table.animate(step, NORMAL);
    this.afterChange();
  }

  private playerMoved(): void {
    if (this.over) return;
    if (!this.clockRunning) {
      this.clockRunning = true;
      this.clockSince = document.hidden ? 0 : performance.now();
    }
    this.afterChange();
  }

  private afterChange(): void {
    this.version++;
    this.hintIndex = 0;
    this.hideNotice();
    if (this.game.isWon()) {
      this.won();
      return;
    }
    this.autosave();
    this.updateStatus();
    void this.checkForDeadEnd();
  }

  private won(): void {
    this.over = true;
    this.stopClock();
    this.table.setInteractive(false);
    store.recordResult(this.game.rule.level, true);
    store.saveAutosave(null);
    this.updateStatus();
    this.showNotice(`You won in ${this.game.moveCount} moves and ${formatTime(this.game.elapsedMs)}!`, this.newGame);
  }

  private showHint(): void {
    if (!this.playing()) return;
    const moves = this.game.hints();
    if (moves.length) this.table.showHint(moves[this.hintIndex++ % moves.length]);
    else if (!this.game.board.reserveUsed) this.table.showReserveHint();
    else this.showNotice('No moves left.', this.undo, this.giveUp);
  }

  /** Looks, in the background, for proof the game can no longer be won. */
  private async checkForDeadEnd(): Promise<void> {
    if (this.game.noMovesLeft()) {
      this.showNotice('No moves left.', this.undo, this.giveUp);
      return;
    }
    const asked = this.version;
    const outcome = await this.solver.deadEnd(toText(this.game));
    if (outcome === 'unsolvable' && asked === this.version && !this.over)
      this.showNotice("There's no way to win from here.", this.undo, this.giveUp);
  }

  private async giveUpGame(): Promise<void> {
    if (!this.playing()) return;
    const ok = await confirm(
      'Give Up',
      'The computer will finish the game, taking back moves if it has to. The game counts as a loss.',
      'Give Up',
    );
    if (!ok) return;
    this.over = true;
    this.stopClock();
    store.recordResult(this.game.rule.level, false);
    store.saveAutosave(null);
    this.solver.cancelDeadEnd();
    this.setBusy(true);
    this.showNotice('Working out how to win…');
    const game = this.game;
    const plan = await this.solver.finish(toText(game));
    if (game !== this.game) return;
    if (!plan) {
      this.setBusy(false);
      this.showNotice("This game can't be won, even from the start.", this.newGame);
      return;
    }
    if (plan.undo > 0) this.showNotice(`Taking back ${plan.undo} ${plan.undo === 1 ? 'move' : 'moves'}…`);
    const pause = (ms: number) => new Promise((r) => setTimeout(r, ms));
    for (let i = 0; i < plan.undo; i++) {
      if (game !== this.game) return;
      this.table.animate(() => game.forceUndo(), QUICK);
      this.updateStatus();
      await pause(plan.undo > 12 ? 170 : 300);
    }
    this.showNotice('Finishing the game…');
    for (const play of plan.plays) {
      if (game !== this.game) return;
      this.table.animate(() => (play === DEAL_RESERVE ? game.useReserve() : game.move(play)), SLOW);
      this.updateStatus();
      await pause(420);
    }
    this.setBusy(false);
    this.showNotice("That's how this game could be won.", this.newGame);
  }

  private setBusy(busy: boolean): void {
    this.busy = busy;
    this.table.setInteractive(!busy && !this.over);
    this.updateActions();
  }

  // ---- settings ----------------------------------------------------------------

  private async showSettings(): Promise<void> {
    const chosen = await settingsDialog({ ...this.settings, level: this.preferredRule().level });
    if (!chosen) return;
    const levelChanged = chosen.level !== this.preferredRule().level;
    this.settings = chosen;
    store.saveSettings(chosen);
    this.applyBack();
    if (levelChanged && chosen.level) {
      if (!this.busy && (this.over || this.game.moveCount === 0)) await this.deal(ruleFor(chosen.level), 0);
      else this.showNotice(`${ruleFor(chosen.level).name} starts with your next game.`, this.newGame);
    }
  }

  private applyBack(): void {
    const s = this.settings;
    this.table.setBack(s.back === CUSTOM_BACK && s.customBack ? s.customBack : backUrl(s.back));
  }

  // ---- saving ------------------------------------------------------------------

  private autosave(): void {
    if (this.over) return;
    this.game.elapsedMs = this.elapsed();
    store.saveAutosave(toText(this.game));
  }

  /** Downloads the game as a save file the desktop version can open too. */
  private exportToFile(): void {
    this.game.elapsedMs = this.elapsed();
    const url = URL.createObjectURL(new Blob([toText(this.game)], { type: 'text/plain' }));
    const a = h('a', { href: url, download: `Scorpion-${this.game.dealNumber || 'game'}.dat` });
    a.click();
    URL.revokeObjectURL(url);
  }

  private importFromFile(): void {
    const input = h('input', { type: 'file', accept: '.dat,text/plain' });
    input.addEventListener('change', async () => {
      const file = input.files?.[0];
      if (!file) return;
      try {
        const game = fromText(await file.text());
        if (await this.confirmAbandon('Open the saved game?')) this.useGame(game);
      } catch {
        await modal('Open Saved Game', [h('p', {}, "That file isn't a saved Scorpion game.")], ['OK']);
      }
    });
    input.click();
  }

  // ---- notice, status and clock ------------------------------------------------

  private showNotice(message: string, ...actions: Action[]): void {
    this.notice.querySelector('.text')!.textContent = message;
    const row = this.notice.querySelector('.actions')!;
    row.replaceChildren(
      ...actions.map((a) => {
        const b = h('button', { type: 'button' }, a.label);
        b.addEventListener('click', () => a.run());
        return b;
      }),
    );
    this.notice.hidden = false;
  }

  private hideNotice(): void {
    this.notice.hidden = true;
  }

  private updateStatus(): void {
    const n = this.game.dealNumber > 0 ? `Game #${this.game.dealNumber.toLocaleString()} · ` : '';
    this.info.textContent = n + this.game.rule.name;
    this.score.textContent = `Moves ${this.game.moveCount} · ${formatTime(this.elapsed())}`;
    this.status.textContent = `${this.info.textContent} · ${this.score.textContent}`;
    this.updateActions();
  }

  private elapsed(): number {
    return this.clockBase + (this.clockSince ? performance.now() - this.clockSince : 0);
  }

  private pauseClock(): void {
    if (this.clockSince) {
      this.clockBase += performance.now() - this.clockSince;
      this.clockSince = 0;
    }
  }

  private resumeClock(): void {
    if (this.clockRunning && !this.clockSince && !this.over) this.clockSince = performance.now();
  }

  private stopClock(): void {
    this.clockBase = this.elapsed();
    this.clockSince = 0;
    this.clockRunning = false;
    this.game.elapsedMs = this.clockBase;
  }
}

function formatTime(ms: number): string {
  const s = Math.floor(ms / 1000);
  const two = (n: number) => String(n).padStart(2, '0');
  return s >= 3600 ? `${Math.floor(s / 3600)}:${two(Math.floor(s / 60) % 60)}:${two(s % 60)}` : `${Math.floor(s / 60)}:${two(s % 60)}`;
}
