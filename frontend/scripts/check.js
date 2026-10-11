// "npm run build" for the plain-JS frontend: there is nothing to compile, so this checks instead that
// every .js file is valid JavaScript and that every relative import points at a file that exists.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const files = [];
(function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.js')) files.push(p);
  }
})(path.join(ROOT, 'src'));

let problems = 0;
for (const f of files) {
  try {
    execFileSync(process.execPath, ['--check', f], { stdio: 'pipe' });
  } catch (e) {
    problems++;
    console.error(`Syntax error in ${path.relative(ROOT, f)}:\n${e.stderr}`);
  }
  const src = fs.readFileSync(f, 'utf8');
  for (const m of src.matchAll(/(?:import|export)[^'"]*?from\s+['"](\.[^'"]+)['"]/g)) {
    const target = path.resolve(path.dirname(f), m[1]);
    if (!fs.existsSync(target)) {
      problems++;
      console.error(`${path.relative(ROOT, f)} imports ${m[1]}, which does not exist`);
    }
  }
}
for (const required of ['index.html', 'src/styles.css', 'public/favicon.svg']) {
  if (!fs.existsSync(path.join(ROOT, required))) { problems++; console.error(`Missing ${required}`); }
}
if (problems) { console.error(`\n${problems} problem(s) found.`); process.exit(1); }
console.log(`Checked ${files.length} JavaScript files: all OK.`);
