import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { completeSso } from '../authStore';

/**
 * Landing page after the single sign-on: the server redirects to /sso#token=...
 * A fragment (#) is never sent to a server, so the token does not end up in access logs.
 */
export default function Sso() {
  const navigate = useNavigate();
  const [failed, setFailed] = useState(false);
  const handled = useRef(false); // React may run an effect twice in development

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;
    const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token');
    // remove the token from the address bar and the browser history right away
    history.replaceState(null, '', '/sso');
    if (token && completeSso(token)) {
      navigate('/persons', { replace: true });
    } else {
      setFailed(true);
    }
  }, [navigate]);

  return (
    <div className="card panel">
      {failed ? (
        <>
          <h2>Single sign-on failed</h2>
          <p className="muted">
            No valid token was received. <Link to="/login">Back to the login page</Link>
          </p>
        </>
      ) : (
        <h2>Signing you in...</h2>
      )}
    </div>
  );
}
