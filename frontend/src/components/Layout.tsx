import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { reportApi } from '../api/endpoints';
import type { Role } from '../api/types';
import { label } from './ui';

interface NavItem { to: string; text: string; icon: string; roles?: Role[]; }

const NAV: NavItem[] = [
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

export function Layout() {
  const { user, logout, hasRole } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [health, setHealth] = useState<Record<string, 'UP' | 'DOWN'> | null>(null);
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  useEffect(() => {
    const load = () => reportApi.gatewayHealth().then((h) => setHealth(h.services)).catch(() => setHealth(null));
    load();
    const t = setInterval(load, 30_000);
    return () => clearInterval(t);
  }, []);

  const down = health ? Object.entries(health).filter(([, s]) => s === 'DOWN').map(([n]) => n) : [];

  return (
    <div className="shell">
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="brand">
          <img src="/favicon.svg" alt="" width={32} height={32} />
          <div>
            <div className="brand-name">WildGuard</div>
            <div className="brand-sub">Protected area operations</div>
          </div>
        </div>
        <nav aria-label="Main">
          <ul className="nav">
            {NAV.filter((n) => !n.roles || hasRole(...n.roles)).map((n) => (
              <li key={n.to}>
                <NavLink to={n.to} end={n.to === '/' || n.to === '/incidents'}>
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d={n.icon} /></svg>
                  {n.text}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="sidebar-foot" title={health ? Object.entries(health).map(([k, v]) => `${k}: ${v}`).join('\n') : ''}>
          <span className={`dot ${health === null ? '' : down.length === 0 ? 'ok' : 'down'}`} />
          {health === null ? 'Service status unknown' : down.length === 0 ? 'All services online' : `Offline: ${down.join(', ')}`}
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <button className="icon-btn menu-btn" aria-label="Open navigation" onClick={() => setMenuOpen((o) => !o)}>☰</button>
          <div className="spacer" />
          {user && (
            <div className="user-box">
              <div>
                <div className="user-name">{user.fullName}</div>
                <div className="user-role">{user.roles.map(label).join(', ')}</div>
              </div>
              <button className="btn btn-ghost" onClick={logout}>Log out</button>
            </div>
          )}
        </header>
        <main id="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
