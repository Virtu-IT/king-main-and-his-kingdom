// Copies the production build (dist/<project>/browser) into the Spring Boot app (src/main/resources/static).
// Usage: npm run publish-to-spring
import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const dist = join(root, 'dist');
const target = resolve(root, '..', 'src', 'main', 'resources', 'static');

const project = existsSync(dist) ? readdirSync(dist).find((d) => existsSync(join(dist, d, 'browser', 'index.html'))) : undefined;
if (!project) {
  console.error('No build found in dist/. Run "npm run build" first.');
  process.exit(1);
}

rmSync(target, { recursive: true, force: true });
mkdirSync(target, { recursive: true });
cpSync(join(dist, project, 'browser'), target, { recursive: true });
console.log(`Copied dist/${project}/browser -> ${target}`);
