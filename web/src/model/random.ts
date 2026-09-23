/**
 * java.util.Random, reproduced exactly so that a deal number shuffles the
 * same cards here as in the Java version of the game.
 */
export class JavaRandom {
  private seed: bigint;

  private static readonly MULTIPLIER = 0x5deece66dn;
  private static readonly MASK = (1n << 48n) - 1n;

  constructor(seed: number | bigint) {
    this.seed = (BigInt(seed) ^ JavaRandom.MULTIPLIER) & JavaRandom.MASK;
  }

  private next(bits: number): number {
    this.seed = (this.seed * JavaRandom.MULTIPLIER + 0xbn) & JavaRandom.MASK;
    // Java returns (int)(seed >>> (48 - bits)): keep 32 bits, signed.
    return Number(BigInt.asIntN(32, this.seed >> BigInt(48 - bits)));
  }

  /** A value in [0, bound), identical to Java's Random.nextInt(bound). */
  nextInt(bound: number): number {
    if (bound <= 0) throw new RangeError('bound must be positive');
    if ((bound & -bound) === bound) {
      return Number((BigInt(bound) * BigInt(this.next(31))) >> 31n);
    }
    let bits: number;
    let val: number;
    do {
      bits = this.next(31);
      val = bits % bound;
    } while (bits - val + (bound - 1) > 0x7fffffff); // Java detects this as int overflow
    return val;
  }
}
