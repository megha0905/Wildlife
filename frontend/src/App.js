// Chooses which screen to show. The screen is picked by the part of the address after #,
// e.g. http://localhost:5173/#/incidents/monthly. Logged-out users only ever see the login screen.
import { getUser, hasRole, onAuthChange } from './auth/auth.js';
import { mount } from './components/dom.js';
import { createLayout } from './components/layout.js';
import { LoginPage } from './pages/LoginPage.js';
import { DashboardPage } from './pages/DashboardPage.js';
import { AnimalsPage } from './pages/AnimalsPage.js';
import { ZonesPage } from './pages/ZonesPage.js';
import { PatrolsPage } from './pages/PatrolsPage.js';
import { IncidentsPage } from './pages/IncidentsPage.js';
import { ZoneMonthlyPage } from './pages/ZoneMonthlyPage.js';
import { RescuesPage } from './pages/RescuesPage.js';
import { ResourcesPage } from './pages/ResourcesPage.js';
import { ProgrammesPage } from './pages/ProgrammesPage.js';
import { ReportsPage } from './pages/ReportsPage.js';
import { UsersPage } from './pages/UsersPage.js';

const ROUTES = {
  '/': DashboardPage,
  '/animals': AnimalsPage,
  '/zones': ZonesPage,
  '/patrols': PatrolsPage,
  '/incidents': IncidentsPage,
  '/incidents/monthly': ZoneMonthlyPage,
  '/rescues': RescuesPage,
  '/resources': ResourcesPage,
  '/programmes': ProgrammesPage,
  '/reports': ReportsPage,
  '/users': UsersPage,
};

const currentPath = () => window.location.hash.replace(/^#/, '') || '/';

export function startApp(root) {
  let layout = null;

  const render = () => {
    // drawers and dialogs belong to the screen being left
    document.querySelectorAll('.drawer, .drawer-backdrop, dialog').forEach((el) => el.remove());

    if (!getUser()) {
      if (layout) { layout.destroy(); layout = null; }
      mount(root, LoginPage());
      return;
    }
    if (!layout) {
      layout = createLayout();
      mount(root, layout.el);
    }
    let path = currentPath();
    if (!ROUTES[path] || (path === '/users' && !hasRole('ADMIN'))) {
      path = '/';
      history.replaceState(null, '', '#/');
    }
    layout.setActive(path);
    mount(layout.main, ROUTES[path]());
    window.scrollTo(0, 0);
  };

  window.addEventListener('hashchange', render);
  onAuthChange((user) => {
    if (user) {
      if (layout) { layout.destroy(); layout = null; } // rebuild the frame for the new user
      history.replaceState(null, '', '#/');
    }
    render();
  });
  render();
}
