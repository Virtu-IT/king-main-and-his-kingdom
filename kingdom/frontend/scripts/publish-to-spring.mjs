// Copies the production build (dist/) into the Spring Boot app (src/main/resources/static).
// Usage: npm run publish-to-spring
import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const dist = join(root, 'dist');
const target = resolve(root, '..', 'src', 'main', 'resources', 'static');

if (!existsSync(join(dist, 'index.html'))) {
  console.error('No build found in dist/. Run "npm run build" first.');
  process.exit(1);
}

rmSync(target, { recursive: true, force: true });
mkdirSync(target, { recursive: true });
cpSync(dist, target, { recursive: true });
console.log(`Copied dist -> ${target}`);
