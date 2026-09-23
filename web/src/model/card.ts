/**
 * Cards are numbers 0-51: suit * 13 + rank, with suits in the order spades,
 * hearts, clubs, diamonds and ranks from Ace (0) to King (12). The same values
 * the Java version and its save files use.
 */
export type Card = number;

export const CARDS = 52;
export const CARDS_IN_SUIT = 13;
export const COLUMNS = 7;
export const RESERVE_SIZE = 3;

export const ACE = 0;
export const KING = 12;

export const SUITS = ['spades', 'hearts', 'clubs', 'diamonds'] as const;
export type Suit = (typeof SUITS)[number];

export const rank = (card: Card): number => card % CARDS_IN_SUIT;
export const suit = (card: Card): number => Math.floor(card / CARDS_IN_SUIT);
/** 0 black, 1 red. */
export const color = (card: Card): number => suit(card) % 2;
export const cardOf = (suitIndex: number, rankIndex: number): Card => suitIndex * CARDS_IN_SUIT + rankIndex;

const RANK_NAMES = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];
const RANK_WORDS = ['Ace', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'Jack', 'Queen', 'King'];
const SUIT_SYMBOLS = ['♠', '♥', '♣', '♦'];

/** Short name, like "10♥". */
export const cardName = (card: Card): string => RANK_NAMES[rank(card)] + SUIT_SYMBOLS[suit(card)];

/** Spoken name, like "10 of hearts". */
export const cardWords = (card: Card): string => `${RANK_WORDS[rank(card)]} of ${SUITS[suit(card)]}`;
