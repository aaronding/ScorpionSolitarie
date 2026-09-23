/*
 * Toolbar icons as inline SVG, on a 24-unit grid: the same shapes as the
 * desktop game's. They take the text color, so they follow the theme.
 */
const svg = (body: string): string =>
  `<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" fill="none" stroke="currentColor" ` +
  `stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`;

const undo = '<path d="M5 10H14a4.5 4.5 0 0 1 0 9H9"/><path d="M9 5.5L4.5 10L9 14.5"/>';

export const ICONS = {
  newGame: svg(
    '<rect x="9" y="3" width="11" height="15" rx="1.3"/>' +
      '<rect x="4" y="6.5" width="11" height="15" rx="1.3" fill="var(--bar-bg)"/>',
  ),
  undo: svg(undo),
  redo: svg(`<g transform="translate(24 0) scale(-1 1)">${undo}</g>`),
  hint: svg(
    '<path d="M9.8 14.2A6 6 0 1 1 14.2 14.2"/><path d="M9.8 14.2V17.5M14.2 14.2V17.5M9.5 17.5H14.5M10.5 21H13.5"/>',
  ),
  giveUp: svg('<path d="M6 3.5V21"/><path d="M6 4H18.5L15.5 8.25L18.5 12.5H6Z" fill="currentColor"/>'),
  settings: svg(
    '<g fill="currentColor" stroke="none">' +
      Array.from(
        { length: 8 },
        (_, i) => `<rect x="10.2" y="1.8" width="3.6" height="5" rx="0.6" transform="rotate(${i * 45} 12 12)"/>`,
      ).join('') +
      '<path fill-rule="evenodd" d="M12 5a7 7 0 1 1 0 14a7 7 0 1 1 0-14zm0 3.8a3.2 3.2 0 1 0 0 6.4a3.2 3.2 0 1 0 0-6.4z"/></g>',
  ),
  more: svg('<path d="M5 7H19M5 12H19M5 17H19"/>'),
};
