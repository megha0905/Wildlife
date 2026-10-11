// Login screen: project intro on the left, sign-in form on the right.
import { login } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';

/** Demo accounts seeded by auth-service. Clicking one fills the form. */
const DEMO = [
  { username: 'officer', password: 'Officer@123', name: 'Megha Gupta', role: 'Forest officer' },
  { username: 'ranger', password: 'Ranger@123', name: 'Soni Kumari', role: 'Ranger' },
  { username: 'vet', password: 'Vet@123', name: 'Dr. Neha Fatima', role: 'Veterinarian' },
  { username: 'admin', password: 'Admin@123', name: 'Srishti Sao', role: 'Administrator' },
  { username: 'analyst', password: 'Analyst@123', name: 'Sakshi Rathi', role: 'Analyst' },
];

const TEAM = ['Megha Gupta', 'Srishti Sao', 'Soni Kumari', 'Neha Fatima', 'Sakshi Rathi'];

const FEATURES = [
  'Wildlife registry with health records and sightings',
  'Patrol planning with no double-booked staff or vehicles',
  'Incident and rescue workflows, step by step',
  'Monthly zone reviews and live conservation dashboards',
];

const FOREST = 'M0 160 L0 110 L30 70 L60 110 L80 80 L105 40 L130 80 L150 105 L180 60 L210 105 L230 85 L260 30 L290 85 L310 100 L340 55 L370 100 L395 75 L425 20 L455 75 L475 95 L505 50 L535 95 L560 70 L600 110 L600 160 Z';

function forestSvg() {
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('class', 'login-forest');
  svg.setAttribute('viewBox', '0 0 600 160');
  svg.setAttribute('preserveAspectRatio', 'none');
  svg.setAttribute('aria-hidden', 'true');
  const p = document.createElementNS(ns, 'path');
  p.setAttribute('d', FOREST);
  svg.append(p);
  return svg;
}

export function LoginPage() {
  const username = h('input', { id: 'u', class: 'input', autocomplete: 'username', required: true });
  const password = h('input', { id: 'p', class: 'input', type: 'password', autocomplete: 'current-password', required: true });
  const errorBox = h('div');
  const submitBtn = h('button', { class: 'btn btn-primary btn-block' }, 'Sign in');

  const demoButtons = DEMO.map((d) => {
    const btn = h('button', { type: 'button', class: 'demo-user' },
      h('span', { class: 'demo-avatar', 'aria-hidden': 'true' }, d.name.replace('Dr. ', '').split(' ').map((w) => w[0]).join('')),
      h('span', { class: 'demo-text' }, h('strong', null, d.name), h('span', null, d.role)));
    btn.addEventListener('click', () => {
      username.value = d.username;
      password.value = d.password;
      demoButtons.forEach((b) => b.classList.toggle('picked', b === btn));
    });
    return btn;
  });

  const form = h('form', { class: 'login-card' },
    h('div', null, h('h2', null, 'Sign in'), h('p', { class: 'muted' }, 'Use your WildGuard account.')),
    errorBox,
    h('div', { class: 'field' }, h('label', { for: 'u' }, 'Username'), username),
    h('div', { class: 'field' }, h('label', { for: 'p' }, 'Password'), password),
    submitBtn,
    h('div', { class: 'demo-logins' },
      h('p', { class: 'demo-title' }, 'Demo accounts: click one to fill the form'),
      h('div', { class: 'demo-grid' }, demoButtons)));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Signing in…';
    mount(errorBox);
    try {
      await login(username.value.trim(), password.value); // on success the app switches to the dashboard
    } catch (err) {
      mount(errorBox, h('div', { class: 'error-banner', role: 'alert' }, err instanceof Error ? err.message : 'Login failed'));
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign in';
    }
  });

  return h('div', { class: 'login-page' },
    h('section', { class: 'login-hero', 'aria-label': 'About WildGuard' },
      h('div', { class: 'login-brand' }, h('img', { src: '/favicon.svg', alt: '', width: 48, height: 48 }), h('span', null, 'WildGuard')),
      h('div', { class: 'login-hero-text' },
        h('h1', null, 'Protecting wildlife, one patrol at a time.'),
        h('p', null, 'Smart wildlife conservation and protected area management for forest departments.'),
        h('ul', { class: 'login-features' }, FEATURES.map((f) => h('li', null, f)))),
      h('p', { class: 'login-team' }, `Developed by ${TEAM.join(' · ')}`),
      forestSvg()),
    h('div', { class: 'login-side' }, form));
}
