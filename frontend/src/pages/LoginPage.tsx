import { useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const DEMO = [
  ['officer', 'Officer@123', 'Forest officer'],
  ['ranger', 'Ranger@123', 'Ranger'],
  ['vet', 'Vet@123', 'Veterinarian'],
  ['analyst', 'Analyst@123', 'Analyst'],
  ['admin', 'Admin@123', 'Administrator'],
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
      <form className="login-card" onSubmit={submit}>
        <img src="/favicon.svg" alt="" width={44} height={44} />
        <h1>WildGuard</h1>
        <p className="muted">Protected area operations for wildlife authorities</p>
        {error && <div className="error-banner" role="alert">{error}</div>}
        <div className="field">
          <label htmlFor="u">Username</label>
          <input id="u" className="input" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="p">Password</label>
          <input id="p" className="input" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
        <details className="demo-logins">
          <summary>Development logins</summary>
          <ul>
            {DEMO.map(([u, p, role]) => (
              <li key={u}>
                <button type="button" className="link-btn" onClick={() => { setUsername(u); setPassword(p); }}>{u}</button>
                <span className="muted"> {role}</span>
              </li>
            ))}
          </ul>
        </details>
      </form>
    </div>
  );
}
