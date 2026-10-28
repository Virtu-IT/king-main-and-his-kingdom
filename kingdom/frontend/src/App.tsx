import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { setSession, useSession } from './authStore';
import Arsenal from './pages/Arsenal';
import Login from './pages/Login';
import Persons from './pages/Persons';
import SecurityLab from './pages/SecurityLab';
import Sso from './pages/Sso';

/** Only a UX convenience: the real protection is on the server. Anyone can edit JavaScript in their browser. */
function RequireLogin({ children }: { children: ReactNode }) {
  return useSession() ? <>{children}</> : <Navigate to="/login" replace />;
}

function readStoredTheme(): string | null {
  try {
    return localStorage.getItem('theme');
  } catch {
    return null;
  }
}

export default function App() {
  const session = useSession();
  const navigate = useNavigate();
  const [dark, setDark] = useState(readStoredTheme() === 'dark');

  useEffect(() => {
    const theme = dark ? 'dark' : 'light';
    document.documentElement.dataset['theme'] = theme;
    try {
      localStorage.setItem('theme', theme);
    } catch {
      /* storage not available: the choice just isn't remembered */
    }
  }, [dark]);

  function logout() {
    setSession(null);
    navigate('/login');
  }

  const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined);

  return (
    <>
      <header>
        <h1>
          <span aria-hidden="true">👑</span> <span className="brand">King Main &amp; his Kingdom</span>
        </h1>
        <nav>
          {session && (
            <>
              <NavLink to="/persons" className={navClass}>
                Persons
              </NavLink>
              <NavLink to="/arsenal" className={navClass}>
                Arsenal
              </NavLink>
            </>
          )}
          <NavLink to="/security-lab" className={navClass}>
            Security lab
          </NavLink>
        </nav>
        <div className="who">
          {session ? (
            <>
              <span className="user">{session.username}</span>
              {session.roles.map((role) => (
                <span key={role} className="tag">
                  {role}
                </span>
              ))}
              <button type="button" className="secondary small" onClick={logout}>
                Log out
              </button>
            </>
          ) : (
            <Link className="btn small" to="/login">
              Log in
            </Link>
          )}
          <button
            type="button"
            className="theme-toggle"
            onClick={() => setDark(!dark)}
            aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {dark ? '☀️' : '🌙'}
          </button>
        </div>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/persons" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/sso" element={<Sso />} />
          <Route path="/persons" element={<RequireLogin><Persons /></RequireLogin>} />
          <Route path="/arsenal" element={<RequireLogin><Arsenal /></RequireLogin>} />
          <Route path="/security-lab" element={<SecurityLab />} />
          <Route path="*" element={<Navigate to="/persons" replace />} />
        </Routes>
      </main>
    </>
  );
}
