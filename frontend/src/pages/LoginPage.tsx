import { useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

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

export function LoginPage() {
  const { user, login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username.trim(), password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <section className="login-hero" aria-label="About WildGuard">
        <div className="login-brand">
          <img src="/favicon.svg" alt="" width={48} height={48} />
          <span>WildGuard</span>
        </div>
        <div className="login-hero-text">
          <h1>Protecting wildlife, one patrol at a time.</h1>
          <p>Smart wildlife conservation and protected area management for forest departments.</p>
          <ul className="login-features">
            {FEATURES.map((f) => <li key={f}>{f}</li>)}
          </ul>
        </div>
        <p className="login-team">Developed by {TEAM.join(' · ')}</p>
        <svg className="login-forest" viewBox="0 0 600 160" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0 160 L0 110 L30 70 L60 110 L80 80 L105 40 L130 80 L150 105 L180 60 L210 105 L230 85 L260 30 L290 85 L310 100 L340 55 L370 100 L395 75 L425 20 L455 75 L475 95 L505 50 L535 95 L560 70 L600 110 L600 160 Z" />
        </svg>
      </section>

      <div className="login-side">
        <form className="login-card" onSubmit={submit}>
          <div>
            <h2>Sign in</h2>
            <p className="muted">Use your WildGuard account.</p>
          </div>
          {error && <div className="error-banner" role="alert">{error}</div>}
          <div className="field">
            <label htmlFor="u">Username</label>
            <input id="u" className="input" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="p">Password</label>
            <input id="p" className="input" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>

          <div className="demo-logins">
            <p className="demo-title">Demo accounts: click one to fill the form</p>
            <div className="demo-grid">
              {DEMO.map((d) => (
                <button type="button" key={d.username} className={`demo-user ${username === d.username ? 'picked' : ''}`}
                  onClick={() => { setUsername(d.username); setPassword(d.password); }}>
                  <span className="demo-avatar" aria-hidden="true">{d.name.replace('Dr. ', '').split(' ').map((w) => w[0]).join('')}</span>
                  <span className="demo-text"><strong>{d.name}</strong><span>{d.role}</span></span>
                </button>
              ))}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
