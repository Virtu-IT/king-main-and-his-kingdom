import { useState } from 'react';
import { ApiError, raw } from '../api';
import { decodeClaims, useSession } from '../authStore';

interface Probe {
  title: string;
  method: 'GET' | 'POST' | 'DELETE';
  url: string;
  body?: unknown;
  rule: string;
}

interface Result {
  status: number;
  message: string;
}

type Mode = 'token' | 'anonymous';

const PROBES: Probe[] = [
  { title: 'Welcome message', method: 'GET', url: '/api/welcome?name=Visitor', rule: 'public (permitAll)' },
  { title: 'Who am I?', method: 'GET', url: '/api/auth/me', rule: 'any logged-in user' },
  { title: 'List persons', method: 'GET', url: '/api/persons', rule: 'any logged-in user' },
  {
    title: 'Add equipment',
    method: 'POST',
    url: '/api/equipment',
    body: { type: 'ARMOR', name: 'Lab shield', weight: 3, defense: 20, material: 'Wood' },
    rule: '@PreAuthorize: KING or KNIGHT',
  },
  { title: 'Delete a person', method: 'DELETE', url: '/api/persons/nobody-here', rule: '@PreAuthorize: KING (a king gets 404: allowed, but nobody to delete)' },
  { title: 'Actuator environment', method: 'GET', url: '/actuator/env', rule: 'URL rule: KING (not exposed, so a king gets 404)' },
  { title: 'Actuator health', method: 'GET', url: '/actuator/health', rule: 'public (permitAll)' },
];

const MODES: Mode[] = ['token', 'anonymous'];

/** Fires requests with and without a token so students see 200, 401, 403 and 404 for themselves. */
export default function SecurityLab() {
  const session = useSession();
  const claims = decodeClaims(session?.token);
  const [results, setResults] = useState<Record<string, Result>>({});
  const [tampered, setTampered] = useState<Result | null>(null);

  async function run(probe: Probe, mode: Mode) {
    let result: Result;
    try {
      const response = await raw(probe.url, {
        method: probe.method,
        body: probe.body,
        anonymous: mode === 'anonymous',
        keepSession: true,
      });
      result = { status: response.status, message: '' };
    } catch (e) {
      const error = e as ApiError;
      result = { status: error.status, message: error.message };
    }
    setResults((current) => ({ ...current, [`${probe.title}|${mode}`]: result }));
  }

  async function tamper() {
    if (!session || !claims) return;
    const [header, , signature] = session.token.split('.');
    const fakePayload = btoa(JSON.stringify({ ...claims, roles: ['KING'] })).replace(/=+$/, '');
    try {
      await raw('/api/persons', {
        anonymous: true,
        keepSession: true,
        headers: { Authorization: `Bearer ${header}.${fakePayload}.${signature}` },
      });
      setTampered({ status: 200, message: 'Accepted?! That should not happen.' });
    } catch (e) {
      const error = e as ApiError;
      setTampered({ status: error.status, message: error.message });
    }
  }

  const css = (status: number) => `code${status >= 200 && status < 300 ? ' good' : ''}${status >= 400 ? ' bad' : ''}`;

  return (
    <div className="two-col">
      <div className="card panel">
        <h2>🔬 Security lab</h2>
        <p className="muted">
          Run each call twice: once <strong>with</strong> your token and once <strong>anonymously</strong>. Log in as
          different users to see the difference between <code>401</code> (who are you?) and <code>403</code> (you may
          not).
        </p>
        <table>
          <thead>
            <tr>
              <th>Call</th>
              <th>Rule on the server</th>
              <th></th>
              <th>Result</th>
            </tr>
          </thead>
          <tbody>
            {PROBES.map((p) => (
              <tr key={p.title}>
                <td>
                  <strong>{p.title}</strong>
                  <br />
                  <code>
                    {p.method} {p.url}
                  </code>
                </td>
                <td className="muted">{p.rule}</td>
                <td className="nowrap">
                  <button type="button" className="small" disabled={!session} onClick={() => void run(p, 'token')}>
                    With token
                  </button>{' '}
                  <button type="button" className="secondary small" onClick={() => void run(p, 'anonymous')}>
                    Anonymous
                  </button>
                </td>
                <td>
                  {MODES.map((mode) => {
                    const r = results[`${p.title}|${mode}`];
                    return (
                      r && (
                        <div key={mode}>
                          <span className={css(r.status)}>{r.status}</span> <span className="muted">{mode}</span>
                          {r.message && <div className="muted small-text">{r.message}</div>}
                        </div>
                      )
                    );
                  })}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card panel">
        <h2>🎫 Your token</h2>
        {claims ? (
          <>
            <p className="muted">
              A JWT is just three Base64 parts. This is the middle one (the claims), decoded in your browser. The server
              trusts it only because of the signature (the third part).
            </p>
            <pre className="printout">{JSON.stringify(claims, null, 2)}</pre>
            <p className="muted">Tamper with it and the server answers 401:</p>
            <button type="button" className="danger small" onClick={() => void tamper()}>
              Send a tampered token
            </button>
            {tampered && (
              <p>
                <span className="code bad">{tampered.status}</span> <span className="muted">{tampered.message}</span>
              </p>
            )}
          </>
        ) : (
          <p className="muted">You are not logged in. Use the Log in button in the header.</p>
        )}
      </div>
    </div>
  );
}
