import { execSync } from 'node:child_process';
import { defineConfig } from 'vitest/config';

// The version shown in About: the latest git tag, like the desktop build.
function version(): string {
  try {
    return execSync('git describe --tags --always', { encoding: 'utf8' }).trim().replace(/^v/, '');
  } catch {
    return 'dev';
  }
}

export default defineConfig({
  // Served from a sub-path on GitHub Pages; relative URLs work anywhere.
  base: './',
  define: {
    __APP_VERSION__: JSON.stringify(version()),
  },
  server: {
    // The card art lives in the Java source tree, one level up.
    fs: { allow: ['..'] },
  },
  test: {
    include: ['test/**/*.test.ts'],
  },
});
