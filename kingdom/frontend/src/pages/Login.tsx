import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, api } from '../api';
import { ErrorBox } from '../components/ErrorBox';

/** The seeded demo users (see DataInitializer). Demo only: never ship passwords in a frontend. */
const DEMO_USERS = [
  { username: 'king', password: 'king1234', label: '👑 King', hint: 'KING + KNIGHT: may do everything' },
  { username: 'lancelot', password: 'lancelot1234', label: '🛡️ Lancelot', hint: 'KNIGHT: may add equipment' },
  { username: 'peasant', password: 'peasant1234', label: '🌾 Peasant', hint: 'SUBJECT: may only look' },
];

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  const [registered, setRegistered] = useState<string | null>(null);

  async function login(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.login(username.trim(), password);
      navigate('/persons');
    } catch (e) {
      setError(e as ApiError);
    } finally {
      setBusy(false);
    }
  }

  async function register(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setRegistered(null);
    try {
      const user = await api.register(newUsername.trim(), newPassword);
      setRegistered(user.username);
      setUsername(user.username);
      setNewUsername('');
      setNewPassword('');
    } catch (e) {
      setError(e as ApiError);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="two-col">
      <form className="card panel" onSubmit={login}>
        <h2>Log in</h2>
        <ErrorBox error={error} />
        <div className="grid">
          <label className="wide">
            Username
            <input name="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
          </label>
          <label className="wide">
            Password
            <input name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
          </label>
        </div>
        <button type="submit" className="btn" disabled={!username || !password || busy}>
          Log in
        </button>

        <h3 className="spaced">Single sign-on</h3>
        <p className="muted">
          Needs the fake identity provider (port 9000) and the profile <code>idp</code>.
        </p>
        <a className="btn secondary-btn" href="/oauth2/authorization/kingdom-idp">
          Log in with the identity provider
        </a>

        <h3 className="spaced">Demo logins</h3>
        <div className="actions">
          {DEMO_USERS.map((u) => (
            <button
              key={u.username}
              type="button"
              className="secondary small"
              title={u.hint}
              onClick={() => {
                setUsername(u.username);
                setPassword(u.password);
              }}
            >
              {u.label}
            </button>
          ))}
        </div>
      </form>

      <form className="card panel" onSubmit={register}>
        <h2>New subject</h2>
        <p className="muted">Registering always makes you a plain SUBJECT.</p>
        {registered && <div className="status ok">Welcome, {registered}! You can log in now.</div>}
        <div className="grid">
          <label className="wide">
            Username (3-30)
            <input name="newUsername" value={newUsername} onChange={(e) => setNewUsername(e.target.value)} required minLength={3} maxLength={30} />
          </label>
          <label className="wide">
            Password (min. 8)
            <input name="newPassword" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} autoComplete="new-password" />
          </label>
        </div>
        <button type="submit" className="secondary" disabled={newUsername.length < 3 || newPassword.length < 8 || busy}>
          Register
        </button>
      </form>
    </div>
  );
}
