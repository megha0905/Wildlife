// The frame around every screen: sidebar with navigation and service status, top bar with the user.
import { reportApi } from '../api/endpoints.js';
import { getUser, hasRole, logout } from '../auth/auth.js';
import { h, icon, mount } from './dom.js';
import { label } from './ui.js';

const NAV = [
  { to: '/', text: 'Dashboard', icon: 'M4 13h7V4H4v9zm9 7h7V11h-7v9zM4 20h7v-5H4v5zm9-16v5h7V4h-7z' },
  { to: '/animals', text: 'Animal registry', icon: 'M12 11c3 0 5 4 5 6s-2 3-5 3-5-1-5-3 2-6 5-6zM7 7.5a2 2 0 1 0 0 .1M17 7.5a2 2 0 1 0 0 .1M10 4a2 2 0 1 0 0 .1M14 4a2 2 0 1 0 0 .1' },
  { to: '/zones', text: 'Zones & map', icon: 'M9 4L3 6v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14' },
  { to: '/patrols', text: 'Patrol planner', icon: 'M6 18a2 2 0 1 0 0 .1M18 6a2 2 0 1 0 0 .1M8.5 18H15a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h6.5' },
  { to: '/incidents', text: 'Incident board', icon: 'M12 3l9.5 17h-19L12 3zM12 10v4M12 17v.5' },
  { to: '/incidents/monthly', text: 'Monthly zone review', icon: 'M4 6h16v14H4zM4 10h16M9 3v4M15 3v4' },
  { to: '/rescues', text: 'Rescue tracker', icon: 'M9 3h6v6h6v6h-6v6H9v-6H3V9h6V3z' },
  { to: '/resources', text: 'Staff & resources', icon: 'M9 8a3 3 0 1 0 0 .1M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6M16 5a3 3 0 0 1 0 6M18 14c2 .8 3 3 3 6' },
  { to: '/programmes', text: 'Programmes', icon: 'M12 4a8 8 0 1 0 0 16 8 8 0 1 0 0-16zM12 8a4 4 0 1 0 0 8 4 4 0 1 0 0-8z' },
  { to: '/reports', text: 'Reports', icon: 'M5 20V10M10 20V4M15 20v-7M20 20v-4' },
  { to: '/users', text: 'Users', icon: 'M12 4a4 4 0 1 0 0 8 4 4 0 1 0 0-8zM4 21c0-4 3.6-7 8-7s8 3 8 7', roles: ['ADMIN'] },
];

/**
 * Builds the frame once and returns { main, setActive, destroy }.
 * main: where the current screen goes. setActive(path): highlights the matching menu item.
 */
export function createLayout() {
  const user = getUser();
  const links = [];
  const status = h('div', { class: 'sidebar-foot' });
  const sidebar = h('aside', { class: 'sidebar' },
    h('div', { class: 'brand' },
      h('img', { src: '/favicon.svg', alt: '', width: 32, height: 32 }),
      h('div', null, h('div', { class: 'brand-name' }, 'WildGuard'), h('div', { class: 'brand-sub' }, 'Protected area operations'))),
    h('nav', { 'aria-label': 'Main' }, h('ul', { class: 'nav' },
      NAV.filter((n) => !n.roles || hasRole(...n.roles)).map((n) => {
        const a = h('a', { href: `#${n.to}` }, icon(n.icon), n.text);
        links.push({ a, to: n.to });
        return h('li', null, a);
      }))),
    status);

  const main = h('main', { id: 'main' });
  const shell = h('div', { class: 'shell' },
    sidebar,
    h('div', { class: 'workspace' },
      h('header', { class: 'topbar' },
        h('button', { class: 'icon-btn menu-btn', 'aria-label': 'Open navigation', onClick: () => sidebar.classList.toggle('open') }, '☰'),
        h('div', { class: 'spacer' }),
        user && h('div', { class: 'user-box' },
          h('div', null,
            h('div', { class: 'user-name' }, user.fullName),
            h('div', { class: 'user-role' }, user.roles.map(label).join(', '))),
          h('button', { class: 'btn btn-ghost', onClick: logout }, 'Log out'))),
      main));

  // Service status dot: asks the gateway every 30 seconds which services are up
  const showHealth = (services) => {
    const down = services ? Object.entries(services).filter(([, s]) => s === 'DOWN').map(([n]) => n) : [];
    status.title = services ? Object.entries(services).map(([k, v]) => `${k}: ${v}`).join('\n') : '';
    mount(status,
      h('span', { class: `dot ${services === null ? '' : down.length === 0 ? 'ok' : 'down'}` }),
      services === null ? 'Service status unknown' : down.length === 0 ? 'All services online' : `Offline: ${down.join(', ')}`);
  };
  const checkHealth = () => reportApi.gatewayHealth().then((r) => showHealth(r.services)).catch(() => showHealth(null));
  showHealth(null);
  checkHealth();
  const timer = setInterval(checkHealth, 30_000);

  const setActive = (path) => {
    for (const { a, to } of links) {
      const exact = to === '/' || to === '/incidents';
      const active = exact ? path === to : path === to || path.startsWith(`${to}/`);
      a.classList.toggle('active', active);
      if (active) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    }
    sidebar.classList.remove('open');
  };

  return { el: shell, main, setActive, destroy: () => clearInterval(timer) };
}
