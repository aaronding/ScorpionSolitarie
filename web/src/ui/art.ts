import { type Card, SUITS, rank, suit } from '../model/card';

// The same SVG card art as the desktop game, straight from its source tree.
const files = import.meta.glob<string>('../../../src/res/cards/**/*.svg', {
  query: '?url',
  import: 'default',
  eager: true,
});

const byName = new Map<string, string>();
for (const [path, url] of Object.entries(files)) {
  byName.set(path.replace(/^.*\/res\/cards\//, '').replace(/\.svg$/, ''), url);
}

const RANK_FILES = ['ace', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'jack', 'queen', 'king'];

export function faceUrl(card: Card): string {
  return byName.get(`${SUITS[suit(card)]}_${RANK_FILES[rank(card)]}`)!;
}

export const CUSTOM_BACK = 'custom';
export const DEFAULT_BACK = 'blue2';

/** Built-in backs, in the order the picker shows them. */
export const BACKS = [
  'blue2', 'red2', 'blue', 'red', 'castle', 'fish',
  'cars', 'astronaut', 'frog', 'abstract', 'abstract_clouds', 'abstract_scene',
];

export function backUrl(name: string): string {
  return byName.get(`backs/${name}`) ?? byName.get(`backs/${DEFAULT_BACK}`)!;
}

/** Starts loading every face so the first deal doesn't flicker. */
export function preload(): void {
  for (let card = 0; card < 52; card++) {
    const img = new Image();
    img.src = faceUrl(card);
  }
}

/**
 * Shrinks a picture the player chose to card size and returns it as a data
 * URL small enough to keep in local storage.
 */
export async function pictureToBack(file: File): Promise<string> {
  const bitmap = await createImageBitmap(file);
  const w = 468;
  const h = Math.round((w * 333) / 234);
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  const g = canvas.getContext('2d')!;
  // White card with rounded corners, the picture cropped to fill the inside.
  const r = w * 0.05;
  g.fillStyle = '#fff';
  g.beginPath();
  g.roundRect(0, 0, w, h, r);
  g.fill();
  const inset = w * 0.045;
  g.save();
  g.beginPath();
  g.roundRect(inset, inset, w - 2 * inset, h - 2 * inset, r * 0.6);
  g.clip();
  const scale = Math.max((w - 2 * inset) / bitmap.width, (h - 2 * inset) / bitmap.height);
  const iw = bitmap.width * scale;
  const ih = bitmap.height * scale;
  g.drawImage(bitmap, (w - iw) / 2, (h - ih) / 2, iw, ih);
  g.restore();
  return canvas.toDataURL('image/jpeg', 0.85);
}
